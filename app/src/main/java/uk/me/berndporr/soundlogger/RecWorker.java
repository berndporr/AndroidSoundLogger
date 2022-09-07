package uk.me.berndporr.soundlogger;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import uk.me.berndporr.iirj.SOSCascade;

public class RecWorker extends Worker {
    public final static String TAG = RecWorker.class.getSimpleName();
    final int SAMPLE_RATE = 48000;
    final int chunkLength = 60; // sec
    final int bufferSize = SAMPLE_RATE * chunkLength;
    final double[][] AweightingSOS = {
            {
                    7.545506848211794848e-01, 0.000000000000000000e+00, 0.000000000000000000e+00, 1.000000000000000000e+00, -4.053225490428303823e-01, 4.107159219064440703e-02
            },
            {
                    1.000000000000000000e+00, -2.000000000000000000e+00, 1.000000000000000000e+00, 1.000000000000000000e+00, -1.893938990943602185e+00, 8.952272888746266588e-01
            },
            {
                    1.000000000000000000e+00, -2.000000000000000000e+00, 1.000000000000000000e+00, 1.000000000000000000e+00, -1.994614459236600190e+00, 9.946217102489287587e-01
            },
    };

    final String RMS_COLLECTION = "audio";

    final OneTimeWorkRequest recWorkRequest =
            new OneTimeWorkRequest.Builder(RecWorker.class)
                    .build();

    public RecWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    private void addToFirestore(String collection, Map<String, Object> data) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        try {
            firestore.collection(collection).add(data);
        } catch (Exception e) {
            Log.e(TAG,"Firebase couldn't save data.",e);
            return;
        }
        Log.d(TAG,"Firebase: data saved: "+collection);
    }

    @NonNull
    @Override
    public Result doWork() {
        final SOSCascade cust = new SOSCascade();
        cust.setup(AweightingSOS);
        short[] audioBuffer = new short[bufferSize];

        @SuppressLint("MissingPermission")
        final AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record can't initialize!");
            try {
                Thread.sleep(1000 * chunkLength);
            } catch (Exception e) {
                Log.e(TAG,"Could not sleep",e);
            }
            WorkManager
                    .getInstance(getApplicationContext())
                    .enqueueUniqueWork(RecWorker.TAG, ExistingWorkPolicy.APPEND,recWorkRequest);
            return Result.success();
        }

        record.startRecording();

        Log.d(TAG, "Start recording");

        long shortsRead = 0;
        int numberOfShort = record.read(audioBuffer, 0, audioBuffer.length);
        record.stop();
        record.release();
        Log.d(TAG, "Finished recording");

        double rms = 0;
        double max = 0;
        int n = 0;
        for (int i = 0; i < numberOfShort; i++) {
            double a = ((double) (audioBuffer[i])) / 32768.0;
            a = Math.abs(a);
            if (a > max) {
                max = a;
            }
            a = cust.filter(a);
            //Log.v(TAG,"value="+a);
            if (i > (SAMPLE_RATE / 20)) {
                rms = rms + a * a;
                n++;
            }
        }
        if (n > 0) {
            rms = rms / (float) n;
            rms = (float) Math.sqrt(rms);
            Log.v(TAG, "RMS = " + rms);
            Log.v(TAG, "max = " + max);
            Map<String, Object> data = new HashMap<>();
            data.put("t", System.currentTimeMillis());
            data.put("r", (float)rms);
            data.put("m", (float)max);
            addToFirestore(RMS_COLLECTION, data);
        } else {
            Log.e(TAG,"No samples from the sound card!");
        }

        WorkManager
                .getInstance(getApplicationContext())
                .enqueueUniqueWork(RecWorker.TAG, ExistingWorkPolicy.APPEND,recWorkRequest);

        return Result.success();
    }
}

