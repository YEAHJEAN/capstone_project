package com.example.liroo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class FragmentSetting : Fragment() {

    // SharedPreferences 파일명
    val PREFERENCE = "com.example.liroo"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragsetting, container, false)

        val sharedPref = activity?.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        val id = sharedPref?.getString("id", null)
        val password = sharedPref?.getString("password", null)
        val email = sharedPref?.getString("email", null)

        if (id != null && password != null && email != null) {
            val userInfoTextView = view.findViewById<TextView>(R.id.userInfoTextView)
            userInfoTextView.text = "ID: $id\nEmail: $email"
        }

        return view
    }
}