package android.support.v7.preference

import android.content.Context
import android.util.AttributeSet

/**
 * Convenience class to instantiate a switch compat from XML.
 */
class SwitchPreference : SwitchPreferenceCompat {

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context) : super(context)
}