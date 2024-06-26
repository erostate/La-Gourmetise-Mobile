package com.example.mobileappnewv.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface RatingDAO {
    // Insert a rating into the database
    @Insert
    suspend fun insert(rating: Rating)

    // Insert a rating into the database from each field
    @Query("INSERT INTO ratings (candidateId, code, companyRating, productRating, priceRating, staffRating, date) VALUES (:candidateId, :code, :companyRating, :productRating, :priceRating, :staffRating, :date)")
    suspend fun insertRating(candidateId: Int, code: String, companyRating: Int, productRating: Int, priceRating: Int, staffRating: Int, date: String)

    // Update a rating in the database with code
    @Query("UPDATE ratings SET companyRating = :companyRating, productRating = :productRating, priceRating = :priceRating, staffRating = :staffRating, date = :date WHERE code = :code")
    suspend fun updateRating(code: String, companyRating: Int, productRating: Int, priceRating: Int, staffRating: Int, date: String)

    // Update code by current code
    @Query("UPDATE ratings SET code = :newCode WHERE code = :code")
    suspend fun updateCode(code: String, newCode: String)

    // Delete all ratings from the database
    @Query("DELETE FROM ratings")
    suspend fun deleteAllRatings()

    // Delete a rating by code from the database
    @Query("DELETE FROM ratings WHERE code = :code")
    suspend fun deleteRatingByCode(code: String)

    // Get all ratings from the database
    @Query("SELECT * FROM ratings")
    suspend fun getAllRatings(): List<Rating>

    // Get the last rating from the database
    @Query("SELECT * FROM ratings ORDER BY id DESC LIMIT 1")
    suspend fun getLastRating(): Rating

    // Get all ratings from the database that have not been exported
    @Query("SELECT * FROM ratings WHERE exported = 0")
    suspend fun getUnexportedRatings(): List<Rating>

    // Check if the code is already in the database
    @Query("SELECT * FROM ratings WHERE code = :code")
    suspend fun getRatingByCode(code: String): Rating

    // Export a rating to the server
    @Query("UPDATE ratings SET exported = 1 WHERE code = :code")
    suspend fun exportRating(code: String)

    // Count the number of ratings where exported = 0 in the database
    @Query("SELECT COUNT(*) FROM ratings WHERE exported = 0")
    suspend fun countUnexportedRatings(): Int
}