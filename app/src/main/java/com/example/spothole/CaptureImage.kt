package com.example.spothole

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.*


@Suppress("DEPRECATION")
class CaptureImage : AppCompatActivity() {
    private lateinit var captureButton: Button
    private lateinit var imageView: ImageView

    private lateinit var storage: FirebaseStorage
    private lateinit var firestore: FirebaseFirestore

    private val REQUEST_IMAGE_CAPTURE = 1
    private val CAMERA_PERMISSION_CODE = 2
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

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cap_image)

        captureButton = findViewById(R.id.capture_button)
        imageView = findViewById(R.id.image_view)

        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()

        captureButton.setOnClickListener {
            // Check for camera permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE
                )
            } else {
                captureImage()
            }
        }
    }

    private fun captureImage() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(imageBitmap)

            val baos = ByteArrayOutputStream()
            imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()

            val imageName = UUID.randomUUID().toString() + ".jpg"
            val imageRef = storage.reference.child("images/$imageName")



            val uploadTask = imageRef.putBytes(data)
            uploadTask.addOnCompleteListener { task: Task<*> ->
                if (task.isSuccessful and getLocation()) {
                    imageRef.downloadUrl.addOnCompleteListener { urlTask: Task<*> ->
                        if (urlTask.isSuccessful) {
                            val downloadUrl = urlTask.result.toString()
                            val image = hashMapOf(
                                "url" to downloadUrl,
                                "Latitute" to currloc?.latitude.toString()  ,
                                "Long" to currloc?.longitude.toString()

                            )
                            firestore.collection("images")
                                .add(image)
                                .addOnSuccessListener { documentReference ->
                                    // Show success message
                                }
                                .addOnFailureListener { e ->
                                    // Show error message
                                }
                        }

                    }
                } else {
                    // Show error message
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            captureImage()
        }
    }


}