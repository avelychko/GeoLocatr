package com.csci448.geolocatr

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import kotlinx.coroutines.flow.*
import java.io.IOException

class LocationUtility(context: Context) {

    companion object {
        private const val LOG_TAG = "448.LocationUtility"
    }

    //Part	2.II.A	– Add	State
    private val mCurrentLocationStateFlow: MutableStateFlow<Location?>
            = MutableStateFlow(null)
    val currentLocationStateFlow: StateFlow<Location?>
        get() = mCurrentLocationStateFlow.asStateFlow()

    private val mCurrentAddressStateFlow: MutableStateFlow<String>
            = MutableStateFlow("")
    val currentAddressStateFlow: StateFlow<String>
        get() = mCurrentAddressStateFlow.asStateFlow()

    //Part	2.II.C	– Get	the	Location
    private val locationRequest = LocationRequest
        .Builder(Priority.PRIORITY_HIGH_ACCURACY, 0L)
        .setMaxUpdates(1)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            mCurrentLocationStateFlow.value = locationResult.lastLocation
        }
    }

    private val fusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    //Step	4 – Good	UI/UX:	Ensure	Location	is	Available
    private val mIsLocationAvailableStateFlow: MutableStateFlow<Boolean>
            = MutableStateFlow(false)
    val isLocationAvailableStateFlow: StateFlow<Boolean>
        get() = mIsLocationAvailableStateFlow.asStateFlow()

    fun verifyLocationSettingsStates(states: LocationSettingsStates?) {
        mIsLocationAvailableStateFlow.update { states?.isLocationUsable ?: false }
    }

    fun checkIfLocationCanBeRetrieved(
        activity: Activity,
        locationLauncher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(activity)
        client.checkLocationSettings(builder.build()).apply {
            addOnSuccessListener { response ->
                verifyLocationSettingsStates(response.locationSettingsStates)
            }
            addOnFailureListener { exc ->
                mIsLocationAvailableStateFlow.update { false }
                if (exc is ResolvableApiException) {
                    locationLauncher
                        .launch(IntentSenderRequest.Builder(exc.resolution).build())
                }
            }
        }
    }

    //Step	3	– Get	the	Address
    private val geocoder = Geocoder(context)

    suspend fun getAddress(location: Location?) {
        val addressTextBuilder = StringBuilder()
        if (location != null) {
            try {
                val addresses = geocoder.getFromLocation(location.latitude,
                    location.longitude,
                    1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    for (i in 0..address.maxAddressLineIndex) {
                        if (i > 0) {
                            addressTextBuilder.append("\n")
                        }
                        addressTextBuilder.append( address.getAddressLine(i) )
                    }
                }
            } catch (e: IOException) {
                Log.e(LOG_TAG, "Error getting address", e)
            }
        }
        mCurrentAddressStateFlow.update { addressTextBuilder.toString() }
    }

    //Part	2.II.B	– Ask	for	Permission
    fun checkPermissionAndGetLocation(activity: Activity,
                                      permissionLauncher: ActivityResultLauncher<Array<String>>) {

        // check if permissions are granted
        if (activity.checkSelfPermission(ACCESS_FINE_LOCATION) == PERMISSION_GRANTED
            || activity.checkSelfPermission(ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED
        ) {
            // Section 1
            // permission has been granted to do what we need
            Log.d(LOG_TAG, "permission granted")
            fusedLocationProviderClient
                .requestLocationUpdates(locationRequest,
                    locationCallback,
                    Looper.getMainLooper())

        } else {
            // permission is currently not granted
            // check if we should ask for permission or not
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, ACCESS_FINE_LOCATION)
                || ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    ACCESS_COARSE_LOCATION
                )
            ) {
                // Section 2
                // user already said no, don't ask again
                Log.d(LOG_TAG, "permission denied")
                Toast.makeText(activity, "We must access your location to plot where you are",
                    Toast.LENGTH_SHORT).show()
            } else {
                // Section 3
                // user hasn ’ t previously declined, ask them
                Log.d(LOG_TAG, "asking for permission")
                // Launch permissionLauncher for ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION
                permissionLauncher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
            }
        }
    }

    //Part	2.IV	– Be	a	Good	Programmer
    fun removeLocationRequest() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    //Lab 12b Part 2
    fun setStartingLocation(location: Location?) {
        mCurrentLocationStateFlow.value = location
    }
}