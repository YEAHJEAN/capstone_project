package com.example.liroo

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.fragment.app.Fragment
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
    private lateinit var resultListView: ListView
    private val client = OkHttpClient()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        resultListView = view.findViewById<ListView>(R.id.resultListView)
        autoCompleteTextView = view.findViewById(R.id.autoCompleteTextView)

        val saveButton = view.findViewById<Button>(R.id.saveButton)
        saveButton.setOnClickListener {
            val editTextTitle = view.findViewById<EditText>(R.id.editTextTitle)
            val editTextAuthor = view.findViewById<EditText>(R.id.editTextAuthor)

            val title = editTextTitle.text.toString().trim()
            val author = editTextAuthor.text.toString().trim()

            if (title.isNotEmpty() && author.isNotEmpty()) {
                // 서버에 데이터 저장 함수 호출 (사용자 ID와 데이터 전달)
                saveToServer(title, author)
            } else {
                // 제목 또는 저자가 비어있는 경우 처리
                // 예를 들어 사용자에게 오류 메시지 표시
            }
        }

        // 결과 목록에서 항목을 선택할 때의 동작을 정의합니다.
        resultListView.setOnItemClickListener { parent, _, position, _ ->
            val selectedItem = parent.getItemAtPosition(position) as String
            processSelectedItem(selectedItem)
        }
    }

    private fun processSelectedItem(selectedItem: String) {
        // 선택한 항목을 가공하여 원하는 데이터 추출
        val selectedTitle = selectedItem.split(" - ")[0] // 제목만 추출
        val selectedAuthor = selectedItem.split(" - ")[1] // 저자만 추출

        // 선택한 데이터를 처리하거나 표시할 수 있도록 원하는 작업을 수행합니다.
        Log.d("FragmentInput", "Selected Title: $selectedTitle")
        Log.d("FragmentInput", "Selected Author: $selectedAuthor")

        // 선택한 제목과 저자를 EditText 등에 표시하여 사용자가 수정할 수 있도록 합니다.
        val editTextTitle = view?.findViewById<EditText>(R.id.editTextTitle)
        val editTextAuthor = view?.findViewById<EditText>(R.id.editTextAuthor)

        editTextTitle?.setText(selectedTitle)
        editTextAuthor?.setText(selectedAuthor)

        // 사용자가 정보를 수정할 수 있도록 EditText를 활성화시킵니다.
        editTextTitle?.isEnabled = true
        editTextAuthor?.isEnabled = true
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fraginput, container, false)

        autoCompleteTextView = view.findViewById(R.id.autoCompleteTextView)
        val searchButton = view.findViewById<Button>(R.id.searchButton)

        searchButton.setOnClickListener {
            val query = autoCompleteTextView.text.toString()
            if (query.isNotEmpty()) {
                fetchRelatedKeywords(query)
            }
        }

        return view
    }

    private fun fetchRelatedKeywords(query: String) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://openapi.naver.com/v1/search/book?query=$encodedQuery"

        val request = Request.Builder()
            .url(url)
            .addHeader("X-Naver-Client-Id", clientId)
            .addHeader("X-Naver-Client-Secret", clientSecret)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("SearchFragment", "Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val responseString = responseBody.string()
                    val jsonObject = JSONObject(responseString)

                    if (jsonObject.has("items")) {
                        val itemsArray = jsonObject.getJSONArray("items")

                        val resultList = mutableListOf<String>()

                        for (i in 0 until itemsArray.length()) {
                            val itemObject = itemsArray.getJSONObject(i)
                            val title = itemObject.getString("title")
                            val author = itemObject.getString("author")
                            val displayText = "$title - $author"
                            resultList.add(displayText)
                        }

                        GlobalScope.launch(Dispatchers.Main) {
                            val adapter = ArrayAdapter(
                                requireContext(),
                                android.R.layout.simple_list_item_1,
                                resultList
                            )
                            resultListView.adapter = adapter
                        }
                    } else {
                        Log.e("SearchFragment", "No 'items' field in the JSON response")
                    }
                }
            }
        })
    }

    private fun saveToServer(title: String, author: String) {
        val userId = getUserId() // 사용자 ID 가져오기

        if (userId != null && title.isNotEmpty() && author.isNotEmpty()) {
            // 서버의 엔드포인트 URL을 적절히 변경하세요
            val url = "http://10.0.2.2:3001/saveData"

            // 제목과 저자를 JSON 객체로 생성
            val jsonObject = JSONObject().apply {
                put("title", title)
                put("author", author)
                put("user_id", userId)
            }

            val requestBody = RequestBody.create(
                "application/json; charset=utf-8".toMediaTypeOrNull(),
                jsonObject.toString()
            )

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                // 필요한 경우 헤더 추가
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("FragmentInput", "Error: ${e.message}")
                    // 실패 시 처리하거나 오류 메시지 표시
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        // 성공 시 처리
                        // 예를 들어 성공 메시지 표시 또는 다른 작업 수행
                        Log.d("FragmentInput", "데이터 저장 성공")

                        // 사용자에게 '저장 성공' 메시지를 Toast로 표시
                        GlobalScope.launch(Dispatchers.Main) {
                            Toast.makeText(activity, "저장 성공", Toast.LENGTH_SHORT).show()

                            // 데이터 저장 성공 시 FragmentShelf 이동
                            val fragmentManager = parentFragmentManager
                            val fragmentTransaction = fragmentManager.beginTransaction()
                            fragmentTransaction.replace(R.id.fragment_container, FragmentShelf())
                            fragmentTransaction.addToBackStack(null) // 선택 사항: 트랜잭션을 백 스택에 추가
                            fragmentTransaction.commit()
                        }
                    } else {
                        // 실패 시 처리
                        // 예를 들어 오류 메시지 표시 또는 다른 작업 수행
                        Log.e("FragmentInput", "데이터 저장 실패")
                    }
                }
            })
        } else {
            // 사용자 ID, 제목 또는 저자가 비어있는 경우 처리
            // 예를 들어 사용자에게 오류 메시지 표시
        }
    }

    private fun getUserId(): String? {
        val sharedPref = activity?.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        return sharedPref?.getString("id", null)
    }
}
