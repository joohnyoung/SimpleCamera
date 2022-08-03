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
import android.view.TextureView
import androidx.core.net.toUri
import androidx.core.view.MotionEventCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.camera.util.FileUtil
import com.example.camera.util.PictureUtil
import kotlinx.coroutines.launch
import java.io.Closeable
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue


class CameraViewModel(application: Application): AndroidViewModel(application) {
    private val app =  application
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
    /** 相机工作的线程 */
    private val cameraThread = HandlerThread("CameraThread").apply { start() }
    private val cameraHandler = Handler(cameraThread.looper)
    /** imageReader 工作的线程 */
    private val imageReaderThread = HandlerThread("ImageReaderThread").apply { start() }
    private val imageReaderHandler = Handler(imageReaderThread.looper)
    /** 前置摄像头 */
    private var frontCameraId: String? = null
    private lateinit var frontCameraCharacteristics: CameraCharacteristics
    /** 后置摄像头 */
    private var backCameraId: String? = null
    private lateinit var backCameraCharacteristics: CameraCharacteristics
    /** 正在使用的摄像头 */
    private lateinit var camera: CameraDevice
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
    /** 设备旋转方向 */
    var deviceOrientation: Int? = null
    /** 视频录制 */
    private lateinit var mediaRecorder: MediaRecorder
    /** 文件存储位置 */
    private val path = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}/MyCamera"
    private lateinit var displayName: String
    /** 缩放倍数 */
    private var zoom = 1
    private lateinit var zoomRect: Rect

    /** 关闭相机 */
    fun closeCamera() {
        captureSession.close()
        camera.close()
        imageReader.close()
    }

    /** 设置相机参数 */
    private fun setupCamera() {
        if (cameraId == null) {// 设置相机相关参数
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
    }

    /** 打开相机 */
    @SuppressLint("MissingPermission")
    private fun openCamera(templateType: Int) {
        val cameraManager = app.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraManager.openCamera(cameraId!!, object : CameraDevice.StateCallback() {
            override fun onOpened(cameraDevice: CameraDevice) {
                Log.e(TAG, "摄像头 ${cameraDevice.id} 已开启")
                camera = cameraDevice
                createCaptureSession(templateType)
            }

            override fun onDisconnected(cameraDevice: CameraDevice) {
                Log.e(TAG, "摄像头 ${cameraDevice.id} 未连接")
            }

            override fun onError(cameraDevice: CameraDevice, error: Int) {
                val msg = when (error) {
                    ERROR_CAMERA_DEVICE -> "设备遇到致命错误"
                    ERROR_CAMERA_DISABLED -> "因设备策略无法打开摄像头"
                    ERROR_CAMERA_IN_USE -> "设备已在使用中"
                    ERROR_CAMERA_SERVICE -> "服务遇到致命错误"
                    ERROR_MAX_CAMERAS_IN_USE -> "已打开的设备过多"
                    else -> "未知"
                }
                Log.e(TAG, "摄像头 ${cameraDevice.id} 错误： $msg")
            }
        }, cameraHandler)
    }

    /** 创建相机捕捉会话 */
    private fun createCaptureSession(templateType: Int) {
        // 设置imageReader
        imageReader = ImageReader.newInstance(
            previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
        var target = listOf(previewSurface, imageReader.surface)
        val orientation = cameraCharacteristics[CameraCharacteristics.SENSOR_ORIENTATION]
        when (templateType) {
            CameraDevice.TEMPLATE_PREVIEW -> {
                captureRequest = camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_PREVIEW).apply { addTarget(previewSurface) }
            }
            CameraDevice.TEMPLATE_STILL_CAPTURE -> {
                captureRequest = camera.createCaptureRequest(
                    CameraDevice.TEMPLATE_STILL_CAPTURE).apply { addTarget(imageReader.surface) }
                captureRequest.set(CaptureRequest.JPEG_ORIENTATION, getOrientation())
            }
            CameraDevice.TEMPLATE_RECORD -> {
                setMediaRecorder()
                captureRequest = camera.createCaptureRequest(
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

        camera.createCaptureSession(target, object : CameraCaptureSession.StateCallback() {

            override fun onConfigured(session: CameraCaptureSession) {
                if (templateType == CameraDevice.TEMPLATE_STILL_CAPTURE) {
                    // 获取图片
                    val imageQueue = ArrayBlockingQueue<Image>(1)
                    imageReader.setOnImageAvailableListener({ reader ->
                        Log.e(TAG, "获取图片")
                        val image = reader.acquireNextImage()
                        imageQueue.add(image)
                    }, imageReaderHandler)

                    session.capture(captureRequest.build(), object : CameraCaptureSession.CaptureCallback() {
                        override fun onCaptureCompleted(
                            session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult
                        ) {
                            super.onCaptureCompleted(session, request, result)
                            Log.e(TAG, "图片捕获完成")

                            viewModelScope.launch {
                                val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
                                val displayName = "IMG_${sdf.format(Date())}.jpg"
                                val image = imageQueue.take()
                                imageReader.setOnImageAvailableListener(null, null)
                                saveImage(
                                    CombinedCaptureResult(
                                    image, result, orientation!!, ImageFormat.JPEG, displayName)
                                )
                            }
                        }
                    }, cameraHandler)
                } else {
                    session.setRepeatingRequest(captureRequest.build(), null, cameraHandler)
                    if (templateType == CameraDevice.TEMPLATE_RECORD) {
                        mediaRecorder.start()
                    }
                }
                captureSession = session
            }

            override fun onConfigureFailed(p0: CameraCaptureSession) {
                Log.e(TAG, "相机会话配置失败")
            }
        }, cameraHandler)
    }

    /** 拍照 */
    fun takePicture() {
        createCaptureSession(CameraDevice.TEMPLATE_STILL_CAPTURE)
        cameraHandler.postDelayed({
            createCaptureSession(CameraDevice.TEMPLATE_PREVIEW)
        }, 500)
    }

    /** 录像 */
    fun videoStart(surfaceTexture: SurfaceTexture, ratio: String) {
        captureSession.close()
        createCaptureSession(CameraDevice.TEMPLATE_RECORD)
    }

    /** 停止录像 */
    fun videoStop() {
        mediaRecorder.stop()
        FileUtil.addFile(app, "${path}/${displayName}", "mp4")
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, File("${path}/${displayName}").toUri())
        app.sendBroadcast(mediaScanIntent)
        createCaptureSession(CameraDevice.TEMPLATE_PREVIEW)
    }

    /** 切换前置、后置摄像头 */
    fun changeCamera(surfaceTexture: SurfaceTexture, ratio: String) {
        Log.e(TAG, "切换摄像头")
        cameraId = when (cameraId) {
            frontCameraId -> backCameraId
            backCameraId -> frontCameraId
            else -> null
        }
        closeCamera()
        setupCamera()
        setPreviewSize(ratio)
        previewSurface = Surface(surfaceTexture)
        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        openCamera(CameraDevice.TEMPLATE_PREVIEW)
    }

    /** 开启预览 */
    fun startPreview(surfaceTexture: SurfaceTexture, ratio: String, model: String) {
        setupCamera()
        setPreviewSize(ratio)
        if (ratio == "3:4" && model == "录像") {
            previewSize = Size(1440, 1080)
        }
        previewSurface = Surface(surfaceTexture)
        surfaceTexture.setDefaultBufferSize(previewSize.width, previewSize.height)
        openCamera(CameraDevice.TEMPLATE_PREVIEW)
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
            setOrientationHint(getOrientation())
            setPreviewDisplay(previewSurface)
            setVideoSize(previewSize.width, previewSize.height)
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA)
            displayName = "VID_${sdf.format(Date())}.mp4"
            setOutputFile("${path}/${displayName}")
        }
        mediaRecorder.prepare()
    }

    /** 保存捕获结果 */
    private fun saveImage(result: CombinedCaptureResult) {
        val buffer = result.image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val file = File("${path}/${result.displayName}")
        // 如果是前置摄像头，做镜像处理
        if (cameraId == frontCameraId) {
            // 镜像处理图片
            val newBytes = PictureUtil.mirrorPicture(bytes)
            // 用原图片数据创建临时文件
            val tempFile = File("${path}/file.jpg")
            tempFile.writeBytes(bytes)
            file.writeBytes(newBytes)
            // 将原图片中的EXIF信息复制到新图片中
            PictureUtil.copyExif(tempFile.absolutePath, file.absolutePath)
            // 删除临时文件
            tempFile.delete()
        } else {
            // 后置摄像头直接写入文件
            file.writeBytes(bytes)
        }
        FileUtil.addFile(app, file.path, "jpg")
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, file.toUri())
        app.sendBroadcast(mediaScanIntent)
        Log.e(TAG, "保存成功")
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
        Log.e(TAG, "Zoom: $zoom")
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

    /** 获取捕获方向 */
    private fun getOrientation(): Int {
        if (deviceOrientation == android.view.OrientationEventListener.ORIENTATION_UNKNOWN) {
            return 0
        }
        val sensorOrientation = cameraCharacteristics[CameraCharacteristics.SENSOR_ORIENTATION]!!
        Log.e(TAG, "deviceOrientation:$deviceOrientation\nsensorOrientation:$sensorOrientation")
        // 将设备旋转角度转换为90度的倍数
        deviceOrientation = (deviceOrientation!! + 45) / 90 * 90
        // 处理前置摄像头(没有做镜像处理）
        val facingFront = cameraCharacteristics[CameraCharacteristics.LENS_FACING] == CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) {
            deviceOrientation = -deviceOrientation!!
        }

        return  (sensorOrientation + deviceOrientation!! + 360) % 360
    }

    /** 设置预览尺寸 */
    private fun setPreviewSize(ratio: String) {
        var aspectRatio: Float
        if (ratio == "3:4") {
            aspectRatio = 4.toFloat() / 3
        } else {
            aspectRatio = 16.toFloat() / 9
        }
        val sizes = cameraCharacteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
            .getOutputSizes(SurfaceTexture::class.java)
        if (sizes != null) {
            for (size in sizes) {
                if (size.width.toFloat() / size.height == aspectRatio) {
                    previewSize = size
                    return
                }
            }
        }
        previewSize = sizes.maxByOrNull { it.width * it.height }!!
    }
}