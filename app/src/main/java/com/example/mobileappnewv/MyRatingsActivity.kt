package com.example.mobileappnewv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch

class MyRatingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_ratings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val db = initDb()
        val ratingDAO = db.ratingDAO()

        val containerAllRatings = findViewById<ListView>(R.id.containerAllRatings)

        // Get all ratings from the database
        lifecycleScope.launch {
            val ratings = ratingDAO.getAllRatings()
            var index = 1
            for (rating in ratings) {
                val candidateId = rating.candidateId
                val code = rating.code
                val companyRating = rating.companyRating
                val productRating = rating.productRating
                val priceRating = rating.priceRating
                val staffRating = rating.staffRating
                val date = rating.date

                // TODO: Found a solution to add the rating to the containerAllRatings

                index++
            }
        }

        val deleteBakeryRating = findViewById<Button>(R.id.deleteBakeryRating)
        deleteBakeryRating.setOnClickListener {
            // Delete all ratings from the database
            lifecycleScope.launch {
                ratingDAO.deleteAllRatings()
            }
        }

        val btnGoBack = findViewById<Button>(R.id.goBack)
        btnGoBack.setOnClickListener {
            val mainPage = Intent(this, MainActivity::class.java)
            startActivity(mainPage)
            finish()
        }
    }

    private fun initDb(): AppDatabase {
        return Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "mydatabase.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()
    }
}