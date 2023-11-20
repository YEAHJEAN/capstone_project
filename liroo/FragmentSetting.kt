package com.example.liroo

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.util.Patterns
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

data class DeleteUserData(
    val id: String
)

data class RegApiResponseS(
    val success: Boolean,
    val message: String
)

interface UpdateApi {
    @POST("update")
    fun updateUser(@Body userData: UpdateUserData): Call<RegApiResponseS>
}

interface DeleteApi {
    @POST("delete")
    fun deleteUser(@Body userData: DeleteUserData): Call<RegApiResponseS>
}

class FragmentSetting : Fragment() {
    val PREFERENCE = "com.example.liroo"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragsetting, container, false)

        val sharedPref = activity?.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        val id = sharedPref?.getString("id", "") ?: ""
        val email = sharedPref?.getString("email", "") ?: ""

        val emailEditText = view.findViewById<EditText>(R.id.emailEditText)
        val saveButton = view.findViewById<Button>(R.id.saveButton)
        val deleteButton = view.findViewById<Button>(R.id.deleteButton)
        val userIDTextView = view.findViewById<TextView>(R.id.userIDTextView)
        val userEmailTextView = view.findViewById<TextView>(R.id.userEmailTextView)

        emailEditText.text = Editable.Factory.getInstance().newEditable(email)
        userIDTextView.text = "$id 님"
        userEmailTextView.text = "이메일 : $email"

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:3001/") // 실제 서버 URL로 변경
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val updateApi = retrofit.create(UpdateApi::class.java)
        val deleteApi = retrofit.create(DeleteApi::class.java)

        saveButton.setOnClickListener {
            val newEmail = emailEditText.text.toString()

            if (!isValidEmail(newEmail)) {
                emailEditText.error = "유효한 이메일을 입력해주세요."
                return@setOnClickListener
            }

            val userData = UpdateUserData(id, newEmail)

            val call = updateApi.updateUser(userData)

            call.enqueue(object : Callback<RegApiResponseS> {
                override fun onResponse(call: Call<RegApiResponseS>, response: Response<RegApiResponseS>) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()
                        if (apiResponse != null && apiResponse.success) {
                            val editor = sharedPref?.edit()
                            editor?.putString("email", newEmail)
                            editor?.apply()
                            showMessage("정보 수정 성공!")

                            userEmailTextView.text = "이메일: $newEmail"
                        } else {
                            showMessage("정보 수정 실패")
                        }
                    } else {
                        showMessage("정보 수정 실패")
                    }
                }

                override fun onFailure(call: Call<RegApiResponseS>, t: Throwable) {
                    showMessage("오류 발생: " + t.message)
                }
            })
        }

        deleteButton.setOnClickListener {
            // AlertDialog Builder 객체 생성
            val builder = AlertDialog.Builder(context)
            builder.setMessage("정말 탈퇴하시겠습니까?")
                .setCancelable(false)
                .setPositiveButton("확인") { dialog, buttonId ->
                    // 확인 버튼을 눌렀을 때의 동작
                    val deleteUserData = DeleteUserData(id)

                    val deleteCall = deleteApi.deleteUser(deleteUserData)

                    deleteCall.enqueue(object : Callback<RegApiResponseS> {
                        override fun onResponse(call: Call<RegApiResponseS>, response: Response<RegApiResponseS>) {
                            if (response.isSuccessful) {
                                val apiResponse = response.body()
                                if (apiResponse != null && apiResponse.success) {
                                    showMessage("회원 탈퇴 성공!")
                                    sharedPref?.edit()?.clear()?.apply()
                                    val intent = Intent(context, LoginActivity::class.java)
                                    startActivity(intent)
                                    activity?.finish()
                                } else {
                                    showMessage("회원 탈퇴 실패")
                                }
                            } else {
                                showMessage("회원 탈퇴 실패")
                            }
                        }

                        override fun onFailure(call: Call<RegApiResponseS>, t: Throwable) {
                            showMessage("오류 발생: " + t.message)
                        }
                    })
                }
                .setNegativeButton("취소") { dialog, buttonId ->
                    // 취소 버튼을 눌렀을 때의 동작
                    dialog.cancel()
                }
            // AlertDialog 객체 생성 및 표시
            val alert = builder.create()
            alert.show()
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
