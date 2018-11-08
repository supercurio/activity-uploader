package supercurio.activityuploader.fitbitwebapi

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import supercurio.activityuploader.SavedToPrefs

@Serializable
data class FitbitToken(

        val accessToken: String,
        val expiresAtSecond: Long

) : SavedToPrefs() {

    override fun save(context: Context) {
        val json = JSON.indented.stringify(serializer(), this)
        saveJson(context, PREF_NAME, json)
    }

    override fun forget(context: Context) {
        super.forgetPref(context, PREF_NAME)
    }

    companion object {
        private const val TAG = "FitbitToken"
        private const val PREF_NAME = "fitbit_token"

        fun load(context: Context): FitbitToken? =
                loadJson(context, PREF_NAME)?.let {
                    return JSON.nonstrict.parse(serializer(), it)
                }
    }
}
