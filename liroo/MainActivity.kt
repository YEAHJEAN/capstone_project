package com.example.liroo

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setFrag(0)

        val btnFragment1 = findViewById<ImageButton>(R.id.btn_home)
        val btnFragment2 = findViewById<ImageButton>(R.id.btn_search)
        val btnFragment3 = findViewById<ImageButton>(R.id.btn_post)
        val btnFragment4 = findViewById<ImageButton>(R.id.btn_setting)

        btnFragment1.setOnClickListener {
            setFrag(0)
        }
        btnFragment2.setOnClickListener {
            setFrag(1)
        }
        btnFragment3.setOnClickListener {
            setFrag(2)
        }
        btnFragment4.setOnClickListener {
            setFrag(3)
        }
    }


    private fun setFrag(fragNum : Int) {
        val ft = supportFragmentManager.beginTransaction()
        when(fragNum)
        {
            0 -> {
                ft.replace(R.id.main_frame, FragmentHome()).commit()
            }
            1 -> {
                ft.replace(R.id.main_frame, FragmentSearch()).commit()
            }
            2 -> {
                ft.replace(R.id.main_frame, FragmentPost()).commit()
            }
            3 -> {
                ft.replace(R.id.main_frame, FragmentSetting()).commit()
            }
        }
    }
}
