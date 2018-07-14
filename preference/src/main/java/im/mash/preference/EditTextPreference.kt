package im.mash.preference

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.content.res.TypedArrayUtils
import android.support.v7.preference.DialogPreference
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet

import im.mash.preference.conductor.R

class EditTextPreference @SuppressLint("RestrictedApi")
constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

    private var summary: String?
    private var hint: String?
    private var inputType: Int
    private var isSingleLine: Boolean
    private var isSelectAllOnFocus: Boolean
    private var isCommitOnEnter: Boolean

    var text: String? = null
        set(text) {
            val wasBlocking = this.shouldDisableDependents()
            val changed = !TextUtils.equals(this.text, text)
            field = text
            if (changed) {
                this.persistString(text)
                notifyChanged()
            }
            val isBlocking = this.shouldDisableDependents()
            if (isBlocking != wasBlocking) {
                this.notifyDependencyChange(isBlocking)
            }

        }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            this(context, attrs, defStyleAttr, R.style.Preference_DialogPreference_EditTextPreference_Material)

    @SuppressLint("RestrictedApi")
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.editTextPreferenceStyle)

    constructor(context: Context) : this(context, null)

    init {
        var a = context.obtainStyledAttributes(attrs, R.styleable.EditTextPreference)

        inputType = TypedArrayUtils.getInt(a, R.styleable.EditTextPreference_inputType,
                R.styleable.EditTextPreference_android_inputType, InputType.TYPE_CLASS_TEXT)

        isSingleLine = TypedArrayUtils.getBoolean(a, R.styleable.EditTextPreference_singleLine,
                R.styleable.EditTextPreference_android_singleLine, true)

        isSelectAllOnFocus = TypedArrayUtils.getBoolean(a, R.styleable.EditTextPreference_selectAllOnFocus,
                R.styleable.EditTextPreference_android_selectAllOnFocus, false)

        hint = TypedArrayUtils.getString(a, R.styleable.EditTextPreference_hint,
                R.styleable.EditTextPreference_android_hint)

        isCommitOnEnter = a.getBoolean(R.styleable.EditTextPreference_commitOnEnter, false)
        a.recycle()

        /* Retrieve the Preference summary attribute since it's private
         * in the Preference class.
         */
        a = context.obtainStyledAttributes(attrs,
                R.styleable.Preference, defStyleAttr, defStyleRes)

        summary = TypedArrayUtils.getString(a, R.styleable.Preference_summary,
                R.styleable.Preference_android_summary)

        a.recycle()
    }

    override fun onGetDefaultValue(a: TypedArray?, index: Int): Any? {
        return a!!.getString(index)
    }

    override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?) {
        this.text = if (restoreValue) this.getPersistedString(this.text) else defaultValue as String?
    }

    override fun shouldDisableDependents(): Boolean {
        return TextUtils.isEmpty(this.text) || super.shouldDisableDependents()
    }

    override fun onSaveInstanceState(): Parcelable {
        val superState = super.onSaveInstanceState()
        return if (this.isPersistent) {
            superState
        } else {
            val myState = SavedState(superState)
            myState.text = this.text
            myState
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state != null && state.javaClass == SavedState::class.java) {
            val myState = state as SavedState?
            super.onRestoreInstanceState(myState!!.getSuperState())
            this.text = myState.text
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    override fun getSummary(): CharSequence {
        return if (summary == null) super.getSummary() else String.format(summary!!, text ?: "")
    }

    override fun setSummary(summary: CharSequence?) {
        super.setSummary(summary)
        if (summary == null && this.summary != null) {
            this.summary = null
        } else if (summary != null && summary != this.summary) {
            this.summary = summary.toString();
        }
    }

    private class SavedState : BaseSavedState {
        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(`in`: Parcel): SavedState {
                    return SavedState(`in`)
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }
        internal var text: String? = null

        constructor(source: Parcel) : super(source) {
            this.text = source.readString()
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeString(this.text)
        }

        constructor(superState: Parcelable) : super(superState)

    }
}
