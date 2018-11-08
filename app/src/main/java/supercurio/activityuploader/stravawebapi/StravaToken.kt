package supercurio.activityuploader.stravawebapi

import android.content.Context
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JSON
import supercurio.activityuploader.SavedToPrefs

@Serializable
data class StravaToken(

        @SerializedName("access_token") val accessToken: String,
        @SerializedName("expires_at") val expiresAtSecond: Long,

        // We're using a refresh token instead of implicit grant for Strava
        @SerializedName("refresh_token") val refreshToken: String,

        // We get athlete information when requesting the token, but will not save it here
        @SerializedName("athlete") @Transient var athlete: StravaAthlete? = null

) : SavedToPrefs() {

    override fun save(context: Context) {
        val json = JSON.indented.stringify(serializer(), this)
        saveJson(context, PREF_NAME, json)
    }

    override fun forget(context: Context) {
        super.forgetPref(context, PREF_NAME)
    }

    companion object {
        private const val TAG = "StravaToken"
        private const val PREF_NAME = "strava_token"

        fun load(context: Context): StravaToken? =
                loadJson(context, PREF_NAME)?.let {
                    return JSON.nonstrict.parse(serializer(), it)
                }
    }
}
