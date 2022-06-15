package com.example.wifilinkspeed

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.PendingIntent.getActivity
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import java.util.*


// Service lifecycle
// Activity->startService() will call onStartCommand() and onDestroy()
// Activity->bindService() will call onBind() and onUnbind() and onDestroy() but NOT onStartCommand()


class ForegroundService : Service() {
	companion object {
		private const val REQUEST_CODE = 0
		private const val NOTIFICATION_ID = 1001
		private const val PERIOD: Long =
			1000 // Android updates wifi info approximately every second
	}

	private lateinit var wifiInfo: WiFiInfo
	private lateinit var notificationManager: NotificationManager
	private var timer: Timer? = null
	private var handler: Handler? = null
	private var handlerTask: Runnable? = null
	private var callbackUpdateActivityText: ((String) -> Unit)? = null

	// this class will be given to the client when the service is bound
	// client can get a reference to the service through it
	class MyBinder(val service: ForegroundService) : Binder()

	// this is the object that receives interactions from clients.
	private val binder = MyBinder(this)

	private fun initializeService() {
		wifiInfo = WiFiInfo(this)
		notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
		createNotificationChannel()
		val notification = createNotification(getString(R.string.notification_message))
		// make service alive forever
		startForeground(NOTIFICATION_ID, notification)
		// start main work
//		updateNotificationByTimer() // works fine, no problems
		updateNotificationByHandler() // more reliable by StackOverflow
	}

	override fun onBind(intent: Intent?): IBinder? = binder

	override fun onUnbind(intent: Intent?): Boolean {
		callbackUpdateActivityText = null
		return super.onUnbind(intent)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		initializeService()
		return super.onStartCommand(intent, flags, startId)
	}

	override fun onDestroy() {
		timer?.cancel()
		timer = null

		handlerTask?.let {
			handler?.removeCallbacks(it)
		}
		handlerTask = null
		handler = null

		stopForeground(true)
		super.onDestroy()
	}

	private fun createNotificationChannel() {
		val channel = NotificationChannel(
			getString(R.string.notification_channel_id),
			getString(R.string.notification_channel_name),
			NotificationManager.IMPORTANCE_LOW // No sound
//			NotificationManager.IMPORTANCE_MIN // No sound and does not appear in the status bar
		).apply {
			description = getString(R.string.notification_description)
		}

		notificationManager.createNotificationChannel(channel)
	}

	private fun createNotification(message: String): Notification {
		val contentIntent = Intent(applicationContext, MainActivity::class.java)
		val contentPendingIntent = getActivity(
			applicationContext,
			REQUEST_CODE,
			contentIntent,
			FLAG_UPDATE_CURRENT
		)

		return Notification.Builder(this, getString(R.string.notification_channel_id))
			.setSmallIcon(R.drawable.ic_wifi_small_icon)
//			.setContentTitle(getString(R.string.notification_title))
			.setContentText(message)
//			.setSubText(getString(R.string.notification_sub_text))
//			.setTicker(getString(R.string.notification_ticker_text))
			// On lock screen shows the notification's full content
			.setVisibility(Notification.VISIBILITY_PUBLIC)
			.setContentIntent(contentPendingIntent)
			.build()
	}

	@Suppress("unused")
	private fun updateNotificationByTimer() {
/*		if (BuildConfig.DEBUG) {
			// Look at timer below, if we did not stop this thread or timer in onDestroy() service
			// will be immortal. It will not stop at all if we call stopService() from activity.
			// Thread or timer will keep service alive.
			// IMPORTANT: always delete created threads and timers.
			Thread {
				while (true) {
					logd("ForegroundService running...")
					Thread.sleep(10000)
				}
			}.start()
		}*/

		timer = Timer().apply {
			schedule(object : TimerTask() {
				override fun run() {
					val linkSpeedStr = wifiInfo.linkSpeedStr(this@ForegroundService)
					callbackUpdateActivityText?.invoke(linkSpeedStr)
					notify(linkSpeedStr)
				}
			}, 0, PERIOD)
		}
	}

	@Suppress("unused")
	private fun updateNotificationByHandler() {
		// A Handler allows you to send and process Message and Runnable objects associated with a thread's MessageQueue.
		// There are two main uses for a Handler:
		// (1) to schedule messages and runnables to be executed at some point in the future;
		// (2) to enqueue an action to be performed on a different thread than your own.
		handler = Handler()
		handlerTask = object : Runnable {
			override fun run() {
				val linkSpeedStr = wifiInfo.linkSpeedStr(this@ForegroundService)
				callbackUpdateActivityText?.invoke(linkSpeedStr)
				notify(linkSpeedStr)
				handler?.postDelayed(this, PERIOD)
			}
		}
		handlerTask?.let {
			handler?.post(it)
		}
	}

	private fun notify(message: String) {
		notificationManager.notify(NOTIFICATION_ID, createNotification(message))
	}

	fun registerCallbackForResults(callback: (String) -> Unit) {
		callbackUpdateActivityText = callback
	}
}
