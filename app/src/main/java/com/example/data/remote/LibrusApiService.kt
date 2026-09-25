package com.example.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

interface LibrusApiService {

    @FormUrlEncoded
    @POST("OAuth/Token")
    suspend fun getOAuthToken(
        @Field("grant_type") grantType: String = "password",
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("librus_rules_accepted") librusRulesAccepted: Boolean = true
    ): Response<LibrusTokenResponse>

    @GET("2.0/Me")
    suspend fun getMe(
        @Header("Authorization") authHeader: String
    ): Response<LibrusMeResponse>
}

object LibrusApiClient {
    private const val BASE_URL = "https://api.librus.pl/"

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    val service: LibrusApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(LibrusApiService::class.java)
    }
}
