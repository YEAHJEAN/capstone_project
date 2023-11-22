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
import androidx.fragment.app.Fragment
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

interface ApiService {
    @POST("saveToDatabase")
    fun saveToDatabase(@Body data: YourDataModel): Call<Void>

    companion object {
        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl("http://10.0.2.2:3001/") // 실제 서버 URL로 변경
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(ApiService::class.java)
        }
    }
}

data class YourDataModel(val userid: String, val texts: List<TextData>)

data class TextData(val title: String, val author: String)


class FragmentCam : Fragment() {
    private val CAMERA_REQUEST_CODE = 100
    private val GALLERY_REQUEST_CODE = 101
    private var extractedText: String = ""
    private lateinit var editText: EditText

    private lateinit var editTextContainer: LinearLayout

    private val titleEditTexts = ArrayList<EditText>() // 제목 EditText 리스트
    private val authorEditTexts = ArrayList<EditText>() // 저자 EditText 리스트

    private val PREFERENCE = "com.example.liroo"

    private var pairCounter = 2 // 한 번에 추가되는 EditText 쌍의 개수를 정하는 변수

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val myBookShelfButton = view.findViewById<Button>(R.id.myBookShelfButton)
        myBookShelfButton.setOnClickListener {
            val fragment = FragmentCamBook()
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(
                    R.id.container,
                    fragment
                ) // 'container'는 프래그먼트가 들어갈 레이아웃의 ID입니다. 실제 레이아웃 ID로 변경하세요.
                .addToBackStack(null)
                .commit()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragcam, container, false)

        editTextContainer = view.findViewById(R.id.editTextContainer)

        val addEditTextButton = view.findViewById<Button>(R.id.addEditTextButton)
        addEditTextButton.setOnClickListener {
            addEditText()
        }

        editText = view.findViewById(R.id.editText) as EditText

        val cameraButton = view.findViewById<Button>(R.id.cameraButton)
        val galleryButton = view.findViewById<Button>(R.id.galleryButton)
        val saveButton = view.findViewById<Button>(R.id.camsaveButton)

        val imageView = view.findViewById<ImageView>(R.id.imageView)
        val textView = view.findViewById<TextView>(R.id.ocrResultTextView)
        val editText = view.findViewById<EditText>(R.id.editText) // 텍스트 수정을 위한 EditText

        val sharedPref = activity?.getSharedPreferences(PREFERENCE, Context.MODE_PRIVATE)
        val id = sharedPref?.getString("id", "") ?: ""

        cameraButton.setOnClickListener {
            startCamera()
            imageView.visibility = View.GONE
            textView.visibility = View.GONE
        }

        galleryButton.setOnClickListener {
            startGallery()
            imageView.visibility = View.GONE
            textView.visibility = View.GONE
        }

        saveButton.setOnClickListener {
            val modifiedText = editText.text.toString()

            val titles = mutableListOf<String>()
            val authors = mutableListOf<String>()

            val title = ArrayList<String>()
            val author = ArrayList<String>()

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

            saveToDatabase(id, titles, authors)
        }
        return view
    }

    private fun startCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    private fun startGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

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
                    val imageBitmap =
                        MediaStore.Images.Media.getBitmap(
                            requireContext().contentResolver,
                            selectedImageUri
                        )
                    processImage(imageBitmap)
                }
            }
        }
    }

    private fun saveToDatabase(userid: String, titles: List<String>, authors: List<String>) {
        val texts = mutableListOf<TextData>()

        // texts 리스트에 데이터 추가
        for (i in titles.indices) {
            val textData = TextData(titles[i], authors[i])
            texts.add(textData)
        }

        val apiService = ApiService.create()
        val data = YourDataModel(userid, texts)
        val call = apiService.saveToDatabase(data)

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    showToast("데이터 저장 성공")
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

    private fun processImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
        val imageView = view?.findViewById<ImageView>(R.id.imageView)
        val textView = view?.findViewById<TextView>(R.id.ocrResultTextView)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val ocrText = StringBuilder()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        ocrText.append(line.text).append("")
                    }
                }

                extractedText = ocrText.toString()
                editText.setText(extractedText)
                editText.visibility = View.VISIBLE

                textView?.visibility = View.GONE

                textView?.text = ocrText.toString()
                textView?.visibility = View.VISIBLE

                imageView?.setImageBitmap(bitmap)
                imageView?.visibility = View.VISIBLE
            }
            .addOnFailureListener { e ->
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

        titleEditTexts.add(titleEditText)
        authorEditTexts.add(authorEditText)
    }
}
