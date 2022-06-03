package com.example.wifilinkspeed

import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

class Utils {
	companion object {
		/**
		 * Provides a String representation of the given time
		 * @param seconds total amount of seconds
		 * @return String in "HH:mm:ss" format
		 */
		fun getTimeFromSeconds(seconds: Long): String {
			val timeZone = TimeZone.getTimeZone("UTC")
			val dateFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
			dateFormatter.timeZone = timeZone
			return dateFormatter.format(Date(seconds * 1000))
		}

		fun logd(message: String) {
			if (BuildConfig.DEBUG) Log.d(this::class.java.simpleName, message)
		}

		fun toast(message: String) {
			Toast.makeText(App.appContext, message, Toast.LENGTH_SHORT).show()
		}
	}
}