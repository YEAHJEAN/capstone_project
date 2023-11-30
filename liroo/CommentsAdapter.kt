package com.example.liroo

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
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

private const val PREFERENCE = "com.example.liroo" // SharedPreferences 키 값
private val client = OkHttpClient()

class CommentsAdapter(
    private val context: Context
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    private var comments: MutableList<Comment> = mutableListOf()
    private var expandedPosition = -1

    class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userIdTextView: TextView = itemView.findViewById(R.id.comment_user_id)
        val contentTextView: TextView = itemView.findViewById(R.id.comment_content)
        val deleteButton: Button = itemView.findViewById(R.id.delete_button)
        val deleteLayout: View = itemView.findViewById(R.id.delete_layout)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, @SuppressLint("RecyclerView") position: Int) {
        val currentPosition = holder.adapterPosition
        if (currentPosition == RecyclerView.NO_POSITION) return

        val comment = comments[currentPosition]

        holder.userIdTextView.text = comment.user_id
        holder.contentTextView.text = comment.content

        // 댓글 삭제 버튼 클릭 이벤트 처리
        holder.deleteButton.setOnClickListener {
            deleteComment(holder.bindingAdapterPosition) // bindingAdapterPosition 사용
        }

        // 삭제 레이아웃 펼치기/접기
        if (position == expandedPosition) {
            holder.deleteLayout.visibility = View.VISIBLE
        } else {
            holder.deleteLayout.visibility = View.GONE
        }

        // 댓글 아이템 클릭 이벤트 처리
        holder.itemView.setOnClickListener {
            if (expandedPosition == position) {
                // 이미 펼쳐진 상태인 경우 접기
                expandedPosition = -1
                notifyItemChanged(position)
            } else {
                // 펼치기
                val prevExpandedPosition = expandedPosition
                expandedPosition = position
                notifyItemChanged(prevExpandedPosition)
                notifyItemChanged(expandedPosition)
            }
        }
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    // 댓글 목록 업데이트
    fun submitList(newComments: List<Comment>) {
        comments.clear()
        comments.addAll(newComments.reversed()) // 댓글 목록을 역순으로 정렬하여 추가
        notifyDataSetChanged()
    }

    // 댓글 삭제 기능 추가
    private fun deleteComment(position: Int) {
        val commentToDelete = comments[position] // 삭제할 댓글
        val userId = getUserId()

        if (userId == commentToDelete.user_id) {
            val url = "http://ec2-3-34-240-75.ap-northeast-2.compute.amazonaws.com:3000/comments/${commentToDelete.comment_id}"

            val jsonObject = JSONObject().apply {
                // 댓글 삭제에 필요한 정보를 JSON 형태로 전달
                put("comment_id", commentToDelete.comment_id) // 예시: 댓글 ID
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
                    Log.e("CommentsAdapter", "Error: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        GlobalScope.launch(Dispatchers.Main) {
                            Toast.makeText(context, "댓글 삭제 성공", Toast.LENGTH_SHORT).show()
                            comments.removeAt(position) // 댓글 목록에서 삭제
                            notifyItemRemoved(position) // RecyclerView에서 댓글 제거
                        }
                    } else {
                        Log.e("CommentsAdapter", "댓글 삭제 실패")
                    }
                }
            })
        } else {
            Toast.makeText(context, "댓글 작성자만 삭제할 수 있습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getUserId(): String? {
        val sharedPref = context.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        return sharedPref?.getString("id", null)
    }
}