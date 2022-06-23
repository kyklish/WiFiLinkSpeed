package io.github.kyklish.wifilinkspeed

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import java.util.*


// Service lifecycle
// Activity->startService() will call onStartCommand() and onDestroy()
// Activity->bindService() will call onBind() and onUnbind() and onDestroy() but NOT onStartCommand()


class ForegroundService : Service() {
	companion object {
		private const val NOTIFICATION_ID = 1001 // must not be 0

		// Android updates wifi info approximately every second
		private const val PERIOD: Long = 1000
	}

	private lateinit var wifiInfo: WiFiInfo
	private lateinit var notificationHolder: NotificationHolder
	private var timer: Timer? = null
	private var handler: Handler? = null
	private var handlerTask: Runnable? = null
	private var callbackUpdateActivityText: ((String) -> Unit)? = null
	private var textPrev: String = ""

	// Debug simultaneously two variants of overlay window
	private var overlayWindowD: OverlayWindow? = null
	private var overlayWindowL: OverlayWindow? = null

	// Release only one variant
	private var overlayWindow: OverlayWindow? = null

	// this class will be given to the client when the service is bound
	// client can get a reference to the service through it
	class MyBinder(val service: ForegroundService) : Binder()

	// this is the object that receives interactions from clients.
	private val binder = MyBinder(this)

	private fun initializeService() {
		wifiInfo = WiFiInfo(this)
		// if you pass 'applicationContext', notification will live longer, then we need it
		notificationHolder = NotificationHolder(this, NOTIFICATION_ID)
		// make service alive forever
		startForeground(NOTIFICATION_ID, notificationHolder.notification)
		// start main work
//		updateUIbyTimer() // works fine, no problems
		updateUIbyHandler() // more reliable by StackOverflow
	}

	override fun onCreate() {
		super.onCreate()
		if (BuildConfig.DEBUG) {
			overlayWindowD = OverlayWindowByDraw(this, 40F, Color.RED).create()
			overlayWindowL = OverlayWindowByLayout(this, 40F, Color.MAGENTA).create()
		} else {
//			overlayWindow = OverlayWindowByDraw(this, 10F, Color.YELLOW).create()
			overlayWindow = OverlayWindowByLayout(this, 10F, Color.YELLOW).create()
		}
	}

	override fun onDestroy() {
		timer?.cancel()
		timer = null

		handlerTask?.let {
			handler?.removeCallbacks(it)
		}
		handlerTask = null
		handler = null

		overlayWindowD?.remove()
		overlayWindowL?.remove()
		overlayWindow?.remove()

		// if flag set to true, notification previously provided to startForeground(int, Notification)
		// will be removed automatically
		stopForeground(true)
		super.onDestroy()
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

	@Suppress("unused")
	private fun updateUIbyTimer() {
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
					updateUI()
				}
			}, 0, PERIOD)
		}
	}

	@Suppress("unused")
	private fun updateUIbyHandler() {
		// A Handler allows you to send and process Message and Runnable objects associated with a thread's MessageQueue.
		// There are two main uses for a Handler:
		// (1) to schedule messages and runnables to be executed at some point in the future;
		// (2) to enqueue an action to be performed on a different thread than your own.
		handler = Handler()
		handlerTask = object : Runnable {
			override fun run() {
				updateUI()
				handler?.postDelayed(this, PERIOD)
			}
		}
		handlerTask?.let {
			handler?.post(it)
		}
	}

	private fun updateUI(forceUpdate: Boolean = false) {
		val linkSpeedStr = wifiInfo.linkSpeedStr(this@ForegroundService)
		// Timer or handler invoke 'updateUI()' before activity set 'callbackUpdateActivityText'.
		// On first call 'callbackUpdateActivityText?.invoke()' is null and not invoked,
		// notification and overlay updated.
		// On second call 'callbackUpdateActivityText' is bound to activity method, but if we got
		// same text to output 'isTextChanged()' method returns 'false' and text in activity not
		// updated.
		// To fix this algorithm problem, force update activity text.
		if (isTextChanged(linkSpeedStr) or forceUpdate) {
			callbackUpdateActivityText?.invoke(linkSpeedStr) // MainActivity
			notificationHolder.notify(linkSpeedStr) // Notification
			overlayWindowD?.setText(linkSpeedStr) // OverlayWindow Debug
			overlayWindowL?.setText(linkSpeedStr) // OverlayWindow Debug
			overlayWindow?.setText(linkSpeedStr) // OverlayWindow Release
		}
	}

	fun registerCallbackForResults(callback: (String) -> Unit) {
		callbackUpdateActivityText = callback
		updateUI(true)
	}

	private fun isTextChanged(text: String): Boolean {
		return if (textPrev == text)
			false
		else {
			textPrev = text
			true
		}
	}
}
