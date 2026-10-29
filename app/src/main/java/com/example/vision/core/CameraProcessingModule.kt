package com.example.vision.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraProcessingModule(
    private val context: Context
) {
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var processCameraProvider: ProcessCameraProvider? = null

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        onInitialized: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            try {
                val provider = cameraProviderFuture.get()
                processCameraProvider = provider

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(previewView.display?.rotation ?: android.view.Surface.ROTATION_0)
                    .build()
                imageCapture = capture

                val selector = CameraSelector.Builder()
                    .requireLensFacing(lensFacing)
                    .build()

                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    capture
                )
                onInitialized?.invoke()
            } catch (e: Exception) {
                onError?.invoke(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun takePicture(
        onSuccess: (FoodFrame) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError(IllegalStateException("Camera capture is not initialized."))
            return
        }

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    try {
                        val rotation = imageProxy.imageInfo.rotationDegrees
                        val bitmap = imageProxyToRotatedBitmap(imageProxy, rotation)
                        imageProxy.close()
                        val frame = FoodFrame(bitmap = bitmap, rotationDegrees = 0)
                        onSuccess(frame)
                    } catch (e: Exception) {
                        imageProxy.close()
                        onError(e)
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    onError(exception)
                }
            }
        )
    }

    suspend fun decodeFromInputStream(
        inputStream: InputStream,
        sourceUriString: String? = null
    ): FoodFrame? = withContext(Dispatchers.IO) {
        try {
            val bytes = inputStream.readBytes()
            inputStream.close()

            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

            var inSampleSize = 1
            val maxDim = 2048
            while (options.outWidth / inSampleSize > maxDim || options.outHeight / inSampleSize > maxDim) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return@withContext null

            FoodFrame(
                bitmap = decoded,
                sourceUri = sourceUriString,
                originalWidth = decoded.width,
                originalHeight = decoded.height
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun imageProxyToRotatedBitmap(image: ImageProxy, rotationDegrees: Int): Bitmap {
        return com.example.domain.ImagePreparationModule.imageProxyToRotatedBitmap(image)
    }

    fun release() {
        try {
            processCameraProvider?.unbindAll()
        } catch (e: Exception) {
            // Ignore
        }
        if (!cameraExecutor.isShutdown) {
            cameraExecutor.shutdown()
        }
    }
}
