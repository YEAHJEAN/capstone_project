package com.example.liroo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.regex.Pattern

data class LoginUserData(
    val id: String,
    val password: String
)

data class LoginApiResponse(
    val success: Boolean,
    val message: String
)

interface LoginApi {
    @POST("login")
    fun loginUser(@Body userData: LoginUserData): Call<LoginApiResponse>
}

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val loginID = findViewById<EditText>(R.id.id)
        val loginPassword = findViewById<EditText>(R.id.pw)
        val btnLogin = findViewById<Button>(R.id.lg_login)
        val btnRegister = findViewById<Button>(R.id.lg_register)
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3000/") // 실제 서버 URL로 변경
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(LoginApi::class.java)

        btnLogin.setOnClickListener {
            val id = loginID.text.toString()
            val password = loginPassword.text.toString()

            if (!isValidPassword(password)) {
                loginPassword.error = "숫자, 문자, 특수문자 중 2가지 포함(6~15자)를 입력해주세요."
                return@setOnClickListener
            }
            val userData = LoginUserData(id, password)
            val call = api.loginUser(userData)

            call.enqueue(object : Callback<LoginApiResponse> {
                override fun onResponse(call: Call<LoginApiResponse>, response: Response<LoginApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.success) {
                            showMessage("로그인 성공!")
                            val intent = Intent(this@LoginActivity, MainActivity::class.java)
                            startActivity(intent)
                        } else {
                            showMessage("로그인 실패")
                        }
                    } else {
                        showMessage("아이디 또는 비밀번호를 확인하세요.")
                    }
                }

                override fun onFailure(call: Call<LoginApiResponse>, t: Throwable) {
                    showMessage("오류 발생")
                }
            })
        }

        btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
    private fun isValidPassword(password: String): Boolean {
        val pattern = "^(?=.*[0-9])(?=.*[a-zA-Z!@#$%^&*]).{6,15}$"
        return Pattern.compile(pattern).matcher(password).matches()
    }
    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
