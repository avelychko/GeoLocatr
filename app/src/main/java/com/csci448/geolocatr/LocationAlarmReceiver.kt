package com.csci448.geolocatr

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location

class LocationAlarmReceiver : BroadcastReceiver() {
    var lastLocation: Location? = null
    fun scheduleAlarm(activity: Activity) {
        // Part 1.II
    }
    override fun onReceive(context: Context, intent: Intent) {
        // Part 1.III
    }
}