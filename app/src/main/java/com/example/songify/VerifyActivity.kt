package com.example.songify

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.songify.network.ApiService
import com.example.songify.network.MyResponse
import com.example.songify.network.SendOtpRequest
import com.example.songify.network.VerifyOtpRequest
import com.example.songify.network.TokenResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent


class VerifyActivity : AppCompatActivity() {

    private lateinit var btnVerify: Button
    private lateinit var verifyOtpLoading: ProgressBar
    private lateinit var tvResendOtp: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify)

        val userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        val otp1 = findViewById<EditText>(R.id.otp1)
        val otp2 = findViewById<EditText>(R.id.otp2)
        val otp3 = findViewById<EditText>(R.id.otp3)
        val otp4 = findViewById<EditText>(R.id.otp4)
        val otp5 = findViewById<EditText>(R.id.otp5)
        val otp6 = findViewById<EditText>(R.id.otp6)

        btnVerify = findViewById(R.id.btnVerify)
        verifyOtpLoading = findViewById(R.id.verifyOtpLoading)
        tvResendOtp = findViewById(R.id.tvResendOtp)

        val otpFields = arrayOf(otp1, otp2, otp3, otp4, otp5, otp6)

        // 🔹 Auto move to next box
        for (i in otpFields.indices) {
            otpFields[i].addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1 && i < otpFields.size - 1) {
                        otpFields[i + 1].requestFocus()
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            // 🔹 Backspace handling
            otpFields[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    otpFields[i].text.isEmpty() &&
                    i > 0
                ) {
                    otpFields[i - 1].requestFocus()
                }
                false
            }
        }

        btnVerify.setOnClickListener {

            val otpValue = otpFields.joinToString("") { it.text.toString() }

            if (otpValue.length != 6) {
                Toast.makeText(this, "Enter 6-digit OTP", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyOtp(userEmail, otpValue)
        }

        tvResendOtp.setOnClickListener {
            resendOtp(userEmail)
        }
    }

    private fun verifyOtp(email: String, otpValue: String) {

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.SONGIFY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)
        val requestBody = VerifyOtpRequest(email, otpValue)

        showLoading(true)

        service.verifyOtp(BuildConfig.SONGIFY_API_KEY, requestBody)
            .enqueue(object : Callback<TokenResponse> {

                override fun onResponse(
                    call: Call<TokenResponse>,
                    response: Response<TokenResponse>
                ) {
                    showLoading(false)
                    if (response.isSuccessful && response.body()?.token != null) {

                        val token = response.body()!!.token

                        // Inside VerifyActivity after successful API response:
                        getSharedPreferences("SongifyPrefs", MODE_PRIVATE).edit()
                            .putString("token", "Bearer $token")
                            .putString("email", email)
                            .apply() // Asynchronous and persistent

                        startActivity(Intent(this@VerifyActivity, MainActivity::class.java))
                        finishAffinity()

                    } else {
                        val errorBody = response.errorBody()?.string()
                        Toast.makeText(
                            this@VerifyActivity,
                            "Invalid OTP. Server Response: $errorBody",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<TokenResponse>, t: Throwable) {
                    showLoading(false)
                    Toast.makeText(
                        this@VerifyActivity,
                        "Network error: ${t.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun resendOtp(email: String) {

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.SONGIFY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(ApiService::class.java)
        val requestBody = SendOtpRequest(email)

        tvResendOtp.isEnabled = false

        service.sendOtp(BuildConfig.SONGIFY_API_KEY, requestBody).enqueue(object : Callback<MyResponse> {
            override fun onResponse(call: Call<MyResponse>, response: Response<MyResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@VerifyActivity, "OTP Resent Successfully!", Toast.LENGTH_SHORT).show()
                    startCooldownTimer()
                } else {
                    Toast.makeText(this@VerifyActivity, "Failed to resend OTP", Toast.LENGTH_SHORT).show()
                    tvResendOtp.isEnabled = true
                }
            }

            override fun onFailure(call: Call<MyResponse>, t: Throwable) {
                Toast.makeText(this@VerifyActivity, "Network error", Toast.LENGTH_SHORT).show()
                tvResendOtp.isEnabled = true
            }
        })
    }

    private fun startCooldownTimer() {
        object : CountDownTimer(30000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                tvResendOtp.text = "Resend OTP in $secondsLeft s"
            }

            override fun onFinish() {
                tvResendOtp.text = "Resend OTP"
                tvResendOtp.isEnabled = true
            }
        }.start()
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            verifyOtpLoading.visibility = View.VISIBLE
            btnVerify.alpha = 0.5f
        } else {
            verifyOtpLoading.visibility = View.GONE
            btnVerify.alpha = 1.0f
        }
    }
}
