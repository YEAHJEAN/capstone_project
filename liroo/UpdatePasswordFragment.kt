package com.example.liroo

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.security.MessageDigest

data class UpdatePasswordDataP(
    val id: String,
    val oldPassword: String,
    val newPassword: String
)

data class RegApiResponseP(
    val success: Boolean,
    val message: String
)

interface UpdatePasswordApi {
    @POST("change_password")
    fun updateUser(@Body userData: UpdatePasswordDataP): Call<RegApiResponseP>
}

class UpdatePasswordFragment : Fragment() {
    val PREFERENCE = "com.example.liroo"

    private fun hashPassword(password: String): String {
        val bytes = password.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("", { str, it -> str + "%02x".format(it) })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_update_password, container, false)

        val sharedPref = activity?.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        val id = sharedPref?.getString("id", "") ?: ""

        val oldPasswordEditText = view.findViewById<EditText>(R.id.oldPasswordEditText)
        val newPasswordEditText = view.findViewById<EditText>(R.id.newPasswordEditText)
        val saveButton = view.findViewById<Button>(R.id.saveButton)

        saveButton.setOnClickListener {
            val oldPassword = oldPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()

            if (!isValidPassword(newPassword)) {
                newPasswordEditText.error = "유효한 비밀번호를 입력해주세요."
                return@setOnClickListener
            }

            // 비밀번호를 해싱합니다.
            val hashedOldPassword = hashPassword(oldPassword)
            val hashedNewPassword = hashPassword(newPassword)

            val retrofit = Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3001/") // 실제 서버 URL로 변경
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val updatePasswordApi = retrofit.create(UpdatePasswordApi::class.java)
            val userData = UpdatePasswordDataP(id, hashedOldPassword, hashedNewPassword)

            val call = updatePasswordApi.updateUser(userData)

            call.enqueue(object : Callback<RegApiResponseP> {
                override fun onResponse(call: Call<RegApiResponseP>, response: Response<RegApiResponseP>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.success) {
                            val editor = sharedPref?.edit()
                            // 해싱된 비밀번호를 저장합니다.
                            editor?.putString("password", hashedNewPassword)
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

                override fun onFailure(call: Call<RegApiResponseP>, t: Throwable) {
                    showMessage("오류 발생: " + t.message)
                }
            })
        }

        return view
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun isValidPassword(password: String): Boolean {
        val regex = Regex("^[a-zA-Z0-9!@#\$%^&*()_+\\-=\\[\\]{};':\",./<>?].{6,15}$")
        return regex.matches(password)
    }
}
