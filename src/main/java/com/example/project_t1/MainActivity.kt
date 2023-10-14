package com.example.project_t1

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.project_t1.ui.theme.Project_t1Theme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnRegister = findViewById<Button>(R.id.btn_signup_login)
        btnRegister.setOnClickListener {
            // Intent를 사용하여 RegisterActivity로 이동
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }
}
