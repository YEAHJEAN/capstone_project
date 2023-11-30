package com.example.liroo

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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
import java.net.URLEncoder

class FragmentInput : Fragment() {

    private val PREFERENCE = "com.example.liroo" // SharedPreferences 키 값
    private val clientId = "f7O8TCH7OQCBPI8XHbhJ"
    private val clientSecret = "DwS8ZW1dsw"

    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var resultRecyclerView: RecyclerView
    private val client = OkHttpClient()

    private var selectedImageUrl: String? = null // 선택된 이미지 URL을 저장하는 변수 추가


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resultRecyclerView = view.findViewById(R.id.resultRecyclerView)
        autoCompleteTextView = view.findViewById(R.id.autoCompleteTextView)

        val saveButton = view.findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            val editTextTitle = view.findViewById<EditText>(R.id.editTextTitle)
            val editTextAuthor = view.findViewById<EditText>(R.id.editTextAuthor)
            val editTextIsbn = view.findViewById<EditText>(R.id.editTextIsbn)

            val title = editTextTitle.text.toString().trim()
            val author = editTextAuthor.text.toString().trim()
            val isbn = editTextIsbn.text.toString().trim()

            if (title.isNotEmpty() && author.isNotEmpty() && isbn.isNotEmpty() && selectedImageUrl != null) {
                saveToServer(title, author, isbn, selectedImageUrl!!)
            } else {
                // Handle empty fields, show error message to the user if needed
            }
        }

        resultRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val searchButton = view.findViewById<Button>(R.id.searchButton)
        searchButton.setOnClickListener {
            val query = autoCompleteTextView.text.toString()
            if (query.isNotEmpty()) {
                fetchRelatedBooks(query)
            }
        }
    }

    private fun processSelectedItem(selectedItem: String, imageUrl: String) {
        val selectedTitle = selectedItem.split(" - ")[0]
        val selectedAuthor = selectedItem.split(" - ")[1]
        val selectedIsbn = selectedItem.split(" - ")[2]

        val editTextTitle = view?.findViewById<EditText>(R.id.editTextTitle)
        val editTextAuthor = view?.findViewById<EditText>(R.id.editTextAuthor)
        val editTextIsbn = view?.findViewById<EditText>(R.id.editTextIsbn)

        editTextTitle?.setText(selectedTitle)
        editTextAuthor?.setText(selectedAuthor)
        editTextIsbn?.setText(selectedIsbn)

        editTextTitle?.isEnabled = true
        editTextAuthor?.isEnabled = true
        editTextIsbn?.isEnabled = true

        selectedImageUrl = imageUrl // 선택된 이미지 URL을 변수에 저장

        // Clear the RecyclerView
        resultRecyclerView.adapter = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fraginput, container, false)
    }

    private fun fetchRelatedBooks(query: String) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://openapi.naver.com/v1/search/book?query=$encodedQuery"

        val request = Request.Builder()
            .url(url)
            .addHeader("X-Naver-Client-Id", clientId)
            .addHeader("X-Naver-Client-Secret", clientSecret)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("FragmentInput", "Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val responseString = responseBody.string()
                    val jsonObject = JSONObject(responseString)

                    if (jsonObject.has("items")) {
                        val itemsArray = jsonObject.getJSONArray("items")

                        val resultList = mutableListOf<String>()

                        val imageURLList = mutableListOf<String>() // 책 이미지 URL을 저장할 리스트 추가

                        for (i in 0 until itemsArray.length()) {
                            val itemObject = itemsArray.getJSONObject(i)
                            val title = itemObject.getString("title")
                            val author = itemObject.getString("author")
                            val isbn = itemObject.getString("isbn")
                            val displayText = "$title - $author - $isbn"
                            resultList.add(displayText)

                            // 이미지 URL 가져오기
                            val imageURL = itemObject.getString("image")
                            imageURLList.add(imageURL)
                        }

                        GlobalScope.launch(Dispatchers.Main) {
                            val adapter = BookAdapter(resultList) { selectedItem ->
                                val index = resultList.indexOf(selectedItem)
                                val imageUrl = imageURLList.getOrNull(index)
                                imageUrl?.let { url ->
                                    processSelectedItem(selectedItem, url)
                                }
                            }
                            // Set the RecyclerView adapter with the search result
                            resultRecyclerView.adapter = adapter
                        }
                    } else {
                        Log.e("FragmentInput", "No 'items' field in the JSON response")
                    }
                }
            }
        })
    }

    private fun saveToServer(title: String, author: String, isbn: String, imageUrl: String) {
        val userId = getUserId()

        if (userId != null && title.isNotEmpty() && author.isNotEmpty() && isbn.isNotEmpty()) {
            val url = "http://ec2-3-34-240-75.ap-northeast-2.compute.amazonaws.com:3000/saveData"


            val jsonObject = JSONObject().apply {
                put("title", title)
                put("author", author)
                put("isbn", isbn)
                put("user_id", userId)
                put("imageUrl", imageUrl)
            }

            val requestBody = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                jsonObject.toString()
            )

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("FragmentInput", "Error: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        GlobalScope.launch(Dispatchers.Main) {
                            Toast.makeText(activity, "저장 성공", Toast.LENGTH_SHORT).show()

                            val fragmentManager = parentFragmentManager
                            val fragmentTransaction = fragmentManager.beginTransaction()
                            fragmentTransaction.replace(R.id.fragment_container, FragmentShelf())
                            fragmentTransaction.addToBackStack(null)
                            fragmentTransaction.commit()
                        }
                    } else {
                        Log.e("FragmentInput", "데이터 저장 실패")
                    }
                }
            })
        } else {
            // Handle empty fields or user ID being null, show error message if needed
        }
    }

    private fun getUserId(): String? {
        val sharedPref = activity?.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        return sharedPref?.getString("id", null)
    }

    private class BookAdapter(
        private val itemList: MutableList<String>,
        private val itemClickListener: (String) -> Unit
    ) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

        class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val bookTextView: TextView = itemView.findViewById(R.id.bookTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_book, parent, false)
            return BookViewHolder(view)
        }

        override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
            val currentItem = itemList[position]
            holder.bookTextView.text = currentItem
            holder.itemView.setOnClickListener {
                itemClickListener(currentItem)
            }
        }

        override fun getItemCount(): Int {
            return itemList.size
        }
    }
}