package www.iesmurgi.mymap

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color.blue
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create

class MainActivity : AppCompatActivity(), OnMapReadyCallback, OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener {

    private lateinit var map: GoogleMap
    private lateinit var btnCalculate: Button
    var poly: Polyline? = null
    private var start: String = ""
    private var end: String = ""

    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnCalculate = findViewById(R.id.btnCalculateRoute)
        btnCalculate.setOnClickListener {
            start = ""
            end = ""
            poly?.remove()
            poly = null
            Toast.makeText(this,"Selecciona punto de origen y final",Toast.LENGTH_SHORT).show()
            if (::map.isInitialized) {
                map.setOnMapClickListener {
                    if (start.isEmpty()) {
                        //"23423424,234234234"
                        start = "${it.longitude},${it.latitude}"
                    } else if (end.isEmpty()) {
                        end = "${it.longitude},${it.latitude}"
                        createRoute()
                    }
                }
            }
        }
        createFragment()
    }

    private fun createRoute() {
        CoroutineScope(Dispatchers.IO).launch {
            val call = getRetrofit().create(ApiService::class.java)
                .getRoute("5b3ce3597851110001cf6248e17ff4e3003c4e4eb9ba8de821591d8d", start, end)
            if (call.isSuccessful) {
                drawRoute(call.body())

            } else {
                Log.i("dioni","no")
            }
        }
    }

    private fun drawRoute(routeResponse: RouteResponse?) {
        val polylineOptions = PolylineOptions()
        routeResponse?.features?.first()?.geometry?.coordinates?.forEach {
            //Longitud y latitud
            polylineOptions.add(LatLng(it[1],it[0]))
        }
        runOnUiThread {
            poly = map.addPolyline(polylineOptions)
        }
    }

    private fun getRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.openrouteservice.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun createFragment() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        //createMarker()
        //createPolylines()
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMyLocationClickListener(this)
        enableLocation()

    }

    /*private fun createPolylines() {
        val polylineOptions = PolylineOptions()
            .add(LatLng(40.419173113350965, -3.705976009368897))
            .add(LatLng(40.4150807746539, -3.706072568893432))
            .add(LatLng(40.41517062907432, -3.7012016773223873))
            .add(LatLng(40.41713105928677, -3.7037122249603267))
            .add(LatLng(40.41926296230622, -3.701287508010864))
            .add(LatLng(40.419173113350965, -3.7048280239105225))
            .width(15f)
            .color(ContextCompat.getColor(this, R.color.kotlin))

        val polyline = map.addPolyline(polylineOptions)

        polyline.isClickable = true

        map.setOnPolylineClickListener { polyline -> changeColor(polyline) }

        /*val pattern = listOf(
            Dot(),Gap(10f),Dash(50f),Gap(10f)
        )

        polyline.pattern = pattern*/


        //polyline.startCap = RoundCap()
        //polyline.endCap = CustomCap(BitmapDescriptorFactory.fromResource(poner imagen en bitmap aqui))
    }*/

    private fun changeColor(polyline: Polyline) {
        when ((0..3).random()) {
            0 -> polyline.color = ContextCompat.getColor(this, R.color.red)
            1 -> polyline.color = ContextCompat.getColor(this, R.color.yellow)
            2 -> polyline.color = ContextCompat.getColor(this, R.color.green)
            3 -> polyline.color = ContextCompat.getColor(this, R.color.blue)
        }
    }

    private fun createMarker() {
        val coordinates = LatLng(28.043893, -16.539329)
        val marker = MarkerOptions().position(coordinates).title("Mi playa favorita!!")
        map.addMarker(marker)
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(coordinates, 18f), 4000, null
        )
    }

    private fun isLocationPermissionGranted() = ContextCompat.checkSelfPermission(
        this,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun enableLocation() {
        if (!::map.isInitialized) return
        if (isLocationPermissionGranted()) {
            map.isMyLocationEnabled = true
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            Toast.makeText(this, "Ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION
            )
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_LOCATION -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                map.isMyLocationEnabled = true
            } else {
                Toast.makeText(
                    this,
                    "Para activar la localizacion ve a ajustes y acepta los permisos",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {}
        }

    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        if (!::map.isInitialized) return //por si no ha dado tiempo para que se haga el mapa
        if (!isLocationPermissionGranted()) {
            map.isMyLocationEnabled = false
            Toast.makeText(
                this,
                "Para activar la localizacion ve a ajustes y acepta los permisos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "Boton pulsado", Toast.LENGTH_SHORT).show()
        return false //sio aqui retornamos false nos llevara a donde este nuestra localizacion, sino no nos llevará
    }

    override fun onMyLocationClick(p0: Location) {
        Toast.makeText(this, "Estas en ${p0.latitude}, ${p0.longitude}", Toast.LENGTH_SHORT).show()
    }
}