#!/usr/bin/python3
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import json
import time
import matplotlib.pyplot as plt
import datetime
import sys
import numpy as np
import pytz

def initFirebase():
    cred = credentials.Certificate('noisyneighbour-e1234-firebase-adminsdk-123456789.json')
    firebase_admin.initialize_app(cred)
    db = firestore.client()
    return db

def getCollection(db, nameOfCollection, pasthr):
    collref = db.collection(nameOfCollection)
    millisec = time.time() * 1000 - pasthr * 60 * 60 * 1000
    query = collref.where(
        u't', u'>', millisec).order_by(u't')
    results = query.get()

    data = {}

    for doc in results:
        # print(u'{} => {}'.format(doc.id, doc.to_dict()))
        data[doc.id] = doc.to_dict()

    tz = pytz.timezone('Europe/London')

    x = []
    y = []
    m = []
    for i in data:
        a = data[i]
        d = datetime.datetime.fromtimestamp(a["t"]/1000)
        x.append(d.astimezone(tz))
        y.append(a["r"])
        m.append(a["m"])

    filename = nameOfCollection+"__"+time.strftime("%b-%d-%Y_%H-%M-%S", time.gmtime())+'.json'

    with open(filename, 'w') as f:
        json.dump(data, f, indent=4)

    print(data)

    dfrom = datetime.datetime.fromtimestamp(millisec / 1000)
    dto = datetime.datetime.fromtimestamp(time.time())
    t = "A-weighted loudness "+dfrom.strftime("%c")+" - "+dto.strftime("%c")
    plt.title(t)
    plt.subplot(211)
    plt.semilogy(x,y)
    plt.xlabel("Date/time")
    plt.ylabel("RMS, perceptual log scale")
    plt.ylim([8.0/32768.0,0.006])

    plt.subplot(212)
    plt.plot(x,np.square(m))
    plt.xlabel("Date/time")
    plt.ylabel("Squared peak loudness")
    plt.ylim([0,0.1])

    plt.show()


db = initFirebase()
if len(sys.argv) > 1:
    pasthr = float(sys.argv[1])
    getCollection(db, u'audio',pasthr)
else:
    print("Duration plz!")
