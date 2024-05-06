package com.example.mobileappnewv.Api

data class RatingBody(
    val code: String,
    val companyRating: Int,
    val productRating: Int,
    val priceRating: Int,
    val staffRating: Int
)
