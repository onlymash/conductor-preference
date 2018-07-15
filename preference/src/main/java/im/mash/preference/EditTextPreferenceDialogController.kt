package im.mash.preference

import android.content.Context
import android.os.Bundle
import android.support.annotation.RestrictTo
import android.view.View
import android.widget.EditText

import android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP
import android.text.InputType
import android.content.DialogInterface
import android.view.KeyEvent
import android.view.inputmethod.InputMethodManager


open class EditTextPreferenceDialogController : PreferenceDialogController() {

    companion object {
        private const val SAVE_STATE_TEXT = "EditTextPreferenceDialogController.text"
        fun newInstance(key: String): EditTextPreferenceDialogController {
            val controller = EditTextPreferenceDialogController()
            controller.args.putString(PreferenceDialogController.ARG_KEY, key)
            return controller
        }
    }

    private var mEditText: EditText? = null
    private var mText: CharSequence? = null
    private val editTextPreference: EditTextPreference
        get() = preference as EditTextPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            mText = editTextPreference.text
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence(SAVE_STATE_TEXT, mText)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mText = savedInstanceState.getCharSequence(SAVE_STATE_TEXT)
    }

    override fun onBindDialogView(view: View) {
        super.onBindDialogView(view)

        mEditText = view.findViewById<View>(android.R.id.edit) as EditText

        if (mEditText == null) {
            throw IllegalStateException("Dialog view must contain an EditText with id" + " @android:id/edit")
        }

        mEditText!!.setSingleLine(editTextPreference.isSingleLine())
        mEditText!!.setSelectAllOnFocus(editTextPreference.isSelectAllOnFocus())
        if (editTextPreference.getInputType() != InputType.TYPE_CLASS_TEXT)
            mEditText!!.inputType = editTextPreference.getInputType()
        mEditText!!.hint = editTextPreference.getHint()
        mEditText!!.setText(mText)
        mEditText!!.requestFocus()
        mEditText!!.post {
            val imm = activity!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(mEditText, InputMethodManager.SHOW_IMPLICIT)
        }
        if (editTextPreference.isCommitOnEnter())
            mEditText!!.setOnEditorActionListener { textView, keyCode, keyEvent ->
                if (keyCode == KeyEvent.KEYCODE_ENDCALL) {
                    onClick(object : DialogInterface {
                        override fun dismiss() {}
                        override fun cancel() {}
                    }, DialogInterface.BUTTON_POSITIVE)
                    dismissDialog()
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        mEditText = null
    }

    @RestrictTo(LIBRARY_GROUP)
    override fun needInputMethod(): Boolean {
        // We want the input method to show, if possible, when dialog is displayed
        return true
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        if (positiveResult) {
            val value = mEditText!!.text.toString()
            if (editTextPreference.callChangeListener(value)) {
                editTextPreference.text = value
            }
        }
    }
}
