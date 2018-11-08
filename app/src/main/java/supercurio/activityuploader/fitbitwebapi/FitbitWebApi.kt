package supercurio.activityuploader.fitbitwebapi

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import supercurio.activityuploader.Constants

/**
 * Created by François Simond on 10/8/18.
 * Part of fitbit-hr
 */
object FitbitWebApi {

    var token: FitbitToken? = null

    fun getService(context: Context, gson: Gson? = null): FitbitService {
        loadToken(context)

        val logging = HttpLoggingInterceptor()
        logging.level = Constants.HTTP_LOGGING_LEVEL

        val jsonConverter = gson ?: GsonBuilder().create()

        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val newRequest = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer ${token?.accessToken}")
                    .build()
            chain.proceed(newRequest)
        }.addInterceptor(logging).build()

        val retrofit = Retrofit.Builder()
                .baseUrl("https://api.fitbit.com/1/")
                .addCallAdapterFactory(CoroutineCallAdapterFactory())
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(jsonConverter))
                .build()

        return retrofit.create(FitbitService::class.java)
    }

    private fun loadToken(context: Context) {
        if (FitbitWebApi.token == null) FitbitToken.load(context)
    }
}
