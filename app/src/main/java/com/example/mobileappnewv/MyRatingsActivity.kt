package com.example.mobileappnewv

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.mobileappnewv.Api.ApiClient
import com.example.mobileappnewv.DAO.ErrorRatingDAO
import com.example.mobileappnewv.DAO.Rating
import com.example.mobileappnewv.DAO.RatingDAO
import kotlinx.coroutines.launch
import okhttp3.internal.http2.ErrorCode
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MyRatingsActivity : AppCompatActivity() {
    inner class MyArrayAdapter(context: Context, resource: Int, objects: ArrayList<String>) :
        ArrayAdapter<String>(context, resource, objects) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.listview_color_layout, parent, false)

            val itemTextView = view.findViewById<TextView>(R.id.item_text)
            itemTextView.text = getItem(position) // Update the item_text TextView with the value

            val indexTextView = view.findViewById<TextView>(R.id.item_index)
            indexTextView.text = (position + 1).toString() // Update the item_index TextView with the index

            return view
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
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
        val errorRatingDAO = db.errorRatingDAO()

        val containerAllRatings = findViewById<ListView>(R.id.containerAllRatings)
        val allRatingsArray = ArrayList<String>()

        val adapter = MyArrayAdapter(this, R.layout.listview_color_layout, allRatingsArray)
        containerAllRatings.adapter = adapter

        // Get all ratings from the database
        lifecycleScope.launch {
            if (ratingDAO.countUnexportedRatings() <= 0) {
                val intent = Intent(this@MyRatingsActivity, MainActivity::class.java)
                startActivity(intent)
            }

            val ratings = ratingDAO.getUnexportedRatings()
            var index = 1
            for (rating in ratings) {
                val candidateId = rating.candidateId
                val code = rating.code
                val companyRating = rating.companyRating ?: 0
                val productRating = rating.productRating?: 0
                val priceRating = rating.priceRating?: 0
                val staffRating = rating.staffRating?: 0
                val finalRating = companyRating + productRating + priceRating + staffRating
                val date = rating.date
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
                val formattedDate = if (date == "") {
                    "Non défini"
                } else {
                    val dateTime = LocalDateTime.parse(date, formatter)
                    "${dateTime.dayOfMonth}/${dateTime.monthValue}/${dateTime.year} à ${dateTime.hour}h${dateTime.minute}"
                }

                val ratingIsError = ratingIsAnError(ratingDAO, errorRatingDAO, code)
                val strRatingIsError = if (ratingIsError) {
                    "\n(Erreur: Cliquer pour voir plus de détails)"
                } else {
                    ""
                }

                allRatingsArray.add("ID candidat: $candidateId\nCode: $code\nNote: $finalRating/20\nDate: $formattedDate $strRatingIsError")
                // Click on a rating to see more details
                containerAllRatings.setOnItemClickListener { _, _, position, _ ->
                    val detailPage = Intent(this@MyRatingsActivity, MyErrorRatingsActivity::class.java)
                    startActivity(detailPage)
                }

                adapter.notifyDataSetChanged()

                index++
            }
        }

        val postBakeryRating = findViewById<Button>(R.id.postBakeryRating)
        postBakeryRating.setOnClickListener {
            // Export all ratings to the server
            lifecycleScope.launch {
                val ratings = ratingDAO.getUnexportedRatings()
                if (ratings.count() >= 5) {
                    exportRatings(ratingDAO)
                } else {
                    showAlert(
                        context = this@MyRatingsActivity,
                        title = "Erreur",
                        message = "Vous devez saisir au moins 5 notes pour les exporter."
                    )
                }
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
        return AppDatabase.initDb(this)
    }

    private suspend fun exportRatings(ratingDAO: RatingDAO) {
        val loadingDialog = Dialog(this)
        loadingDialog.setContentView(R.layout.loading_layout)
        loadingDialog.window!!.setLayout(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        loadingDialog.show()

        val apiClient = ApiClient()

        val ratings = ratingDAO.getUnexportedRatings()
        if (ratings.count() >= 5) {
            lifecycleScope.launch {
                val db = initDb()
                val errorRatingDAO = db.errorRatingDAO()

                val totalElements = ratings.count()
                var statusSuccess = 0
                var statusCodeNotFound = 0
                var statusCodeNotGived = 0
                var statusCodeAlreadyUsed = 0
                var statusError = 0

                for (rating in ratings) {
                    val companyRating = rating.companyRating ?: 0
                    val productRating = rating.productRating?: 0
                    val priceRating = rating.priceRating?: 0
                    val staffRating = rating.staffRating?: 0

                    val call = apiClient.exportRatings(
                        this@MyRatingsActivity,
                        rating.code,
                        companyRating,
                        productRating,
                        priceRating,
                        staffRating
                    )

                    when (call) {
                        "success" -> {
                            statusSuccess++
                            ratingDAO.exportRating(rating.code)
                        }
                        "code-not-found" -> {
                            statusCodeNotFound++
                            errorRatingDAO.insertErrorRatingByStatus(rating.candidateId, rating.code, companyRating, productRating, priceRating, staffRating, "code-not-found")
                        }
                        "code-not-gived" -> {
                            statusCodeNotGived++
                            errorRatingDAO.insertErrorRatingByStatus(rating.candidateId, rating.code, companyRating, productRating, priceRating, staffRating, "code-not-gived")
                        }
                        "code-already-used" -> {
                            statusCodeAlreadyUsed++
                            errorRatingDAO.insertErrorRatingByStatus(rating.candidateId, rating.code, companyRating, productRating, priceRating, staffRating, "code-already-used")
                        }
                        else -> {
                            statusError++
                            errorRatingDAO.insertErrorRatingByStatus(rating.candidateId, rating.code, companyRating, productRating, priceRating, staffRating, "error")
                        }
                    }
                }

                if (statusSuccess + statusCodeNotFound + statusCodeNotGived + statusCodeAlreadyUsed + statusError == totalElements) {
                    if (statusSuccess == totalElements) {
                        showAlert(
                            context = this@MyRatingsActivity,
                            title = "Succès",
                            message = "Toutes les notes ont bien été exportées.",
                            positiveAction = {
                                val mainPage = Intent(this@MyRatingsActivity, MainActivity::class.java)
                                startActivity(mainPage)
                                finish()
                            }
                        )
                    } else {
                        var strMessage = "Une erreur est survenue lors de l'exportation des notes :\n"
                        if (statusCodeNotFound > 0) {
                            strMessage += "- $statusCodeNotFound code(s) saisi(s) n'existe(nt) pas.\n"
                        }
                        if (statusCodeNotGived > 0) {
                            strMessage += "- $statusCodeNotGived code(s) saisi(s) n'a(ont) pas été donné(s) par la boutique.\n"
                        }
                        if (statusCodeAlreadyUsed > 0) {
                            strMessage += "- $statusCodeAlreadyUsed code(s) saisi(s) a(ont) déjà été utilisé(s).\n"
                        }
                        if (statusError > 0) {
                            strMessage += "- $statusError erreur(s) inconnue(s).\n"
                        }
                        showAlert(
                            context = this@MyRatingsActivity,
                            title = "Erreur",
                            message = strMessage,
                            negativeButtonText = "Voir les détails",
                            negativeAction = {
                                val detailsPage = Intent(this@MyRatingsActivity, MyErrorRatingsActivity::class.java)
                                startActivity(detailsPage)
                                finish()
                            }
                        )
                    }
                } else {
                    showAlert(
                        context = this@MyRatingsActivity,
                        title = "Erreur /2",
                        message = "Une erreur est survenue lors de l'exportation des notes."
                    )
                }

                loadingDialog.dismiss()
            }
        } else {
            loadingDialog.dismiss()

            showAlert(
                context = this@MyRatingsActivity,
                title = "Erreur",
                message = "Vous devez saisir au moins 5 notes pour les exporter."
            )
        }
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

    private suspend fun ratingIsAnError(ratingDAO: RatingDAO, errorRatingDAO: ErrorRatingDAO, code: String): Boolean {
        val rating = ratingDAO.getRatingByCode(code)
        val errorRating = errorRatingDAO.getElementByCode(code)

        return errorRating != null
    }
}