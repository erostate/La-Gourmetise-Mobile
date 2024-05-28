package com.example.mobileappnewv.DAO

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "error_ratings")
data class ErrorRating(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val candidateId: Int,
    val code: String,
    val companyRating: Int?,
    val productRating: Int?,
    val priceRating: Int?,
    val staffRating: Int?,
    val status: String
)