package com.example.mobileappnewv.Api

import android.content.Context
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets

class ApiClient {
    private val BASE_URL = "http://10.0.2.2:8000/api/"

    suspend fun exportRatings(context: Context, code: String, companyRating: Int, productRating: Int, priceRating: Int, staffRating: Int): String {
        return withContext(Dispatchers.IO) {
            var statement = "cant_access_to_request"

            val url: URL = URI.create(BASE_URL + "ratings?code=" + code).toURL()
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection

            try {
                // Request method: POST
                connection.requestMethod = "POST"

                // Set the content type
                connection.setRequestProperty("Content-Type", "application/ld+json")
                connection.setRequestProperty("Accept", "application/ld+json")

                // Write the request body
                val os = connection.outputStream
                val writer = BufferedWriter(OutputStreamWriter(os, StandardCharsets.UTF_8))
                val jsonParam = JSONObject().apply {
                    put("companyRating", companyRating)
                    put("productRating", productRating)
                    put("priceRating", priceRating)
                    put("staffRating", staffRating)
                }
                writer.write(jsonParam.toString())
                writer.flush()
                writer.close()
                os.close()

                // Get the response code
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) { // success
                    val `in` = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = StringBuilder()
                    var inputLine: String?
                    while (`in`.readLine().also { inputLine = it } != null) {
                        response.append(inputLine)
                    }
                    `in`.close()

                    println("_____Exported rating_____")
                    println(response.toString())

                    // If response body is : "result" = "rating-not-posted
                    statement = when {
                        response.toString().contains("rating-not-posted") -> "rating-not-posted"
                        response.toString().contains("code-not-gived") -> "code-not-gived"
                        response.toString().contains("code-already-used") -> "code-already-used"
                        response.toString().contains("code-not-found") -> "code-not-found"
                        else -> "success"
                    }

//                    withContext(Dispatchers.Main) {
//                        val builder = AlertDialog.Builder(context)
//                        builder.setTitle("testtttt")
//                        builder.setMessage("Coucou toi: $statement")
//                        builder.setPositiveButton("Ok", null)
//                        builder.show()
//                    }
                }
            } finally {
                connection.disconnect()
            }

            statement
        }
    }
}