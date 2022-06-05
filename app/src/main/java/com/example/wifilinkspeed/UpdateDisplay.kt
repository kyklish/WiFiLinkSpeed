package com.example.wifilinkspeed

import android.app.Activity
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.TextView
import java.util.*

class UpdateDisplay(
	private val activity: Activity,
	private val wifiManager: WifiManager,
	private val textView: TextView,
	private val period: Long
) {

	init {
		updateDisplayByTimer()
	}

	private fun showWifiLinkSpeed() {
		// always update [connectionInfo] structure before reading [linkSpeed]
		val wifiInfo = wifiManager.connectionInfo
		if (wifiInfo != null && wifiInfo.linkSpeed != -1) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
				setTextView(
					App.appContext?.getString(
						R.string.text_speed_tx_rx,
						wifiInfo.txLinkSpeedMbps,
						wifiInfo.rxLinkSpeedMbps,
						WifiInfo.LINK_SPEED_UNITS
					)
				)
			else
//				setTextView( "" + wifiInfo.linkSpeed + WifiInfo.LINK_SPEED_UNITS
				setTextView(
					App.appContext?.getString(
						R.string.text_speed,
						wifiInfo.linkSpeed,
						WifiInfo.LINK_SPEED_UNITS
					)
				)
		} else
			setTextView(App.appContext?.getString(R.string.text_info))
	}

	private fun updateDisplayByTimer() {
		Timer().schedule(object : TimerTask() {
			override fun run() {
				showWifiLinkSpeed()
			}
		}, 0, period)
	}

	private fun setTextView(text: String?) {
		activity.runOnUiThread {
			// Only the original thread that created a view hierarchy can touch its views.
			// So run on the main (UI) thread.
			textView.text = text
		}
	}
}