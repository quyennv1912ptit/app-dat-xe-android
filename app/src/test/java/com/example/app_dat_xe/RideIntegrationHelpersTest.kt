package com.example.app_dat_xe

import com.example.app_dat_xe.data.remote.ApiEnvelopeConverterFactory
import com.example.app_dat_xe.data.remote.parseStompFrame
import com.example.app_dat_xe.feature.ride.stepFromStatus
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

class RideIntegrationHelpersTest {
    @Test
    fun stepFromStatusMatchesRideProgress() {
        assertEquals(0, stepFromStatus("DRIVER_ASSIGNED"))
        assertEquals(1, stepFromStatus("DRIVER_ARRIVING"))
        assertEquals(2, stepFromStatus("DRIVER_ARRIVED"))
        assertEquals(3, stepFromStatus("VEHICLE_HANDOVER"))
        assertEquals(4, stepFromStatus("IN_PROGRESS"))
        assertEquals(5, stepFromStatus("ARRIVED_DESTINATION"))
        assertEquals(6, stepFromStatus("VEHICLE_RETURNED"))
        assertEquals(0, stepFromStatus("UNKNOWN"))
    }

    @Test
    fun parsesStompCommandHeadersAndBody() {
        val frame = parseStompFrame(
            "MESSAGE\r\ndestination:/user/queue/trip\r\nmessage-id:abc-123\r\n\r\n{\"type\":\"RIDE_OFFER\"}"
        )

        assertNotNull(frame)
        assertEquals("MESSAGE", frame?.command)
        assertEquals("/user/queue/trip", frame?.headers?.get("destination"))
        assertEquals("abc-123", frame?.headers?.get("message-id"))
        assertEquals("{\"type\":\"RIDE_OFFER\"}", frame?.body)
    }

    @Test
    fun unwrapsApiDataEnvelopeBeforeDeserializing() {
        val retrofit = Retrofit.Builder().baseUrl("https://example.test/").build()
        val factory = ApiEnvelopeConverterFactory()
        @Suppress("UNCHECKED_CAST")
        val converter = factory.responseBodyConverter(
            EnvelopeTestPayload::class.java,
            emptyArray<Annotation>(),
            retrofit
        ) as Converter<okhttp3.ResponseBody, EnvelopeTestPayload>
        val responseBody = """{"success":true,"data":{"value":"ride"}}"""
            .toResponseBody("application/json".toMediaType())

        assertEquals("ride", converter.convert(responseBody)?.value)
    }

    data class EnvelopeTestPayload(val value: String)
}
