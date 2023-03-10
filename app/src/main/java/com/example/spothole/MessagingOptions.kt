package com.example.spothole

// Solace PubSub+ Broker Options

// Fill in your Solace Cloud PubSub+ Broker's 'MQTT Host' and 'Password' options.
// This information can be found under:
// https://console.solace.cloud/services/ -> <your-service> -> 'Connect' -> 'MQTT'
const val SOLACE_CLIENT_USER_NAME = "solace-cloud-client"
const val SOLACE_CLIENT_PASSWORD = "qh6cbj197fqdpihbn32kah347n"
const val SOLACE_MQTT_HOST = "ssl://mr-connection-tqf857015d8.messaging.solace.cloud:8883"

// Other options
const val SOLACE_CONNECTION_TIMEOUT = 3
const val SOLACE_CONNECTION_KEEP_ALIVE_INTERVAL = 60
const val SOLACE_CONNECTION_CLEAN_SESSION = true
const val SOLACE_CONNECTION_RECONNECT = true