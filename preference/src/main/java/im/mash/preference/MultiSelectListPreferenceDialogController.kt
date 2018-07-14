package im.mash.preference

import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog

import java.util.ArrayList
import java.util.HashSet

class MultiSelectListPreferenceDialogController : PreferenceDialogController() {

    companion object {

        private const val SAVE_STATE_VALUES = "MultiSelectListPreferenceDialogController.values"
        private const val SAVE_STATE_CHANGED = "MultiSelectListPreferenceDialogController.changed"
        private const val SAVE_STATE_ENTRIES = "MultiSelectListPreferenceDialogController.entries"
        private const val SAVE_STATE_ENTRY_VALUES = "MultiSelectListPreferenceDialogController.entryValues"

        fun newInstance(key: String): MultiSelectListPreferenceDialogController {
            val controller = MultiSelectListPreferenceDialogController()
            controller.args.putString(PreferenceDialogController.ARG_KEY, key)
            return controller
        }
    }

    private val mNewValues = HashSet<String>()
    private var mPreferenceChanged: Boolean = false
    private var mEntries: Array<CharSequence>? = null
    private var mEntryValues: Array<CharSequence>? = null

    private val listPreference: MultiSelectListPreference
        get() = preference as MultiSelectListPreference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val preference = listPreference

            if (preference.entries == null || preference.entryValues == null) {
                throw IllegalStateException(
                        "MultiSelectListPreference requires an entries array and " + "an entryValues array.")
            }

            mNewValues.clear()
            mNewValues.addAll(preference.values)
            mPreferenceChanged = false
            mEntries = preference.entries
            mEntryValues = preference.entryValues
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(SAVE_STATE_VALUES, ArrayList(mNewValues))
        outState.putBoolean(SAVE_STATE_CHANGED, mPreferenceChanged)
        outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries)
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_VALUES, mEntryValues)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mNewValues.clear()
        mNewValues.addAll(savedInstanceState.getStringArrayList(SAVE_STATE_VALUES))
        mPreferenceChanged = savedInstanceState.getBoolean(SAVE_STATE_CHANGED, false)
        mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES)
        mEntryValues = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_VALUES)
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)

        val entryCount = mEntryValues!!.size
        val checkedItems = BooleanArray(entryCount)
        for (i in 0 until entryCount) {
            checkedItems[i] = mNewValues.contains(mEntryValues!![i].toString())
        }
        builder.setMultiChoiceItems(mEntries, checkedItems
        ) { dialog, which, isChecked ->
            if (isChecked) {
                mPreferenceChanged = mPreferenceChanged or mNewValues.add(
                        mEntryValues!![which].toString())
            } else {
                mPreferenceChanged = mPreferenceChanged or mNewValues.remove(
                        mEntryValues!![which].toString())
            }
        }
    }

    override fun onDialogClosed(positiveResult: Boolean) {
        val preference = listPreference
        if (positiveResult && mPreferenceChanged) {
            val values = mNewValues
            if (preference.callChangeListener(values)) {
                preference.values = values
            }
        }
        mPreferenceChanged = false
    }
}