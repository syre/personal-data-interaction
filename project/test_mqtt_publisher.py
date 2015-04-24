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

command = "loc_update" #loc_update, loc_remove or nudge
mail = "syrelyre@gmail.com"
target_mail = "handiiandii@gmail.com" #for nudge
lat = 55.821
lng = 12.440
topic = mail + ".55.828.12.437" #"handiiandii@gmail.com.55.707.12.536"
msg = """{email:%s, targetEmail:%s, command:%s, lat: %f, lng: %f}""" % (mail, target_mail, command, lat, lng)
#msg = """{email:%s, command:loc_update, lat: %f, lng: %f}""" % (mail, lat, lng)
print("topic = %s\nmsg = %s" % (topic, msg))
client.publish(topic, payload=msg)
