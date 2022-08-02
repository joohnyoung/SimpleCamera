package com.example.camera.util

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Environment
import android.provider.MediaStore
import com.example.camera.viewmodel.CameraViewModel
import java.io.ByteArrayOutputStream

class PictureUtil {
    companion object {
        /** 复制 EXIF 信息 */
        fun copyExif(old: String, new: String) {
            val oldExif = ExifInterface(old)
            val newExif = ExifInterface(new)
            val cls = ExifInterface::class.java
            val fields = cls.fields
            // 遍历，复制EXIF信息
            for (field in fields) {
                val fieldName = field.name
                if (fieldName.startsWith("TAG")) {
                    val attribute = field.get(cls).toString()
                    val value = oldExif.getAttribute(attribute)
                    if (value != null) {
                        newExif.setAttribute(attribute, value)
                    }
                }
            }
            newExif.saveAttributes()
        }

        /** 镜像图片 */
        fun mirrorPicture(bytes: ByteArray): ByteArray {
            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val matrix = Matrix()
            matrix.postScale(-1F, 1F)
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            return baos.toByteArray()
        }
    }
}