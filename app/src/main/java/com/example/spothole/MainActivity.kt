package com.example.spothole

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.spothole.mqtt.MqttClientHelper
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.net.URI
import java.time.LocalTime
import java.util.*
import kotlin.collections.ArrayList


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private var PICK_IMAGE_REQUEST:Int?=1
    private var imageUri:Uri?=null
//
    private lateinit var captureButton: Button


    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore

    private val REQUEST_IMAGE_CAPTURE = 1
    private val CAMERA_PERMISSION_CODE = 2
//
    //    private var ResultLauncher
    private lateinit var cameraintent: ActivityResultLauncher<Intent>
    private lateinit var imageView: ImageView

    private lateinit var sensorManager: SensorManager
    private var sensor : Sensor ?= null
    private val mqttClient by lazy {
        MqttClientHelper(this)
    }
    private var currloc : Location? = null
    private lateinit var locationManager: LocationManager
    private val coordinates = ArrayList<LatLng>()
    private val locationPermissionCode = 2
    private val lListener: LocationListener =
        LocationListener { location -> currloc = location }
    private fun getLocation(): Boolean {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if ((ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        }

        currloc = locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER)
        locationManager.requestLocationUpdates(LocationManager.FUSED_PROVIDER, 1000, 0f, lListener)

        return currloc != null
    }

    private val mListener: SensorEventListener = object : SensorEventListener {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onSensorChanged(event: SensorEvent) {
            if (event.values[0] -9.8 > 8) {
            Log.d("MY_APP", event.values[1].toString())
                sensorManager.unregisterListener(this)
                if(getLocation()) {
                    mqttClient.publish(
                        "spothole",
                        currloc?.latitude.toString() + " , " + currloc?.longitude.toString()
                    )
                    Log.d("LOC12345", currloc?.latitude.toString() + " , " + currloc?.longitude.toString())

                    val db = FirebaseFirestore.getInstance()
                    //store code in firebase
                    val user: MutableMap<String, Any> = HashMap()
                    user["Longitude"] = currloc?.longitude.toString()
                    user["Latitude"] = currloc?.latitude.toString()
                    user["Date"] = LocalTime.now()

                    db.collection("PotHole")
                        .add(user)
                        .addOnSuccessListener { documentReference -> Log.d("Message", "DocumentSnapshot added with ID: " + documentReference.id) }
                        .addOnFailureListener { e -> Log.w("Message", "Error adding document",e)}
                }
                Handler().postDelayed({
                    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                }, 2000)


            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            Log.d("MY_APP", "$sensor - $accuracy")
        }
    }
    private fun setMqttCallBack() {
        mqttClient.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(b: Boolean, s: String) {
                val snackbarMsg = "Connected to host"
                Log.w("Debug", snackbarMsg)
                Snackbar.make(findViewById(android.R.id.content), snackbarMsg, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            override fun connectionLost(throwable: Throwable) {
                val snackbarMsg = "Connection to host lost"
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
                Log.d(TAG, "messageArrived: $coordinates")

            }

            override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {
                Log.w("Debug", "Message published to host '$SOLACE_MQTT_HOST'")
            }
        })
    }

    private fun checkForPermissions() {
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    // success
                    Log.d("RPMT", "success")
                } else {
                    Log.d("RPMT", "failure")
                    // failure
                }
            }

        // check permission
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("RPMT", "Coarse location success")
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            Log.d("RPMT", "Fine location success")
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WAKE_LOCK) == PackageManager.PERMISSION_GRANTED) {
            Log.d("RPMT", "Wake lock success")
        } else {
            requestPermissionLauncher.launch(Manifest.permission.WAKE_LOCK)
        }
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED) {
            Log.d("RPMT", "Internet success")
        } else {
            requestPermissionLauncher.launch(Manifest.permission.INTERNET)
        }
        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED) {
            Log.d("RPMT", "Network state success")
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_NETWORK_STATE)
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //imageView=findViewById(R.id.image_view2)
        setContentView(R.layout.activity_main)
        setMqttCallBack()
        checkForPermissions()
//        cameraintent=registerForActivityResult(){result->
//
//        }

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
            if(mqttClient.isConnected()) {
                startActivity(
                    Intent(
                        android.content.Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/dir/?api=1")
                    )
                )
                mqttClient.subscribe("spothole")
                sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
                sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                sensorManager.registerListener(mListener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            }
            else {
                Snackbar.make(findViewById(android.R.id.content), "Solace Client not connected", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }

        }
        cameraintent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result->
            if(result.resultCode== RESULT_OK)
            {
                val data=result.data!!.getStringExtra("data")
                Toast.makeText(this,data,Toast.LENGTH_SHORT).show()
            }
        }
        captureButton= findViewById(R.id.upload_img)

        captureButton.setOnClickListener(){
            val intent= Intent(this,CaptureImage::class.java)
            intent.putExtra("coordinates", coordinates)
            startActivity(intent)
           //cameraintent.launch(Intent("android.media.action.IMAGE_CAPTURE"))
        }
    }
}
