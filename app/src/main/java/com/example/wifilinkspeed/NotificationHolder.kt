package com.example.wifilinkspeed

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.Icon
import android.os.Build


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
//			NotificationManager.IMPORTANCE_HIGH // Sound, pop on screen
			NotificationManager.IMPORTANCE_DEFAULT // Sound
//			NotificationManager.IMPORTANCE_LOW // No sound
//			NotificationManager.IMPORTANCE_MIN // No sound, compact, not appear in status bar
		).apply {
			description = getString(R.string.notification_description)
			setSound(null, null) // disable any sound from notification
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
		val bitmap = createBitmapFromString(message)
		val icon = Icon.createWithBitmap(bitmap)
		notificationBuilder.setSmallIcon(icon)
		notificationBuilder.setContentText(message)
		notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
	}

	private fun getString(id: Int): String {
		return context.resources.getString(id)
	}

	@Suppress("UnnecessaryVariable")
	@SuppressLint("ObsoleteSdkInt")
	private fun createBitmapFromString(text: String): Bitmap {
		// hardcoded version of status bar height: 24px or 25px
		val statusBarHeight =
			kotlin.math.ceil(
				(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) 24 else 25)
						* context.resources.displayMetrics.density
			)
		var xDrawText = 0F
		val size = statusBarHeight // size of square bitmap (future small icon for notification)
		val paint = Paint()
		paint.apply {
			isAntiAlias = true
			color = Color.WHITE
			// TODO: why 1.5? (density: ldpi=0.75, mdpi=1, hdpi=1.5, xhdpi=2)
			textSize = size / 1.5F // two lines, three digits in each line
//			textSize = size / 2F // two lines, four digits in each line
			textAlign = Paint.Align.RIGHT
			typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
		}
		// read comments in OverlayView@init{}
		if (paint.textAlign == Paint.Align.RIGHT)
			xDrawText = size

		val textList = text.split('\n')
		val textFirstLine = textList.getOrElse(0) { "" }.removeSuffix("Mbps Tx")
		val textSecondLine = textList.getOrElse(1) { "" }.removeSuffix("Mbps Rx")

		val bitmap =
			Bitmap.createBitmap(size.toInt(), size.toInt(), Bitmap.Config.ARGB_8888)

		val canvas = Canvas(bitmap)
		canvas.drawText(textFirstLine, xDrawText, size / 2, paint)
		canvas.drawText(textSecondLine, xDrawText, size, paint)
		return bitmap
	}
}
