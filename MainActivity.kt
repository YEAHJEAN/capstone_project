package com.example.liroo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val Password = findViewById<EditText>(R.id.pw)
        val btnRegister = findViewById<Button>(R.id.main_register)
        val btnLogin = findViewById<Button>(R.id.main_login)

        btnLogin.setOnClickListener {
            val password = Password.text.toString()

            if (isValidPassword(password)) {
                // 비밀번호가 패턴과 일치하면 RegisterActivity로 진행
                val intent = Intent(this, RegisterActivity::class.java) //바꿔야 될듯
                startActivity(intent)
            } else {
                // 비밀번호가 패턴과 일치하지 않으면 오류 메시지를 표시하거나 필요한대로 처리합니다.
                Password.error = "숫자, 문자, 특수문자 중 2가지 포함(6~15자)를 입력해주세요."
            }
        }

        btnRegister.setOnClickListener {
            // Intent를 사용하여 RegisterActivity로 이동
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun isValidPassword(password: String): Boolean {
        val pattern = "^(?=.*[0-9])(?=.*[a-zA-Z!@#$%^&*]).{6,15}$"
        return Pattern.compile(pattern).matcher(password).matches()
    }
}
