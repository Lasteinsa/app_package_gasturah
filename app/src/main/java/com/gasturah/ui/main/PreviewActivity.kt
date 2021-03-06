package com.gasturah.ui.main

import ApiConfig
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.gasturah.MainActivity
import com.gasturah.data.util.Loading
import com.gasturah.databinding.ActivityPreviewBinding
import com.gasturah.response.ContentItem
import com.gasturah.response.ContentRecognize
import com.gasturah.response.RecognizeResponse
import com.gasturah.service.ApiConfigCC
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream


class PreviewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPreviewBinding
    private val loading = Loading(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()
        getImage()
        setupAction()
    }

    private fun setupAction() {
        binding.button.setOnClickListener {
            loading.showLoading()
            uploadPhoto(binding.imagePreview.drawable.toBitmap())
        }
    }

    private fun reduceFileImage(file: File): File {
        val bitmap = BitmapFactory.decodeFile(file.path)
        var compressQuality = 80
        var streamLength: Int

        do {
            val bmpStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, bmpStream)
            val bmpPicByteArray = bmpStream.toByteArray()
            streamLength = bmpPicByteArray.size
            compressQuality -= 10
        } while (streamLength > 250000)

        bitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, FileOutputStream(file))

        return file
    }

    private fun bitmapToFile(bitmap: Bitmap, fileNameToSave: String): File? {
        var file: File? = null

        return try {
            file = File(Environment.getExternalStorageDirectory().toString() + File.separator + fileNameToSave)
            file.createNewFile()

            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos) // YOU can also save it in JPEG
            val bitmapdata = bos.toByteArray()

            val fos = FileOutputStream(file)
            fos.write(bitmapdata)
            fos.flush()
            fos.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            file
        }
    }

    private fun uploadPhoto(bmp: Bitmap) {
        val fileNotReduced = bitmapToFile(bmp, "Android/media/com.gasturah/Gasturah/gasturah.jpeg" ) as File
        val file   = reduceFileImage(fileNotReduced)

        val requestImageFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val imageMultiPart: MultipartBody.Part = MultipartBody.Part.createFormData(
            "file",
            file.name,
            requestImageFile
        )

        binding.imagePreview.setImageBitmap(bmp)

        ApiConfigCC.getApiService().recognize(imageMultiPart).enqueue(object: Callback<RecognizeResponse> {
            override fun onResponse(
                call: Call<RecognizeResponse>,
                response: Response<RecognizeResponse>
            ) {
                loading.dismissLoading()
                if(response.isSuccessful) {
                    Toast.makeText(this@PreviewActivity, "File Uploaded : ${response.message()}", Toast.LENGTH_SHORT).show()
                    val respond = response.body()?.content
                    if(respond != null ) {
                        val data = ContentItem(
                            respond.nama,
                            respond.foto,
                            respond.detail,
                            respond.sumber,
                            respond.latitude,
                            respond.longitude
                        )
                        val moveToDetail = Intent(this@PreviewActivity, DetailActivity::class.java )
                        moveToDetail.putExtra(MainActivity.DATA, data)
                        startActivity(moveToDetail)
                        finish()
                    }
                } else {
                    Log.d("Failure To Send ELSE: ", response.toString())
                    Toast.makeText(this@PreviewActivity, "ERROR : ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RecognizeResponse>, t: Throwable) {
                loading.dismissLoading()
                Toast.makeText(this@PreviewActivity, "ERROR : ${t.message}", Toast.LENGTH_SHORT).show()
                Log.d("Failure To Send : ", t.message.toString())
            }
        })
    }

    private fun getImage() {
        val photo = intent.getStringExtra("photo")
        try {
            Log.d("DIREKTORI", photo.toString())
            val fileInputStream: FileInputStream    = openFileInput(photo)
            val bmp: Bitmap                         = BitmapFactory.decodeStream(fileInputStream)
            fileInputStream.close()
            binding.imagePreview.setImageBitmap(bmp)

            Glide.with(this).asBitmap()
                .load(bmp)
                .transform(RoundedCorners(32))
                .into(binding.imagePreview)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    companion object {
        const val CAMERA_X_RESULT = 200
    }
}