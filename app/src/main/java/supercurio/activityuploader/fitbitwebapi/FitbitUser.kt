package supercurio.activityuploader.fitbitwebapi


import android.content.Context
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JSON
import supercurio.activityuploader.SavedToPrefs

@Serializable
data class FitbitUser(

        @SerializedName("displayName") val displayName: String,
        @SerializedName("fullName") val fullName: String?,
        @SerializedName("avatar") val avatar: String?,
        @SerializedName("avatar150") val avatar150: String?,
        @SerializedName("avatar640") val avatar640: String?,
        @SerializedName("offsetFromUTCMillis") val offsetFromUTCMillis: Long

) : SavedToPrefs() {

    override fun save(context: Context) {
        val json = JSON.indented.stringify(serializer(), this)
        saveJson(context, PREF_NAME, json)
    }

    override fun forget(context: Context) {
        super.forgetPref(context, PREF_NAME)
    }

    companion object {
        private const val PREF_NAME = "fitbit_user"

        fun load(context: Context): FitbitUser? =
                loadJson(context, PREF_NAME)?.let {
                    return JSON.nonstrict.parse(serializer(), it)
                }
    }
}
