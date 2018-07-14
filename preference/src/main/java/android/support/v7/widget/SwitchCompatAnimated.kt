package android.support.v7.widget

import android.content.Context
import android.support.annotation.RestrictTo
import android.util.AttributeSet
import android.view.View

/**
 * Works around https://code.google.com/p/android/issues/detail?id=196652.
 * Class copied from https://github.com/consp1racy/android-support-preference
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class SwitchCompatAnimated : SwitchCompat {
    private var isInSetChecked = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun setChecked(checked: Boolean) {
        isInSetChecked = true
        super.setChecked(checked)
        isInSetChecked = false
    }

    override fun isShown(): Boolean {
        return if (isInSetChecked) {
            visibility == View.VISIBLE
        } else super.isShown()
    }
}