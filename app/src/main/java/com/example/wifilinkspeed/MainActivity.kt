package com.example.wifilinkspeed

import android.app.Activity
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.TextView
import java.util.*


class MainActivity : Activity() {

	private lateinit var textView: TextView
	private lateinit var wifiManager: WifiManager
	private val period: Long = 250

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		// https://developer.android.com/training/scheduling/wakelock
		// Set wake lock in activity_layout.xml [android:keepScreenOn="true"] (which I did)
		// OR set flag in MainActivity.kt
//		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

//		textView = findViewById(R.id.textInfo)
		wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

//		updateDisplayByTimer()
//		updateDisplayByHandler()

//		TODO("Add keep screen on to Workout timer app")
//		TODO("Remove unnecessary flavour builds")

		UpdateDisplay(this,wifiManager,findViewById(R.id.textInfo1),250)
		UpdateDisplay(this,wifiManager,findViewById(R.id.textInfo2),500)
		UpdateDisplay(this,wifiManager,findViewById(R.id.textInfo3),750)
		UpdateDisplay(this,wifiManager,findViewById(R.id.textInfo4),1000)
	}

	override fun onStop() {
		super.onStop()
	}

	override fun onStart() {
		super.onStart()
//		showWifiLinkSpeed()
	}

	override fun onResume() {
		super.onResume()
//		showWifiLinkSpeed()
	}

	private fun showWifiLinkSpeed() {
		// always update [connectionInfo] structure before reading [linkSpeed]
		val wifiInfo = wifiManager.connectionInfo
		if (wifiInfo != null && wifiInfo.linkSpeed != -1) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
				textView.text =
					getString(
						R.string.text_speed_tx_rx,
						wifiInfo.txLinkSpeedMbps,
						wifiInfo.rxLinkSpeedMbps,
						WifiInfo.LINK_SPEED_UNITS
					)
			else
//				textView.text = "" + wifiInfo.linkSpeed + WifiInfo.LINK_SPEED_UNITS
				textView.text =
					getString(
						R.string.text_speed,
						wifiInfo.linkSpeed,
						WifiInfo.LINK_SPEED_UNITS
					)
		} else
			textView.text = getString(R.string.text_info)
	}

	private fun updateDisplayByTimer() {
		Timer().schedule(object : TimerTask() {
			override fun run() {
				runOnUiThread {
					// Only the original thread that created a view hierarchy can touch its views.
					// So run on the main (UI) thread.
					showWifiLinkSpeed()
				}
			}
		}, 0, period)
	}

	private fun updateDisplayByHandler() {
		// A Handler allows you to send and process Message and Runnable objects associated with a thread's MessageQueue.
		// There are two main uses for a Handler:
		// (1) to schedule messages and runnables to be executed at some point in the future;
		// (2) to enqueue an action to be performed on a different thread than your own.
		Handler().postDelayed({
			showWifiLinkSpeed()
		}, period)
	}
}
