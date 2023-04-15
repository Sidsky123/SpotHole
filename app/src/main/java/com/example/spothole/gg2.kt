//package com.example.spothole
//
//import android.graphics.Bitmap
//import com.google.android.gms.maps.CameraUpdateFactory
//import com.google.android.gms.maps.GoogleMap
//import com.google.android.gms.maps.model.BitmapDescriptorFactory
//import com.google.android.gms.maps.model.LatLng
//import com.google.android.gms.maps.model.Marker
//import com.google.android.gms.maps.model.MarkerOptions
//
//class gg2 {
//    var mMap: GoogleMap? = null
//    var marker: Marker? = null
//    var latLng: LatLng =
//        LatLng(lat.toDouble(), /* !!! Hit visitElement for element type: class org.jetbrains.kotlin.nj2k.tree.JKErrorExpression !!! */. toDouble ())
//    var options = MarkerOptions().position(latLng)
//    var bitmap = createUserBitmap()
//    private fun createUserBitmap(): Bitmap? {
//        return null
//    }
//
//    init {
//        options.title("Ketan Ramani")
//        options.icon(BitmapDescriptorFactory.fromBitmap(bitmap!!))
//        options.anchor(0.5f, 0.907f)
//        marker = mMap!!.addMarker(options)
//        mMap!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
//        mMap!!.animateCamera(CameraUpdateFactory.zoomTo(15f), 2000, null)
//    }
//}