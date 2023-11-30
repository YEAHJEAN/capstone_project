package com.example.liroo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder

class FragmentPlus : Fragment() {
    private var currentPairEditTextLayout: View? = null

    private val CAMERA_REQUEST_CODE = 100
    private val GALLERY_REQUEST_CODE = 101
    private var extractedText: String = ""
    private lateinit var editText: EditText
    private lateinit var editTextContainer: LinearLayout
    private val titleEditTexts = ArrayList<EditText>() // 제목 EditText 리스트
    private val authorEditTexts = ArrayList<EditText>() // 저자 EditText 리스트
    private val isbnEditTexts = ArrayList<EditText>() // 저자 EditText 리스트
    private val PREFERENCE = "com.example.liroo"

    private val clientId = "f7O8TCH7OQCBPI8XHbhJ"
    private val clientSecret = "DwS8ZW1dsw"
    private lateinit var autoCompleteTextView: AutoCompleteTextView
    private lateinit var resultRecyclerView: RecyclerView

    private val client = OkHttpClient()

    private var selectedImageUrl: String? = null // 선택된 이미지 URL을 저장하는 변수 추가

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view1 = inflater.inflate(R.layout.pair_edit_text, container, false)
        editTextContainer = view1.findViewById(R.id.editTextContainer)
        autoCompleteTextView = view1.findViewById(R.id.autoCompleteTextView) // 초기화 추가


        val view = inflater.inflate(R.layout.fragplus, container, false)
        editTextContainer = view.findViewById(R.id.editTextContainer)
        val addEditTextButton = view.findViewById<AppCompatButton>(R.id.addEditTextButton)
        addEditTextButton.setOnClickListener {
            val pairEditTextLayout = layoutInflater.inflate(R.layout.pair_edit_text, null)
            val searchButton = pairEditTextLayout.findViewById<Button>(R.id.searchButton)
            val autoCompleteTextView =
                pairEditTextLayout.findViewById<AutoCompleteTextView>(R.id.autoCompleteTextView)

            searchButton.setOnClickListener {
                val query = autoCompleteTextView.text.toString()
                if (query.isNotEmpty()) {
                    fetchRelatedKeywords(query) // 검색어가 있다면 관련 키워드를 가져옵니다.
                    if (resultRecyclerView.visibility == View.GONE) {
                        resultRecyclerView.visibility = View.VISIBLE
                    } else {
                        resultRecyclerView.visibility = View.GONE
                    }
                } else {
                    Log.e("FragmentPlus", "검색어를 입력해주세요.")
                }
                currentPairEditTextLayout = pairEditTextLayout
            }
            editTextContainer.addView(pairEditTextLayout)
            val removePairButton = pairEditTextLayout.findViewById<Button>(R.id.removePairButton)
            removePairButton.setOnClickListener {
                editTextContainer.removeView(pairEditTextLayout)
            }
        }
        editText = view.findViewById(R.id.editText) as EditText
        val cameraButton = view.findViewById<ImageButton>(R.id.cameraButton)
        val galleryButton = view.findViewById<ImageButton>(R.id.galleryButton)

        val saveButton = view.findViewById<Button>(R.id.camSaveButton)
        saveButton.setOnClickListener {
            for (i in 0 until editTextContainer.childCount) {
                val pairEditTextLayout = editTextContainer.getChildAt(i)
                val editTextTitle = pairEditTextLayout.findViewById<EditText>(R.id.titleEditText)
                val editTextAuthor = pairEditTextLayout.findViewById<EditText>(R.id.authorEditText)
                val editTextIsbn = pairEditTextLayout.findViewById<EditText>(R.id.isbnEditText)

                val title = editTextTitle.text.toString().trim()
                val author = editTextAuthor.text.toString().trim()
                val isbn = editTextIsbn.text.toString().trim()

                if (title.isNotEmpty() && author.isNotEmpty() && isbn.isNotEmpty() && selectedImageUrl != null) {
                    // 서버에 데이터 저장 함수 호출 (사용자 ID와 데이터 전달)
                    saveToServer(title, author, isbn, selectedImageUrl!!)
                } else {
                    Toast.makeText(requireContext(), "제목, 저자, ISBN을 모두 입력해주세요.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }


        val imageView = view.findViewById<ImageView>(R.id.imageView)
        val sharedPref = activity?.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        val id = sharedPref?.getString("id", "") ?: ""

        cameraButton.setOnClickListener {
            startCamera()
            imageView.visibility = View.GONE
        }

        galleryButton.setOnClickListener {
            startGallery()
            imageView.visibility = View.GONE
        }

        // RecyclerView initialization and setup
        resultRecyclerView = view.findViewById(R.id.resultRecyclerView)
        resultRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        return view
    }

    // 카메라 앱 열기
    private fun startCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    // 갤러리 열기
    private fun startGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    // 액티비티 결과 처리
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == AppCompatActivity.RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    processImage(imageBitmap)
                }

                GALLERY_REQUEST_CODE -> {
                    val selectedImageUri = data?.data
                    val imageBitmap = MediaStore.Images.Media.getBitmap(
                        requireContext().contentResolver, selectedImageUri
                    )
                    processImage(imageBitmap)
                }
            }
        }
    }

    // 이미지 처리
    private fun processImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        val imageView = view?.findViewById<ImageView>(R.id.imageView)

        recognizer.process(image).addOnSuccessListener { visionText ->
            val ocrText = StringBuilder()

            for (block in visionText.textBlocks) {
                for (line in block.lines) {
                    ocrText.append(line.text).append("")
                }
            }

            extractedText = ocrText.toString()
            editText.setText(extractedText)
            editText.visibility = View.VISIBLE


            imageView?.setImageBitmap(bitmap)
            imageView?.visibility = View.VISIBLE
        }.addOnFailureListener { e ->
            e.printStackTrace()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun addEditText() {
        val inflater =
            requireContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val newEditTextPair = inflater.inflate(R.layout.pair_edit_text, null) as LinearLayout

        // 부모 레이아웃에 새로운 EditText 쌍 추가
        editTextContainer.addView(newEditTextPair)

        // 새로 추가된 EditText 쌍에 대해 제목과 저자 EditText 추가
        val titleEditText = newEditTextPair.findViewById<EditText>(R.id.titleEditText)
        val authorEditText = newEditTextPair.findViewById<EditText>(R.id.authorEditText)
        val isbnEditText = newEditTextPair.findViewById<EditText>(R.id.isbnEditText)

        // 추가된 EditText 쌍에 대한 searchButton 추가
        val searchButton = newEditTextPair.findViewById<Button>(R.id.searchButton)
        searchButton.setOnClickListener {
            val query = autoCompleteTextView.text.toString()
            if (query.isNotEmpty()) {
                fetchRelatedKeywords(query)
            } else {
                Log.e("FragmentPlus", "autoCompleteTextView or saveButton is null")
            }
        }
        // Search button click event logic here...
        // 추가된 EditText 쌍에 대한 removePairButton 추가
        val removePairButton = newEditTextPair.findViewById<Button>(R.id.removePairButton)
        removePairButton.setOnClickListener {
            removeEditText(newEditTextPair)
        }

        titleEditTexts.add(titleEditText)
        authorEditTexts.add(authorEditText)
        isbnEditTexts.add(isbnEditText)
    }

    // removePairButton을 클릭하여 EditText 쌍을 제거하는 함수
    private fun removeEditText(editTextPair: View) {
        // 부모 레이아웃에서 해당 EditText 쌍을 제거
        editTextContainer.removeView(editTextPair)

        // 해당 EditText 쌍을 리스트에서 제거
        val titleEditText = editTextPair.findViewById<EditText>(R.id.titleEditText)
        val authorEditText = editTextPair.findViewById<EditText>(R.id.authorEditText)
        val isbnEditText = editTextPair.findViewById<EditText>(R.id.isbnEditText)
        titleEditTexts.remove(titleEditText)
        authorEditTexts.remove(authorEditText)
        isbnEditTexts.remove(isbnEditText)
    }


    private fun processSelectedItem(selectedItem: String, imageUrl: String) {
        // 선택한 항목을 가공하여 원하는 데이터 추출
        val selectedTitle = selectedItem.split(" - ")[0] // 제목만 추출
        val selectedAuthor = selectedItem.split(" - ")[1] // 저자만 추출
        val selectedIsbn = selectedItem.split(" - ")[2] // ISBN만 추출

        // 선택한 제목과 저자를 EditText 등에 표시하여 사용자가 수정할 수 있도록 합니다.
        val editTextTitle = currentPairEditTextLayout?.findViewById<EditText>(R.id.titleEditText)
        val editTextAuthor = currentPairEditTextLayout?.findViewById<EditText>(R.id.authorEditText)
        val editTextIsbn = currentPairEditTextLayout?.findViewById<EditText>(R.id.isbnEditText)

        editTextTitle?.setText(selectedTitle)
        editTextAuthor?.setText(selectedAuthor)
        editTextIsbn?.setText(selectedIsbn)

        // 사용자가 정보를 수정할 수 있도록 EditText를 활성화시킵니다.
        editTextTitle?.isEnabled = true
        editTextAuthor?.isEnabled = true
        editTextIsbn?.isEnabled = true

        selectedImageUrl = imageUrl // 선택된 이미지 URL을 변수에 저장

        resultRecyclerView.visibility = View.GONE
    }

    private fun fetchRelatedKeywords(query: String) {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://openapi.naver.com/v1/search/book?query=$encodedQuery"

        val request = Request.Builder()
            .url(url)
            .addHeader("X-Naver-Client-Id", clientId)
            .addHeader("X-Naver-Client-Secret", clientSecret)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("FragmentPlus", "Error: ${e.message}")
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
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
                            resultList.add(displayText)// 이미지 URL 가져오기

                            val imageURL = itemObject.getString("image")
                            imageURLList.add(imageURL)
                        }

                        // fetchRelatedKeywords(query) 함수 내부에서 검색 결과를 리사이클러뷰에 표시하는 부분을 추가합니다.
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
                        Log.e("FragmentPlus", "No 'items' field in the JSON response")
                    }
                }
            }
        })

    }

    private fun saveToServer(title: String, author: String, isbn: String, imageUrl: String) {
        val userId = getUserId() // 사용자 ID 가져오기

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

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    Log.e("FragmentPlus", "Error: ${e.message}")
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
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
                        Log.e("FragmentPlus", "데이터 저장 실패")
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

    private class BookAdapter(
        private val itemList: MutableList<String>,
        private val itemClickListener: (String) -> Unit
    ) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

        class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val bookTextView: TextView = itemView.findViewById(R.id.bookTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_book1, parent, false)
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