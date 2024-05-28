package com.example.mobileappnewv

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
import com.example.mobileappnewv.DAO.ErrorRating
import com.example.mobileappnewv.DAO.ErrorRatingDAO
import kotlinx.coroutines.launch

class MyErrorRatingsActivity : AppCompatActivity() {
    var ELEMENTNUM = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_my_error_ratings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack = findViewById<Button>(R.id.goBack)
        btnBack.setOnClickListener {
            val intent = Intent(this, MyRatingsActivity::class.java)
            startActivity(intent)
        }

        val btnPrev = findViewById<Button>(R.id.previousRating)
        val btnNext = findViewById<Button>(R.id.nextRating)
        val editRating = findViewById<Button>(R.id.editRating)
        val deleteRating = findViewById<Button>(R.id.deleteRating)

        val db = initDb()
        val ratingDAO = db.ratingDAO()
        val errorRatingDAO = db.errorRatingDAO()

        btnNext.setOnClickListener {
            lifecycleScope.launch {
                loadElement(errorRatingDAO)
            }
        }
        btnPrev.setOnClickListener {
            lifecycleScope.launch {
                if (ELEMENTNUM > 1) {
                    ELEMENTNUM -= 1
                    loadElement(errorRatingDAO)
                }
            }
        }

        lifecycleScope.launch {
            countRating(errorRatingDAO)

            val firstElem = getFirstElement()
            if (firstElem == null) {
                val intent = Intent(this@MyErrorRatingsActivity, MyRatingsActivity::class.java)
                startActivity(intent)
                finish()
            }
            if (firstElem != null) {
                if (firstElem.id != ELEMENTNUM) {
                    ELEMENTNUM = firstElem.id
                }
            }

            loadElement(errorRatingDAO)
        }

        editRating.setOnClickListener {
            val intent = Intent(this, ErrorRatingActivity::class.java)
            intent.putExtra("code", findViewById<TextView>(R.id.codeRating).text)
            startActivity(intent)
        }

        deleteRating.setOnClickListener {
            lifecycleScope.launch {
                errorRatingDAO.deleteByCode(findViewById<TextView>(R.id.codeRating).text.toString())
                ratingDAO.deleteRatingByCode(findViewById<TextView>(R.id.codeRating).text.toString())

                val builder = AlertDialog.Builder(this@MyErrorRatingsActivity)
                builder.setTitle("Note supprimée")
                builder.setMessage("La note a été supprimée avec succès.")
                builder.setPositiveButton("Ok") { dialog, _ ->
                    dialog.dismiss()
                    lifecycleScope.launch {
                        loadElement(errorRatingDAO)
                    }
                }
                builder.show()
            }
        }
    }

    private fun initDb(): AppDatabase {
        return AppDatabase.initDb(this)
    }

    private suspend fun countRating(errorRatingDAO: ErrorRatingDAO) {
        if (errorRatingDAO.count() == 0) {
            val homePage = Intent(this, MyRatingsActivity::class.java)
            startActivity(homePage)
        }
    }

    // Get an element from the database
    private suspend fun getElement(errorRatingDAO: ErrorRatingDAO): ErrorRating? {
        val element = errorRatingDAO.getElementById(ELEMENTNUM)
        if (element != null) {
            this.ELEMENTNUM++
            return element
        } else {
            this.ELEMENTNUM = getFirstElement()?.id?: 1
            return getElement(errorRatingDAO)
        }
    }

    private suspend fun getFirstElement(): ErrorRating? {
        val db = initDb()
        val errorRatingDAO = db.errorRatingDAO()

        val element = errorRatingDAO.getFirstElement()?: null

        return element
    }

    private suspend fun loadElement(errorRatingDAO: ErrorRatingDAO) {
        countRating(errorRatingDAO)

        val totalNote = findViewById<TextView>(R.id.totalNote)
        val codeRating = findViewById<TextView>(R.id.codeRating)
        val noteRating = findViewById<TextView>(R.id.noteRating)
        val detailNoteRating = findViewById<TextView>(R.id.detailNoteRating)
        val reasonRating = findViewById<TextView>(R.id.reasonRating)

        val countTotalNote = "Total de note: " + errorRatingDAO.count().toString()
        totalNote.text = countTotalNote

        val element = getElement(errorRatingDAO)
        println("_____ELEMENT222222_____")
        println(element)

        if (element != null) {
            val companyRating = element.companyRating?: 0
            val productRating = element.productRating?: 0
            val priceRating = element.priceRating?: 0
            val staffRating = element.staffRating?: 0
            val note = (companyRating + productRating + priceRating + staffRating).toString() + "/20"
            val detailNote = "-Boulangerie: " + companyRating.toString() + "/5\n" +
                    "-Produits: " + productRating.toString() + "/5\n" +
                    "-Prix: " + priceRating.toString() + "/5\n" +
                    "-Equipe: " + staffRating.toString() + "/5"
            var status = ""
            status = when (element.status) {
                "code-not-found" -> {
                    "Code non trouvé"
                }
                "code-not-gived" -> {
                    "Code non donné par la boutique"
                }
                "code-already-used" -> {
                    "Code déjà utilisé"
                }
                else -> {
                    "Erreur inconnue"
                }
            }
            codeRating.text = element.code
            noteRating.text = note
            detailNoteRating.text = detailNote
            reasonRating.text = status
        }
    }
}