package com.csci448.geolocatr

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.location.Location
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.startActivity
import java.util.*

class LocationAlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val LOG_TAG = "448.LocationAlarmReceiver"
        private const val ALARM_ACTION = "448_ALARM_ACTION"
        private const val EXTRA_LATITUDE = "latitude"
        private const val EXTRA_LONGITUDE = "longitude"
        private fun createIntent(context: Context, location: Location?): Intent {
            val intent = Intent(context, LocationAlarmReceiver::class.java).apply {
                action = ALARM_ACTION
                putExtra(EXTRA_LATITUDE, location?.latitude ?: 0.0)
                putExtra(EXTRA_LONGITUDE, location?.longitude ?: 0.0)
            }
            return intent
        }
    }
    var lastLocation: Location? = null
    private fun scheduleAlarm(activity: Activity) {
        // Part 1.II
        val alarmManager = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = createIntent(activity, lastLocation)

        val pendingIntent = PendingIntent.getBroadcast(
            activity,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val alarmDelayInSeconds = 10
        val alarmTimeInUTC = System.currentTimeMillis() + alarmDelayInSeconds * 1_000L
        Log.d(LOG_TAG, "Setting alarm for ${
            SimpleDateFormat("MM/dd/yyyy HH:mm:ss",
            Locale.US).format(Date(alarmTimeInUTC))}")

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(LOG_TAG, "running on Version S or newer, checking if can schedule exact alarms")
            if (alarmManager.canScheduleExactAlarms()) {
                Log.d(LOG_TAG, "can schedule exact alarms")
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                    alarmTimeInUTC,
                    pendingIntent)
            } else {
                Log.d(LOG_TAG, "can’t schedule exact alarms, launching intent to bring up settings")
                val settingsIntent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(activity, settingsIntent, null)
            }
        } else {
            Log.d(LOG_TAG, "running on Version R or older, can set alarm directly")
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                alarmTimeInUTC,
                pendingIntent)
        }
    }
    override fun onReceive(context: Context, intent: Intent) {
        // Part 1.III
        Log.d(LOG_TAG, "received alarm for action ${intent.action}")
        if (intent.action == ALARM_ACTION) {
            val latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
            val longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
            Log.d(LOG_TAG, "received our intent with $latitude / $longitude")
        }

        if (ActivityCompat
                .checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "have permission to post notifications")
            val notificationManager = NotificationManagerCompat.from(context)

            val channelID = "myMagicId"
            val channelName = "ChannelName"
            val channelDesc = "This channel is for..."
            val notificationTitle = "You Are Here!"
            val lat = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0);
            val long = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0);
            val notificationText = "You are at $lat/$long."

            val channel =
                NotificationChannel(
                    channelID,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = channelDesc
                }
            notificationManager.createNotificationChannel(channel)

            val startingLocation = Location("").apply {
                latitude = lat
                longitude = long
            }
            val deepLinkPendingIntent = MainActivity
                .createPendingIntent(context, startingLocation)

            val notification = NotificationCompat.Builder(context, channelID)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setContentIntent(deepLinkPendingIntent)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(0, notification)
        }
    }

    fun checkPermissionAndScheduleAlarm(
        activity: Activity,
        permissionLauncher: ActivityResultLauncher<Array<String>>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Log.d(LOG_TAG, "running on Version Tiramisu or newer, need permission")
            if (activity.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
                Log.d(LOG_TAG, "have notification permission")
                scheduleAlarm(activity)
            } else {
                if (ActivityCompat
                        .shouldShowRequestPermissionRationale(activity,
                            android.Manifest.permission.POST_NOTIFICATIONS)) {
                        Log.d(LOG_TAG, "previously denied notification permission")
                // display toast with reason
                } else {
                    Log.d(LOG_TAG, "request notification permission")
                    permissionLauncher.launch(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS))
                }
            }
        } else {
            Log.d(LOG_TAG, "running on Version S or older, post away")
            scheduleAlarm(activity)
        }
    }
}