package com.example.liroo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class UpdateUserData(
    val id: String,
    val email: String
)

data class RegApiResponseS(
    val success: Boolean,
    val message: String
)

interface UpdateApi {
    @POST("update")
    fun updateUser(@Body userData: UpdateUserData): Call<RegApiResponseS>
}

class FragmentSetting : Fragment() {
    // SharedPreferences 파일명
    val PREFERENCE = "com.example.liroo"

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragsetting, container, false)

        val sharedPref = activity?.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        val id = sharedPref?.getString("id", "") ?: ""
        val email = sharedPref?.getString("email", "") ?: ""

        val idEditText = view.findViewById<EditText>(R.id.idEditText)
        val emailEditText = view.findViewById<EditText>(R.id.emailEditText)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        val userInfoTextView = view.findViewById<TextView>(R.id.userInfoTextView) // 추가

        idEditText.text = Editable.Factory.getInstance().newEditable(id)
        emailEditText.text = Editable.Factory.getInstance().newEditable(email)
        userInfoTextView.text = "ID: $id\nEmail: $email" // 추가

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3001/") // 실제 서버 URL로 변경
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api = retrofit.create(UpdateApi::class.java)

        saveButton.setOnClickListener {
            val newId = idEditText.text.toString()
            val newEmail = emailEditText.text.toString()

            val userData = UpdateUserData(newId, newEmail)

            val call = api.updateUser(userData)

            call.enqueue(object : Callback<RegApiResponseS> {
                override fun onResponse(call: Call<RegApiResponseS>, response: Response<RegApiResponseS>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.success) {
                            // 정보 수정 성공한 경우
                            val editor = sharedPref?.edit()
                            editor?.putString("id", newId)
                            editor?.putString("email", newEmail)
                            editor?.apply()
                            showMessage("정보 수정 성공!")

                            userInfoTextView.text = "ID: $newId\nEmail: $newEmail" // 추가
                        } else {
                            // 서버에서 성공하지 않은 응답을 받은 경우
                            showMessage("정보 수정 실패")
                        }
                    } else {
                        // 서버에서 오류 응답을 받은 경우
                        showMessage("정보 수정 실패")
                    }
                }

                override fun onFailure(call: Call<RegApiResponseS>, t: Throwable) {
                    // 오류 처리
                    showMessage("오류 발생: " + t.message)
                }
            })
        }

        return view
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
