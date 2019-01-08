package `fun`.kitsunebi.kitsunebi4android.storage

import `fun`.kitsunebi.kitsunebi4android.R
import android.content.Context

open class Preferences {
    companion object {
        fun putString(context: Context, k: String, v: String) {
            val sharedPref = context.getSharedPreferences(context.getString(R.string.config_preference), Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putString(k, v)
                commit()
            }
        }

        fun getString(context: Context, k: String, default: String?): String {
            val sharedPref = context.getSharedPreferences(
                    context.getString(R.string.config_preference), Context.MODE_PRIVATE)
            if (default != null) {
                return sharedPref.getString(k, default!!)
            } else {
                return sharedPref.getString(k, "")
            }
        }

        fun putBool(context: Context, k: String, v: Boolean) {
            val sharedPref = context.getSharedPreferences(context.getString(R.string.config_preference), Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putBoolean(k, v)
                commit()
            }
        }

        fun getBool(context: Context, k: String, default: Boolean?): Boolean {
            val sharedPref = context.getSharedPreferences(
                    context.getString(R.string.config_preference), Context.MODE_PRIVATE)
            if (default != null) {
                return sharedPref.getBoolean(k, default!!)
            } else {
                return sharedPref.getBoolean(k, false)
            }
        }

        fun putInt(context: Context, k: String, v: Int) {
            val sharedPref = context.getSharedPreferences(context.getString(R.string.config_preference), Context.MODE_PRIVATE)
            with(sharedPref.edit()) {
                putInt(k, v)
                commit()
            }
        }

        fun getInt(context: Context, k: String, default: Int?): Int {
            val sharedPref = context.getSharedPreferences(
                    context.getString(R.string.config_preference), Context.MODE_PRIVATE)
            if (default != null) {
                return sharedPref.getInt(k, default!!)
            } else {
                return sharedPref.getInt(k, 0)
            }
        }
    }
}
