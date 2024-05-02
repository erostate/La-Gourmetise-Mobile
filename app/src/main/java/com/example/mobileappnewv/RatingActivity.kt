package com.example.mobileappnewv

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class RatingActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_rating)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val db = initDb()
        val ratingDAO = db.ratingDAO()

        var code = ""
        var candidateId = 0
        var method = ""
        var stars = intArrayOf()
        val intentCode = intent.getStringExtra("code")
        if (intentCode != null) {
            code = intentCode
            candidateId = intent.getIntExtra("candidateId", 0)
            method = "intent"
            for (i in 1..4) {
                stars += intent.getIntExtra("stars$i", 0)
            }
        } else {
            // Get the last rating from the database
            lifecycleScope.launch {
                val lastRating = ratingDAO.getLastRating()
                if (lastRating != null) {
                    code = lastRating.code
                    candidateId = lastRating.candidateId
                    method = "database"
                    stars = intArrayOf(
                        lastRating.companyRating ?: 0,
                        lastRating.productRating ?: 0,
                        lastRating.priceRating ?: 0,
                        lastRating.staffRating ?: 0
                    )
                } else {
                    val builder = AlertDialog.Builder(this@RatingActivity)
                    builder.setTitle("Erreur")
                    builder.setMessage("Aucun code trouvé.")
                    builder.setPositiveButton("Ok") { _, _ ->
                        val homePage = Intent(this@RatingActivity, MainActivity::class.java)
                        startActivity(homePage)
                        finish()
                    }
                    builder.show()
                    return@launch
                }
            }
        }

        val ratingCompanyNote = findViewById<RatingBar>(R.id.ratingCompanyNote)
        val ratingProductNote = findViewById<RatingBar>(R.id.ratingProductNote)
        val ratingPriceNote = findViewById<RatingBar>(R.id.ratingPriceNote)
        val ratingStaffNote = findViewById<RatingBar>(R.id.ratingStaffNote)

        ratingCompanyNote.rating = stars[0].toFloat()
        ratingProductNote.rating = stars[1].toFloat()
        ratingPriceNote.rating = stars[2].toFloat()
        ratingStaffNote.rating = stars[3].toFloat()

        val btnSendBakeryRating = findViewById<Button>(R.id.sendBakeryRating)
        btnSendBakeryRating.setOnClickListener {
            val companyRating = ratingCompanyNote.rating.toInt()
            val productRating = ratingProductNote.rating.toInt()
            val priceRating = ratingPriceNote.rating.toInt()
            val staffRating = ratingStaffNote.rating.toInt()

            lifecycleScope.launch {
                val today = LocalDateTime.now().toString()
                val existRating = ratingDAO.getRatingByCode(code)
                if (existRating != null) {
                    ratingDAO.updateRating(code, companyRating, productRating, priceRating, staffRating, today)
                } else {
                    val addRating = Rating(0, candidateId, code, companyRating, productRating, priceRating, staffRating, 0, today)
                    ratingDAO.insert(addRating)
                }
            }

            val builder = AlertDialog.Builder(this@RatingActivity)
            builder.setTitle("Merci")
            builder.setMessage("Votre évaluation a bien été enregistrée.")
            builder.setPositiveButton("Ok") { _, _ ->
                val mainPage = Intent(this@RatingActivity, MainActivity::class.java)
                startActivity(mainPage)
                finish()
            }
            builder.show()
        }


        // TODO: ONLY FOR DEV
        val builder = AlertDialog.Builder(this@RatingActivity)
        builder.setTitle("Code")
        builder.setMessage("Candidate ID: ${candidateId}\nVotre code est : ${code}\n\nMethod: ${method}")
        builder.setPositiveButton("OK", null)
        builder.show()

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
        ).build()
    }
}