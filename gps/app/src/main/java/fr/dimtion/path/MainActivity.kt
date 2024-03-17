package fr.dimtion.path

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class MainActivity : AppCompatActivity() {

    private val ACCESS_FINE_LOCATION_REQUEST: Int = 0
    private val REQUEST_CHECK_SETTINGS: Int = 10

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for (location in locationResult.locations) {
                    Log.i("loc fuse", location.toString())
                }
            }
        }
        requestPermissions()
    }

    private fun createLocationRequest() {
        val locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
        if (locationRequest == null) {
            Log.e("loc", " no locationRequest")
            return
        }
        this.locationRequest = locationRequest
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->

            initLocationRequests()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(
                        this@MainActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e("loc ", sendEx.toString())
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun initLocationRequests() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CHECK_SETTINGS -> {
                Log.i("activityResult", "REQUEST_CHECK_SETTINGS, $requestCode, $data")
                initLocationClient()
                createLocationRequest()
            }
        }
    }

    private fun initLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        updateLocation(null)
    }

    @SuppressLint("MissingPermission")
    fun updateLocation(view: View?) {
        val lastLocation = fusedLocationClient.lastLocation

        lastLocation.addOnSuccessListener { location: Location? ->
            Log.i("user loc", location.toString())
        }
        lastLocation.addOnFailureListener { exception ->
            Log.e("user loc", exception.toString())
        }
    }

    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                Log.i("perm", "Show permission")
            } else {
            }
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                ACCESS_FINE_LOCATION_REQUEST
            )
        } else {
            Log.i("perm", "Loc permission already granted")
            initLocationClient()
            createLocationRequest()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            ACCESS_FINE_LOCATION_REQUEST -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.i("perm", "Location permission granted")
                    initLocationClient()
                    createLocationRequest()
                } else {
                    Log.e("perm", "Location permission denied")
                }
                return
            }
            else -> {
                Log.e("perm", "Unknown permission request code $ACCESS_FINE_LOCATION_REQUEST")
            }
        }
    }
}
