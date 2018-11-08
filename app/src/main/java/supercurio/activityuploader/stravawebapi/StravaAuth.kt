package supercurio.activityuploader.stravawebapi

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import supercurio.activityuploader.R
import supercurio.activityuploader.oauth.Oauth2.getRedirectUri

data class StravaAuth(

        val token: StravaToken,
        val athlete: StravaAthlete

) {
    companion object {
        private const val TAG = "StravaAuth"
        private const val AUTHORIZE_URL = "https://www.strava.com/oauth/authorize"

        fun buildAuthUri(context: Context): Uri {
            val url = AUTHORIZE_URL +
                    "?response_type=code" +
                    "&client_id=${context.getString(R.string.oauth_strava_client_id)}" +
                    "&redirect_uri=${getRedirectUri(context,
                                                    R.string.oauth_callback_strava_path)}" +
                    "&scope=${StravaConfig.SCOPE.joinToString(",") { it }}" +
                    "&approval_prompt=${StravaConfig.OAUTH_APPROVAL_PROMPT}"

            Log.d(TAG, "Strava auth URL: $url")

            return url.toUri()
        }

        suspend fun fromCallbackUri(context: Context, uri: Uri): StravaAthlete? {
            var code: String? = null
            val scopeList = mutableListOf<String>()

            uri.queryParameterNames.forEach { key ->
                Log.i(TAG, "$key: " + uri.getQueryParameter(key))

                when (key) {
                    "code" -> code = uri.getQueryParameter(key)
                    "scope" -> scopeList += uri.getQueryParameter(key)?.split(',') ?: listOf()
                }
            }

            if (scopeList.isEmpty() || !scopeList.containsAll(StravaConfig.SCOPE)) {
                // TODO: error management of missing scopes
                Log.e(TAG, "Missing scope")
                return null
            }

            code?.let {
                return requestToken(context, it)
            }

            return null
        }

        private suspend fun requestToken(context: Context, code: String): StravaAthlete? {

            val service = StravaWebApi.getOauthService()

            val stravaToken = service.accessToken(
                    context.getString(R.string.oauth_strava_client_id),
                    context.getString(R.string.oauth_strava_client_secret),
                    code).await()
            Log.i(TAG, "Token response: $stravaToken")

            val athlete = stravaToken.athlete
            stravaToken.athlete?.save(context)

            stravaToken.athlete = null
            stravaToken.save(context)

            StravaWebApi.token = stravaToken

            return athlete
        }
    }
}
