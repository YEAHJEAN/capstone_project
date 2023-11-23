package com.example.liroo

import android.content.Context
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
import retrofit2.http.*
import java.security.MessageDigest
import java.util.regex.Pattern

data class LoginUserData(
    val id: String,
    val password: String
)

data class LoginApiResponse(
    val success: Boolean,
    val message: String
)

data class UserInfo(
    val id: String,
    val password: String,
    val email: String
)

interface LoginApi {
    @POST("login")
    fun loginUser(@Body userData: LoginUserData): Call<LoginApiResponse>

    @GET("user_info")
    fun getUserInfo(@Header("Authorization") token: String): Call<UserInfo>
}

class LoginActivity : AppCompatActivity() {
    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    val PREFERENCE = "com.example.liroo"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        if (isLoggedIn) {
            // 이미 로그인된 상태라면 메인 화면으로 이동
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // 로그인되지 않은 상태라면 로그인 화면 표시
            setContentView(R.layout.activity_login)

            val loginID = findViewById<EditText>(R.id.id)
            val loginPassword = findViewById<EditText>(R.id.pw)
            val btnLogin = findViewById<Button>(R.id.lg_login)
            val btnRegister = findViewById<Button>(R.id.lg_register)

            val retrofit = Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3001/") // 실제 서버 URL로 변경
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val api = retrofit.create(LoginApi::class.java)

            btnLogin.setOnClickListener {
                val id = loginID.text.toString()
                val password = loginPassword.text.toString()
                val hashedPassword = hashPassword(password) // 입력된 비밀번호를 해싱

                if (!isValidPassword(password)) {
                    loginPassword.error = "숫자, 문자, 특수문자 중 2가지 포함(6~15자)를 입력해주세요."
                    return@setOnClickListener
                }

                val userData = LoginUserData(id, hashedPassword) // 해싱된 비밀번호로 객체 생성
                val call = api.loginUser(userData) // 해싱된 비밀번호 객체를 서버로 전송

                call.enqueue(object : Callback<LoginApiResponse> {
                    override fun onResponse(
                        call: Call<LoginApiResponse>,
                        response: Response<LoginApiResponse>
                    ) {
                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            if (apiResponse != null && apiResponse.success) {
                                showMessage("로그인 성공!")

                                val callUserInfo =
                                    api.getUserInfo("$id:$hashedPassword") // 해싱된 비밀번호로 사용자 정보 가져오기

                                callUserInfo.enqueue(object : Callback<UserInfo> {
                                    override fun onResponse(
                                        call: Call<UserInfo>,
                                        response: Response<UserInfo>
                                    ) {
                                        val userInfo = response.body()
                                        if (userInfo != null) {
                                            saveUserInfo(
                                                userInfo.id,
                                                hashedPassword,
                                                userInfo.email
                                            )
                                            val intent =
                                                Intent(this@LoginActivity, MainActivity::class.java)
                                            startActivity(intent)
                                            finish()  // 현재 액티비티를 종료
                                        } else {
                                            showMessage("사용자 정보를 불러오는데 실패했습니다.")
                                        }
                                    }

                                    override fun onFailure(call: Call<UserInfo>, t: Throwable) {
                                        showMessage("오류 발생: " + t.message)
                                    }
                                })
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
    }

    private fun saveUserInfo(id: String, password: String, email: String, isLoggedIn: Boolean = true) {
        val sharedPref = getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("id", id)
            putString("password", password)
            putString("email", email)
            putBoolean("isLoggedIn", isLoggedIn) // 로그인 상태 저장
            apply()
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
