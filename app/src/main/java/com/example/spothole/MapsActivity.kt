package com.example.spothole

import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.text.isDigitsOnly

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.spothole.databinding.ActivityMapsBinding
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private var coordinates = ArrayList<LatLng>()
    private var coordinates2 = ArrayList<LatLng>()
    private val data_img = ArrayList<ImageData>()
    private val data=ArrayList<PotholeData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val holes = intent.extras?.get("coordinates").toString()
        if (holes != "[]"){
            val strs = holes.split("),")
            for (i in 0..(strs.size - 1)) {
                val str = strs[i].split(',')
                val lat = str[0].slice(11..str[0].lastIndex).toDouble()
                val lon = str[1].slice(0..str[1].lastIndex - 2).toDouble()
                coordinates.add(LatLng(lat, lon))
//            print("lat:$lat")
//            print("lon:$lon")
            }

            println("HOLES:${holes}")
        }
        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        val firestore= Firebase.firestore
        firestore.collection("PotHole").get().addOnSuccessListener{result->
            for (document in result)
            {
                Log.d(TAG, "asdfg $document")


                //var url= document["url"].toString()
                var lt=document["Latitude"].toString()
                var lngt=document["Longitude"].toString()
//                if(lt.isDigitsOnly() || lt.isEmpty() != null) {
//
//
//                    lt = document["Latitude"].toString()
//
//                }
//                if(lngt.isDigitsOnly() || lngt.isEmpty() != null) {
//
//
//                    lngt = document["Longitude"].toString()
//
//                }
                val Pots= PotholeData(lat=lt,longt=lngt)
                data.add(Pots)

            }
            Log.d(TAG, "onCreate: Values are $data")

            val firestore_img= Firebase.firestore

            firestore_img.collection("images").get().addOnSuccessListener{result->
                for (document in result)
                {
                    Log.d(TAG, "abcdefgh $document")


                    //var url= document["url"].toString()
                    var lt=document["Latitute"].toString()
                    var lngt=document["Long"].toString()
                    var url=document["url"].toString()
//                if(lt.isDigitsOnly() || lt.isEmpty() != null) {
//
//
//                    lt = document["Latitude"].toString()
//
//                }
//                if(lngt.isDigitsOnly() || lngt.isEmpty() != null) {
//
//
//                    lngt = document["Longitude"].toString()
//
//                }
                    val image_pots= ImageData(lat=lt,longt=lngt,url=url)
                    data_img.add(image_pots)
                }
                Log.d(TAG, "on image create $data_img")
                Log.d(TAG, "onMapReady with Success: We are chaapo $data and $coordinates2")
                mapReady()
            }

        }
    }




    fun mapReady(){

//        if(data.isNotEmpty()) {
            for (x in data_img) {
                val xlat = x.lat
                val xlong = x.longt
                val ltlng =
                    xlat?.let { xlong?.let { it1 -> LatLng(it.toDouble(), it1.toDouble()) } }
                coordinates2.add(ltlng!!)
                if(x.url!=null) {
                    val imgy: String? = x.url
                    Log.d(TAG, "onIMAGEREADY: Surya and $imgy")
                    mMap.addMarker(MarkerOptions().position(ltlng).title("Image Link").snippet(imgy).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)))
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(ltlng!!))
                }

                for(y in data)
                {
                    if((y.lat!=xlat) && (y.longt!=xlong)){
                        val y_latlng =
                            y.lat?.let { y.longt?.let { it1 -> LatLng(it.toDouble(), it1.toDouble()) } }
                        mMap.addMarker(MarkerOptions().position(y_latlng!!))
                    }


                }
//                mMap.addMarker(MarkerOptions().position(ltlng))

                Log.d(TAG, "onMapReady: printing values of Coordinates2 : $coordinates2")
            }

//        }
        if(coordinates2.isNotEmpty()) {
            val builder = LatLngBounds.Builder()
            for (latLng in coordinates2) {
                builder.include(latLng)
            }
            val bounds = builder.build()
            val padding = 100 // offset from edges of the map in pixels
            val cu = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            mMap.animateCamera(cu)
        }
    }
}



