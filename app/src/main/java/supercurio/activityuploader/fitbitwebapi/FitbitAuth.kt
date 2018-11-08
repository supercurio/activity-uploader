package supercurio.activityuploader.fitbitwebapi

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import kotlinx.serialization.Serializable
import supercurio.activityuploader.R
import supercurio.activityuploader.fitbitwebapi.FitbitConfig.SCOPE
import supercurio.activityuploader.oauth.Oauth2.getRedirectUri

@Serializable
data class FitbitAuth(

        val token: FitbitToken,
        val user: FitbitUser

) {
    companion object {
        private const val TAG = "FitbitAuth"
        private const val AUTHORIZE_URL = "https://www.fitbit.com/oauth2/authorize"

        fun buildAuthUri(context: Context): Uri {
            val url = AUTHORIZE_URL +
                    "?response_type=token" +
                    "&client_id=${context.getString(R.string.oauth_fitbit_client_id)}" +
                    "&redirect_uri=${getRedirectUri(context,
                                                    R.string.oauth_callback_fitbit_path)}" +
                    "&scope=${FitbitConfig.SCOPE.joinToString(" ") { it }}" +
                    "&prompt=${FitbitConfig.PROMPT}" +
                    "&expires_in=2592000"

            Log.i(TAG, "Fitbit auth URL: $url")

            return url.toUri()
        }


        suspend fun fromCallbackUri(context: Context, callbackUri: Uri): FitbitAuth? {
            val uriFragment = callbackUri.fragment ?: return null
            val uri = ("?" + uriFragment.removePrefix("#")).toUri()

            var token: String? = null
            var expires: Long = -1

            uri.queryParameterNames.forEach { key ->
                Log.i(TAG, "$key: " + uri.getQueryParameter(key))

                when (key) {
                    "access_token" -> token = uri.getQueryParameter(key)
                    "scope" -> {
                        val scopeList = uri.getQueryParameter(key)?.split(' ') ?: listOf()
                        if (!scopeList.containsAll(SCOPE)) {
                            Log.e(TAG, "Missing scope")
                            return null
                        }
                    }
                    "expires_in" -> {
                        val expiresIn = (uri.getQueryParameter(key) ?: "$expires").toLong()
                        expires = System.currentTimeMillis() / 1_000 + expiresIn
                    }
                }
            }


            token?.let {
                if (expires < 0) return null

                val fitbitToken = FitbitToken(it, expires)
                fitbitToken.save(context)
                FitbitWebApi.token = fitbitToken

                val user = getUser(context)

                return FitbitAuth(fitbitToken, user)
            }

            return null
        }

        private suspend fun getUser(context: Context): FitbitUser {
            val service = FitbitWebApi.getService(context)
            val user = service.getProfile().await().user
            user.save(context)

            return user
        }
    }
}
