package supercurio.activityuploader.oauth

import android.content.Context
import supercurio.activityuploader.R

object Oauth2 {

    fun getRedirectUri(context: Context, resId: Int): String =
            context.getString(R.string.oauth_callback_redirect_uri,
                              context.getString(R.string.oauth_callback_scheme),
                              context.getString(R.string.oauth_callback_host),
                              context.getString(resId))

}
