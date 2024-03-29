package io.github.kyklish.wifilinkspeed

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import io.github.kyklish.wifilinkspeed.Utils.Companion.setTextView


class MainActivity : Activity() {
	companion object {
		const val GET_PERMISSION_OVERLAY_REQUEST = 0
	}

	private var isColdStart = false

	private lateinit var textView: TextView
	private lateinit var buttonView: Button
	private lateinit var checkboxView: CheckBox

	private lateinit var sharedPref: SharedPreferences

	private var foregroundService: ForegroundService? = null

	private val connection = object : ServiceConnection {
		override fun onServiceConnected(name: ComponentName, service: IBinder) {
			// the service is connected, we can get a reference to it
			foregroundService = (service as ForegroundService.MyBinder).service
			foregroundService?.registerCallbackForResults(::setTextInfoFromService)
		}

		override fun onServiceDisconnected(name: ComponentName) {
			foregroundService = null
		}
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		// https://developer.android.com/training/scheduling/wakelock
		// Set wake lock in activity_layout.xml [android:keepScreenOn="true"] (which I did)
		// OR set flag in MainActivity.kt
//		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

		// onCreate() executes only during "Cold Start"
		isColdStart = true
		textView = findViewById(R.id.textInfo)
		setDefaultInfoText()
		buttonView = findViewById(R.id.buttonService)
		setButtonServiceText()
		checkboxView = findViewById(R.id.checkBoxOverlay)
		// [left] ?: [right] - elvis operator﻿, return [right] if [left] is null
		sharedPref = this.getPreferences(Context.MODE_PRIVATE)
		val savedCheckBox = sharedPref.getInt(getString(R.string.checkbox_overlay), 1)
		checkboxView.isChecked = savedCheckBox.toBoolean()
	}

/*	Official Android documentation.

	If you need to interact with the service only while your activity is visible, you should bind
	during onStart() and unbind during onStop().

	If you want your activity to receive responses even while it is stopped in the background,
	then you can bind during onCreate() and unbind during onDestroy().

	You don't usually bind and unbind during your activity's onResume() and onPause(), because these
	callbacks occur at every lifecycle transition and you should keep the processing that occurs
	at these transitions to a minimum. Also, if multiple activities in your application bind to
	the same service and there is a transition between two of those activities, the service may be
	destroyed and recreated as the current activity unbinds (during pause) before the next one binds
	(during resume).

	When a service is unbound from all clients, the Android system destroys it (unless it was also
	started with a startService() call). As such, you don't have to manage the lifecycle of your
	service if it's purely a bound service — the Android system manages it for you based on whether
	it is bound to any clients.

	However, if you choose to implement the onStartCommand() callback method, then you must explicitly
	stop the service, because the service is now considered to be started. In this case, the service
	runs until the service stops itself with stopSelf() or another component calls stopService(),
	regardless of whether it is bound to any clients. */

	override fun onStart() {
		super.onStart()
		// "Cold Start" - always start service.
		// "Hot Start" - do not start service, just bind to it if service is running.
		// It's prevent this scenario: if we stop service, switch to another app, and switch back,
		// our app will start service, but we stopped it just moment ago, this behavior is annoying.
//		if (isColdStart) Utils.toast("Cold Start") else Utils.toast("Hot Start")
		if (isColdStart) {
			isColdStart = false
			startForegroundServiceAndBind(buttonView)
		} else
			if (foregroundServiceRunning())
				startForegroundServiceAndBind(buttonView)
	}

	override fun onStop() {
		unbindForegroundService()
		super.onStop()
	}

	@Suppress("DEPRECATION")
	// getRunningServices() is deprecated since Android 8 Oreo.
	// We deliberately don't have an API to check whether a service is  running because,
	// nearly without fail, when you want to do something  like that you end up with race conditions
	// in your code. (this is what one of android dev team member said).
	// For backwards compatibility, it will still return the caller's own services.
	private fun foregroundServiceRunning(): Boolean {
		val activityManager = getSystemService(Context.ACTIVITY_SERVICE)
				as ActivityManager
		for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
			if (ForegroundService::class.java.name == service.service.className) {
				return true
			}
		}
		return false
	}

	/*
		As discussed in the Services document, you can create a service that is both started and bound.
		That is, you can start a service by calling startService(), which allows the service to run
		indefinitely, and you can also allow a client to bind to the service by calling bindService().

		If you do allow your service to be started and bound, then when the service has been started,
		the system does not destroy the service when all clients unbind. Instead, you must explicitly
		stop the service by calling stopSelf() or stopService().

		Application components (activities, services, and content providers) can bind to a service
		by calling bindService(). The Android system then calls the service's onBind() method,
		which returns an IBinder for interacting with the service.

		You can connect multiple clients to a service simultaneously. However, the system caches
		the IBinder service communication channel. In other words, the system calls the service's
		onBind() method to generate the IBinder only when the first client binds. The system then
		delivers that same IBinder to all additional clients that bind to that same service,
		without calling onBind() again.

		When the last client unbinds from the service, the system destroys the service,
		unless the service was also started by startService() or startForegroundService().

		The service and client must be in the same application so that the client can cast
		the returned object and properly call its APIs. The service and client must also be in
		the same process, because this technique does not perform any marshaling across processes.

		If your client is still bound to a service when your app destroys the client, destruction
		causes the client to unbind.
	*/
	private fun startForegroundServiceAndBind(buttonView: Button? = null) {
		if (!canDrawOverlays()) {
			requestOverlayDisplayPermission()
			return
		}

		if (!foregroundServiceRunning()) {
			buttonView?.text = getString(R.string.button_service_stop)
			// startForegroundService() will trigger onStartCommand() in service.
			val serviceIntent = Intent(this, ForegroundService::class.java)
				.putExtra(
					getString(R.string.service_extra_param_overlay),
					checkboxView.isChecked
				)
			applicationContext.startForegroundService(serviceIntent)
		}

		// BIND_AUTO_CREATE automatically create the service.
		// bindService() will trigger onBind() in service and not trigger onStartCommand() in service.
		if (foregroundService == null) {
			// bind the service, and start it if needed
			bindService(Intent(this, ForegroundService::class.java), connection, BIND_AUTO_CREATE)
		}
	}

	private fun stopForegroundServiceAndUnbind(buttonView: Button? = null) {
		if (foregroundServiceRunning()) {
			buttonView?.text = getString(R.string.button_service_start)
			setDefaultInfoText()
			unbindForegroundService()
			stopService(Intent(this, ForegroundService::class.java))
		}
	}

	private fun unbindForegroundService() {
		// only executed when not-null
		foregroundService?.let {
			// disconnect from the service, and nullify the reference
			unbindService(connection)
			foregroundService = null
		}
	}

	private fun setDefaultInfoText() {
		textView.text = getString(R.string.text_info)
	}

	private fun setButtonServiceText() {
		if (foregroundServiceRunning()) {
			buttonView.text = getString(R.string.button_service_stop)
		} else {
			buttonView.text = getString(R.string.button_service_start)
		}
	}

	private fun setTextInfoFromService(text: String) {
		setTextView(this, textView, text)
	}

	@SuppressLint("ObsoleteSdkInt")
	private fun canDrawOverlays(): Boolean {
		// Android Version is lesser than Marshmallow (API 23)
		// doesn't need 'Display over other apps' permission enabling.
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
			return true
		return Settings.canDrawOverlays(this)
	}

	private fun requestOverlayDisplayPermission() {
		val dialog = AlertDialog.Builder(this)
			// This dialog can be closed, just by taping outside the dialog-box
			.setTitle(getString(R.string.alert_dialog_title))
			.setMessage(getString(R.string.alert_dialog_message))
			.setPositiveButton(getString(R.string.alert_dialog_positive_button)) { _, _ ->
				val intent =
					Intent(
						Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
						Uri.parse("package:$packageName")
					)
				startActivityForResult(intent, GET_PERMISSION_OVERLAY_REQUEST)
			}
			.create()
		dialog.show()
	}

	@Suppress("UNUSED_PARAMETER")
	fun openNotificationSettings(button: View) {
		// NOTIFICATION SETTINGS
		val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
			.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)

		// ALL IS CORRECT BUT NOT WORK :(
		// CHANNEL NOTIFICATION SETTINGS
//		val settingsIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
//			.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//			.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
//			.putExtra(Settings.EXTRA_CHANNEL_ID, R.string.notification_channel_id)

		startActivity(settingsIntent)
	}

	fun toggleService(button: View) {
		if (foregroundServiceRunning()) {
			stopForegroundServiceAndUnbind(button as Button)
		} else {
			startForegroundServiceAndBind(button as Button)
		}
	}

	fun toggleOverlay(checkbox: View) {
		val isChecked = (checkbox as CheckBox).isChecked

		if (isChecked) {
			foregroundService?.createOverlay()
			foregroundService?.updateUI(true)
		} else {
			foregroundService?.destroyOverlay()
		}

		with (sharedPref.edit()) {
			putInt(getString(R.string.checkbox_overlay), isChecked.toInt())
			apply()
		}
	}

	private fun Boolean.toInt() = if (this) 1 else 0
	private fun Int.toBoolean() = this != 0
}
// TODO: on permission request MainActivity leaks window. We need dialog.dismiss() in onPause() activity:
//  android - Activity has leaked window that was originally added - Stack Overflow.rar
// TODO: proper request permissions from official doc:
//  android - Request app permissions  _  Android Developers.rar
// TODO: stop updateUI() on CONNECTIVITY_CHANGE broadcast message.
// TODO: create checkbox to "enable overlay" window and save it to sharedPrefs.
// TODO: ask overlay permission only when "enable overlay" setting is checked and service is starting.
//  If user not grant permission, uncheck "enable overlay" and start service without overlay window.
// TODO: add "notification settings" button which launch system notification settings for app's
//  foreground notification (not notification channel!).
// TODO: fix frame drops on activity cold start
