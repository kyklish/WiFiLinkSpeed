package io.github.kyklish.wifilinkspeed

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.Gravity
import android.view.View

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
