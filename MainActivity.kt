package com.example.vapi

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS

class MainActivity : AppCompatActivity() {
    private val CAMERA_REQUEST_CODE = 100
    private val GALLERY_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cameraButton = findViewById<Button>(R.id.cameraButton)
        val galleryButton = findViewById<Button>(R.id.galleryButton)

        val imageView = findViewById<ImageView>(R.id.imageView)
        val textView = findViewById<TextView>(R.id.ocrResultTextView)

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

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                CAMERA_REQUEST_CODE -> {
                    val imageBitmap = data?.extras?.get("data") as Bitmap
                    processImage(imageBitmap)
                }
                GALLERY_REQUEST_CODE -> {
                    val selectedImageUri = data?.data
                    val imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedImageUri)
                    processImage(imageBitmap)
                }
            }
        }
    }

    private fun processImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(DEFAULT_OPTIONS)
        val imageView = findViewById<ImageView>(R.id.imageView)
        val textView = findViewById<TextView>(R.id.ocrResultTextView)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // OCR 결과 처리 및 TextView에 출력
                val ocrText = StringBuilder()
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        ocrText.append(line.text).append("\n")
                    }
                }

                textView.text = ocrText.toString()
                textView.visibility = View.VISIBLE // 텍스트뷰 보이기

                imageView.setImageBitmap(bitmap)
                imageView.visibility = View.VISIBLE // 이미지뷰 보이기
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
            }
    }
}
