package com.example.camera.ui

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.camera.R
import com.example.camera.databinding.FragmentShowBinding

class ShowFragment : Fragment() {
    private var _showFragmentBinding: FragmentShowBinding? = null
    private val showFragmentBinding get() = _showFragmentBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _showFragmentBinding = FragmentShowBinding.inflate(inflater, container, false)
        // 获取数据
        val showFragmentArgs = ShowFragmentArgs.fromBundle(requireArguments())

        val path = showFragmentArgs.path
        val type = showFragmentArgs.type

        val imageView = showFragmentBinding.showPicture
        val videoView = showFragmentBinding.showVideo

        // 操作数据
        if (type == "jpg") {
            imageView.visibility = View.VISIBLE
            videoView.visibility = View.GONE
            // 打开照片
            val bitmap = BitmapFactory.decodeFile(path)
            imageView.setImageBitmap(bitmap)
        } else {
            imageView.visibility = View.GONE
            videoView.visibility = View.VISIBLE
            // 打开录像
            videoView.setVideoPath(path)
            videoView.start()
        }

        // Inflate the layout for this fragment
        return showFragmentBinding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _showFragmentBinding = null
    }
}