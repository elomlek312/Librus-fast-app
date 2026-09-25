package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LibrusTokenResponse(
    @Json(name = "access_token") val accessToken: String?,
    @Json(name = "token_type") val tokenType: String?,
    @Json(name = "expires_in") val expiresIn: Long?,
    @Json(name = "refresh_token") val refreshToken: String?,
    @Json(name = "error") val error: String?,
    @Json(name = "error_description") val errorDescription: String?
)

@JsonClass(generateAdapter = true)
data class LibrusMeResponse(
    @Json(name = "Me") val me: LibrusMeData?
)

@JsonClass(generateAdapter = true)
data class LibrusMeData(
    @Json(name = "User") val user: LibrusUserData?,
    @Json(name = "Class") val schoolClass: LibrusClassData?
)

@JsonClass(generateAdapter = true)
data class LibrusUserData(
    @Json(name = "FirstName") val firstName: String?,
    @Json(name = "LastName") val lastName: String?,
    @Json(name = "Login") val login: String?
)

@JsonClass(generateAdapter = true)
data class LibrusClassData(
    @Json(name = "Number") val number: Int?,
    @Json(name = "Symbol") val symbol: String?
)
