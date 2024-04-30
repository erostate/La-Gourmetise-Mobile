package com.example.mobileappnewv

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ratings")
data class Rating(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val candidateId: Int,
    val code: String,
    val companyRating: Int?,
    val productRating: Int?,
    val priceRating: Int?,
    val staffRating: Int?,
    val exported: Int = 0,
    val date: String
)