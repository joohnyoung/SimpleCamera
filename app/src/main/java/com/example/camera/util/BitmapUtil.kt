package com.example.camera.util

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.createBitmap
import com.example.camera.viewmodel.CameraViewModel
import java.io.ByteArrayOutputStream

object BitmapUtil {

    /** 镜像图片 */
    fun Bitmap.mirror(): Bitmap {
        val matrix = Matrix()
        matrix.postScale(-1F, 1F)
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    /** 旋转图片 */
    fun Bitmap.rotate(orientation: Int): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(orientation.toFloat())
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    /** Bitmap 转 ByteArray */
    fun Bitmap.toByteArray(): ByteArray {
        val os = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 100, os)
        return  os.toByteArray()
    }

    /** ByteArray 转 Bitmap */
    fun ByteArray.toBitmap(): Bitmap {
        return BitmapFactory.decodeByteArray(this, 0, size)
    }
}