package com.csci448.geolocatr

import android.location.Location
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.csci448.geolocatr.ui.theme.GeoLocatrTheme
import com.google.android.gms.location.LocationSettingsStates

class MainActivity : ComponentActivity() {
    private	lateinit var locationUtility: LocationUtility
    private	lateinit var locationPermissionLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var locationLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val locationAlarmReceiver = LocationAlarmReceiver()
    private	lateinit var notificationPermissionLauncher: ActivityResultLauncher<Array<String>>

    companion object {
        private const val ROUTE_LOCATION = "location"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        locationUtility = LocationUtility(this@MainActivity)
        locationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                // process if permissions were granted
                locationUtility.checkPermissionAndGetLocation(this@MainActivity, locationPermissionLauncher)
            }

        locationLauncher = registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.let { data ->
                    val states = LocationSettingsStates.fromIntent(data)
                    locationUtility.verifyLocationSettingsStates(states)
                }
            }
        }

        notificationPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                // process if permissions were granted
                locationAlarmReceiver.checkPermissionAndScheduleAlarm(this@MainActivity, notificationPermissionLauncher)
            }

        super.onCreate(savedInstanceState)
        setContent {
            val locationState = locationUtility
                .currentLocationStateFlow
                .collectAsStateWithLifecycle(lifecycle = this@MainActivity.lifecycle)
            val addressState = locationUtility
                .currentAddressStateFlow
                .collectAsStateWithLifecycle(lifecycle = this@MainActivity.lifecycle)
            val isLocationAvailableState = locationUtility
                .isLocationAvailableStateFlow
                .collectAsStateWithLifecycle(lifecycle = this@MainActivity.lifecycle)

            LaunchedEffect(locationState.value) {
                locationUtility.getAddress(locationState.value)
            }

            GeoLocatrTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navHostController = rememberNavController()
                    NavHost(
                        navController = navHostController,
                        startDestination = ROUTE_LOCATION
                    ) {
                        composable(
                            route = ROUTE_LOCATION
                        ) {
                            LocationScreen(
                                location = locationState.value,
                                locationAvailable = isLocationAvailableState.value,
                                onGetLocation = {
                                    locationUtility.checkPermissionAndGetLocation(
                                        this@MainActivity,
                                        locationPermissionLauncher
                                    )
                                },
                                address = addressState.value,
                                onNotify = { lastLocation ->
                                    locationAlarmReceiver.lastLocation = lastLocation
                                    locationAlarmReceiver.checkPermissionAndScheduleAlarm(
                                        this@MainActivity, notificationPermissionLauncher
                                    )
                                })
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        locationUtility.removeLocationRequest()
    }

    override fun onStart() {
        super.onStart()
        locationUtility.checkIfLocationCanBeRetrieved(this, locationLauncher)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val locationState = remember { mutableStateOf<Location?>(null) }
    val addressState = remember { mutableStateOf("") }
    LocationScreen(
        location = locationState.value,
        locationAvailable = true,
        onGetLocation = {
            locationState.value = Location("").apply {
                latitude = 1.35
                longitude = 103.87
            }
            addressState.value = "Singapore"
        },
        address = addressState.value
    ) {}
}