package com.example.liroo

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.POST
import retrofit2.http.Path

interface PostApi2 {
    @POST("posts/{post_id}/like")
    fun likePost(@Path("post_id") postId: Int): Call<Unit>

    @POST("posts/{post_id}/unlike")
    fun unlikePost(@Path("post_id") postId: Int): Call<Unit>
}

class PostAdapter(
    private val context: Context,
    postList: List<PostItem> // 초기화 시에 역순으로 정렬된 리스트를 전달받도록 수정
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {
    fun updatePostList(newPostList: List<PostItem>) {
        postList = newPostList.reversed()
        notifyDataSetChanged()
    }
    var postList: List<PostItem> = postList.reversed() // 역순으로 정렬하여 저장
    private val retrofit = Retrofit.Builder()
        .baseUrl("http://ec2-3-34-240-75.ap-northeast-2.compute.amazonaws.com:3000/") // 실제 서버 URL로 변경
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val postApi2 = retrofit.create(PostApi2::class.java)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.your_item_layout, parent, false)
        return PostViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val currentPost = postList[position]
        // 이미지 로드 및 표시
        // 이미지 로드 및 표시
        val imageUrl = currentPost.imageUrl // 이미지 URL 가져오기
        Glide.with(holder.itemView.context)
            .load(imageUrl)
            .diskCacheStrategy(DiskCacheStrategy.NONE) // 캐시 사용 안 함
            .into(holder.imageBook) // imageBook ImageView에 이미지 설정

        holder.textTitle.text = currentPost.title
        holder.textId.text = currentPost.user_id
        holder.textBookTitle.text = currentPost.book_title  // 책 제목 설정
        holder.textLikes.text = "좋아요 ${currentPost.likes}개"

// 리스트 아이템 클릭 리스너 설정
        holder.itemView.setOnClickListener {
            val fragment = FragmentPostDetail()
            val bundle = Bundle()
            bundle.putString("post_id", currentPost.post_id.toString())
            fragment.arguments = bundle

            val fragmentManager =
                (holder.itemView.context as AppCompatActivity).supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }



        holder.buttonLike.setImageResource(
            if (currentPost.isLiked) R.drawable.round_thumb_up_alt_white_24
            else R.drawable.outline_thumb_up_alt_white_24
        )
        // 리스트 아이템 클릭 리스너 설정
        holder.buttonLike.setOnClickListener {
            val isLiked = currentPost.isLiked

            if (isLiked) {
                // 이미 좋아요를 누른 상태이므로, 좋아요 취소 요청을 보냅니다.
                postApi2.unlikePost(currentPost.post_id).enqueue(object : Callback<Unit> {
                    override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                        if (response.isSuccessful) {
                            currentPost.likes--
                            currentPost.isLiked = false
                            holder.buttonLike.setImageResource(R.drawable.outline_thumb_up_alt_white_24)
                            holder.textLikes.text = "좋아요 ${currentPost.likes}개"
                        } else {
                            showMessage("좋아요 취소 실패")
                        }
                    }

                    override fun onFailure(call: Call<Unit>, t: Throwable) {
                        showMessage("네트워크 오류 발생")
                    }
                })
            } else {
                // 좋아요를 누르지 않은 상태이므로, 좋아요 요청을 보냅니다.
                postApi2.likePost(currentPost.post_id).enqueue(object : Callback<Unit> {
                    override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                        if (response.isSuccessful) {
                            currentPost.likes++
                            currentPost.isLiked = true
                            holder.buttonLike.setImageResource(R.drawable.round_thumb_up_alt_white_24)
                            holder.textLikes.text = "좋아요 ${currentPost.likes}개"
                        } else {
                            showMessage("좋아요 실패")
                        }
                    }

                    override fun onFailure(call: Call<Unit>, t: Throwable) {
                        showMessage("네트워크 오류 발생")
                    }
                })
            }
        }
    }

    override fun getItemCount(): Int {
        return postList.size
    }

    class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        val textId: TextView = itemView.findViewById(R.id.textContent)
        val textBookTitle: TextView = itemView.findViewById(R.id.textBookTitle)  // 책 제목을 보여줄 TextView 추가
        val buttonLike: ImageButton = itemView.findViewById(R.id.btnLike)
        val textLikes: TextView = itemView.findViewById(R.id.textLikes)
        val imageBook: ImageView = itemView.findViewById(R.id.imageBook) // ImageView로 선언된 부분

    }

    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}