package com.example.wifilinkspeed

import android.app.Application
import android.content.Context

class App : Application() {
	companion object {
		private var context: Context? = null
		val appContext: Context?
			get() = context
	}

	override fun onCreate() {
		super.onCreate()
		context = applicationContext
	}
}