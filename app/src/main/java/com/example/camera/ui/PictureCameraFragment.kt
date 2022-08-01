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
import com.example.camera.databinding.FragmentPictureCameraBinding
import com.example.camera.util.FileUtil
import com.example.camera.viewmodel.CameraViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PictureCameraFragment : Fragment() {
    private val TAG = PictureCameraFragment::class.simpleName
    // 视图绑定
    private var _pictureCameraBinding: FragmentPictureCameraBinding? = null
    private val pictureCameraBinding get() = _pictureCameraBinding!!

    private val cameraViewModel: CameraViewModel by activityViewModels()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _pictureCameraBinding = FragmentPictureCameraBinding.inflate(inflater, container, false)
        // 设置 TextureView
        pictureCameraBinding.textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                if (pictureCameraBinding.textureView.isVisible) {
                    Log.e(TAG, "TextureView已就绪")
                    cameraViewModel.setupCamera(pictureCameraBinding.textureView)
                    // 初始化相机
                    cameraViewModel.openCamera(CameraDevice.TEMPLATE_PREVIEW)
                }
            }
            override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean  = false
            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
        }
        // 切换后置摄像头与前置摄像头
        pictureCameraBinding.btnChangeCamera.setOnClickListener {
            it.isEnabled = false
            cameraViewModel.changeCamera()
            it.postDelayed({ it.isEnabled = true }, 500)
        }
        // 拍照
        pictureCameraBinding.btnCaptureImage.setOnClickListener {
            it.isEnabled = false
            lifecycleScope.launch(Dispatchers.IO) {
                cameraViewModel.deviceOrientation = requireActivity().windowManager.defaultDisplay.orientation
                cameraViewModel.takePicture()
                FileUtil.getFiles(requireContext(),)
                it.postDelayed({ it.isEnabled = true }, 500)
            }
        }
        // 切换模式
        pictureCameraBinding.btnChangeModel.setOnClickListener {
            Log.e(TAG, "切换录像模式")
            cameraViewModel.closeCamera()
            Navigation.findNavController(requireActivity(), R.id.fragmentContainer).navigate(
                R.id.action_imageCameraFragment_to_videoCameraFragment)
        }
        // 打开相册
        pictureCameraBinding.gallery.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                FileUtil.getFiles(requireContext())
            }
            it.postDelayed({
                Navigation.findNavController(requireActivity(), R.id.fragmentContainer).navigate(
                    R.id.action_imageCameraFragment_to_galleryFragment)
            }, 500)
        }
        // TextureView触摸
        pictureCameraBinding.textureView.setOnTouchListener{ _, event ->
            cameraViewModel.onTouchEvent(event)
        }
        return pictureCameraBinding.root
    }


    override fun onResume() {
        super.onResume()
        if (pictureCameraBinding.textureView.isAvailable && pictureCameraBinding.textureView.isVisible) {
            cameraViewModel.setupCamera(pictureCameraBinding.textureView)
            cameraViewModel.openCamera(CameraDevice.TEMPLATE_PREVIEW)
        }
    }

    override fun onStop() {
        super.onStop()
        cameraViewModel.closeCamera()
    }

    override fun onDestroy() {
        _pictureCameraBinding = null
        super.onDestroy()
    }
}
