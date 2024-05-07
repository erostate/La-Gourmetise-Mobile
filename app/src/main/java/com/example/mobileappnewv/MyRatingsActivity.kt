package com.example.mobileappnewv

//import com.example.mobileappnewv.Api.RatingBody

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
import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.mobileappnewv.Api.ApiClient
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.IOException
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

        val containerAllRatings = findViewById<ListView>(R.id.containerAllRatings)
        val allRatingsArray = ArrayList<String>()

        val adapter = MyArrayAdapter(this, R.layout.listview_color_layout, allRatingsArray)
        containerAllRatings.adapter = adapter

        // Get all ratings from the database
        lifecycleScope.launch {
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

                allRatingsArray.add("ID candidat: $candidateId\nCode: $code\nNote: $finalRating/20\nDate: $formattedDate")
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
                    exportRatings(ratingDAO)
//                    val builder = androidx.appcompat.app.AlertDialog.Builder(this@MyRatingsActivity)
//                    builder.setTitle("Erreur")
//                    builder.setMessage("Vous devez saisir au moins 5 notes pour les exporter.")
//                    builder.setPositiveButton("OK", null)
//                    builder.show()
                }
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

    private suspend fun exportRatings(ratingDAO: RatingDAO) {
        val loadingDialog = Dialog(this)
        loadingDialog.setContentView(R.layout.loading_layout)
        loadingDialog.window!!.setLayout(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        loadingDialog.show()

        var statement = false

        val apiClient = ApiClient()

        val ratings = ratingDAO.getUnexportedRatings()
        if (ratings.count() >= 5) {
            for (rating in ratings) {
                val companyRating = rating.companyRating ?: 0
                val productRating = rating.productRating?: 0
                val priceRating = rating.priceRating?: 0
                val staffRating = rating.staffRating?: 0
//                val ratingBody = RatingBody(rating.code, companyRating, productRating, priceRating, staffRating)
                val call = apiClient.exportRatings(this@MyRatingsActivity, rating.code, companyRating, productRating, priceRating, staffRating)
                println("__--__RESULT__--__")
                println(call)
//                if (call == "success") {
//                    ratingDAO.exportRating(rating.code)
//
//                    val builder = AlertDialog.Builder(this@MyRatingsActivity)
//                    builder.setTitle("Succès")
//                    builder.setMessage("Les notes ont bien été exportées.")
//                    builder.setPositiveButton("Ok") { _, _ ->
//                        val mainPage = Intent(this@MyRatingsActivity, MainActivity::class.java)
//                        startActivity(mainPage)
//                        finish()
//                    }
//                    builder.show()
//
//                    break
//                } else if (call == "rating_not_posted") {
//                    val builder = AlertDialog.Builder(this@MyRatingsActivity)
//                    builder.setTitle("Erreur")
//                    builder.setMessage("Une erreur est survenue lors de l'exportation de la note.\nCode utilisé: ${rating.code}")
//                    builder.setPositiveButton("OK", null)
//                    builder.setNegativeButton("Modifier le code") { _, _ ->
//                        // TODO: Create the Activity
//                        val mainPage = Intent(this@MyRatingsActivity, MainActivity::class.java)
//                        startActivity(mainPage)
//                        finish()
//                    }
//                    builder.show()
//                } else if (call == "code-not-gived") {
//                    val builder = AlertDialog.Builder(this@MyRatingsActivity)
//                    builder.setTitle("Erreur")
//                    builder.setMessage("Le code saisi n'a pas été donné par une boulangerie.\nCode utilisé: ${rating.code}")
//                    builder.setPositiveButton("OK", null)
//                    builder.setNegativeButton("Modifier le code") { _, _ ->
//                        // TODO: Create the Activity
//                        val mainPage = Intent(this@MyRatingsActivity, MainActivity::class.java)
//                        startActivity(mainPage)
//                        finish()
//                    }
//                    builder.show()
//                } else if (call == "code-already-used") {
//                    val builder = AlertDialog.Builder(this@MyRatingsActivity)
//                    builder.setTitle("Erreur")
//                    builder.setMessage("Le code saisi a déjà été utilisé.\nCode: ${rating.code}")
//                    builder.setPositiveButton("OK", null)
//                    builder.setNegativeButton("Modifier le code") { _, _ ->
//                        // TODO: Create the Activity
//                        val mainPage = Intent(this@MyRatingsActivity, MainActivity::class.java)
//                        startActivity(mainPage)
//                        finish()
//                    }
//                    builder.show()
//                } else {
//                    val builder = AlertDialog.Builder(this@MyRatingsActivity)
//                    builder.setTitle("Erreur")
//                    builder.setMessage("Une erreur est survenue lors de l'exportation des notes.")
//                    builder.setPositiveButton("OK", null)
//                    builder.show()
//                }

                // Check if the iteration is the last one
                if (ratings.indexOf(rating) == ratings.size - 1) {
                    statement = true
                    loadingDialog.dismiss()
                }
            }
        } else {
            for (rating in ratings) {
                val companyRating = rating.companyRating ?: 0
                val productRating = rating.productRating?: 0
                val priceRating = rating.priceRating?: 0
                val staffRating = rating.staffRating?: 0

                val url = "http://10.0.2.7:8000/api/ratings?code=${rating.code}"

                val reqBody = JSONObject()
                reqBody.put("companyRating", companyRating)
                reqBody.put("productRating", productRating)
                reqBody.put("priceRating", priceRating)
                reqBody.put("staffRating", staffRating)

                val client: OkHttpClient = OkHttpClient()
                val request: okhttp3.Request = okhttp3.Request.Builder()
                    .url(url)
                    .post(
                        okhttp3.RequestBody.create(
                            okhttp3.MediaType.parse("application/ld+json"),
                            reqBody.toString()
                        )
                    )
                    .addHeader("Content-Type", "application/ld+json")
                    .addHeader("Accept", "application/ld+json")
                    .build()

                client.newCall(request).enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: IOException) {
                        println("_____REQUEST FAILED_____")
                        println(e)
                        println("_____REQUEST FAILED_____")
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        val responseData = response.body()?.string()

                        // Get the response body
                        val jsonObject = responseData?.let { JSONObject(it) }
                        val result = jsonObject?.getString("result")

                        runOnUiThread {
                            when (result) {
                                "rating-not-posted" -> {
                                    println("_____Rating not posted_____")
                                    val builder = AlertDialog.Builder(this@MyRatingsActivity)
                                    builder.setTitle("Erreur")
                                    builder.setMessage("Une erreur est survenue lors de l'exportation de la note.\nCode utilisé: ${rating.code}")
                                    builder.setPositiveButton("OK", null)
                                    builder.setNegativeButton("Modifier le code") { _, _ ->
                                        // TODO: Create the Activity
                                        val mainPage = Intent(this@MyRatingsActivity, MainActivity::class.java)
                                        startActivity(mainPage)
                                        finish()
                                    }
                                    builder.show()
                                }
                                "code-not-gived" -> {
                                    println("_____Code not gived_____")
                                    val builder = AlertDialog.Builder(this@MyRatingsActivity)
                                    builder.setTitle("Erreur")
                                    builder.setMessage("Le code saisi n'a pas été donné par une boulangerie.\nCode utilisé: ${rating.code}")
                                    builder.setPositiveButton("OK", null)
                                    builder.setNegativeButton("Modifier le code") { _, _ ->
                                        // TODO: Create the Activity
                                        val mainPage = Intent(this@MyRatingsActivity, MainActivity::class.java)
                                        startActivity(mainPage)
                                        finish()
                                    }
                                    builder.show()
                                }
                                "code-already-used" -> {
                                    println("_____Code already used_____")
                                    val builder = AlertDialog.Builder(this@MyRatingsActivity)
                                    builder.setTitle("Erreur")
                                    builder.setMessage("Le code saisi a déjà été utilisé.\nCode: ${rating.code}")
                                    builder.setPositiveButton("OK", null)
                                    builder.setNegativeButton("Modifier le code") { _, _ ->
                                        // TODO: Create the Activity
                                        val mainPage = Intent(this@MyRatingsActivity, MainActivity::class.java)
                                        startActivity(mainPage)
                                        finish()
                                    }
                                    builder.show()
                                }
                                else -> {
                                    println("_____Success_____")
                                    lifecycleScope.launch {
                                        ratingDAO.exportRating(rating.code)
                                    }

                                    val builder = AlertDialog.Builder(this@MyRatingsActivity)
                                    builder.setTitle("Succès")
                                    builder.setMessage("Les notes ont bien été exportées.")
                                    builder.setPositiveButton("Ok") { _, _ ->
                                        val mainPage = Intent(this@MyRatingsActivity, MainActivity::class.java)
                                        startActivity(mainPage)
                                        finish()
                                    }
                                    builder.show()
                                }
                            }
                        }
                    }
                })

                // Check if the iteration is the last one
                if (ratings.indexOf(rating) == ratings.size - 1) {
                    statement = true
                    loadingDialog.dismiss()
                }
            }

            statement = true
            loadingDialog.dismiss()
        }
    }
}