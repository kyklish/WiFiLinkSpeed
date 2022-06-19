package com.example.wifilinkspeed

import android.app.Activity
import android.content.res.Resources
import android.util.Log
import android.widget.TextView
import android.widget.Toast

@Suppress("unused")
class Utils {
	companion object {
		/**
		 * Set text in UI thread from any background thread.
		 * @param activity Activity with TextView element.
		 * @param textView TextView element.
		 * @param text Text.
		 */
		fun setTextView(activity: Activity, textView: TextView, text: String?) {
			activity.runOnUiThread {
				// Only the original thread that created a view hierarchy can touch its views.
				// So run on the main (UI) thread.
				textView.text = text
			}
		}

		/**
		 * Convert font size from sp to px.
		 * @param resources System resources used to get scaleDensity.
		 * @param sizeSp Font size in sp units.
		 * @return Return font size in px units.
		 */
		fun spToPx(resources: Resources, sizeSp: Float): Float {
			return sizeSp * resources.displayMetrics.scaledDensity
		}

		fun logd(message: String) {
			if (BuildConfig.DEBUG) Log.d(this::class.java.simpleName, message)
		}

		fun toast(message: String) {
			Toast.makeText(App.appContext, message, Toast.LENGTH_SHORT).show()
		}
	}
}