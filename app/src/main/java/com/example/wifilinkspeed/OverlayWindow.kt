package com.example.wifilinkspeed

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.graphics.*
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView

interface OverlayWindow {
	fun create(): OverlayWindow
	fun remove()
	fun setText(text: String)
}

abstract class OverlayWindowBase(context: Context, fontSizeSp: Float) : OverlayWindow {
	protected lateinit var view: View
	private var windowManager = context.getSystemService(Service.WINDOW_SERVICE) as WindowManager

	// set overlay window width to display width to avoid text flickering on window recreation
	// or window update when text message width is changed
	protected var windowWidth = context.resources.displayMetrics.widthPixels
	var mLayoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
		// xml - inflate view via layout_overlay.xml layout
		// paint - low level draw text via pain and canvas
//		500, 100, // width&height = works with 'paint' and 'xml'
//		WindowManager.LayoutParams.WRAP_CONTENT, // width&height = works with 'xml' but cause window animation, when text width and height changed
//		WindowManager.LayoutParams.WRAP_CONTENT, // width&height = works with 'xml' but cause window animation, when text width and height changed
//		0, // xpos&ypos = works with 'paint' and 'xml'
//		0, // xpos&ypos = works with 'paint' and 'xml'
		WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
		WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
		PixelFormat.TRANSLUCENT
//		PixelFormat.OPAQUE // works with 'paint' and 'xml' (better for testing, can see overlay area)
	).apply {
		gravity = Gravity.END or Gravity.TOP
		width = windowWidth
		// TODO: '+ 15' is for padding above and under text, don't know how to calculate value
		height = Utils.spToPx(context.resources, fontSizeSp).toInt() * 2 + 30
	}
	private var textPrev: String = ""

	override fun create(): OverlayWindow {
		windowManager.addView(view, mLayoutParams)
		// here we implement fancy chain calls like android did :)
		return this
	}

	override fun remove() {
		windowManager.removeView(view)
	}

	protected fun isTextChanged(text: String): Boolean {
		return if (textPrev == text)
			false
		else {
			textPrev = text
			true
		}
	}
}

// Use xml layout to show text in overlay window
@Suppress("unused")
@SuppressLint("InflateParams")
class OverlayWindowByLayout(context: Context, fontSizeSp: Float, fontColor: Int) :
	OverlayWindowBase(context, fontSizeSp) {
	private var textView: TextView

	init {
		val inflater = LayoutInflater.from(context)
		view = inflater.inflate(R.layout.layout_overlay, null)
		textView = view.findViewById(R.id.textOverlay)
		textView.apply {
			setTextColor(fontColor)
			textSize = fontSizeSp
			if ((mLayoutParams.gravity and Gravity.END) == Gravity.END)
				textAlignment = View.TEXT_ALIGNMENT_VIEW_END
			if (BuildConfig.DEBUG)
				setBackgroundColor(Color.GRAY)
		}
	}

	override fun setText(text: String) {
		if (isTextChanged(text)) {
			textView.text = text
			// Android views automatically call invalidate() when their properties change,
			// such as the background color or the text in a TextView.
//			view.invalidate()
		}
	}
}

// Use low level func calls to draw on overlay window
@Suppress("unused")
class OverlayWindowByDraw(context: Context, fontSizeSp: Float, fontColor: Int) :
	OverlayWindowBase(context, fontSizeSp) {
	init {
		view = OverlayView(
			context,
			this,
			windowWidth.toFloat(),
			Utils.spToPx(context.resources, fontSizeSp),
			fontColor
		)
		if (BuildConfig.DEBUG)
			mLayoutParams.y = context.resources.displayMetrics.heightPixels / 2
	}

	override fun setText(text: String) {
		if (isTextChanged(text)) {
			val textList = text.split('\n')
			(view as OverlayView).textFirstLine = textList.getOrElse(0) { "" }
			(view as OverlayView).textSecondLine = textList.getOrElse(1) { "" }
			// Custom view must call invalidate() to get know Android, that data is updated and we need
			// redraw our custom view (Android will call onDraw() method of our custom view).
			// https://developer.android.com/guide/topics/graphics/hardware-accel
			// Hardware acceleration enabled by default. Once a view is rendered into a hardware
			// layer (into hardware texture), its drawing code does not have to be executed until
			// the view calls invalidate().
			view.invalidate()
		}
	}
}
