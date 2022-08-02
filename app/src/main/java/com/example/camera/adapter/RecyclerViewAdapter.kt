package com.example.camera.adapter

import android.app.Activity
import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.provider.MediaStore
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.camera.R
import com.example.camera.ui.GalleryFragment
import com.example.camera.ui.ShowFragment
import com.example.camera.ui.ShowFragmentArgs
import com.example.camera.util.FileUtil
import java.io.File
import java.io.FileInputStream

class RecyclerViewAdapter(
    private val activity: Activity,
    private val context: Context,
    private val data: ArrayList<FileUtil.CustomFile>,
): RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>() {

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerViewAdapter.ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.image_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerViewAdapter.ViewHolder, position: Int) {
        val customFile = data[position]
        holder.imageView.setImageBitmap(customFile.thumbnail)
        // 点击事件
        holder.imageView.setOnClickListener {
            val bundle = ShowFragmentArgs.Builder(
                customFile.path, customFile.type).build().toBundle()
            Navigation.findNavController(activity, R.id.fragmentContainer).navigate(
                R.id.action_galleryFragment_to_showFragment, bundle)
        }
    }

    override fun getItemCount(): Int {
        return  data.size
    }
}