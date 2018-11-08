package supercurio.activityuploader.fitbitwebapi

import kotlinx.coroutines.Deferred
import retrofit2.http.GET
import retrofit2.http.Path

interface FitbitService {

    @GET("user/-/profile.json")
    fun getProfile()
            : Deferred<FitbitProfile>

    @GET("user/-/devices.json")
    fun listDevices()
            : Deferred<List<FitbitDevice>>

    @GET("user/-/activities/heart/" +
                 "date/{date}/{end-date}/" +
                 "{detail-level}/" +
                 "time/{start-time}/{end-time}.json")
    fun getHrData(@Path("date") startDate: String,
                  @Path("start-time") startTime: String,
                  @Path("end-date") endDate: String,
                  @Path("end-time") endTime: String,
                  @Path("detail-level") detailLevel: FitbitDetailLevel)
            : Deferred<FitbitHrData>

    @GET("user/-/activities/calories/" +
                 "date/{date}/{end-date}/" +
                 "{detail-level}/" +
                 "time/{start-time}/{end-time}.json")
    fun getCaloriesData(@Path("date") startDate: String,
                        @Path("start-time") startTime: String,
                        @Path("end-date") endDate: String,
                        @Path("end-time") endTime: String,
                        @Path("detail-level") detailLevel: FitbitDetailLevel)
            : Deferred<FitbitCaloriesData>

    @GET("user/-/activities/steps/" +
                 "date/{date}/{end-date}/" +
                 "{detail-level}/" +
                 "time/{start-time}/{end-time}.json")
    fun getStepsData(@Path("date") startDate: String,
                     @Path("start-time") startTime: String,
                     @Path("end-date") endDate: String,
                     @Path("end-time") endTime: String,
                     @Path("detail-level") detailLevel: FitbitDetailLevel)
            : Deferred<FitbitStepsData>
}
