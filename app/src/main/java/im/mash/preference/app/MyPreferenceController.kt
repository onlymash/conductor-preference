package im.mash.preference.app

import android.os.Bundle
import android.util.Log
import im.mash.preference.EditTextPreference
import im.mash.preference.PreferenceController

class MyPreferenceController : PreferenceController() {

    companion object {
        private const val TAG = "MyPreferenceController"
    }
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
        val et = findPreference("edit_text_test") as EditTextPreference
        Log.i(TAG, "type: ${et.getInputType()}, summery: ${et.summary}")
    }

    override fun onCreateItemDecoration(): DividerDecoration {
        return CategoryDivideDividerDecoration()
    }
}