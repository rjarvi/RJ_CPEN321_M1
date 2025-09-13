package com.cpen321.usermanagement.data.remote.api

import com.cpen321.usermanagement.data.remote.dto.NewsRequest
import com.cpen321.usermanagement.data.remote.dto.NewsResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface NewsInterface {
    @POST("news/hobbies")
    suspend fun getNewsByHobbies(@Body request: NewsRequest): NewsResponse
}