package com.example.liroo

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    companion object {
        var instance: MainActivity? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        instance = this

        setFrag(0)

        val btnFragment1 = findViewById<ImageButton>(R.id.btn_home)
        val btnFragment2 = findViewById<ImageButton>(R.id.btn_search)
        val btnFragment3 = findViewById<ImageButton>(R.id.btn_plus)
        val btnFragment4 = findViewById<ImageButton>(R.id.btn_shelf)
        val btnFragment5 = findViewById<ImageButton>(R.id.btn_setting)

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
        btnFragment5.setOnClickListener {
            setFrag(4)
        }
    }

    override fun onBackPressed() {
        val fragmentManager = supportFragmentManager
        val count = fragmentManager.backStackEntryCount

        if (count > 0) {
            // 스택에 프래그먼트가 있을 때
            fragmentManager.popBackStack()
        } else {
            // 스택이 비어있을 때 기본 동작 수행
            super.onBackPressed()
        }
    }

    fun setFrag(fragNum: Int) {
        val ft = supportFragmentManager.beginTransaction()

        when (fragNum) {
            0 -> {
                ft.replace(R.id.fragment_container, FragmentHome())
            }

            1 -> {
                ft.replace(R.id.fragment_container, FragmentSearch())
            }

            2 -> {
                // Modal 프래그먼트는 addToBackStack을 하지 않음
                val bookshelfRecognitionModal = PlusModalFragment()
                bookshelfRecognitionModal.setOptionClickListener(object :
                    PlusModalFragment.OptionClickListener {
                    override fun onOptionSelected(option: String) {
                        // 옵션 선택 이벤트 처리
                    }
                })
                bookshelfRecognitionModal.show(
                    supportFragmentManager,
                    "bookshelf_recognition_modal"
                )
                return  // Modal 프래그먼트는 스택에 추가하지 않음
            }

            3 -> {
                ft.replace(R.id.fragment_container, FragmentShelf())
            }

            4 -> {
                ft.replace(R.id.fragment_container, FragmentSetting())
            }
        }

        ft.addToBackStack(null)  // 프래그먼트를 스택에 추가
        ft.commit()
    }
}
