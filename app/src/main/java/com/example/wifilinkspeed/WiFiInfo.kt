package com.example.wifilinkspeed

import android.app.Activity
import android.content.Context
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build

class WiFiInfo(context: Context) {
	private val wifiManager = context.getSystemService(Activity.WIFI_SERVICE) as WifiManager
	private var i = 0 // FOR DEBUG

	fun linkSpeedStr(context: Context): String {
		if (BuildConfig.DEBUG) {
			val text = arrayOf(
				"0\n",
				"4Mbps Tx\n4Mbps Rx",
				"40Mbps Tx\n4Mbps Rx",
				"40Mbps Tx\n400Mbps Rx",
				"Debug Long String\n"
			)
			val strText = text[i++]
			if (i > text.lastIndex)
				i = 0
			return strText
		} else {
			val strSpeed: String
			// always update [connectionInfo] structure before reading [linkSpeed]
			val wifiInfo = wifiManager.connectionInfo
			if (wifiInfo != null && wifiInfo.linkSpeed != -1) {
				strSpeed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
					context.getString(
						R.string.text_speed_tx_rx,
						wifiInfo.txLinkSpeedMbps,
						wifiInfo.rxLinkSpeedMbps,
						WifiInfo.LINK_SPEED_UNITS
					)
				else
//				strSpeed = "" + wifiInfo.linkSpeed + WifiInfo.LINK_SPEED_UNITS
					context.getString(
						R.string.text_speed,
						wifiInfo.linkSpeed,
						WifiInfo.LINK_SPEED_UNITS
					)
			} else
				strSpeed = context.getString(R.string.text_info)
			return strSpeed
		}
	}
}
