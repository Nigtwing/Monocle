package com.techno.monocle.service

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log

class BluetoothBackgroundService : Service() {
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    override fun onBind(intent: Intent): IBinder? {
        // Return null since this is not a bound service
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Perform your background tasks here
        // This method is called when the service is started

        handler = Handler()
        runnable = object : Runnable {
            override fun run() {
                // Perform your background tasks here
                Log.d("MyBackgroundService", "Printing every second")

                // Delay the next execution by 1 second
                handler.postDelayed(this, 1000)
            }
        }

        // Start the runnable
        handler.post(runnable)


        // Return START_STICKY to automatically restart the service if it gets terminated
        return START_STICKY
    }

    override fun onDestroy() {
        // Cleanup tasks can be performed here
        super.onDestroy()

        // Stop the runnable when the service is destroyed
        handler.removeCallbacks(runnable)
    }
}