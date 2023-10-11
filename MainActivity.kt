package com.example.liroo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.liroo.ui.theme.MainActivity
import android.os.Handler
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the loading layout as the content view
        setContentView(R.layout.activity_loading)

        // Delay for 3 seconds (3000 milliseconds)
        val delayMillis: Long = 3000

        // Create a Handler to post a delayed action
        val handler = Handler()
        handler.postDelayed({
            // After the delay, set the main activity layout as content
            setContentView(R.layout.activity_main)
        }, delayMillis)
    }
}