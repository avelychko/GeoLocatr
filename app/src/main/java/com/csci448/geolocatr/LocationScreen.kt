package com.csci448.geolocatr

import android.location.Location
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@Composable
fun LocationScreen(location: Location?, locationAvailable: Boolean,
                   onGetLocation: () -> Unit, address: String,
                   onNotify: (Location) -> Unit) {

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 0f)
    }

    val context = LocalContext.current
    LaunchedEffect(location) {
        if(location != null) {
            // include all points that should be within the bounds of the zoom
            // convex hull
            val bounds = LatLngBounds.Builder()
                .include(LatLng(location.latitude, location.longitude))
                .build()
                    // add padding
            val padding = context.resources
                .getDimensionPixelSize(R.dimen.map_inset_padding)
            // create a camera update to smoothly move the map view
            val cameraUpdate = CameraUpdateFactory.newLatLngBounds(bounds, padding)
            // move our camera!
            cameraPositionState.animate(cameraUpdate)
        }
    }

    val dataStoreManager = remember { DataStoreManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    val dataTrafficState = dataStoreManager
        .dataTrafficFlow
        .collectAsStateWithLifecycle(
            initialValue = false,
            lifecycle = lifecycleOwner.lifecycle
        )
    val dataLocationState = dataStoreManager
        .dataLocationFlow
        .collectAsStateWithLifecycle(
            initialValue = false,
            lifecycle = lifecycleOwner.lifecycle
        )

    val mapUiSettings = MapUiSettings(
        myLocationButtonEnabled = dataLocationState.value
    )
    val mapProperties = MapProperties(
        isTrafficEnabled = dataTrafficState.value,
        isMyLocationEnabled = dataLocationState.value
    )

    Column(modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Latitude / Longitude:")
        Text(text = (location?.latitude ?: "").toString() + " / "
                + (location?.longitude ?: "").toString())
        Text(text = "Address:")
        Text(text = address, textAlign = TextAlign.Center)
        Row() {
            Button(onClick = onGetLocation, enabled = locationAvailable) {
                Text(text = "Get Current Location")
            }
            Button(
                enabled = (location != null),
                onClick = { onNotify(location!!) }
            ) {  Text(text = "Notify Me Later") }
        }

        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Traffic", Modifier.padding(5.dp))
                Switch(checked = dataTrafficState.value,
                    onCheckedChange = {
                        coroutineScope.launch { dataStoreManager.setTrafficData(it) }
                    })
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "My Location", Modifier.padding(5.dp))
                Switch(checked = dataLocationState.value,
                    onCheckedChange = {
                        coroutineScope.launch { dataStoreManager.setLocationData(it) }
                    })
            }
        }

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = mapUiSettings,
            properties = mapProperties
        ) {
            if(location != null) {
                val markerState = MarkerState().apply {
                    position = LatLng(location.latitude, location.longitude)
                }
                Marker(
                    state = markerState,
                    title = address,
                    snippet = "${location.latitude} / ${location.longitude}"
                )
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PreviewLocationScreen() {
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