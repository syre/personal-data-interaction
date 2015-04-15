#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import paho.mqtt.client as paho
import json

def on_connect(client, userdata, flags, rc):
	print("Connected with result code "+str(rc))

def on_disconnect(client, userdata, rc):
	if rc != 0:
		print("Unexpected disconnection.")

def on_message(client, userdata, msg):
	print(msg.payload)

client = paho.Client()
client.on_connect = on_connect
client.on_message = on_message
client.on_disconnect = on_disconnect
client.connect("syrelyre.dk", 1883, 60)
client.subscribe("syrelyre@gmail.com.55.784.12.519",2)
while client.loop() == 0:
    pass

