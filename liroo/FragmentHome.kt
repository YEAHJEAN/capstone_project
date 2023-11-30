package com.example.liroo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

data class PostItem(  // Post를 PostItem으로 변경
    val post_id: Int,
    val title: String,
    val content: String,
    val isbn: String,
    val book_title: String,
    val user_id: String,
    var likes: Int,  // 좋아요 개수를 추가
    var isLiked: Boolean,  // 좋아요 상태를 추가
    val imageUrl: String, // 이미지 URL을 저장할 필드 추가
    val imagebook: String? // 이미지 URL을 담을 변수 선언

)

interface PostApi1 {
    @GET("posts")
    fun getPosts(): Call<List<PostItem>>  // Post를 PostItem으로 변경
}

class FragmentHome : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fraghome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.postRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        // 빈 어댑터를 먼저 설정합니다.
        val adapter = PostAdapter(requireContext(), emptyList())
        recyclerView.adapter = adapter

        val retrofit = Retrofit.Builder()
            .baseUrl("http://ec2-3-34-240-75.ap-northeast-2.compute.amazonaws.com:3000/") // 실제 서버 URL로 변경
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val postApi1 = retrofit.create(PostApi1::class.java)

        val call = postApi1.getPosts()
        call.enqueue(object : Callback<List<PostItem>> {
            override fun onResponse(
                call: Call<List<PostItem>>,
                response: Response<List<PostItem>>
            ) {
                if (response.isSuccessful) {
                    val posts = response.body()
                    posts?.let {
                        adapter.postList = it.reversed()
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    showMessage("서버 오류 발생")
                }
            }

            override fun onFailure(
                call: Call<List<PostItem>>,
                t: Throwable
            ) {  // Post를 PostItem으로 변경
                showMessage("오류 발생: " + t.message)
            }
        })
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}