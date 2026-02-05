package com.example.songify

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.songify.network.ApiService
import com.example.songify.network.MyResponse
import com.example.songify.network.SendOtpRequest
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class LoginActivity : AppCompatActivity() {

    private lateinit var btnSend: Button
    private lateinit var sendOtpLoading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is already logged in
        val prefs = getSharedPreferences("SongifyPrefs", MODE_PRIVATE)
        val token = prefs.getString("token", null)
        if (token != null) {
            // User is logged in, go to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        btnSend = findViewById(R.id.btnSendOtp)
        sendOtpLoading = findViewById(R.id.sendOtpLoading)

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

        btnSend.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Step 1: Create the specific request object
            // We use SendOtpRequest so only {"email": "..."} is sent to the API
            val requestBody = SendOtpRequest(email)

            showLoading(true)

            // Step 2: Call the service
            service.sendOtp(BuildConfig.SONGIFY_API_KEY, requestBody).enqueue(object : Callback<MyResponse> {
                override fun onResponse(call: Call<MyResponse>, response: Response<MyResponse>) {
                    showLoading(false)

                    // PRINT THE RAW RESPONSE DATA
                    val statusCode = response.code()
                    val responseBody = response.body()?.message
                    val errorBody = response.errorBody()?.string()

                    android.util.Log.d("API_RESULT", "Status Code: $statusCode")

                    if (response.isSuccessful) {
                        android.util.Log.d("API_RESULT", "Success Message: $responseBody")
                        Toast.makeText(this@LoginActivity, "OTP Sent Successfully!", Toast.LENGTH_SHORT).show()

                        // Navigate to Verify Component
                        val intent = Intent(this@LoginActivity, VerifyActivity::class.java)
                        intent.putExtra("USER_EMAIL", email)
                        startActivity(intent)
                    } else {
                        android.util.Log.e("API_RESULT", "Error Details: $errorBody")
                        Toast.makeText(this@LoginActivity, "Server Error: $statusCode. $errorBody", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<MyResponse>, t: Throwable) {
                    showLoading(false)
                    android.util.Log.e("NETWORK_ERROR", "Reason: ${t.message}")

                    if (t is java.net.UnknownHostException) {
                        Toast.makeText(this@LoginActivity, "No Internet / DNS Failure", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@LoginActivity, "Failed: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            sendOtpLoading.visibility = View.VISIBLE
            btnSend.alpha = 0.5f
        } else {
            sendOtpLoading.visibility = View.GONE
            btnSend.alpha = 1.0f
        }
    }
}