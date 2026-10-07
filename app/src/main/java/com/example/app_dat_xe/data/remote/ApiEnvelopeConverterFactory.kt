package com.example.app_dat_xe.data.remote

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type

/**
 * Backend trả {"success":true,"data":{...}}.
 * Converter này bóc lấy "data" rồi mới parse vào model (LoginResponse, ProfileResponse...).
 * Response không có phong bì thì parse nguyên như cũ.
 */
class ApiEnvelopeConverterFactory(private val gson: Gson = Gson()) : Converter.Factory() {

    private val gsonFactory = GsonConverterFactory.create(gson)

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        val adapter = gson.getAdapter(TypeToken.get(type))

        return Converter<ResponseBody, Any?> { body ->
            body.use {
                val root = JsonParser.parseString(it.string())
                val payload = if (
                    root.isJsonObject &&
                    root.asJsonObject.has("success") &&
                    root.asJsonObject.has("data")
                ) {
                    root.asJsonObject.get("data")
                } else {
                    root
                }

                if (payload == null || payload.isJsonNull) null
                else adapter.fromJsonTree(payload)
            }
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<Annotation>,
        methodAnnotations: Array<Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? =
        gsonFactory.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
}