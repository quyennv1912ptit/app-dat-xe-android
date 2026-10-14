package com.example.app_dat_xe.data.remote

import com.google.gson.annotations.SerializedName

data class DeviceTokenRequest(
    val installationId: String,
    val fcmToken: String,
    val platform: String = "ANDROID"
)

data class EstimateRideRequest(
    val vehicleId: Long,
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropoffAddress: String,
    val dropoffLat: Double,
    val dropoffLng: Double,
    val scheduledAt: String? = null
)

data class EstimateRideResponse(
    val pricingRuleId: Long = 0,
    val distanceKm: Double = 0.0,
    val estimatedDurationMinutes: Long = 0,
    val baseFare: Long = 0,
    val distanceFare: Long = 0,
    val waitingFare: Long = 0,
    val nightSurcharge: Long = 0,
    val returnFee: Long = 0,
    val estimatedFare: Long = 0
)

data class CreateRideRequest(
    val vehicleId: Long,
    val type: String = "INSTANT",
    val scheduledAt: String? = null,
    val pickupAddress: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropoffAddress: String,
    val dropoffLat: Double,
    val dropoffLng: Double,
    val paymentMethod: String = "CASH",
    val note: String? = null
)

data class CreateRideResponse(
    val id: Long,
    val status: String,
    val estimatedFare: Long,
    val createdAt: String? = null
)

data class RideStatusResponse(
    val rideId: Long,
    val previousStatus: String,
    val currentStatus: String,
    val changedAt: String? = null
)

data class DriverAvailabilityRequest(val online: Boolean)
data class RideProgressRequest(val note: String? = null)
data class CancelRideRequest(val reason: String)

data class HandoverPhotoRequest(val angle: String, val fileUrl: String)
data class ConfirmHandoverRequest(val confirmed: Boolean = true)
data class CreateHandoverReportRequest(
    val odometer: Long,
    val fuelLevel: Int? = null,
    val notes: String? = null,
    val photos: List<HandoverPhotoRequest>
)

data class MapboxGeocodingResponse(val features: List<MapboxGeocodingFeature> = emptyList())
data class MapboxGeocodingFeature(
    val center: List<Double> = emptyList(),
    @SerializedName("place_name") val placeName: String? = null,
    val text: String? = null
)

data class RideOfferPayload(
    val rideId: Long,
    val expiresAt: Long,
    val pickup: RideAddressPayload,
    val dropoff: RideAddressPayload,
    val distanceToPickupKm: Double,
    val etaToPickupMin: Int,
    val tripDistanceKm: Double,
    val estDurationMin: Int,
    val fareEstimate: Long,
    val driverEarning: Long,
    val paymentMethod: String,
    val vehicle: RideVehiclePayload,
    val customer: RideCustomerPayload,
    val note: String?,
    val serverTimeOffsetMs: Long = 0L
)

data class RideAddressPayload(val lat: Double, val lng: Double, val address: String)
data class RideVehiclePayload(val type: String, val transmission: String)
data class RideCustomerPayload(val displayName: String, val rating: Double)

data class RidePageResponse(
    val content: List<RideSummaryData> = emptyList(),
    val page: Int = 0,
    val size: Int = 0,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val hasNext: Boolean = false
)

data class RideSummaryData(
    val id: Long,
    val pickupAddress: String? = null,
    val dropoffAddress: String? = null,
    val status: String,
    val estimatedFare: Long = 0,
    val finalFare: Long? = null,
    val scheduledAt: String? = null
)

data class RideDetailData(
    val id: Long = 0,
    val customerId: Long = 0,
    val driverId: Long? = null,
    val vehicleId: Long = 0,
    val pickupAddress: String? = null,
    val pickup: RideCoordinateData = RideCoordinateData(),
    val dropoffAddress: String? = null,
    val dropoff: RideCoordinateData = RideCoordinateData(),
    val estimatedFare: Long = 0,
    val finalFare: Long? = null,
    val status: String = "",
    val handoverReports: List<RideHandoverReportData> = emptyList()
)

data class RideCoordinateData(val lat: Double? = null, val lng: Double? = null)
data class RideHandoverReportData(
    val phase: String = "",
    val confirmedByCustomer: Boolean = false,
    val odometer: Long = 0
)
