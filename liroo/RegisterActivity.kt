package com.example.liroo

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        val etPassword = findViewById<EditText>(R.id.et_pw)
        val etPassCheck = findViewById<EditText>(R.id.et_pc)
        val etEmail = findViewById<EditText>(R.id.et_email)
        val btnRegister = findViewById<Button>(R.id.btn_register)

        btnRegister.setOnClickListener {
            val password = etPassword.text.toString()
            val passcheck = etPassCheck.text.toString()
            val email = etEmail.text.toString()

            if (isValidPassword(password) && isValidEmail(email) && password == passcheck) {
                // 비밀번호와 이메일이 모두 유효하고 비밀번호가 일치할 경우에 수행할 작업을 여기에 추가하세요.
                // 예: 회원가입 처리 또는 다른 작업 수행
            } else {
                if (!isValidPassword(password)) {
                    etPassword.error = "숫자, 문자, 특수문자 중 2가지 포함(6~15자)를 입력해주세요."
                }
                if (!isValidEmail(email)) {
                    etEmail.error = "올바른 이메일 주소를 입력해주세요."
                }
                if (password != passcheck) {
                    etPassCheck.error = "비밀번호가 일치하지 않습니다."
                }
            }
        }
    }

    private fun isValidPassword(password: String): Boolean {
        val pattern = "^(?=.*[a-zA-Z0-9])(?=.*[a-zA-Z!@#$%^&*])(?=.*[0-9!@#$%^&*]).{6,15}$"
        return Pattern.compile(pattern).matcher(password).matches()
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return Pattern.compile(emailPattern).matcher(email).matches()
    }
}
