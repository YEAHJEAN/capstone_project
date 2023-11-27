package com.example.liroo

import android.content.Context
import android.content.SharedPreferences
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
import retrofit2.http.Query

data class Book(
    val title: String,
    val author: String,
    val isbn: String,
)

interface BookApi {
    @GET("books")
    fun getBooks(@Query("userId") userId: String): Call<List<Book>>
}

class FragmentShelf : Fragment() {
    private lateinit var userPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragbook, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView: RecyclerView = view.findViewById(R.id.bookRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // SharedPreferences 객체 생성
        userPrefs = requireContext().getSharedPreferences("com.example.liroo", Context.MODE_PRIVATE)

        // SharedPreferences에서 사용자 ID 가져오기
        val userId = userPrefs.getString("id", null)

        if (!userId.isNullOrEmpty()) {
            val retrofit = Retrofit.Builder()
                .baseUrl("http://ec2-3-34-240-75.ap-northeast-2.compute.amazonaws.com:3000/") // 실제 서버 URL로 변경
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val bookApi = retrofit.create(BookApi::class.java)

            val call = bookApi.getBooks(userId)

            call.enqueue(object : Callback<List<Book>> {
                override fun onResponse(call: Call<List<Book>>, response: Response<List<Book>>) {
                    if (response.isSuccessful) {
                        val books = response.body()?.toMutableList() // toMutableList()를 사용하여 MutableList로 변환
                        books?.let {
                            val bookAdapter = BookAdapter(requireContext(), it)
                            recyclerView.adapter = bookAdapter // RecyclerView에 Adapter 설정
                        }
                    } else {
                        showMessage("서버 오류 발생")
                    }
                }

                override fun onFailure(call: Call<List<Book>>, t: Throwable) {
                    showMessage("오류 발생: " + t.message)
                }
            })
        } else {
            showMessage("로그인한 사용자 ID가 없습니다.")
            // 로그인이 되어 있지 않은 경우 로그인 화면으로 이동하거나 원하는 작업을 수행할 수 있습니다.
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
