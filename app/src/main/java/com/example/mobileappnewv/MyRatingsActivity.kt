package com.example.mobileappnewv

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.example.mobileappnewv.Api.ApiClient
import com.example.mobileappnewv.Api.RatingBody
import kotlinx.coroutines.launch
import org.chromium.base.Callback
import org.chromium.base.Log
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.system.exitProcess

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
                    val builder = androidx.appcompat.app.AlertDialog.Builder(this@MyRatingsActivity)
                    builder.setTitle("Erreur")
                    builder.setMessage("Vous devez saisir au moins 5 notes pour les exporter.")
                    builder.setPositiveButton("OK", null)
                    builder.show()
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
        val apiClient = ApiClient()
        val apiService = apiClient.getClient()

        val ratings = ratingDAO.getUnexportedRatings()
        if (ratings.count() >= 5) {
            for (rating in ratings) {
//                ratingDAO.exportRating(rating.code)

                val companyRating = rating.companyRating ?: 0
                val productRating = rating.productRating?: 0
                val priceRating = rating.priceRating?: 0
                val staffRating = rating.staffRating?: 0
                val ratingBody = RatingBody(rating.code, companyRating, productRating, priceRating, staffRating)
                val call = apiService.exportRating(ratingBody)
                call.enqueue(object : retrofit2.Callback<Rating> {
                    override fun onResponse(call: Call<Rating>, response: Response<Rating>) {
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            Log.d("API_RESPONSE", "Response Body: $responseBody")
                        } else {
                            Log.e("API_ERROR", "Response Error: ${response.errorBody()}")
                        }
                    }

                    override fun onFailure(call: Call<Rating>, t: Throwable) {
                        println("_____FAILURE:_____")
                        Log.e("API_FAILURE", "Failure: $t")
                    }
                })
            }

//            val builder = androidx.appcompat.app.AlertDialog.Builder(this@MyRatingsActivity)
//            builder.setTitle("Succès")
//            builder.setMessage("Les notes ont bien été exportées.")
//            builder.setPositiveButton("OK", null)
//            builder.show()
        }
    }
}