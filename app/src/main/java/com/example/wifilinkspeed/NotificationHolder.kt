package com.example.wifilinkspeed

import android.app.*
import android.content.Context
import android.content.Intent

class NotificationHolder(
	private val context: Context,
	private val NOTIFICATION_ID: Int
) {
	companion object {
		private const val REQUEST_CODE = 0
	}

	val notification: Notification
	private var notificationManager =
		context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
	private var notificationBuilder: Notification.Builder

	init {
		createNotificationChannel()
		notificationBuilder =
			Notification.Builder(context, getString(R.string.notification_channel_id))
		notification = createNotification(
			context,
			notificationBuilder,
			getString(R.string.notification_message)
		)
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

	private fun createNotification(
		context: Context,
		notificationBuilder: Notification.Builder,
		message: String
	): Notification {
		val contentIntent = Intent(context, MainActivity::class.java)
		val contentPendingIntent = PendingIntent.getActivity(
			context,
			REQUEST_CODE,
			contentIntent,
			PendingIntent.FLAG_UPDATE_CURRENT
		)

		return notificationBuilder
			.setSmallIcon(R.drawable.ic_wifi_small_icon)
//			.setContentTitle(getString(R.string.notification_title))
			.setContentText(message)
//			.setSubText(getString(R.string.notification_sub_text))
//			.setTicker(getString(R.string.notification_ticker_text))
			// On lock screen shows the notification's full content
			.setVisibility(Notification.VISIBILITY_PUBLIC)
			// if launch activity from notification not work read this:
			// "android - How To Start An Activity From Background in Android 10 (READ LAST COMMENT deep linking) - Stack Overflow.rar"
			.setContentIntent(contentPendingIntent)
			.build()
	}

	// called, when 'message' text is changed (different than already in notification)
	fun notify(message: String) {
		notificationBuilder.setContentText(message)
		notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
	}

	private fun getString(id: Int): String {
		return context.resources.getString(id)
	}
}