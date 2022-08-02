package com.example.camera.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Environment
import android.provider.MediaStore
import android.util.Size
import androidx.lifecycle.MutableLiveData
import com.example.camera.R
import kotlinx.coroutines.GlobalScope
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList

object FileUtil {
    data class folder (
        val name: String,
        val filesNumber: Int
    )

    data class CustomFile (
        val path: String,
        val type: String,
        val thumbnail: Bitmap,
        val time: String
    )

    val files = ArrayList<CustomFile>()

    /** 获取文件夹中的所有文件 */
    fun getFiles(context: Context) {
        files.clear()
        openFolder(context, "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/MyCamera")
    }

    /** 添加新文件 */
    fun addFile(application: Application, filePath: String, type: String) {
        val file = CustomFile(
            filePath, type, getThumbnail(application, File(filePath), type), getFileTime(filePath)
        )
        val tempFiles = ArrayList<CustomFile>()
        tempFiles.addAll(files)
        files.clear()
        files.add(file)
        files.addAll(tempFiles)
    }

    /** 对文件进行时间降序排序 */
    private fun sortFiles() {
        Collections.sort(files, object : Comparator<CustomFile>{
            override fun compare(p0: CustomFile, p1: CustomFile): Int {
                if (p1.time > p0.time) {
                    return 1
                } else {
                    return -1
                }
            }
        })
    }

    /** 打开文件夹 */
    private fun openFolder(context: Context, path: String) {
        val allFiles = File(path).listFiles()
        if (allFiles != null) {
            for (file in allFiles) {
                val type = getFileType(file.path)
                if (type == "folder") {
                    openFolder(context, file.path)
                } else if (type == "jpg" || type == "mp4") {
                    files.add(
                        CustomFile(file.path, type, getThumbnail(context, file, type), getFileTime(file.path))
                    )
                    sortFiles()
                }
            }
        }
    }

    /** 得到文件名字 */
    private fun getFileTime(filePath: String): String {
        val file = File(filePath)
        return SimpleDateFormat("yyyyMMddHHmmss").format(Date(file.lastModified()))
    }

    /** 判断文件和文件夹 */
    private fun getFileType(filePath: String): String {
        val dot = filePath.lastIndexOf(".")
        return if (dot > 0) {
            filePath.substring(dot+1).toLowerCase()
        } else {
            "folder"
        }
    }

    /** 获取图片、视频的预览画面 */
    private fun getThumbnail(context: Context, file: File, type: String): Bitmap {
        var bitmap: Bitmap? = null
        if (type == "jpg") {
            bitmap = BitmapFactory.decodeFile(file.path)
        } else if (type == "mp4") {
            bitmap = ThumbnailUtils.createVideoThumbnail(file.path, MediaStore.Images.Thumbnails.MINI_KIND)
        }

        // 裁剪图片
        if (bitmap != null) {
            val metric = context.resources.displayMetrics
            val width = metric.widthPixels / 3 - 10
            val height = width
            bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height)
        } else {
            bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.model_video)
        }
        return bitmap
    }
}