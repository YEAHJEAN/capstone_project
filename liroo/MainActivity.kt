package com.example.liroo

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setFrag(0)

        val btnFragment1 = findViewById<Button>(R.id.btn_fragment1)
        val btnFragment2 = findViewById<Button>(R.id.btn_fragment2)
        val btnFragment3 = findViewById<Button>(R.id.btn_fragment3)
        val btnFragment4 = findViewById<Button>(R.id.btn_fragment4)

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
                ft.replace(R.id.main_frame, Fragmant1()).commit()
            }
            1 -> {
                ft.replace(R.id.main_frame, Fragmant2()).commit()
            }
            2 -> {
                ft.replace(R.id.main_frame, Fragmant3()).commit()
            }
            3 -> {
                ft.replace(R.id.main_frame, Fragmant4()).commit()
            }
        }
    }
}
