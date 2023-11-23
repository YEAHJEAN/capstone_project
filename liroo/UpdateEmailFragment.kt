package com.example.liroo

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

data class UpdateEmailDataE(
    val id: String,
    val email: String
)

data class RegApiResponseE(
    val success: Boolean,
    val message: String
)

interface UpdateEmailApi {
    @POST("change_email")
    fun updateUser(@Body userData: UpdateEmailDataE): Call<RegApiResponseE>
}

class UpdateEmailFragment : Fragment() {
    val PREFERENCE = "com.example.liroo"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_update_email, container, false)

        val sharedPref = activity?.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        val id = sharedPref?.getString("id", "") ?: ""
        val email = sharedPref?.getString("email", "") ?: ""

        val emailEditText = view.findViewById<EditText>(R.id.emailEditText)
        val saveButton = view.findViewById<Button>(R.id.saveButton)

        emailEditText.text = Editable.Factory.getInstance().newEditable(email)

        saveButton.setOnClickListener {
            val newEmail = emailEditText.text.toString()

            if (!isValidEmail(newEmail)) {
                emailEditText.error = "유효한 이메일을 입력해주세요."
                return@setOnClickListener
            }

            val retrofit = Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3001/") // 실제 서버 URL로 변경
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val UpdateEmailApi = retrofit.create(UpdateEmailApi::class.java)
            val userData = UpdateEmailDataE(id, newEmail)

            val call = UpdateEmailApi.updateUser(userData)

            call.enqueue(object : Callback<RegApiResponseE> {
                override fun onResponse(
                    call: Call<RegApiResponseE>,
                    response: Response<RegApiResponseE>
                ) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.success) {
                            val editor = sharedPref?.edit()
                            editor?.putString("email", newEmail)
                            editor?.apply()
                            showMessage("정보 수정 성공!")

                            // 이전 화면으로 돌아가기
                            activity?.supportFragmentManager?.popBackStack()
                        } else {
                            showMessage("정보 수정 실패")
                        }
                    } else {
                        showMessage("정보 수정 실패")
                    }
                }

                override fun onFailure(call: Call<RegApiResponseE>, t: Throwable) {
                    showMessage("오류 발생: " + t.message)
                }
            })
        }

        return view
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}
