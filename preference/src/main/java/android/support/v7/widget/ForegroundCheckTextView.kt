package android.support.v7.widget

import android.annotation.TargetApi
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.widget.CheckedTextView

import im.mash.preference.conductor.R

/**
 * Extension of [CheckedTextView] that adds a Foreground drawable.
 */
class ForegroundCheckTextView
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null,
                          defStyleAttr: Int = 0) : AppCompatCheckedTextView(context, attrs, defStyleAttr) {

    private var mForeground: Drawable? = null

    private val mSelfBounds = Rect()

    private val mOverlayBounds = Rect()

    private var mForegroundGravity = Gravity.FILL

    protected var mForegroundInPadding = true

    internal var mForegroundBoundsChanged = false

    init {
        init(context, attrs, defStyleAttr)
    }

    private fun init(context: Context, attrs: AttributeSet?, defStyleAttr: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.ForegroundCheckTextView,
                defStyleAttr, 0)

        mForegroundGravity = a.getInt(
                R.styleable.ForegroundCheckTextView_android_foregroundGravity, mForegroundGravity)

        val d = a.getDrawable(R.styleable.ForegroundCheckTextView_android_foreground)
        if (d != null) {
            foreground = d
        }

        mForegroundInPadding = a.getBoolean(
                R.styleable.ForegroundCheckTextView_foregroundInsidePadding, true)

        a.recycle()
    }

    /**
     * Describes how the foreground is positioned.
     *
     * @return foreground gravity.
     * @see .setForegroundGravity
     */
    override fun getForegroundGravity(): Int {
        return mForegroundGravity
    }

    /**
     * Describes how the foreground is positioned. Defaults to START and TOP.
     *
     * @param foregroundGravity See [Gravity]
     * @see .getForegroundGravity
     */
    override fun setForegroundGravity(foregroundGravity: Int) {
        var fg = foregroundGravity
        if (mForegroundGravity != fg) {
            if (fg and Gravity.RELATIVE_HORIZONTAL_GRAVITY_MASK == 0) {
                fg = fg or Gravity.START
            }

            if (fg and Gravity.VERTICAL_GRAVITY_MASK == 0) {
                fg = fg or Gravity.TOP
            }

            mForegroundGravity = fg

            if (mForegroundGravity == Gravity.FILL && mForeground != null) {
                val padding = Rect()
                mForeground!!.getPadding(padding)
            }

            requestLayout()
        }
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return super.verifyDrawable(who) || who === mForeground
    }

    override fun jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState()
        if (mForeground != null) {
            mForeground!!.jumpToCurrentState()
        }
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        if (mForeground != null && mForeground!!.isStateful) {
            mForeground!!.state = drawableState
        }
    }

    /**
     * Supply a Drawable that is to be rendered on top of all of the child
     * views in the frame layout.  Any padding in the Drawable will be taken
     * into account by ensuring that the children are inset to be placed
     * inside of the padding area.
     *
     * @param drawable The Drawable to be drawn on top of the children.
     */
    override fun setForeground(drawable: Drawable?) {
        if (mForeground !== drawable) {
            if (mForeground != null) {
                mForeground!!.callback = null
                unscheduleDrawable(mForeground)
            }

            mForeground = drawable

            if (drawable != null) {
                setWillNotDraw(false)
                drawable.callback = this
                if (drawable.isStateful) {
                    drawable.state = drawableState
                }
                if (mForegroundGravity == Gravity.FILL) {
                    val padding = Rect()
                    drawable.getPadding(padding)
                }
            } else {
                setWillNotDraw(true)
            }
            requestLayout()
            invalidate()
        }
    }

    /**
     * Returns the drawable used as the foreground of this FrameLayout. The
     * foreground drawable, if non-null, is always drawn on top of the children.
     *
     * @return A Drawable or null if no foreground was set.
     */
    override fun getForeground(): Drawable? {
        return mForeground
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mForegroundBoundsChanged = mForegroundBoundsChanged or changed
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mForegroundBoundsChanged = true
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        if (mForeground != null) {
            val foreground = mForeground

            if (mForegroundBoundsChanged) {
                mForegroundBoundsChanged = false
                val selfBounds = mSelfBounds
                val overlayBounds = mOverlayBounds

                val w = right - left
                val h = bottom - top

                if (mForegroundInPadding) {
                    selfBounds.set(0, 0, w, h)
                } else {
                    selfBounds.set(paddingLeft, paddingTop,
                            w - paddingRight, h - paddingBottom)
                }

                Gravity.apply(mForegroundGravity, foreground!!.intrinsicWidth,
                        foreground.intrinsicHeight, selfBounds, overlayBounds)
                foreground.bounds = overlayBounds
            }

            foreground!!.draw(canvas)
        }
    }

    override fun drawableHotspotChanged(x: Float, y: Float) {
        super.drawableHotspotChanged(x, y)
        if (mForeground != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mForeground!!.setHotspot(x, y)
            }
        }
    }
}
