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
mail = "syrelyre@gmail.com"
lat = 55.83038
lng = 12.42859
topic = mail + ".55.83.12.428" #"handiiandii@gmail.com.55.707.12.536"
msg = """{email:%s, command:loc_update, lat: %f, lng: %f}""" % (mail, lat, lng)
#msg = """{email:handiiandii@gmail.com, command:loc_remove}"""
print("topic = %s\nmsg = %s" % (topic, msg))
client.publish(topic, payload=msg)
