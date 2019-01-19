package `fun`.kitsunebi.kitsunebi4android.ui.perapp

import `fun`.kitsunebi.kitsunebi4android.R
import android.content.Context
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class PerAppFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = getString(R.string.config_preference)
        preferenceManager.sharedPreferencesMode = Context.MODE_PRIVATE
        setPreferencesFromResource(R.xml.per_app, rootKey)
    }
}