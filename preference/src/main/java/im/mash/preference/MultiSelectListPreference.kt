package im.mash.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.ArrayRes
import android.support.v4.content.res.TypedArrayUtils
import android.support.v7.preference.Preference
import android.support.v7.preference.internal.AbstractMultiSelectListPreference
import android.util.AttributeSet

import java.util.Collections
import java.util.HashSet

import im.mash.preference.conductor.R


class MultiSelectListPreference

@SuppressLint("RestrictedApi")
constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) :
        AbstractMultiSelectListPreference(context, attrs, defStyleAttr, defStyleRes) {

    private var mEntries: Array<CharSequence>? = null
    private var mEntryValues: Array<CharSequence>? = null
    private val mValues = HashSet<String>()

    val selectedItems: BooleanArray
        get() {
            val entries = mEntryValues
            val entryCount = entries!!.size
            val values = mValues
            val result = BooleanArray(entryCount)

            for (i in 0 until entryCount) {
                result[i] = values.contains(entries[i].toString())
            }

            return result
        }

    init {

        val a = context.obtainStyledAttributes(attrs,
                R.styleable.MultiSelectListPreference, defStyleAttr,
                defStyleRes)

        mEntries = TypedArrayUtils.getTextArray(a,
                R.styleable.MultiSelectListPreference_entries,
                R.styleable.MultiSelectListPreference_android_entries)

        mEntryValues = TypedArrayUtils.getTextArray(a,
                R.styleable.MultiSelectListPreference_entryValues,
                R.styleable.MultiSelectListPreference_android_entryValues)

        a.recycle()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : this(context, attrs, defStyleAttr, 0) {}

    @SuppressLint("RestrictedApi")
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, TypedArrayUtils.getAttr(context,
            R.attr.dialogPreferenceStyle,
            android.R.attr.dialogPreferenceStyle))

    constructor(context: Context) : this(context, null)

    fun setEntries(entries: Array<CharSequence>) {
        mEntries = entries
    }

    fun setEntries(@ArrayRes entriesResId: Int) {
        setEntries(context.resources.getTextArray(entriesResId))
    }

    override fun getEntries(): Array<CharSequence>? {
        return mEntries
    }

    fun setEntryValues(entryValues: Array<CharSequence>) {
        mEntryValues = entryValues
    }

    fun setEntryValues(@ArrayRes entryValuesResId: Int) {
        setEntryValues(context.resources.getTextArray(entryValuesResId))
    }

    override fun getEntryValues(): Array<CharSequence>? {
        return mEntryValues
    }

    override fun setValues(values: Set<String>) {
        mValues.clear()
        mValues.addAll(values)

        persistStringSet(values)
    }

    override fun getValues(): Set<String> {
        return mValues
    }

    fun findIndexOfValue(value: String?): Int {
        if (value != null && mEntryValues != null) {
            for (i in mEntryValues!!.indices.reversed()) {
                if (mEntryValues!![i] == value) {
                    return i
                }
            }
        }
        return -1
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Any {
        val defaultValues = a!!.getTextArray(index)
        val result = HashSet<String>()

        for (defaultValue in defaultValues) {
            result.add(defaultValue.toString())
        }

        return result
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        values = if (restoreValue) getPersistedStringSet(mValues) else defaultValue as Set<String>
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        if (isPersistent) {
            // No need to save instance state
            return superState
        }

        val myState = SavedState(superState)
        myState.values = values
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || state.javaClass != SavedState::class.java) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }

        val myState = state as SavedState?
        super.onRestoreInstanceState(myState!!.getSuperState())
        values = myState.values
    }

    private class SavedState : Preference.BaseSavedState {

        companion object {
            @JvmField
            val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(source: Parcel): SavedState {
                    return SavedState(source)
                }
                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }

        lateinit var values: Set<String>

        constructor(source: Parcel) : super(source) {
            val size = source.readInt()
            values = HashSet()
            val strings = arrayOfNulls<String>(size)
            source.readStringArray(strings)
            Collections.addAll<String>(values as HashSet<String>, *strings)
        }

        constructor(superState: Parcelable) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(values.size)
            dest.writeStringArray(values.toTypedArray())
        }

    }
}
