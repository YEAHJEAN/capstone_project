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

data class Post(
    val id: String, val title: String, val content: String
)

interface PostApi1 {
    @GET("posts")
    fun getPosts(): Call<List<Post>>
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

        val retrofit = Retrofit.Builder().baseUrl("http://ec2-3-34-240-75.ap-northeast-2.compute.amazonaws.com:3000/") // 실제 서버 URL로 변경
            .addConverterFactory(GsonConverterFactory.create()).build()

        val postApi1 = retrofit.create(PostApi1::class.java)

        val call = postApi1.getPosts()

        call.enqueue(object : Callback<List<Post>> {
            override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                if (response.isSuccessful) {
                    val posts = response.body()
                    posts?.let {
                        val reversedList = it.reversed()
                        val adapter = PostAdapter(requireContext(), reversedList)
                        recyclerView.adapter = adapter
                    }
                } else {
                    showMessage("서버 오류 발생")
                }
            }

            override fun onFailure(call: Call<List<Post>>, t: Throwable) {
                showMessage("오류 발생: " + t.message)
            }
        })
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
