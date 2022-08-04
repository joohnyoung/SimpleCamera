package com.example.camera.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.*
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import androidx.core.net.toUri
import androidx.core.view.MotionEventCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.util.FileUtil
import com.example.camera.util.BitmapUtil
import com.example.camera.util.BitmapUtil.mirror
import com.example.camera.util.BitmapUtil.rotate
import com.example.camera.util.BitmapUtil.toBitmap
import com.example.camera.util.BitmapUtil.toByteArray
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class CameraViewModel(private val app: Application): AndroidViewModel(app) {
    private val TAG = CameraViewModel::class.simpleName
    /** 存储捕获的数据 */
    data class CombinedCaptureResult(
        val image: Image,
        val metadata: CaptureResult,
        val orientation: Int,
        val format: Int,
        val displayName: String
    ) : Closeable {
        override fun close() = image.close()
    }
    /** 图片缓冲阅读器 */
    private lateinit var imageReader: ImageReader
    /** 相机线程 */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)
    /** 前置摄像头 */
    private var frontCameraId: String? = null
    private lateinit var frontCameraCharacteristics: CameraCharacteristics
    /** 后置摄像头 */
    private var backCameraId: String? = null
    private lateinit var backCameraCharacteristics: CameraCharacteristics
    /** 正在使用的摄像头 */
    private lateinit var cameraDevice: CameraDevice
    private var cameraId: String? = null
    private lateinit var cameraCharacteristics: CameraCharacteristics
    /** 相机捕获请求 */
    private lateinit var captureRequest: CaptureRequest.Builder
    /** 相机捕获会话 */
    private lateinit var captureSession: CameraCaptureSession
    /** 用于显示预览画面的容器 */
    private lateinit var previewSurface: Surface
    /** 预览画面的尺寸 */
    private lateinit var previewSize: Size
    /** 旋转角度 */
    private var orientation = 0
    /** 视频录制 */
    private lateinit var mediaRecorder: MediaRecorder
    /** 文件存储位置 */
    private val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/MyCamera"
    private lateinit var displayName: String
    /** 缩放倍数 */
    private var zoom = 1
    private lateinit var zoomRect: Rect

    /** 初始化相机参数 */
    fun initCameraInfo(surfaceTexture: SurfaceTexture, ratio: String) {
        if (cameraId == null) {
            val cameraManager = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            // 获取前置、后置摄像头参数
            val cameraIdList = cameraManager.cameraIdList
            cameraIdList.forEach { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                if (characteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_FRONT && frontCameraId == null) {
                    frontCameraId = cameraId
                    frontCameraCharacteristics = characteristics
                } else if (characteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_BACK && backCameraId == null) {
                    backCameraId = cameraId
                    backCameraCharacteristics = characteristics
                }
            }
            // 默认打开后置摄像头
            cameraId = backCameraId
            cameraCharacteristics = backCameraCharacteristics

        } else {
            cameraCharacteristics = when (cameraId) {
                frontCameraId -> frontCameraCharacteristics
                else -> backCameraCharacteristics
            }
        }
        // 初始化缩放参数
        zoom = 1
        // 创建文件夹
        File(path).mkdir()
        // 设置预览尺寸
        setPreviewSize(ratio)
        // 设置预览界面
        previewSurface = Surface(surfaceTexture)
        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        // 开启相机
        openCamera()
    }

    /** 设置预览尺寸 */
    private fun setPreviewSize(ratio: String) {
        val aspectRatio: Float = if (ratio == "3:4") {
            4.toFloat() / 3
        } else {
            16.toFloat() / 9
        }
        // 获取设备支持的的所有尺寸
        val sizes = cameraCharacteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            .getOutputSizes(SurfaceTexture::class.java)
        // 选择比例合适的尺寸
        if (sizes != null) {
            for (size in sizes) {
                if (size.width.toFloat() / size.height == aspectRatio) {
                    previewSize = size
                    return
                }
            }
        }
    }

    /** 打开相机 */
    @SuppressLint("MissingPermission")
    private fun openCamera() {
        val cameraManager = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.openCamera(cameraId!!, object : CameraDevice.StateCallback() {
            override fun onOpened(device: CameraDevice) {
                cameraDevice = device
                createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            }
            override fun onDisconnected(cameraDevice: CameraDevice) {
                Log.e(TAG, "相机 ${cameraDevice.id} 未连接")
            }
            override fun onError(cameraDevice: CameraDevice, error: Int) {
                Log.e(TAG, "相机 ${cameraDevice.id} 打开失败")
            }
        }, cameraHandler)
    }

    /** 创建相机捕获申请 */
    private fun createCaptureRequest(templateType: Int) {
        // 设置imageReader
        imageReader = ImageReader.newInstance(
            previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireNextImage()
            // 获取图片数据
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            savePicture(bytes)
        }, cameraHandler)

        var target = listOf(previewSurface, imageReader.surface)
        when (templateType) {
            CameraDevice.TEMPLATE_PREVIEW -> {
                captureRequest = cameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(previewSurface) }
            }
            CameraDevice.TEMPLATE_STILL_CAPTURE -> {
                captureRequest = cameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE).apply { addTarget(imageReader.surface) }
            }
            CameraDevice.TEMPLATE_RECORD -> {
                setMediaRecorder()
                captureRequest = cameraDevice.createCaptureRequest(
                    CameraDevice.TEMPLATE_RECORD).apply {
                        addTarget(previewSurface)
                        addTarget(mediaRecorder.surface)
                    }
                captureRequest.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                target = listOf(previewSurface, mediaRecorder.surface)
            }
        }
        if (zoom != 1) {
            captureRequest.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
        }
        // 创建 CaptureSession 对象
        cameraDevice.createCaptureSession(target, object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                createCaptureSession(templateType)
            }
            override fun onConfigureFailed(p0: CameraCaptureSession) {
                Log.e(TAG, "相机预览失败")
            }
        }, cameraHandler)
    }

    /** 创建相机预览会话 */
    private fun createCaptureSession(templateType: Int) {
        when (templateType) {
            CameraDevice.TEMPLATE_STILL_CAPTURE -> {
                captureSession.capture(captureRequest.build(), null, cameraHandler)
            }
            else -> {
                captureSession.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
            }
        }
    }

    /** 拍照 */
    fun takePicture() {
        createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        cameraHandler.postDelayed({
            createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        }, 500)
    }

    /** 录像 */
    fun videoStart() {
        captureSession.close()
        createCaptureRequest(CameraDevice.TEMPLATE_RECORD)
        mediaRecorder.start()
    }

    /** 停止录像 */
    fun videoStop() {
        mediaRecorder.stop()
        FileUtil.addFile(app, "${path}/${displayName}", "mp4")
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, File("${path}/${displayName}").toUri())
        app.sendBroadcast(mediaScanIntent)
        createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
    }

    /** 关闭相机 */
    fun closeCamera() {
        captureSession.close()
        cameraDevice.close()
        imageReader.close()
    }

    /** 释放线程 */
    fun releaseThread() {
        cameraThread.quitSafely()
    }

    /** 切换前置、后置摄像头 */
    fun changeCamera(surfaceTexture: SurfaceTexture, ratio: String) {
        cameraId = when (cameraId) {
            frontCameraId -> backCameraId
            backCameraId -> frontCameraId
            else -> null
        }
        cameraDevice.close()
        initCameraInfo(surfaceTexture, ratio)
    }

    /** 设置MediaRecorder */
    private fun setMediaRecorder() {
        mediaRecorder = MediaRecorder()
        mediaRecorder.reset()
        mediaRecorder.apply {
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setVideoEncodingBitRate(8*previewSize.width*previewSize.height)
            setVideoFrameRate(30)
            setOrientationHint(orientation)
            setPreviewDisplay(previewSurface)
            setVideoSize(previewSize.width, previewSize.height)
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
            displayName = "VID_${sdf.format(Date())}.mp4"
            setOutputFile("${path}/${displayName}")
        }
        mediaRecorder.prepare()
    }

    /** 保存捕获结果 */
    private fun savePicture(bytes: ByteArray) {
        // 用当前时间为图片文件命名
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
        val displayName = "IMG_${sdf.format(Date())}.jpg"
        val file = File("${path}/${displayName}")

        val bitmap = bytes.toBitmap().rotate(orientation)
        val resultBytes: ByteArray = if (cameraId == frontCameraId) { // 如果是前置摄像头，做镜像处理
            bitmap.mirror().toByteArray()
        } else { // 后置摄像头无需修改
            bitmap.toByteArray()
        }
        file.writeBytes(resultBytes)
        FileUtil.addFile(app, file.path, "jpg")
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, file.toUri())
        app.sendBroadcast(mediaScanIntent)
    }

    /** 画面触摸处理 */
    private var oldDis = 1
    fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount == 2) {
            val action = MotionEventCompat.getActionMasked(event)
            when (action) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    // 计算两根手指间的距离
                    val x = event.getX(0) - event.getX(1)
                    val y = event.getY(0) - event.getY(1)
                    oldDis = Math.sqrt((x * x + y * y).toDouble()).toInt()
                }
                MotionEvent.ACTION_MOVE -> {
                    val x = event.getX(0) - event.getX(1)
                    val y = event.getY(0) - event.getY(1)
                    val newDis = Math.sqrt((x * x + y * y).toDouble()).toInt()
                    if (newDis > oldDis) {
                        handleZoom(true)
                    } else {
                        handleZoom(false)
                    }
                    oldDis = newDis
                }
            }
        }
        return true
    }

    /** 缩放处理 */
    private fun handleZoom(isZoomIn: Boolean) {
        val maxZoom = cameraCharacteristics[CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM]!!
        // 缩放的参数
        val factor = 150
        if (isZoomIn && zoom < factor) {
            zoom++
        } else if (zoom > 0) {
            zoom--
        }
        val rect = cameraCharacteristics[CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE]!!
        // 计算最小像素变化单元
        val minW = ((rect.width() - rect.width() / maxZoom) / (2 * factor)).toInt()
        val minH = ((rect.height() - rect.height() / maxZoom) / (2 * factor)).toInt()
        // 计算裁剪的宽高像素
        val cropW = minW * zoom
        val cropH = minH * zoom
        zoomRect = Rect(rect.left + cropW, rect.top + cropH, rect.right - cropW, rect.bottom - cropH)
        // 设置预览
        captureRequest.set(CaptureRequest.SCALER_CROP_REGION, zoomRect)
        captureSession.stopRepeating()
        captureSession.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
    }

    /** 设置照片的旋转角度 */
    fun setOrientation(deviceOrientation: Int) {
        // 将设备旋转角度转换为90度的倍数
        var mDeviceOrientation = (deviceOrientation + 45) / 90 * 90
        val sensorOrientation = cameraCharacteristics[CameraCharacteristics.SENSOR_ORIENTATION]!!
        if (cameraId == frontCameraId) {
            mDeviceOrientation = -mDeviceOrientation
        }
        orientation = (sensorOrientation + mDeviceOrientation + 360) % 360
    }
}