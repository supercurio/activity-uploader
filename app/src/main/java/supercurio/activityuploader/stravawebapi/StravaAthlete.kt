package supercurio.activityuploader.stravawebapi

import android.content.Context
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import supercurio.activityuploader.SavedToPrefs

@Serializable
data class StravaAthlete(

        @SerializedName("id") val id: Int,
        @SerializedName("username") val username: String,
        @SerializedName("firstname") val firstName: String,
        @SerializedName("lastname") val lastName: String,
        @SerializedName("profile") val profilePictureUrl: String

) : SavedToPrefs() {

    override fun save(context: Context) {
        val json = JSON.indented.stringify(serializer(), this)
        saveJson(context, PREF_NAME, json)
    }


    override fun forget(context: Context) {
        super.forgetPref(context, PREF_NAME)
    }

    companion object {
        fun load(context: Context): StravaAthlete? =
                loadJson(context, PREF_NAME)?.let {
                    return JSON.nonstrict.parse(serializer(), it)
                }

        private const val TAG = "StravaAthlete"
        private const val PREF_NAME = "strava_athlete"
    }
}
