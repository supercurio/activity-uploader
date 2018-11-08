package supercurio.activityuploader

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit

abstract class SavedToPrefs {

    abstract fun save(context: Context)
    abstract fun forget(context: Context)

    fun forgetPref(context: Context, name: String) {
        context.getServicesPrefs().edit {
            remove(name)
        }
    }

    fun saveJson(context: Context, name: String, json: String) {
        Log.d(TAG, "Save to $name: $json")

        context.getServicesPrefs().edit {
            putString(name, json)
        }
    }

    companion object {
        private const val TAG = "SavedToPrefs"
        fun loadJson(context: Context, prefName: String): String? =
                context.getServicesPrefs()
                        .getString(prefName, null)
    }
}

fun Context.getServicesPrefs(): SharedPreferences = this.getSharedPreferences("services",
                                                                              Context.MODE_PRIVATE)
