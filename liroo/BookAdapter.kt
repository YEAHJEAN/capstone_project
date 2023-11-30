package com.example.liroo

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

private val PREFERENCE = "com.example.liroo" // SharedPreferences 키 값
private val client = OkHttpClient()

class BookAdapter(

    private val context: Context,
    private var bookList: MutableList<Book>  // bookList를 MutableList로 변경

) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private var expandedPosition = -1

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bookTitle: TextView = itemView.findViewById(R.id.bookTitle)
        val bookAuthor: TextView = itemView.findViewById(R.id.bookAuthor)
        val bookIsbn: TextView = itemView.findViewById(R.id.bookIsbn)
        val buttonLayout: ViewGroup = itemView.findViewById(R.id.buttonLayout) // 버튼 레이아웃
        val imageBook: ImageView = itemView.findViewById(R.id.imageBook) // ImageView로 선언된 부분

        init {
            // ViewHolder가 만들어질 때 버튼 레이아웃은 일단 숨기기
            buttonLayout.visibility = View.GONE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        // XML 레이아웃에서 뷰 생성
        val view = LayoutInflater.from(context).inflate(R.layout.your_book_layout, parent, false)
        return BookViewHolder(view)
    }

    override fun getItemCount(): Int {
        return bookList.size
    }

    // 뷰 홀더에 데이터 바인딩
    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val currentBook = bookList[position]
        val imageUrl = currentBook.imageUrl // 이미지 URL 가져오기
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.NONE) // 캐시 사용 안 함
            .into(holder.imageBook) // imageBook ImageView에 이미지 설정

        // 책 정보를 TextView에 설정
        holder.bookTitle.text = currentBook.book_title
        holder.bookAuthor.text = currentBook.author
        holder.bookIsbn.text = currentBook.isbn

        // 리스트 아이템 클릭 시 버튼 레이아웃 표시/숨김 토글
        val isExpanded = position == expandedPosition
        holder.buttonLayout.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.itemView.isActivated = isExpanded

        holder.itemView.setOnClickListener {
            expandedPosition = if (isExpanded) -1 else position
            notifyDataSetChanged()
        }

        // 게시물 작성 버튼 클릭 이벤트 처리
        val postButton = holder.itemView.findViewById<Button>(R.id.postButton)
        postButton.setOnClickListener {
            // 버튼 숨기기
            holder.buttonLayout.visibility = View.GONE

            // Fragment 교체
            val fragment = FragmentPostBook()
            val bundle = Bundle()
            bundle.putString("isbnData", currentBook.isbn) // ISBN 데이터 전달
            fragment.arguments = bundle

            val fragmentManager =
                (holder.itemView.context as AppCompatActivity).supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(
                    R.id.fragment_container,
                    fragment
                ) // R.id.fragment_container에 FragmentPostBook 교체
                .addToBackStack(null)
                .commit()
        }

        // 책 정보 수정 버튼 클릭 이벤트 처리
        val editButton = holder.itemView.findViewById<Button>(R.id.editButton)
        editButton.setOnClickListener {
            // 책 정보 삭제 로직
            deleteBookInfo(currentBook, position) // position 파라미터 추가

            // 버튼 숨기기
            holder.buttonLayout.visibility = View.GONE
        }
    }

    private fun getUserId(): String? {
        val sharedPref = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        return sharedPref?.getString("id", null)
    }

    private fun deleteBookInfo(book: Book, position: Int) { // position 파라미터 추가
        val userId = getUserId()

        if (userId != null) {
            val url = "http://ec2-3-34-240-75.ap-northeast-2.compute.amazonaws.com:3000/deleteData"

            val jsonObject = JSONObject().apply {
                put("book_title", book.book_title)
                put("author", book.author)
                put("isbn", book.isbn)
                put("user_id", userId)
            }

            val requestBody = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                jsonObject.toString()
            )

            val request = Request.Builder()
                .url(url)
                .delete(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("BookAdapter", "Error: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        context?.let {
                            GlobalScope.launch(Dispatchers.Main) {
                                Toast.makeText(it, "삭제 성공", Toast.LENGTH_SHORT).show()
                                bookList.removeAt(position) // bookList에서 해당 책 제거
                                notifyDataSetChanged() // RecyclerView에 데이터 변경 알림
                            }
                        }
                    } else {
                        Log.e("BookAdapter", "데이터 삭제 실패")
                    }
                }
            })
        }
    }
}
