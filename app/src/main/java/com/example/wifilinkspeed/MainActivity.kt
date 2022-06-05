package com.example.wifilinkspeed

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import com.example.wifilinkspeed.Utils.Companion.setTextView
import java.util.*


class MainActivity : Activity() {

	private lateinit var textView: TextView
	private lateinit var wifiManager: WifiManager
	private val period: Long = 1000 // Android updates wifi info approximately every second

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		// https://developer.android.com/training/scheduling/wakelock
		// Set wake lock in activity_layout.xml [android:keepScreenOn="true"] (which I did)
		// OR set flag in MainActivity.kt
//		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

		textView = findViewById(R.id.textInfo)
		wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

		if (!foregroundServiceRunning()) {
			val serviceIntent = Intent(this, ForegroundService::class.java)
			applicationContext.startForegroundService(serviceIntent)
		}

		updateDisplayByTimer()
//		updateDisplayByHandler()
	}

//	override fun onStop() {
//		super.onStop()
//	}

//	override fun onStart() {
//		super.onStart()
//	}

//	override fun onResume() {
//		super.onResume()
//	}

	private fun updateDisplayByTimer() {
		Timer().schedule(object : TimerTask() {
			override fun run() {
				showWifiLinkSpeed()
			}
		}, 0, period)
	}

	@Suppress("unused")
	private fun updateDisplayByHandler() {
		// A Handler allows you to send and process Message and Runnable objects associated with
		//	a thread's MessageQueue.
		// There are two main uses for a Handler:
		// (1) to schedule messages and runnables to be executed at some point in the future;
		// (2) to enqueue an action to be performed on a different thread than your own.
		Handler().postDelayed({
			showWifiLinkSpeed()
		}, period)
	}

	private fun showWifiLinkSpeed() {
		// always update [connectionInfo] structure before reading [linkSpeed]
		val wifiInfo = wifiManager.connectionInfo
		if (wifiInfo != null && wifiInfo.linkSpeed != -1) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
				setTextView(
					this,
					textView,
					getString(
						R.string.text_speed_tx_rx,
						wifiInfo.txLinkSpeedMbps,
						wifiInfo.rxLinkSpeedMbps,
						WifiInfo.LINK_SPEED_UNITS
					)
				)
			else
//				setTextView(this, textView, "" + wifiInfo.linkSpeed + WifiInfo.LINK_SPEED_UNITS
				setTextView(
					this,
					textView,
					getString(
						R.string.text_speed,
						wifiInfo.linkSpeed,
						WifiInfo.LINK_SPEED_UNITS
					)
				)
		} else
			setTextView(this, textView, getString(R.string.text_info))

	}

	@Suppress("DEPRECATION")
	// getRunningServices() is deprecated since Android 8 Oreo.
	//	We deliberately don't have an API to check whether a service is  running because,
	//	nearly without fail, when you want to do something  like that you end up with race conditions
	//	in your code. (this is what one of android dev team member said)
	// For backwards compatibility, it will still return the caller's own services.
	fun foregroundServiceRunning(): Boolean {
		val activityManager = getSystemService(Context.ACTIVITY_SERVICE)
				as ActivityManager
		for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
			if (ForegroundService::class.java.name == service.service.className) {
				return true
			}
		}
		return false
	}
}
