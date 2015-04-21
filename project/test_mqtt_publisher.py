#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import paho.mqtt.client as paho
import json

def on_connect(client, userdata, flags, rc):
	print("Connected with result code "+str(rc))
	print(client.publish("amq.topic", "hello"))

def on_disconnect(client, userdata, flags, rc):
	if rc != 0:
		print("Unexpected disconnection.")
def on_message(client, userdata, msg):
	print(msg.payload)

client = paho.Client()
client.on_connect = on_connect
client.on_message = on_message
client.on_disconnect = on_disconnect
client.connect("syrelyre.dk", 1883, 60)
<<<<<<< HEAD
mail = "syrelyre@gmail.com"
lat = 55.83038
lng = 12.42859
string_lat = str(lat)
string_lng = str(lng)
topic = mail + "."+string_lat[:6]+"."+string_lng[:6] #"handiiandii@gmail.com.55.707.12.536" 
=======
mail = "handiiandii@gmail.com"
lat = 55.705
lng = 12.535
topic = mail + ".55.71.12.54" #"handiiandii@gmail.com.55.707.12.536"
>>>>>>> a0e27328cfb188d5c33467c7ba5f3eb4f8ff1e83
msg = """{email:%s, command:loc_update, lat: %f, lng: %f}""" % (mail, lat, lng)
print("topic = %s\nmsg = %s" % (topic, msg))
#client.publish(topic, payload=msg)
