package supercurio.activityuploader.stravawebapi

import android.content.Context
import android.util.Log
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import supercurio.activityuploader.Constants
import supercurio.activityuploader.R

object StravaWebApi {

    private const val TAG = "StravaWebApi"

    var token: StravaToken? = null

    fun getOauthService(): StravaService {
        val logging = HttpLoggingInterceptor()
        logging.level = Constants.HTTP_LOGGING_LEVEL

        val client = OkHttpClient.Builder()
                .addInterceptor(logging).build()

        val retrofit = Retrofit.Builder()
                .baseUrl("https://www.strava.com/oauth/")
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

        return retrofit.create(StravaService::class.java)
    }

    suspend fun getService(context: Context): StravaService {
        if (token == null)
            token = StravaToken.load(context)

        // refresh token if it expires in less than 60s
        token?.let {
            if (it.expiresAtSecond < (System.currentTimeMillis() / 1000) - 60) {
                token = try {
                    getOauthService()
                            .refreshToken(context.getString(R.string.oauth_strava_client_id),
                                          context.getString(R.string.oauth_strava_client_secret),
                                          it.refreshToken)
                            .await()
                } catch (e: Throwable) {
                    Log.e(TAG, "Unable to refresh access token: ${e.message}")
                    e.printStackTrace()
                    null
                }?.apply {
                    save(context)
                }
            }
        }

        val logging = HttpLoggingInterceptor()
        logging.level = Constants.HTTP_LOGGING_LEVEL

        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${token?.accessToken}")
                    .build()
            chain.proceed(newRequest)
        }.addInterceptor(logging).build()


        val retrofit = Retrofit.Builder()
                .baseUrl("https://www.strava.com/api/v3/")
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()

        return retrofit.create(StravaService::class.java)
    }

    suspend fun deAuthorize(context: Context) {
        getService(context)
                .deauthorize()
                .await()

        token?.forget(context)
        token = null
    }
}
