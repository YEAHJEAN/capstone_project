package com.example.liroo

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

data class YourDataModel(val text: String) // 여기에 데이터 모델을 정의합니다.

class FragmentCam : Fragment() {
    private val CAMERA_REQUEST_CODE = 100
    private val GALLERY_REQUEST_CODE = 101
    private var extractedText: String = "" // 추출된 텍스트 저장 변수
    private lateinit var editText: EditText // editText 변수 선언

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragcam, container, false)
        editText = view.findViewById(R.id.editText) as EditText // editText 초기화

        val cameraButton = view.findViewById<Button>(R.id.cameraButton)
        val galleryButton = view.findViewById<Button>(R.id.galleryButton)
        val saveButton = view.findViewById<Button>(R.id.camsaveButton) // Save 버튼 추가


        val imageView = view.findViewById<ImageView>(R.id.imageView)
        val textView = view.findViewById<TextView>(R.id.ocrResultTextView)
        val editText = view.findViewById<EditText>(R.id.editText) // 텍스트 수정을 위한 EditText

        cameraButton.setOnClickListener {
            startCamera()
            imageView.visibility = View.GONE // 이미지뷰 숨기기
            textView.visibility = View.GONE // 텍스트뷰 숨기기
        }

        galleryButton.setOnClickListener {
            startGallery()
            imageView.visibility = View.GONE // 이미지뷰 숨기기
            textView.visibility = View.GONE // 텍스트뷰 숨기기
        }

        saveButton.setOnClickListener {
            // EditText에서 사용자가 수정한 텍스트를 추출하여 저장
            val modifiedText = editText.text.toString()
            extractedText = modifiedText

            // 여기서 추출된 텍스트를 저장하고 필요한 후속 작업을 수행합니다.
            // 예를 들어, 데이터베이스에 저장하거나 다른 처리를 할 수 있습니다.

            // 수정된 텍스트를 텍스트뷰에 표시하여 사용자에게 보여줍니다.
            textView.text = modifiedText
            textView.visibility = View.VISIBLE
            editText.visibility = View.GONE

            // Save 버튼을 눌렀을 때 서버로 데이터를 전송하도록 처리합니다.
            saveToDatabase(extractedText)
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
                        MediaStore.Images.Media.getBitmap(requireContext().contentResolver, selectedImageUri)
                    processImage(imageBitmap)
                }
            }
        }
    }

    private fun saveToDatabase(text: String) {
        val apiService = ApiService.create()
        val data = YourDataModel(text)

        val call = apiService.saveToDatabase(data)
        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    // 서버에 데이터가 성공적으로 저장됨
                    showToast("데이터 저장 성공")
                } else {
                    // 서버에 데이터 저장 실패
                    showToast("데이터 저장 실패")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                // 통신 실패 처리
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
                // OCR 결과 처리 및 TextView에 출력
                val ocrText = StringBuilder()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        ocrText.append(line.text).append("\n")
                    }
                }

                extractedText = ocrText.toString() // 추출된 텍스트 저장
                editText.setText(extractedText)
                editText.visibility = View.VISIBLE

                textView?.visibility = View.GONE

                textView?.text = ocrText.toString()
                textView?.visibility = View.VISIBLE // 텍스트뷰 보이기

                imageView?.setImageBitmap(bitmap)
                imageView?.visibility = View.VISIBLE // 이미지뷰 보이기
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()

    }
}
