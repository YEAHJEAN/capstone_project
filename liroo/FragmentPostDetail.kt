package com.example.liroo

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

data class GetPostDetailResponse(
    val success: Boolean,
    val message: String,
    val post: PostItem
)

data class Comment(
    val comment_id: Int,
    val post_id: Int,
    val user_id: String,
    val content: String
)

data class GetCommentsResponse(
    val success: Boolean,
    val message: String,
    val comments: List<Comment>
)

interface GetPostDetailApi {
    @Headers("Accept: application/json")
    @GET("posts/{post_id}")
    fun getPostDetail(@Path("post_id") isbn: String): Call<GetPostDetailResponse>
}

interface GetCommentsApi {
    @Headers("Accept: application/json")
    @GET("posts/{post_id}/comments")
    fun getComments(@Path("post_id") post_id: String): Call<GetCommentsResponse>
}

interface PostCommentApi {
    @Headers("Accept: application/json")
    @POST("posts/{post_id}/comments")
    fun postComment(@Path("post_id") post_id: String, @Body comment: Comment): Call<ResponseBody>
}

class FragmentPostDetail : Fragment() {
    private lateinit var userPrefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_post_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userPrefs =
            requireContext().getSharedPreferences("com.example.liroo", Context.MODE_PRIVATE)

        val commentView = view.findViewById<LinearLayout>(R.id.commentView)
        val commentButton = view.findViewById<ImageButton>(R.id.btnComment)
        commentButton.setOnClickListener {
            if (commentView.visibility == View.GONE) {
                commentView.visibility = View.VISIBLE
            } else {
                commentView.visibility = View.GONE
            }
        }

        val idTextView = view.findViewById<TextView>(R.id.post_id)
        val titleTextView = view.findViewById<TextView>(R.id.post_title)
        val contentTextView = view.findViewById<TextView>(R.id.post_content)
        val isbnTextView = view.findViewById<TextView>(R.id.post_isbn)
        val userIdTextView = view.findViewById<TextView>(R.id.post_user_id)

        val post_id = arguments?.getString("post_id")
        if (post_id == null) {
            showMessage("Post ID 값이 없습니다.")
            return
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("http://ec2-3-34-240-75.ap-northeast-2.compute.amazonaws.com:3000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val getPostDetailApi = retrofit.create(GetPostDetailApi::class.java)

        val callGetPostDetail = getPostDetailApi.getPostDetail(post_id)

        callGetPostDetail.enqueue(object : Callback<GetPostDetailResponse> {
            override fun onResponse(
                call: Call<GetPostDetailResponse>,
                response: Response<GetPostDetailResponse>
            ) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.success) {
                        idTextView.text = apiResponse.post.post_id.toString()
                        titleTextView.text = apiResponse.post.title
                        contentTextView.text = apiResponse.post.content
                        isbnTextView.text = apiResponse.post.isbn
                        userIdTextView.text = apiResponse.post.user_id
                    } else {
                        showMessage("게시물을 가져오는데 실패했습니다.")
                    }
                } else {
                    showMessage("서버 오류 발생")
                }
            }

            override fun onFailure(call: Call<GetPostDetailResponse>, t: Throwable) {
                showMessage("오류 발생: " + t.message)
            }
        })

        val commentsRecyclerView = view.findViewById<RecyclerView>(R.id.commentRecyclerView)
        commentsRecyclerView.layoutManager = LinearLayoutManager(context)

        val commentsAdapter = CommentsAdapter(requireContext())
        commentsRecyclerView.adapter = commentsAdapter


        commentsRecyclerView.adapter = commentsAdapter

        val getCommentsApi = retrofit.create(GetCommentsApi::class.java)

        val callGetComments = getCommentsApi.getComments(post_id)

        callGetComments.enqueue(object : Callback<GetCommentsResponse> {
            override fun onResponse(
                call: Call<GetCommentsResponse>,
                response: Response<GetCommentsResponse>
            ) {
                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null && apiResponse.success) {
                        commentsAdapter.submitList(apiResponse.comments)
                    } else {
                        showMessage("댓글을 가져오는데 실패했습니다.")
                    }
                } else {
                    showMessage("서버 오류 발생")
                }
            }

            override fun onFailure(call: Call<GetCommentsResponse>, t: Throwable) {
                showMessage("오류 발생: " + t.message)
            }
        })

        val commentEditText = view.findViewById<EditText>(R.id.commentEditText)
        val postCommentButton = view.findViewById<Button>(R.id.submitCommentButton)

        postCommentButton.setOnClickListener {
            val sharedPreferences = requireActivity().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
            val userId = userPrefs.getString("id", null)
            if (userId.isNullOrEmpty()) {
                showMessage("로그인이 필요합니다.")
                return@setOnClickListener
            }

            val commentText = commentEditText.text?.toString()?.trim() // 댓글 텍스트를 가져옴 (null이거나 공백일 경우 처리)
            if (commentText.isNullOrBlank()) {
                showMessage("내용을 입력해주세요.") // 댓글 내용이 비어 있을 때 메시지 표시
                return@setOnClickListener
            }

            val comment = Comment(0, post_id.toInt(), userId, commentText)

            val postCommentApi = retrofit.create(PostCommentApi::class.java)
            val callPostComment = postCommentApi.postComment(post_id, comment)

            callPostComment.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        showMessage("댓글이 작성되었습니다.")
                        commentEditText.setText("") // EditText 내용을 지웁니다.

                        // 댓글 작성이 성공하면 다시 댓글 목록을 가져옴
                        val callGetComments = getCommentsApi.getComments(post_id)

                        callGetComments.enqueue(object : Callback<GetCommentsResponse> {
                            override fun onResponse(
                                call: Call<GetCommentsResponse>,
                                response: Response<GetCommentsResponse>
                            ) {
                                if (response.isSuccessful) {
                                    val apiResponse = response.body()
                                    if (apiResponse?.success == true) {
                                        apiResponse.comments?.let { comments ->
                                            commentsAdapter.submitList(comments)
                                        } ?: showMessage("댓글을 가져오는데 실패했습니다.")
                                    } else {
                                        showMessage("댓글을 가져오는데 실패했습니다.")
                                    }
                                } else {
                                    showMessage("서버 오류 발생")
                                }
                            }

                            override fun onFailure(call: Call<GetCommentsResponse>, t: Throwable) {
                                showMessage("오류 발생: " + t.message)
                            }
                        })
                    } else {
                        showMessage("댓글 작성에 실패하였습니다.")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    showMessage("오류 발생: " + t.message)
                }
            })
        }
    }
    private fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
