package com.example.songify.network

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

data class MyResponse(val message: String)
data class TokenResponse(val token: String)
data class SendOtpRequest(val email: String)
data class VerifyOtpRequest(val email: String, val otp: String)
data class FeedbackRequest(val email: String, val feedback: String)

interface ApiService {
    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("send-otp")
    fun sendOtp(@Header("x-api-key") apiKey: String, @Body body: SendOtpRequest): Call<MyResponse>

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("verify-otp")
    fun verifyOtp(@Header("x-api-key") apiKey: String, @Body body: VerifyOtpRequest): Call<TokenResponse>

    @Headers(
        "Content-Type: application/json",
        "Accept: application/json"
    )
    @POST("feedback")
    fun submitFeedback(
        @Header("x-api-key") apiKey: String,
        @Header("Authorization") token: String,
        @Body body: FeedbackRequest
    ): Call<MyResponse>

    // CHANGE: Use ResponseBody to avoid "Expected BEGIN_OBJECT but was BEGIN_ARRAY" errors
    @GET("songs")
    fun getSongs(
        @Header("x-api-key") apiKey: String,
        @Header("Authorization") token: String
    ): Call<SongResponse> // This MUST be SongResponse
}