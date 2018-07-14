package im.mash.preference.app

import android.os.Bundle
import im.mash.preference.PreferenceController

class MyPreferenceController : PreferenceController() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.settings)
    }
}