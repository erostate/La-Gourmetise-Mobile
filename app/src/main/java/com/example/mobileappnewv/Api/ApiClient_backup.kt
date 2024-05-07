package com.example.mobileappnewv.Api

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask

class ApiClient_backup {
    private val BASE_URL = "http://10.0.2.2:8000/api/"

    fun exportRatings(context: Context?, ratingBody: RatingBody): String {
        val futureTask = FutureTask<String>(object : Callable<String?> {
            var returnStatement = "cant_access_to_request"
            override fun call(): String {
                try {
                    val url = URL(BASE_URL + "ratings?code=" + ratingBody.code)
                    val conn = url.openConnection() as HttpURLConnection
                    conn.setRequestMethod("POST")
                    conn.setRequestProperty("Accept", "application/ld+json")
                    conn.setRequestProperty("Content-Type", "application/ld+json")

                    // Write the email and password in the request body
                    val os = conn.outputStream
                    val writer = BufferedWriter(OutputStreamWriter(os, StandardCharsets.UTF_8))
                    val jsonParam = JSONObject()
                    jsonParam.put("companyRating", ratingBody.companyRating)
                    jsonParam.put("productRating", ratingBody.productRating)
                    jsonParam.put("priceRating", ratingBody.priceRating)
                    jsonParam.put("staffRating", ratingBody.staffRating)
                    writer.write(jsonParam.toString())
                    writer.flush()
                    writer.close()
                    os.close()
                    val responseCode = conn.getResponseCode()
                    if (responseCode == HttpURLConnection.HTTP_OK) { // success
                        val `in` = BufferedReader(InputStreamReader(conn.inputStream))
                        var inputLine: String?
                        val response = StringBuilder()
                        while (`in`.readLine().also { inputLine = it } != null) {
                            response.append(inputLine)
                        }
                        `in`.close()

                        // If response body is : "result" = "rating-not-posted
                        returnStatement = if (response.toString().contains("rating-not-posted")) {
                            "rating_not_posted"
                        } else if (response.toString().contains("code-not-gived")) {
                            "code-not-gived"
                        } else if (response.toString().contains("code-already-used")) {
                            "code-already-used"
                        } else if (response.toString().contains("rating-not-posted")) {
                            "rating-not-posted"
                        } else {
                            "success"
                        }
                    } else {
                        // If response code is not 200, export failed
                        returnStatement = "rating-not-posted"
                    }
                } catch (e: Exception) {
                    returnStatement = "request-failed"
                }
                return returnStatement
            }
        })
        Thread(futureTask).start()
        return try {
            futureTask.get() // waits for the task to complete and gets the result
        } catch (e: InterruptedException) {
            "request-failed"
        } catch (e: ExecutionException) {
            "request-failed"
        }
    }
}