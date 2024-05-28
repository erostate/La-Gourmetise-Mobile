package com.example.mobileappnewv

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.mobileappnewv.Api.ApiClient
import kotlinx.coroutines.launch

class ErrorRatingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_error_rating)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<Button>(R.id.goBack)
        btnBack.setOnClickListener {
            val intent = Intent(this, MyErrorRatingsActivity::class.java)
            startActivity(intent)
        }

        val exportedCode = intent.getStringExtra("code")
        if (exportedCode == null) {
            val intent = Intent(this, MyErrorRatingsActivity::class.java)
            startActivity(intent)
        }
        val separatedCode = exportedCode?.split("-")

        val db = initDb()
        val ratingDAO = db.ratingDAO()
        val errorRatingDAO = db.errorRatingDAO()

        val lastCode = findViewById<TextView>(R.id.lastCode)
        val newCode1 = findViewById<TextView>(R.id.newCode1)
        val newCode2 = findViewById<TextView>(R.id.newCode2)
        val newCode3 = findViewById<TextView>(R.id.newCode3)
        val newCode4 = findViewById<TextView>(R.id.newCode4)
        val newCode5 = findViewById<TextView>(R.id.newCode5)

        val validSendBakeryCode = findViewById<Button>(R.id.validSendBakeryCode)

        lastCode.text = exportedCode
        newCode1.text = separatedCode?.get(0)
        newCode2.text = separatedCode?.get(1)
        newCode3.text = separatedCode?.get(2)
        newCode4.text = separatedCode?.get(3)
        newCode5.text = separatedCode?.get(4)

        validSendBakeryCode.setOnClickListener {
            lifecycleScope.launch {
                val newCode = "${newCode1.text}-${newCode2.text}-${newCode3.text}-${newCode4.text}-${newCode5.text}"

                val rating = ratingDAO.getRatingByCode(lastCode.text?.toString() ?: "")

                val apiClient = ApiClient()

                val call = apiClient.exportRatings(
                    this@ErrorRatingActivity,
                    newCode,
                    rating.companyRating?:0,
                    rating.productRating?:0,
                    rating.priceRating?:0,
                    rating.staffRating?:0
                )
                when (call) {
                    "success" -> {
                        ratingDAO.updateCode(lastCode.text.toString(), newCode)
                        errorRatingDAO.deleteByCode(lastCode.text.toString())
                        ratingDAO.exportRating(newCode)

                        showAlert(
                            this@ErrorRatingActivity,
                            "Succès",
                            "Le code a été modifié avec succès.",
                            "Ok",
                            {
                                val intent = Intent(this@ErrorRatingActivity, MyRatingsActivity::class.java)
                                startActivity(intent)
                            },
                        )
                    }
                    "code-not-found" -> {
                        val builder = AlertDialog.Builder(this@ErrorRatingActivity)
                        builder.setTitle("Erreur")
                        builder.setMessage("Le code saisi n'a pas été trouvé.")
                        builder.setPositiveButton("Ok") { _, _ -> }
                        builder.show()
                    }
                    "code-not-gived" -> {
                        val builder = AlertDialog.Builder(this@ErrorRatingActivity)
                        builder.setTitle("Erreur")
                        builder.setMessage("Le code saisi n'a pas été donné par une boutique.")
                        builder.setPositiveButton("Ok") { _, _ -> }
                        builder.show()
                    }
                    "code-already-used" -> {
                        val builder = AlertDialog.Builder(this@ErrorRatingActivity)
                        builder.setTitle("Erreur")
                        builder.setMessage("Le code saisi a déjà été utilisé.")
                        builder.setPositiveButton("Ok") { _, _ -> }
                        builder.show()
                    }
                    else -> {
                        val builder = AlertDialog.Builder(this@ErrorRatingActivity)
                        builder.setTitle("Erreur")
                        builder.setMessage("Une erreur inconnue est survenue.")
                        builder.setPositiveButton("Ok") { _, _ -> }
                        builder.show()
                    }
                }
            }
        }
    }

    private fun initDb(): AppDatabase {
        return AppDatabase.initDb(this)
    }

    private fun showAlert(
        context: Context,
        title: String,
        message: String,
        positiveButtonText: String = "OK",
        positiveAction: (() -> Unit)? = null,
        negativeButtonText: String? = null,
        negativeAction: (() -> Unit)? = null
    ) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        if (positiveAction != null) {
            builder.setPositiveButton(positiveButtonText) { _, _ -> positiveAction.invoke() }
        }
        negativeButtonText?.let {
            builder.setNegativeButton(it) { _, _ -> negativeAction?.invoke() }
        }
        builder.show()
    }
}