package com.example.mobileappnewv.Api

import android.media.RouteListingPreference
import com.example.mobileappnewv.Rating
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @GET("endpoint")
    fun getItems(): Call<List<RouteListingPreference.Item>>

    @POST("endpoint")
    fun createItem(@Body newItem: RouteListingPreference.Item): Call<RouteListingPreference.Item>

    // Export a rating to the server :
    // POST '/api/ratings?code='
    // Body: companyRating int, productRating int, priceRating int, staffRating int
    @POST("exportRating")
    fun exportRating(@Body ratingBody: RatingBody): Call<Rating>
}