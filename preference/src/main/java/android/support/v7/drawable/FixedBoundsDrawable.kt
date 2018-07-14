package android.support.v7.drawable

import android.annotation.TargetApi
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build

/**
 * A wrapped [Drawable] that force use its own bounds to draw.
 *
 * It maybe a little dirty. But if we don't do that, during the expanding animation, there will be
 * one or two frame using wrong bounds because of parent view sets bounds.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class FixedBoundsDrawable(private val mDrawable: Drawable) : Drawable(), Drawable.Callback {
    var fixedBounds = Rect()
        set(bounds) = setFixedBounds(bounds.left, bounds.top, bounds.right, bounds.bottom)

    fun setFixedBounds(left: Int, top: Int, right: Int, bottom: Int) {
        fixedBounds.set(left, top, right, bottom)
        setBounds(left, top, right, bottom)
    }

    override fun getOutline(outline: Outline) {
        mDrawable.getOutline(outline)
    }

    override fun draw(canvas: Canvas) {
        mDrawable.bounds = fixedBounds
        mDrawable.draw(canvas)
    }

    override fun setAlpha(alpha: Int) {
        mDrawable.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mDrawable.colorFilter = colorFilter
    }

    override fun getOpacity(): Int {
        return mDrawable.opacity
    }

    override fun invalidateDrawable(who: Drawable) {
        val callback = callback
        callback?.invalidateDrawable(this)
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        val callback = callback
        callback?.scheduleDrawable(this, what, `when`)
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        val callback = callback
        callback?.unscheduleDrawable(this, what)
    }
}