package com.example.camera.ui

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraDevice
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import com.example.camera.R
import com.example.camera.databinding.FragmentVideoCameraBinding
import com.example.camera.util.FileUtil
import com.example.camera.viewmodel.CameraViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoCameraFragment : Fragment() {
    private val TAG = VideoCameraFragment::class.simpleName
    // 视图绑定
    private var _videoCameraBinding: FragmentVideoCameraBinding? = null
    private val videoCameraBinding get() = _videoCameraBinding!!

    private val cameraViewModel: CameraViewModel by activityViewModels()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _videoCameraBinding = FragmentVideoCameraBinding.inflate(inflater, container, false)
        // 设置 TextureView
        videoCameraBinding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                if (videoCameraBinding.textureView.isVisible) {
                    Log.e(TAG, "TextureView已就绪")
                    cameraViewModel.setupCamera(videoCameraBinding.textureView)
                    // 初始化相机
                    cameraViewModel.openCamera(CameraDevice.TEMPLATE_PREVIEW)
                }
            }
            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {}
            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean =false
            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {}
        }
        // 切换后置摄像头与前置摄像头
        videoCameraBinding.btnChangeCamera.setOnClickListener {
            it.isEnabled = false
            cameraViewModel.changeCamera()
            it.postDelayed({ it.isEnabled = true }, 500)
        }
        // 切换模式
        videoCameraBinding.btnChangeModel.setOnClickListener {
            Log.e(TAG, "切换拍照模式")
            cameraViewModel.closeCamera()
            Navigation.findNavController(requireActivity(), R.id.fragmentContainer).navigate(
                R.id.action_videoCameraFragment_to_imageCameraFragment)
        }
        // 录像
        var isVideoStart = false
        videoCameraBinding.btnCaptureVideo.setOnClickListener {
            it.isEnabled = false
            if (isVideoStart) {
                cameraViewModel.videoStop()
                videoCameraBinding.btnCaptureVideo.setBackgroundResource(R.drawable.capture_start)
                isVideoStart = false
            } else {
                cameraViewModel.deviceOrientation = requireActivity().windowManager.defaultDisplay.orientation
                videoCameraBinding.btnCaptureVideo.setBackgroundResource(R.drawable.capture_stop)
                cameraViewModel.videoStart()
                isVideoStart = true
            }
            it.postDelayed({ it.isEnabled = true }, 500)
        }
        // 打开相册
        videoCameraBinding.gallery.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                FileUtil.getFiles(requireContext())
            }
            it.postDelayed({
                Navigation.findNavController(requireActivity(), R.id.fragmentContainer).navigate(
                    R.id.action_videoCameraFragment_to_galleryFragment)
            }, 500)

        }
        // TextureView触摸
        videoCameraBinding.textureView.setOnTouchListener{ _, event ->
            cameraViewModel.onTouchEvent(event)
        }

        return videoCameraBinding.root
    }

    override fun onResume() {
        super.onResume()
        if (videoCameraBinding.textureView.isAvailable && videoCameraBinding.textureView.isVisible) {
            cameraViewModel.setupCamera(videoCameraBinding.textureView)
            cameraViewModel.openCamera(CameraDevice.TEMPLATE_PREVIEW)
        }
    }

    override fun onStop() {
        super.onStop()
        cameraViewModel.closeCamera()
    }

    override fun onDestroyView() {
        _videoCameraBinding = null
        super.onDestroyView()
    }
}