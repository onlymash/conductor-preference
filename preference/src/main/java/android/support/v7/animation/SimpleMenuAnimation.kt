package android.support.v7.animation

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.FloatEvaluator
import android.animation.ObjectAnimator
import android.animation.TypeEvaluator
import android.annotation.TargetApi
import android.graphics.Rect
import android.os.Build
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.support.v7.drawable.FixedBoundsDrawable
import android.support.v7.widget.SimpleMenuPopupWindow
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

import java.util.Objects


/**
 * Helper class to create and start animation of Simple Menu.
 *
 * TODO let params styleable
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
object SimpleMenuAnimation {

    fun postStartEnterAnimation(popupWindow: SimpleMenuPopupWindow, background: FixedBoundsDrawable,
                                width: Int, height: Int,
                                startX: Int, startY: Int, start: Rect,
                                itemHeight: Int, elevation: Int, selectedIndex: Int) {
        Objects.requireNonNull<FixedBoundsDrawable>(popupWindow.background).fixedBounds = Rect()
        popupWindow.contentView.clipBounds = Rect()

        popupWindow.contentView.post(Runnable {
            // return if already dismissed
            if (popupWindow.contentView.parent == null) {
                return@Runnable
            }
            startEnterAnimation(popupWindow.contentView, background, width, height, startX, startY, start, itemHeight, elevation, selectedIndex)
        })
    }

    fun startEnterAnimation(view: View, background: FixedBoundsDrawable,
                            width: Int, height: Int,
                            centerX: Int, centerY: Int, start: Rect,
                            itemHeight: Int, elevation: Int, selectedIndex: Int) {
        val holder = PropertyHolder(background, view)
        val backgroundAnimator = createBoundsAnimator(
                holder, width, height, centerX, centerY, start)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
                backgroundAnimator,
                createElevationAnimator(view.parent as View, elevation.toFloat()))
        animatorSet.duration = backgroundAnimator.duration
        animatorSet.start()

        val delay: Long = 0

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val offset = selectedIndex - i
                startChild(view.getChildAt(i), delay + 30 * Math.abs(offset),
                        if (offset == 0) 0 else (itemHeight * 0.2).toInt() * if (offset < 0) -1 else 1)
            }
        }
    }

    private fun startChild(child: View, delay: Long, translationY: Int) {
        child.alpha = 0f

        val alphaAnimator = ObjectAnimator.ofFloat(child, "alpha", 0.0f, 1.0f)
        alphaAnimator.duration = 200
        alphaAnimator.interpolator = AccelerateInterpolator()

        val translationAnimator = ObjectAnimator.ofFloat(child, "translationY", translationY.toFloat(), 0f)
        translationAnimator.duration = 275
        translationAnimator.interpolator = DecelerateInterpolator()

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(alphaAnimator, translationAnimator)
        animatorSet.startDelay = delay
        animatorSet.start()
    }

    private fun getBounds(
            width: Int, height: Int, centerX: Int, centerY: Int): Array<Rect> {
        val endWidth = Math.max(centerX, width - centerX)
        val endHeight = Math.max(centerY, height - centerY)

        val endLeft = centerX - endWidth
        val endRight = centerX + endWidth
        val endTop = centerY - endHeight
        val endBottom = centerY + endHeight

        val end = Rect(endLeft, endTop, endRight, endBottom)
        val max = Rect(0, 0, width, height)

        return arrayOf(end, max)
    }

    private fun createBoundsAnimator(holder: PropertyHolder, width: Int, height: Int, centerX: Int, centerY: Int, start: Rect): Animator {
        val speed = 4096

        val endWidth = Math.max(centerX, width - centerX)
        val endHeight = Math.max(centerY, height - centerY)

        val rect = getBounds(width, height, centerX, centerY)
        val end = rect[0]
        val max = rect[1]

        var duration = (Math.max(endWidth, endHeight).toFloat() / speed * 1000).toLong()
        duration = Math.max(duration, 150)
        duration = Math.min(duration, 300)

        val animator = ObjectAnimator
                .ofObject(holder, SimpleMenuBoundsProperty.BOUNDS, RectEvaluator(max), start, end)
        animator.interpolator = DecelerateInterpolator()
        animator.duration = duration
        return animator
    }

    private fun createElevationAnimator(view: View, elevation: Float): Animator {
        val animator = ObjectAnimator.ofObject(view, View.TRANSLATION_Z.toString(), FloatEvaluator() as TypeEvaluator<*>, -elevation, 0f)
        animator.interpolator = FastOutSlowInInterpolator()
        return animator
    }
}
