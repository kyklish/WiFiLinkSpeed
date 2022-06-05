package com.example.wifilinkspeed

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.example.wifilinkspeed.Utils.Companion.logd


class ForegroundService : Service() {
	private val notificationId = 1001
	private lateinit var notificationManager : NotificationManager

	override fun onBind(intent: Intent?): IBinder? {
		return null
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		Thread {
			while (true) {
				logd("Service is running...")
				Thread.sleep(2000)
			}
		}.start()

		notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
		createNotificationChannel()
//		val notification = createNotification("text")
		val notification = createNotification(getString(R.string.notification_message))
		startForeground(notificationId, notification)

		return super.onStartCommand(intent, flags, startId)
	}

	private fun createNotificationChannel() {
		val channel = NotificationChannel(
			getString(R.string.notification_channel_id),
			getString(R.string.notification_channel_name),
			NotificationManager.IMPORTANCE_LOW // No sound
//			NotificationManager.IMPORTANCE_MIN // No sound and does not appear in the status bar
		).apply {
			description = getString(R.string.notification_description)
			setShowBadge(false) // Notification dot on app icon
		}

		notificationManager.createNotificationChannel(channel)
	}

	private fun createNotification(message: String): Notification {
		return Notification.Builder(this, getString(R.string.notification_channel_id))
			.setSmallIcon(R.drawable.ic_wifi_small_icon)
//			.setContentTitle(getString(R.string.notification_title))
			.setContentText(message)
			.setSubText(getString(R.string.notification_sub_text))
//			.setTicker(getString(R.string.notification_ticker_text))
			// On lock screen shows the notification's full content
			.setVisibility(Notification.VISIBILITY_PUBLIC)
			.build()
	}

	fun notify(message: String) {
		notificationManager.notify(notificationId, createNotification(message))
	}

	override fun onDestroy() {
		logd("Service is destroyed.")
		super.onDestroy()
	}
}