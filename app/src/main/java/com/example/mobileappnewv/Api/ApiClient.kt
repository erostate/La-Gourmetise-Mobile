package com.example.mobileappnewv.Api

import android.content.Context
import androidx.appcompat.app.AlertDialog
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

    fun exportRatings(context: Context, code: String, companyRating: Int, productRating: Int, priceRating: Int, staffRating: Int): Thread {
        return Thread {
            var statement = "cant_access_to_request"

            val url: URL = URI.create(BASE_URL + "ratings?code=" + code).toURL()
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection

            // Request method: POST
            connection.requestMethod = "POST"

            // Set the content type
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")

            // Write the request body
            val os = connection.outputStream
            val writer = BufferedWriter(OutputStreamWriter(os, StandardCharsets.UTF_8))
            val jsonParam = JSONObject()
            jsonParam.put("companyRating", companyRating)
            jsonParam.put("productRating", productRating)
            jsonParam.put("priceRating", priceRating)
            jsonParam.put("staffRating", staffRating)
            writer.write(jsonParam.toString())
            writer.flush()
            writer.close()
            os.close()

            // Get the response code
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) { // success
                val `in` = BufferedReader(InputStreamReader(connection.inputStream))
                var inputLine: String?
                val response = StringBuilder()
                while (`in`.readLine().also { inputLine = it } != null) {
                    response.append(inputLine)
                }
                `in`.close()

                println("_____Exported rating_____")
                println(response.toString())

                // If response body is : "result" = "rating-not-posted
                statement = if (response.toString().contains("rating-not-posted")) {
                    "rating_not_posted"
                } else if (response.toString().contains("code-not-gived")) {
                    "code-not-gived"
                } else if (response.toString().contains("code-already-used")) {
                    "code-already-used"
                } else {
                    "success"
                }

                val builder = AlertDialog.Builder(context)
                builder.setTitle("testtttt")
                builder.setMessage("Coucou toi: $statement")
                builder.setPositiveButton("Ok", null)
                builder.show()
            }

            println("_____PLS WOOOORK_____")
            println(responseCode)
        }
    }
}