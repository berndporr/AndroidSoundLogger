#!/usr/bin/python3
import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import json
import time
import sys

def initFirebase():
    cred = credentials.Certificate('noisyneighbour-765696-firebase-adminsdk-1233446879.json')
    firebase_admin.initialize_app(cred)
    db = firestore.client()
    return db

def getCollection(db, nameOfCollection):
    collref = db.collection(nameOfCollection)
    query = collref.order_by("t")
    results = query.get()

    plays = {}

    for doc in results:
        plays[doc.id] = doc.to_dict()

    filename = nameOfCollection+"__"+time.strftime("%b-%d-%Y_%H-%M-%S", time.gmtime())+'.json'

    with open(filename, 'w') as f:
        json.dump(plays, f, indent=4)

if len(sys.argv) > 1 and sys.argv[1] in "all":
    db = initFirebase()
    getCollection(db, u'audio')
else:
    print("Plz say all")
