package com.example.liroo

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

data class DeleteUserData(
    val id: String
)

data class RegApiResponseS(
    val success: Boolean,
    val message: String
)

interface DeleteApi {
    @POST("delete")
    fun deleteUser(@Body userData: DeleteUserData): Call<RegApiResponseS>
}

class FragmentSetting : Fragment() {
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

        val deleteButton = view.findViewById<Button>(R.id.deleteButton)
        val logoutButton = view.findViewById<Button>(R.id.logoutButton)
        val userIDTextView = view.findViewById<TextView>(R.id.userIDTextView)
        val userEmailTextView = view.findViewById<TextView>(R.id.userEmailTextView)
        val emailEditButton = view.findViewById<Button>(R.id.emailEditButton)
        val passwordEditButton = view.findViewById<Button>(R.id.passwordEditButton)

        userIDTextView.text = "$id 님"
        userEmailTextView.text = "이메일 : $email"

        emailEditButton.setOnClickListener {
            val fragmentManager = activity?.supportFragmentManager
            val fragmentTransaction = fragmentManager?.beginTransaction()
            fragmentTransaction?.replace(R.id.fragment_container, UpdateEmailFragment())
            fragmentTransaction?.addToBackStack(null)
            fragmentTransaction?.commit()
        }

        passwordEditButton.setOnClickListener {
            val fragmentManager = activity?.supportFragmentManager
            val fragmentTransaction = fragmentManager?.beginTransaction()
            fragmentTransaction?.replace(R.id.fragment_container, UpdatePasswordFragment())
            fragmentTransaction?.addToBackStack(null)
            fragmentTransaction?.commit()
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("http://ec2-3-34-240-75.ap-northeast-2.compute.amazonaws.com:3000/") // 실제 서버 URL로 변경
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val deleteApi = retrofit.create(DeleteApi::class.java)

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
                        override fun onResponse(
                            call: Call<RegApiResponseS>,
                            response: Response<RegApiResponseS>
                        ) {
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

        logoutButton.setOnClickListener {
            // AlertDialog Builder 객체 생성
            val builder = AlertDialog.Builder(context)
            builder.setMessage("로그아웃 하시겠습니까?")
                .setCancelable(false)
                .setPositiveButton("확인") { dialog, buttonId ->
                    // 확인 버튼을 눌렀을 때의 동작
                    sharedPref?.edit()?.clear()?.apply()
                    showMessage("로그아웃 되었습니다.")
                    val intent = Intent(context, LoginActivity::class.java)
                    startActivity(intent)
                    activity?.finish()
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
}
