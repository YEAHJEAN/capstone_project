package com.example.liroo

import android.annotation.SuppressLint
import android.content.Context
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

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

data class RegUserData(
    val id: String,
    val password: String,
    val email: String
)

data class RegApiResponse(
    val success: Boolean,
    val message: String
)

interface RegApi {
    @POST("register")
    fun registerUser(@Body userData: RegUserData): Call<RegApiResponse>
}

class RegisterActivity : AppCompatActivity() {
    // SharedPreferences 파일명
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md: MessageDigest
        return try {
            md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.fold("", { str, it -> str + "%02x".format(it) })
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
            ""
        }
    }
    val PREFERENCE = "com.example.liroo"

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val regId = findViewById<EditText>(R.id.rg_id)
        val regTextPassword = findViewById<EditText>(R.id.rg_pw)
        val regTextEmail = findViewById<EditText>(R.id.rg_email)
        val regTextPasswordCheck = findViewById<EditText>(R.id.rg_pc)
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3001/") // 실제 서버 URL로 변경
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(RegApi::class.java)

        val register = findViewById<Button>(R.id.btn_register)
        register.setOnClickListener {
            val id = regId.text.toString()
            val password = regTextPassword.text.toString()
            val passwordCheck = regTextPasswordCheck.text.toString()
            val email = regTextEmail.text.toString()

            val hashedPassword = hashPassword(password) // 비밀번호를 해싱하여 저장


            if (!isValidId(id)) {
                regId.error = "영어, 숫자를 사용 (2~12자)를 입력해주세요."
                return@setOnClickListener
            }

            if (!isValidPassword(password, passwordCheck)) {
                regTextPassword.error = "숫자, 문자, 특수문자 중 2가지 포함 (6~15자)를 입력해주세요."
                regTextPasswordCheck.error = "비밀번호가 일치하지 않습니다."
                return@setOnClickListener
            }

            if (!isValidEmail(email)) {
                regTextEmail.error = "유효한 이메일을 입력해주세요."
                return@setOnClickListener
            }

            val userData = RegUserData(id, hashPassword(password), email)

            val call = api.registerUser(userData)

            call.enqueue(object : Callback<RegApiResponse> {
                override fun onResponse(call: Call<RegApiResponse>, response: Response<RegApiResponse>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.success) {
                            // 회원가입 성공한 경우
                            showMessage("회원가입 성공!")
                            saveUserInfo(id, password, email) // 회원가입 성공 후 사용자 정보를 저장
                            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                            startActivity(intent)
                        } else {
                            // 서버에서 성공하지 않은 응답을 받은 경우
                            showMessage("회원가입 실패")
                        }
                    } else {
                        // 서버에서 오류 응답을 받은 경우
                        showMessage("이미 사용중인 아이디 입니다.")
                    }
                }

                override fun onFailure(call: Call<RegApiResponse>, t: Throwable) {
                    // 오류 처리
                    showMessage("오류 발생: " + t.message)
                }
            })
        }
    }

    // SharedPreferences에 사용자 정보 저장
    private fun saveUserInfo(id: String, password: String, email: String) {
        val sharedPref = getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        val hashedPassword = hashPassword(password) // 비밀번호를 해싱하여 저장

        with(sharedPref.edit()) {
            putString("id", id)
            putString("password", hashedPassword)
            putString("email", email)
            apply()
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidId(id: String): Boolean {
        val regex = Regex("^[a-zA-Z0-9].{2,12}\$")
        return regex.matches(id)
    }

    private fun isValidPassword(password: String, passwordcheck: String): Boolean {
        val regex = Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{};':\",./<>?].{6,15}$")
        return password.length >= 8 && password == passwordcheck && regex.matches(password)
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
