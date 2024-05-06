package com.example.mobileappnewv.Api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiClient {
    private val BASE_URL = "http://10.0.2.2:8000/api/"
    private var retrofit: Retrofit? = null

    fun getClient(): ApiService {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return retrofit!!.create(ApiService::class.java)
    }
}