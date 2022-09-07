#!/usr/bin/python3
import matplotlib.pyplot as plt
import scipy
import numpy as np
from scipy import signal
import datetime
import json
import sys
import pytz

if len(sys.argv) < 2:
    print("Filename plz!")
    quit()

f = open(sys.argv[1],"r")
js = f.read()
data = json.loads(js)

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

dfrom = min(x)
dto = max(x)
t = "A-weighted loudness "+dfrom.strftime("%c")+" - "+dto.strftime("%c")
print(t)
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

