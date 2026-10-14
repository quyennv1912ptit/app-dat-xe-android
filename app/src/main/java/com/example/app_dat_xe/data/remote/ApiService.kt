package com.example.app_dat_xe.data.remote

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Header
import retrofit2.http.PUT
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part
interface ApiService {

    @POST("api/device-tokens")
    suspend fun registerDeviceToken(
        @Body request: DeviceTokenRequest
    ): Response<String>

    @DELETE("api/device-tokens/{installationId}")
    suspend fun unregisterDeviceToken(
        @Path("installationId") installationId: String
    ): Response<String>

    @POST("rides/estimate")
    suspend fun estimateRide(
        @Body request: EstimateRideRequest
    ): Response<EstimateRideResponse>

    @GET("api/location/geocode")
    suspend fun geocodeAddress(
        @Query("address") address: String
    ): Response<JsonElement>

    @POST("rides")
    suspend fun createRide(
        @Body request: CreateRideRequest
    ): Response<CreateRideResponse>

    @GET("rides")
    suspend fun listMyRides(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100
    ): Response<RidePageResponse>

    @GET("rides/{rideId}")
    suspend fun getRideDetail(
        @Path("rideId") rideId: Long
    ): Response<RideDetailData>

    @PUT("driver/me/availability")
    suspend fun setDriverAvailability(
        @Body request: DriverAvailabilityRequest
    ): Response<JsonElement>

    @POST("offers/{rideId}/accept")
    suspend fun acceptRideOffer(
        @retrofit2.http.Path("rideId") rideId: Long
    ): Response<JsonElement>

    @POST("offers/{rideId}/reject")
    suspend fun rejectRideOffer(
        @retrofit2.http.Path("rideId") rideId: Long
    ): Response<JsonElement>

    @POST("rides/{rideId}/start-arriving")
    suspend fun startArriving(
        @retrofit2.http.Path("rideId") rideId: Long,
        @Body request: RideProgressRequest = RideProgressRequest()
    ): Response<RideStatusResponse>

    @POST("rides/{rideId}/arrived")
    suspend fun driverArrived(
        @retrofit2.http.Path("rideId") rideId: Long,
        @Body request: RideProgressRequest = RideProgressRequest()
    ): Response<RideStatusResponse>

    @POST("rides/{rideId}/start")
    suspend fun startRide(
        @retrofit2.http.Path("rideId") rideId: Long,
        @Body request: RideProgressRequest = RideProgressRequest()
    ): Response<RideStatusResponse>

    @POST("rides/{rideId}/arrive-destination")
    suspend fun arriveDestination(
        @retrofit2.http.Path("rideId") rideId: Long,
        @Body request: RideProgressRequest = RideProgressRequest()
    ): Response<RideStatusResponse>

    @POST("rides/{rideId}/complete")
    suspend fun completeRide(
        @retrofit2.http.Path("rideId") rideId: Long,
        @Body request: RideProgressRequest = RideProgressRequest()
    ): Response<RideStatusResponse>

    @POST("rides/{rideId}/cancel")
    suspend fun cancelRide(
        @retrofit2.http.Path("rideId") rideId: Long,
        @Body request: CancelRideRequest
    ): Response<RideStatusResponse>

    @POST("rides/{rideId}/handover/pickup")
    suspend fun createPickupHandover(
        @retrofit2.http.Path("rideId") rideId: Long,
        @Body request: CreateHandoverReportRequest
    ): Response<JsonElement>

    @POST("rides/{rideId}/handover/dropoff")
    suspend fun createDropoffHandover(
        @retrofit2.http.Path("rideId") rideId: Long,
        @Body request: CreateHandoverReportRequest
    ): Response<JsonElement>

    @POST("rides/{rideId}/handover/pickup/confirm")
    suspend fun confirmPickupHandover(
        @retrofit2.http.Path("rideId") rideId: Long,
        @Body request: ConfirmHandoverRequest
    ): Response<JsonElement>

    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>
    @POST("api/auth/facebook")
    suspend fun loginWithFacebook(
        @Body request: LoginRequest
    ): Response<LoginResponse>
    @POST("api/auth/link-phone")
    suspend fun linkPhone(
        @Body request: LinkPhoneRequest
    ): Response<LoginResponse>
    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<LoginResponse>
    @POST("api/auth/google")
    suspend fun loginWithGoogle(
        @Body request: LoginRequest
    ): Response<LoginResponse>
    @GET("api/auth/test")
    suspend fun testBackend(): Response<String>
    @GET("api/profile")
    suspend fun getProfile(
        @Header("Authorization") token: String,
        @Query("role") role: String
    ): Response<ProfileResponse>

    @PUT("api/profile/avatar")
    suspend fun updateAvatar(
        @Header("Authorization") token: String,
        @Query("role") role: String,
        @Body request: AvatarRequest
    ): Response<ProfileResponse>

    @Multipart
    @POST("api/profile/avatar/upload")
    suspend fun uploadAvatar(
        @Header("Authorization") token: String,
        @Query("role") role: String,
        @Part file: MultipartBody.Part
    ): Response<ProfileResponse>

}
