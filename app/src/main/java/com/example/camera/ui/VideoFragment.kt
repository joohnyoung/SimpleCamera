package com.example.camera.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.VideoView
import com.example.camera.R
import com.example.camera.databinding.FragmentShowBinding
import com.example.camera.databinding.FragmentVideoBinding

class VideoFragment : Fragment() {

    private var _videoFragmentBinding: FragmentVideoBinding? = null
    private val videoFragmentBinding get() = _videoFragmentBinding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _videoFragmentBinding = FragmentVideoBinding.inflate(inflater, container, false)
        // 获取数据
        val videoFragmentArgs = VideoFragmentArgs.fromBundle(requireArguments())
        val path = videoFragmentArgs.path

        videoFragmentBinding.videoView.apply {
            setVideoPath(path)
            start()
        }

        return videoFragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val videoFragmentArgs = VideoFragmentArgs.fromBundle(requireArguments())
        val path = videoFragmentArgs.path

        val videoView = view.findViewById<VideoView>(R.id.videoView)
        videoView.setVideoPath(path)
        videoView.start()
    }
}