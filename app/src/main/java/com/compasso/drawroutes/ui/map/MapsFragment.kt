package com.compasso.drawroutes.ui.map

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.compasso.drawroutes.R
import com.compasso.drawroutes.utils.GoogleMapDTO
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request


class MapsFragment : Fragment(), OnMapReadyCallback {
    private lateinit var myLocation: LatLng
    private lateinit var destination: LatLng
    private lateinit var nameDestination: String
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var bounds: LatLngBounds
    private lateinit var mapFragment: SupportMapFragment


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }


    private fun checkPermissionForMap() {
        val checkSelfPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (checkSelfPermission != PackageManager.PERMISSION_GRANTED) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }


    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                //TODO
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Places.initialize(requireContext(), getString(R.string.google_maps_key))
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireActivity())
        mapFragment = (childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?)!!
        checkPermissionForMap()
        configAutoCompletFragment()
        mapFragment.getMapAsync(this)
        configGetLocationButton()
    }

    private fun configGetLocationButton() {
        val saveBt = view?.findViewById<FloatingActionButton>(R.id.home_button_get_location)
        saveBt?.setOnClickListener { enableMyLocation() }
    }

    private fun configAutoCompletFragment() {
        // Initialize the AutocompleteSupportFragment.
        val autocompleteFragment =
            childFragmentManager.findFragmentById(R.id.autoComplete)
                    as AutocompleteSupportFragment

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS
            )
        )

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                destination = LatLng(place.latLng!!.latitude, place.latLng!!.longitude)
                nameDestination = place.name!!
                enableMyLocation()
                val checkSelfPermission = ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
                if (checkSelfPermission == PackageManager.PERMISSION_GRANTED) {
                    getPlacesToDirection()
                }
            }

            override fun onError(status: Status) {
                Log.i(ContentValues.TAG, "An error occurred: $status")
            }
        })
    }

    private fun getInitialPositionMapa() {
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 16f))
    }

    private fun enableMyLocation() {
        val task = fusedLocationProviderClient.lastLocation
        val checkSelfPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (!::map.isInitialized) return
        if (checkSelfPermission == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            map.uiSettings.isMyLocationButtonEnabled = false
        } else {
            checkPermissionForMap()
        }
        task.addOnSuccessListener {
            if (it != null) {
                myLocation = LatLng(it.latitude, it.longitude)
                getInitialPositionMapa()
            }
        }

    }

    override fun onMapReady(googleMap: GoogleMap?) {
        map = googleMap ?: return
        enableMyLocation()
    }


    private fun getPlacesToDirection(
    ) {
        map.clear()
        map.addMarker(
            MarkerOptions().position(destination).title(nameDestination)
        )
        map.addMarker(
            MarkerOptions().position(myLocation).title("My Location")
        )

        bounds =
            LatLngBounds.Builder().include(myLocation).include(destination)
                .build()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 250))

        val url = getDirectionURL(myLocation, destination)
        GetDirection(url)
    }

    private fun getDirectionURL(origin: LatLng, dest: LatLng): String {
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}&destination=${dest.latitude},${dest.longitude}&sensor=false&mode=driving&key=${
            getString(
                R.string.google_maps_key
            )
        }"
    }

    private inner class GetDirection(val url: String) {
        val getDirections = CoroutineScope(IO).launch {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body!!.string()
            val result = ArrayList<List<LatLng>>()
            try {
                val respObj = Gson().fromJson(data, GoogleMapDTO::class.java)
                val path = ArrayList<LatLng>()

                for (i in 0 until respObj.routes[0].legs[0].steps.size) {
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
                result.add(path)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            withContext(Main) {
                val lineoption = PolylineOptions()
                for (i in result.indices) {
                    lineoption.addAll(result[i])
                    lineoption.width(15f)
                    lineoption.color(Color.MAGENTA)
                    lineoption.geodesic(true)
                }
                map.addPolyline(lineoption)
            }
        }
    }

    fun decodePolyline(encoded: String): List<LatLng> {

        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                b = encoded[index++].toInt() - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            val latLng = LatLng((lat.toDouble() / 1E5), (lng.toDouble() / 1E5))
            poly.add(latLng)
        }
        return poly
    }

}