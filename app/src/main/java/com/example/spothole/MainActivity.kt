package com.example.spothole

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.spothole.mqtt.MqttClientHelper
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.snackbar.Snackbar
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage


class MainActivity : AppCompatActivity() {
    private lateinit var sensorManager: SensorManager
    private var sensor : Sensor ?= null
    private val mqttClient by lazy {
        MqttClientHelper(this)
    }
    private lateinit var currloc : Location
    private lateinit var locationManager: LocationManager
    private val coordinates = ArrayList<LatLng>()
    private val locationPermissionCode = 2
    private val lListener: LocationListener =
        LocationListener { location -> currloc = location }
    private fun getLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        }
        currloc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)!!
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0f, lListener)
    }

    private val mListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.values[0] - 9.8 > 8) {
            Log.d("MY_APP", event.values[1].toString())
                sensorManager.unregisterListener(this)
                getLocation()
                mqttClient.publish("spothole", currloc.latitude.toString() + " , "+ currloc.longitude.toString())
                Log.d("LOC", currloc.latitude.toString() + " , "+ currloc.longitude.toString())
                Handler().postDelayed({
                    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                }, 2000)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Log.d("MY_APP", "$sensor - $accuracy")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setMqttCallBack()
        var listPotholes: Button = findViewById(R.id.list_potholes)
        listPotholes.setOnClickListener{
            val i = (Intent(this, MapsActivity::class.java))
            i.putExtra("coordinates", coordinates)
            println("Coordinates passed to the next Activity")
            startActivity(i)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        var driveMode:Button = findViewById(R.id.drive_mode)
        driveMode.setOnClickListener{
            startActivity(Intent(android.content.Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/dir/?api=1")))
            mqttClient.subscribe("spothole")
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            sensorManager.registerListener(mListener,sensor,SensorManager.SENSOR_DELAY_NORMAL)

        }
    }

    private fun setMqttCallBack() {
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(b: Boolean, s: String) {
                val snackbarMsg = "Connected to host:\n'$SOLACE_MQTT_HOST'."
                Log.w("Debug", snackbarMsg)
                Snackbar.make(findViewById(android.R.id.content), snackbarMsg, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            override fun connectionLost(throwable: Throwable) {
                val snackbarMsg = "Connection to host lost:\n'$SOLACE_MQTT_HOST'"
                Log.w("Debug", snackbarMsg)
                Snackbar.make(findViewById(android.R.id.content), snackbarMsg, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            @Throws(Exception::class)
            override fun messageArrived(topic: String, mqttMessage: MqttMessage) {
                Log.w("Debug", "Message received from host '$SOLACE_MQTT_HOST': $mqttMessage")
                val coord = LatLng(mqttMessage.toString().split(',')[0].toDouble(), mqttMessage.toString().split(',')[1].toDouble());
                coordinates.add(coord)
                println("Coordinates added to the list: $coordinates")

            }

            override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {
                Log.w("Debug", "Message published to host '$SOLACE_MQTT_HOST'")
            }
        })
    }

}
