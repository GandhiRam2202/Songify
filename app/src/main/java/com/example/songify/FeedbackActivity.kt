package com.example.songify

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.songify.network.ApiService
import com.example.songify.network.FeedbackRequest
import com.example.songify.network.MyResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FeedbackActivity : AppCompatActivity() {

    private lateinit var btnSubmit: Button
    private lateinit var etFeedbackMessage: EditText
    private lateinit var submitFeedbackLoading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        btnSubmit = findViewById(R.id.btnSubmitFeedback)
        etFeedbackMessage = findViewById(R.id.etFeedbackMessage)
        submitFeedbackLoading = findViewById(R.id.submitFeedbackLoading)

        btnSubmit.setOnClickListener {
            val feedbackMessage = etFeedbackMessage.text.toString().trim()
            if (feedbackMessage.isEmpty()) {
                Toast.makeText(this, "Please enter your feedback", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val prefs = getSharedPreferences("SongifyPrefs", Context.MODE_PRIVATE)
            val email = prefs.getString("email", null)
            val token = prefs.getString("token", null)

            if (email != null && token != null) {
                submitFeedback(token, email, feedbackMessage)
            } else {
                Toast.makeText(this, "Could not retrieve your email or token. Please log in again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun submitFeedback(token: String, email: String, message: String) {
        showLoading(true)

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.SONGIFY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        val service = retrofit.create(ApiService::class.java)
        val requestBody = FeedbackRequest(email, message)

        service.submitFeedback(BuildConfig.SONGIFY_API_KEY, token, requestBody).enqueue(object : Callback<MyResponse> {
            override fun onResponse(call: Call<MyResponse>, response: Response<MyResponse>) {
                showLoading(false)
                if (response.isSuccessful) {
                    Toast.makeText(this@FeedbackActivity, "Feedback submitted successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@FeedbackActivity, "Failed to submit feedback. Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<MyResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@FeedbackActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            submitFeedbackLoading.visibility = View.VISIBLE
            btnSubmit.alpha = 0.5f
        } else {
            submitFeedbackLoading.visibility = View.GONE
            btnSubmit.alpha = 1.0f
        }
    }
}