package com.example.camera.ui

import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.camera.R
import com.example.camera.adapter.RecyclerViewAdapter
import com.example.camera.databinding.FragmentGalleryBinding
import com.example.camera.util.FileUtil
import java.io.File

class GalleryFragment : Fragment() {
    // 视图绑定
    private var _galleryBinding: FragmentGalleryBinding? = null
    private val galleryBinding get() = _galleryBinding!!

    private var itemNumber = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _galleryBinding = FragmentGalleryBinding.inflate(inflater, container, false)

        galleryBinding.recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)
        galleryBinding.recyclerView.adapter = RecyclerViewAdapter(requireActivity(), requireContext(), FileUtil.files)
        // 滑动监听，用于更新数据
        galleryBinding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val lastPosition = layoutManager.findLastVisibleItemPosition()
                val totalItemCount = layoutManager.itemCount
                if (lastPosition != totalItemCount-1) {
                    recyclerView.adapter!!.notifyDataSetChanged()
                }
            }
        })

        return galleryBinding.root
    }

    override fun onDestroy() {
        _galleryBinding = null
        super.onDestroy()
    }
}