package com.example.mobileappnewv.DAO

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ErrorRatingDAO {
    // Insert a rating into the database
    @Insert
    suspend fun insert(errorRating: ErrorRating)

    // Insert an error rating into the database
    @Query("INSERT INTO error_ratings (candidateId, code, companyRating, productRating, priceRating, staffRating, status) VALUES (:candidateId, :code, :companyRating, :productRating, :priceRating, :staffRating, :status)")
    suspend fun insertErrorRatingByStatus(candidateId: Int, code: String, companyRating: Int?, productRating: Int?, priceRating: Int?, staffRating: Int?, status: String)

    // Delete all error ratings from the database
    @Query("DELETE FROM error_ratings")
    suspend fun deleteAll()

    // Delete an error rating by code from the database
    @Query("DELETE FROM error_ratings WHERE code = :code")
    suspend fun deleteByCode(code: String)

    // Count the number of error ratings in the database
    @Query("SELECT COUNT(*) FROM error_ratings")
    suspend fun count(): Int

    // Get element by id from the database
    @Query("SELECT * FROM error_ratings WHERE id = :id")
    suspend fun getElementById(id: Int): ErrorRating

    // Get element by code from the database
    @Query("SELECT * FROM error_ratings WHERE code = :code")
    suspend fun getElementByCode(code: String): ErrorRating

    // Get the first element from the database
    @Query("SELECT * FROM error_ratings ORDER BY id ASC LIMIT 1")
    suspend fun getFirstElement(): ErrorRating
}