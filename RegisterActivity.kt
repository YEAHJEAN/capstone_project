package com.example.liroo

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Patterns
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

data class UserData(
    val id: String,
    val password: String,
    val email: String
)

data class ApiResponse(
    val success: Boolean,
    val message: String
)

interface MyApi {
    @POST("register")
    fun registerUser(@Body userData: UserData): Call<ApiResponse>
}

class RegisterActivity : AppCompatActivity() {
    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val editTextId = findViewById<EditText>(R.id.et_id)
        val editTextPassword = findViewById<EditText>(R.id.et_pw)
        val editTextEmail = findViewById<EditText>(R.id.et_email)
        val editTextPasswordCheck = findViewById<EditText>(R.id.et_pc)
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3001/") // 실제 서버 URL로 변경
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(MyApi::class.java)

        val register = findViewById<Button>(R.id.btn_register)
        register.setOnClickListener {
            val id = editTextId.text.toString()
            val password = editTextPassword.text.toString()
            val passwordCheck = editTextPasswordCheck.text.toString()
            val email = editTextEmail.text.toString()

            if (!isValidId(id)) {
                editTextId.error = "유효한 아이디를 입력하세요 (영어와 숫자만 가능)"
                return@setOnClickListener
            }

            if (!isValidPassword(password, passwordCheck)) {
                editTextPassword.error = "유효한 비밀번호를 입력하세요 (8자 이상, 영어, 숫자, 특수문자만 가능)"
                editTextPasswordCheck.error = "비밀번호가 일치하지 않습니다"
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                editTextEmail.error = "유효한 이메일을 입력하세요"
                return@setOnClickListener
            }

            val userData = UserData(id, password, email)

            val call = api.registerUser(userData)

            call.enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.success) {
                            // 회원가입 성공한 경우
                            showMessage("회원가입 성공!")
                            val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                            startActivity(intent)

                        } else {
                            // 서버에서 성공하지 않은 응답을 받은 경우
                            showMessage("회원가입 실패: ${apiResponse?.message ?: "알 수 없는 오류"}")
                        }
                    } else {
                        // 서버에서 오류 응답을 받은 경우
                        showMessage("서버 오류: ${response.code()}")
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    // 오류 처리
                    showMessage("오류 발생: " + t.message)
                }
            })
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidId(id: String): Boolean {
        val regex = Regex("^[a-zA-Z0-9]*\$")
        return regex.matches(id)
    }

    private fun isValidPassword(password: String, passwordcheck: String): Boolean {
        val regex = Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{};':\",./<>?]*$")
        return password.length >= 8 && password == passwordcheck && regex.matches(password)
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
