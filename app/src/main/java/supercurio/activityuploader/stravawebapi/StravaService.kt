package supercurio.activityuploader.stravawebapi

import kotlinx.coroutines.Deferred
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface StravaService {

    @POST("token")
    @FormUrlEncoded
    fun accessToken(@Field("client_id") clientId: String,
                    @Field("client_secret") clientSecret: String,
                    @Field("code") code: String,
                    @Field("grant_type") grantType: String = "authorization_code")
            : Deferred<StravaToken>

    @POST("token")
    @FormUrlEncoded
    fun refreshToken(@Field("client_id") clientId: String,
                     @Field("client_secret") clientSecret: String,
                     @Field("refresh_token") refreshToken: String,
                     @Field("grant_type") grantType: String = "refresh_token")
            : Deferred<StravaToken>

    @POST("/oauth/deauthorize")
    fun deauthorize(): Deferred<Response<Unit>>

    @POST("uploads")
    @Multipart
    fun uploadActivity(@Part() file: MultipartBody.Part,
                       @Part("data_type") dataType: RequestBody,
                       @Part("external_id") externalId: RequestBody? = null,
                       @Part("trainer") performedOnTrainer: RequestBody? = null,
                       @Part("commute") commute: RequestBody? = null,
                       @Part("name") name: RequestBody? = null,
                       @Part("description") description: RequestBody? = null)
            : Deferred<Response<StravaUpload>>

    @GET("uploads/{upload-id}")
    fun getUpload(@Path("upload-id") uploadId: Long)
            : Deferred<StravaUpload>
}
