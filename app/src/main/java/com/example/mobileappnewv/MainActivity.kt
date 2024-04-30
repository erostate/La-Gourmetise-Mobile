package com.example.mobileappnewv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.room.migration.Migration
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val db = initDb()
        val ratingDAO = db.ratingDAO()

        val btnViewMyRatings: Button = findViewById(R.id.viewMyRatings)
        // Check if the user has a rating in the database
        lifecycleScope.launch {
            val ratings = ratingDAO.getUnexportedRatings()
            if (ratings.isNotEmpty()) {
                btnViewMyRatings.isEnabled = true
            }
        }

        btnViewMyRatings.setOnClickListener {
            val myRatingsPage = Intent(this, MyRatingsActivity::class.java)
            startActivity(myRatingsPage)
        }

        val btnValidBakeryCode: Button = findViewById(R.id.validBakeryCode)
        btnValidBakeryCode.setOnClickListener {
            val code1: String = findViewById<EditText>(R.id.code1).text.toString()
            val code2: String = findViewById<EditText>(R.id.code2).text.toString()
            val code3: String = findViewById<EditText>(R.id.code3).text.toString()
            val code4: String = findViewById<EditText>(R.id.code4).text.toString()
            val code5: String = findViewById<EditText>(R.id.code5).text.toString()

            if (code1.isEmpty() || code2.isEmpty() || code3.isEmpty() || code4.isEmpty() || code5.isEmpty()) {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Erreur")
                builder.setMessage("Merci d'entrer un code valide.")
                builder.setPositiveButton("OK", null)
                builder.show()
                return@setOnClickListener
            }

            val candidateId: Int = code1.toInt()
            val code = "$code1-$code2-$code3-$code4-$code5"

            // Check if the code is already in the database
            lifecycleScope.launch {
                val rating = ratingDAO.getRatingByCode(code)
                if (rating != null) {
                    val builder = AlertDialog.Builder(this@MainActivity)
                    builder.setTitle("Erreur")
                    builder.setMessage("Ce code a déjà été saisi.\nVoulez-vous le modifier ?\n\nID: ${rating.id}\nCode: ${rating.code}")
                    builder.setPositiveButton("Oui") { _, _ ->
                        val ratingPage = Intent(this@MainActivity, RatingActivity::class.java)
                        startActivity(ratingPage)
                        finish()
                    }
                    builder.setNegativeButton("Non", null)
                    builder.show()
                    return@launch
                }
            }

            // Insert the code into the database
            val rating = Rating(0, candidateId, code, null, null, null, null, 0, "")
            lifecycleScope.launch {
                ratingDAO.insert(rating)
            }

            // Change to the rating page
//            val ratingPage = Intent(this, RatingActivity::class.java)
//            startActivity(ratingPage)
//            finish()
        }
    }

    private fun initDb(): AppDatabase {
        return Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "mydatabase.db"
        ).addMigrations(AppDatabase.MIGRATION_1_2).build()
    }
}