package com.example.liroo

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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

data class postData(
    val user_id: String,
    val title: String,
    val content: String,
    val isbn: String  // ISBN 필드 추가
)

data class PostApiResponse(
    val success: Boolean,
    val message: String
)

interface PostApi {
    @POST("posts/create")
    fun createPost(@Body postData: postData): Call<PostApiResponse>
}


class FragmentPostBook : Fragment() {
    // SharedPreferences 파일명
    val PREFERENCE = "com.example.liroo"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post_book, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val titleEditText = view.findViewById<EditText>(R.id.editTextTitle)
        val contentEditText = view.findViewById<EditText>(R.id.editTextContent)
        val postButton = view.findViewById<Button>(R.id.postButton)

        val isbnEditText = view.findViewById<EditText>(R.id.editTextIsbn)
        val isbnData = arguments?.getString("isbnData")
        isbnEditText.setText(isbnData)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://ec2-3-34-240-75.ap-northeast-2.compute.amazonaws.com:3000/") // 실제 서버 URL로 변경
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val postApi = retrofit.create(PostApi::class.java)

        postButton.setOnClickListener {

            val pref = this.activity?.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
            val id = pref?.getString("id", "") // 로그인한 사용자의 ID를 가져옴
            val title = titleEditText.text.toString()
            val content = contentEditText.text.toString()
            val isbn = isbnEditText.text.toString()
            if (id != null && id.isNotEmpty() && title.isNotEmpty() && content.isNotEmpty()&& isbn.isNotEmpty()) {

                val call = postApi.createPost(postData(id, title, content, isbn))

                call.enqueue(object : Callback<PostApiResponse> {
                    override fun onResponse(call: Call<PostApiResponse>, response: Response<PostApiResponse>) {
                        if (response.isSuccessful) {
                            val apiResponse = response.body()
                            if (apiResponse != null && apiResponse.success) {
                                showMessage("게시글이 작성되었습니다.")
                                Handler(Looper.getMainLooper()).postDelayed({
                                    MainActivity.instance?.setFrag(0)
                                }, 500)
                            } else {
                                showMessage("게시글 작성 실패")
                            }
                        } else {
                            showMessage("서버 오류 발생")
                        }
                    }

                    override fun onFailure(call: Call<PostApiResponse>, t: Throwable) {
                        showMessage("오류 발생: " + t.message)
                    }
                })
            } else {
                showMessage("제목과 내용을 모두 입력해주세요.")
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}