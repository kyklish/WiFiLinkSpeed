package com.example.wifilinkspeed

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.graphics.*
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
		// TODO: remove override default params.y for window
		mLayoutParams.y = context.resources.displayMetrics.heightPixels / 2
	}

	override fun setText(text: String) {
		if (isTextChanged(text)) {
			val textList = text.split('\n')
			(view as OverlayView).textFirstLine = textList.getOrElse(0) { "" }
			(view as OverlayView).textSecondLine = textList.getOrElse(1) { "" }
			// Custom view must call invalidate() to get know Android, that data is updated and we need
			// redraw our custom view (Android will call onDraw() method of our custom view).
			view.invalidate()
		}
	}
}

// custom View to draw text
class OverlayView(
	context: Context,
	overlayWindow: OverlayWindowBase?,
	windowWidth: Float,
	private val fontSizePx: Float,
	fontColor: Int
) :
	View(context) {
	// secondary constructor for android tools (required)
	constructor(context: Context) : this(
		context,
		null,
		0F,
		0F,
		0
	)

	private var xDrawText: Float = 0F
	var textFirstLine: String = ""
	var textSecondLine: String = ""

	private val paint: Paint = Paint()

	init {
		paint.apply {
			isAntiAlias = true
			typeface = Typeface.create("serif-monospace", Typeface.BOLD)
			color = fontColor
			textSize = fontSizePx
			if ((overlayWindow!!.mLayoutParams.gravity and Gravity.END) == Gravity.END)
				textAlign = Paint.Align.RIGHT
			// if you need transparency set alpha channel
//			alpha = 255
			// or alpha and color on one function call
//			setARGB(255, 255, 0, 0)
		}
		// if textAlign set  LEFT, X coordinate for text draw will be 'strait' (left to right)
		// if textAlign set RIGHT, X coordinate for text draw will be 'reversed' (right to left)
		if (paint.textAlign == Paint.Align.RIGHT)
			xDrawText = windowWidth
	}

/*	// use this if you inherit from ViewGroup
	override fun dispatchDraw(canvas: Canvas?) {
		super.dispatchDraw(canvas)
		if (BuildConfig.DEBUG)
			canvas?.drawColor(Color.GRAY)
		canvas?.drawText(textFirstLine, xDrawText, fontSizePx, paint)
		canvas?.drawText(textSecondLine, xDrawText, fontSizePx * 2, paint)
	}*/

	// use this if you inherit from View
	override fun onDraw(canvas: Canvas?) {
		super.onDraw(canvas)
		if (BuildConfig.DEBUG)
			canvas?.drawColor(Color.GRAY)
		canvas?.drawText(textFirstLine, xDrawText, fontSizePx, paint)
		canvas?.drawText(textSecondLine, xDrawText, fontSizePx * 2, paint)
	}

	override fun onLayout(arg0: Boolean, arg1: Int, arg2: Int, arg3: Int, arg4: Int) {}
}
