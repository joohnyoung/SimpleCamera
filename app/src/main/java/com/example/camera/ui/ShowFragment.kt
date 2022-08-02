package com.example.camera.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ThumbnailUtils
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.Navigation
import com.example.camera.R
import com.example.camera.databinding.FragmentShowBinding
import java.io.File

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
        val layout = showFragmentBinding.showLayout

        var bitmap: Bitmap? = null
        if (type == "jpg") {
            bitmap = BitmapFactory.decodeFile(path)
            imageView.setImageBitmap(bitmap)
        } else if (type == "mp4") {
            bitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.MINI_KIND)!!
            imageView.setImageBitmap(bitmap)
            val videoStart = Button(requireContext())
            videoStart.setBackgroundResource(R.drawable.video_start)
            layout.addView(videoStart)
            videoStart.setOnClickListener {
                val bundle = VideoFragmentArgs.Builder(path).build().toBundle()

                Navigation.findNavController(requireActivity(), R.id.fragmentContainer).navigate(
                    R.id.action_showFragment_to_videoFragment, bundle)
            }
        }

        return showFragmentBinding.root
    }

    override fun onDestroy() {
        super.onDestroy()
        _showFragmentBinding = null
    }
}