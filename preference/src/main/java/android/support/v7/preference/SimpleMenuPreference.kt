package android.support.v7.preference

import android.content.Context
import android.content.res.TypedArray
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v7.widget.SimpleMenuPopupWindow
import android.util.AttributeSet
import android.view.View

import im.mash.preference.conductor.R

/**
 * A version of [ListPreference] that use
 * [Simple Menus](https://material.io/guidelines/components/menus.html#menus-simple-menus)
 * in Material Design as drop down.
 *
 * On pre-Lollipop, it will fallback [ListPreference].
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class SimpleMenuPreference(context: Context, attrs: AttributeSet?, defStyleAttr: Int,
                           defStyleRes: Int) : ListPreference(context, attrs, defStyleAttr, defStyleRes) {

    private var mAnchor: View? = null
    private var mItemView: View? = null
    private val mPopupWindow: SimpleMenuPopupWindow?

    @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) R.attr.dialogPreferenceStyle else R.attr.simpleMenuPreferenceStyle) : this(context, attrs, defStyle, R.style.Preference_SimpleMenuPreference) {}

    init {

        val a = context.obtainStyledAttributes(
                attrs, R.styleable.SimpleMenuPreference, defStyleAttr, defStyleRes)

        val popupStyle = a.getResourceId(R.styleable.SimpleMenuPreference_popupStyle, R.style.Preference_SimpleMenuPreference_Popup)

        mPopupWindow = SimpleMenuPopupWindow(context, attrs, R.styleable.SimpleMenuPreference_popupStyle, popupStyle)
        mPopupWindow.onItemClickListener = object : SimpleMenuPopupWindow.OnItemClickListener {
            override fun onClick(i: Int) {
                val value = entryValues[i].toString()
                if (callChangeListener(value)) {
                    setValue(value)
                }
            }
        }

        a.recycle()
    }

    override fun onClick() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            super.onClick()
            return
        }

        if (entries == null || entries.size == 0) {
            return
        }

        if (mPopupWindow == null) {
            return
        }

        mPopupWindow.entries = entries
        mPopupWindow.selectedIndex = findIndexOfValue(value)

        val container = mItemView!!.parent as View

        mPopupWindow.show(mItemView!!, container, mAnchor!!.x.toInt())
    }

    override fun setEntries(entries: Array<CharSequence>) {
        super.setEntries(entries)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return
        }

        mPopupWindow!!.requestMeasure()
    }

    override fun setValue(value: String) {
        super.setValue(value)
    }

    override fun onBindViewHolder(view: PreferenceViewHolder) {
        super.onBindViewHolder(view)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return
        }

        mItemView = view.itemView
        mAnchor = view.itemView.findViewById(android.R.id.empty)

        if (mAnchor == null) {
            throw IllegalStateException("SimpleMenuPreference item layout must contain" + "a view id is android.R.id.empty to support iconSpaceReserved")
        }
    }
}
