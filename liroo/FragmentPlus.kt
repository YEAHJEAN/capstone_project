package com.example.liroo

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// Retrofit을 사용하여 서버로 데이터를 전송하기 위한 API 인터페이스 정의
interface ApiService {
    @POST("saveToDatabase")
    fun saveToDatabase(@Body data: YourDataModel): Call<Void>

    companion object {
        fun create(): ApiService {
            val retrofit = Retrofit.Builder().baseUrl("http://10.0.2.2:3001/") // 실제 서버 URL로 변경
                .addConverterFactory(GsonConverterFactory.create()).build()
            return retrofit.create(ApiService::class.java)
        }
    }
}

// 서버로 전송될 데이터 모델 정의
data class YourDataModel(val userid: String, val texts: List<TextData>)
data class TextData(val title: String, val author: String)

class FragmentPlus : Fragment() {
    private val CAMERA_REQUEST_CODE = 100
    private val GALLERY_REQUEST_CODE = 101
    private var extractedText: String = ""
    private lateinit var editText: EditText

    private lateinit var editTextContainer: LinearLayout

    private val titleEditTexts = ArrayList<EditText>() // 제목 EditText 리스트
    private val authorEditTexts = ArrayList<EditText>() // 저자 EditText 리스트

    private val PREFERENCE = "com.example.liroo"

    private var pairCounter = 2 // 한 번에 추가되는 EditText 쌍의 개수를 정하는 변수

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragplus, container, false)

        editTextContainer = view.findViewById(R.id.editTextContainer)

        val addEditTextButton = view.findViewById<AppCompatButton>(R.id.addEditTextButton)
        addEditTextButton.setOnClickListener {
            addEditText()
        }

        editText = view.findViewById(R.id.editText) as EditText

        val cameraButton = view.findViewById<ImageButton>(R.id.cameraButton)
        val galleryButton = view.findViewById<ImageButton>(R.id.galleryButton)
        val saveButton = view.findViewById<Button>(R.id.camsaveButton)

        val imageView = view.findViewById<ImageView>(R.id.imageView)

        // SharedPreferences를 사용하여 사용자 아이디 가져오기
        val sharedPref = activity?.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        val id = sharedPref?.getString("id", "") ?: ""

        // 카메라 버튼 클릭 시
        cameraButton.setOnClickListener {
            startCamera()
            imageView.visibility = View.GONE
        }

        // 갤러리 버튼 클릭 시
        galleryButton.setOnClickListener {
            startGallery()
            imageView.visibility = View.GONE
        }

        // 저장 버튼 클릭 시
        saveButton.setOnClickListener {
            val modifiedText = editText.text.toString()

            val titles = mutableListOf<String>()
            val authors = mutableListOf<String>()

            val title = ArrayList<String>()
            val author = ArrayList<String>()

            // 추가된 EditText 쌍에서 제목과 저자 정보 추출
            for (i in 0 until editTextContainer.childCount) {
                val pair = editTextContainer.getChildAt(i) as LinearLayout
                val titleEditText = pair.findViewById<EditText>(R.id.titleEditText)
                val authorEditText = pair.findViewById<EditText>(R.id.authorEditText)

                titles.add(titleEditText.text.toString())
                authors.add(authorEditText.text.toString())
            }

            // 사용자 아이디 가져오기
            val sharedPref = activity?.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
            val id = sharedPref?.getString("id", "") ?: ""

            // 서버로 데이터 전송
            saveToDatabase(id, titles, authors)
        }
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

    // 서버에 데이터 저장
    private fun saveToDatabase(id: String, titles: List<String>, authors: List<String>) {
        val shelf = mutableListOf<TextData>()

        // 텍스트가 비어 있는지 확인하고 비어 있으면 경고 메시지 표시
        if (titles.any { it.isEmpty() } || authors.any { it.isEmpty() }) {
            showToast("제목과 저자 모두 입력해주세요.")
            return
        }

        // shelf 리스트에 데이터 추가
        for (i in titles.indices) {
            val textData = TextData(titles[i], authors[i])
            shelf.add(textData)
        }

        val apiService = ApiService.create()
        val data = YourDataModel(id, shelf)
        val call = apiService.saveToDatabase(data)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    showToast("데이터 저장 성공")

                    // 저장에 성공하면 FragmentShelf 이동
                    val fragmentShelf = FragmentShelf()
                    val transaction: FragmentTransaction? = fragmentManager?.beginTransaction()
                    transaction?.replace(R.id.fragment_container, fragmentShelf)
                    transaction?.addToBackStack(null)
                    transaction?.commit()
                } else {
                    showToast("데이터 저장 실패")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showToast("통신 실패")
                t.printStackTrace()
            }
        })
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

        // 추가된 EditText 쌍에 대한 removePairButton 추가
        val removePairButton = newEditTextPair.findViewById<Button>(R.id.removePairButton)
        removePairButton.setOnClickListener {
            removeEditText(newEditTextPair)
        }

        titleEditTexts.add(titleEditText)
        authorEditTexts.add(authorEditText)
    }

    // removePairButton을 클릭하여 EditText 쌍을 제거하는 함수
    private fun removeEditText(editTextPair: View) {
        // 부모 레이아웃에서 해당 EditText 쌍을 제거
        editTextContainer.removeView(editTextPair)

        // 해당 EditText 쌍을 리스트에서 제거
        val titleEditText = editTextPair.findViewById<EditText>(R.id.titleEditText)
        val authorEditText = editTextPair.findViewById<EditText>(R.id.authorEditText)
        titleEditTexts.remove(titleEditText)
        authorEditTexts.remove(authorEditText)
    }
}