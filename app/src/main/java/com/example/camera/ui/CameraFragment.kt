package com.example.camera.ui

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.widget.Toast
import androidx.core.view.get
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.Navigation
import com.example.camera.R
import com.example.camera.databinding.FragmentCameraBinding
import com.example.camera.viewmodel.CameraViewModel

class CameraFragment : Fragment() {
    private val TAG = CameraFragment::class.simpleName
    // 视图绑定
    private var _cameraBinding: FragmentCameraBinding? = null
    private val cameraBinding get() = _cameraBinding!!

    /** 当前使用的 SurfaceTexture */
    private lateinit var surfaceTexture: SurfaceTexture

    private val cameraViewModel: CameraViewModel by activityViewModels()

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _cameraBinding = FragmentCameraBinding.inflate(inflater, container, false)
        // 设置 TextureView ,默认尺寸为3：4
        val width = requireActivity().windowManager.defaultDisplay.width
        var height = width / 3 * 4
        addTextureView(width, height)
        // 切换后置摄像头与前置摄像头
        cameraBinding.btnChangeCamera.setOnClickListener {
            it.isEnabled = false
            val ratio = cameraBinding.textPreviewSize.text.toString()
            cameraViewModel.changeCamera(surfaceTexture, ratio)
            it.postDelayed({ it.isEnabled = true }, 500)
        }
        // 捕获画面
        var isCaptureVideo = false
        cameraBinding.btnCaptureImage.setOnClickListener {
            it.isEnabled = false
            cameraViewModel.deviceOrientation =
                requireActivity().windowManager.defaultDisplay.orientation
            if (cameraBinding.textCameraModel.text == "拍照") {
                cameraViewModel.takePicture()
            } else { // 录像
                if (!isCaptureVideo) {
                    it.setBackgroundResource(R.drawable.capture_stop)
                    cameraViewModel.videoStart(surfaceTexture, cameraBinding.textPreviewSize.text.toString())
                    isCaptureVideo = true
                } else {
                    it.setBackgroundResource(R.drawable.capture_start)
                    cameraViewModel.videoStop()
                    isCaptureVideo = false
                }
            }
            it.postDelayed({ it.isEnabled = true }, 500)
        }
        // 切换模式
        cameraBinding.btnChangeModel.setOnClickListener {
            Log.e(TAG, "切换拍摄模式")
            val textView = cameraBinding.textCameraModel
            val button = cameraBinding.btnChangeModel
            if (textView.text == "拍照") {
                Toast.makeText(requireContext(), "切换为录像模式", Toast.LENGTH_SHORT).show()
                button.setBackgroundResource(R.drawable.model_video)
                textView.text = "录像"
                cameraBinding.textPreviewSize.text = "9:16"
                height = width / 9 * 16
                cameraBinding.preLayout.removeAllViews()
                addTextureView(width, height)
            } else {
                Toast.makeText(requireContext(), "切换为拍照模式", Toast.LENGTH_SHORT).show()
                button.setBackgroundResource(R.drawable.model_picture)
                textView.text = "拍照"
                cameraBinding.textPreviewSize.text = "3:4"
                height = width / 9 * 12
                cameraBinding.preLayout.removeAllViews()
                addTextureView(width, height)
            }
        }
        // 切换预览尺寸
        cameraBinding.btnChangeSize.setOnClickListener {
            Log.e(TAG, "切换预览尺寸")
            val textView = cameraBinding.textPreviewSize
            if (textView.text == "3:4") {
                Toast.makeText(requireContext(), "尺寸切换为9:16", Toast.LENGTH_SHORT).show()
                textView.text = "9:16"
                height = width / 9 * 16
                cameraBinding.preLayout.removeAllViews()
            } else {
                if (cameraBinding.textCameraModel.text == "录像") {
                    Toast.makeText(requireContext(), "录像模式不支持3:4尺寸", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                } else {
                    Toast.makeText(requireContext(), "尺寸切换为3:4", Toast.LENGTH_SHORT).show()
                    textView.text = "3:4"
                    height = width / 9 * 12
                    cameraBinding.preLayout.removeAllViews()
                }
            }
            addTextureView(width, height)
        }
        // 打开相册
        cameraBinding.btnGallery.setOnClickListener {
            Navigation.findNavController(requireActivity(), R.id.fragmentContainer).navigate(
                R.id.action_imageCameraFragment_to_galleryFragment)
        }

        return cameraBinding.root
    }

    /** 为FrameLayout添加TextureView */
    private fun addTextureView(width: Int, height: Int) {
        val textureView = TextureView(requireContext())
        val params = ViewGroup.LayoutParams(width, height)
        textureView.layoutParams = params
        cameraBinding.preLayout.addView(textureView)
        textureView.surfaceTextureListener = surfaceTextureListener
        textureView.setOnTouchListener(onTouchListner)
    }

    // 创建 SurfaceTexture 监听
    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener{
        override fun onSurfaceTextureAvailable(mSurfaceTexture: SurfaceTexture, width: Int, height: Int) {
            Log.e(TAG, "TextureView已就绪")
            surfaceTexture = mSurfaceTexture
            val ratio = cameraBinding.textPreviewSize.text.toString()
            val model = cameraBinding.textCameraModel.text.toString()
            cameraViewModel.startPreview(surfaceTexture, ratio, model)
        }
        override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean  = false
        override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
    }

    // TextureView 触摸监听
    private val onTouchListner = object : View.OnTouchListener {
        override fun onTouch(p0: View?, event: MotionEvent): Boolean {
            cameraViewModel.onTouchEvent(event)
            return true
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onStop() {
        super.onStop()
        cameraViewModel.closeCamera()
    }

    override fun onDestroy() {
        _cameraBinding = null
        super.onDestroy()
    }
}
