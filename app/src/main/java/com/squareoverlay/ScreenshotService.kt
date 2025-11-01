package com.squareoverlay

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

class ScreenshotService(context: Context) {
    private val context: Context?
    private val projectionManager: MediaProjectionManager
    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    private val screenWidth: Int
    private val screenHeight: Int
    private val screenDensity: Int

    private var isCapturing = false
    private var currentZoomFolder: String? = null // Track current zoom folder

    init {
        this.context = context
        this.projectionManager =
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        val metrics = context.getResources().getDisplayMetrics()
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels
        screenDensity = metrics.densityDpi
    }

    fun createScreenCaptureIntent(): Intent {
        return projectionManager.createScreenCaptureIntent()
    }

    fun startProjection(resultCode: Int, data: Intent) {
        release()

        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        mediaProjection!!.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                cleanup()
            }
        }, null)

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2)

        virtualDisplay = mediaProjection!!.createVirtualDisplay(
            "SquareOverlayCapture",
            screenWidth,
            screenHeight,
            screenDensity,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader!!.getSurface(),
            null,
            null
        )
    }

    fun captureScreenshot(
        xPercent: Float,
        yPercent: Float,
        widthPercent: Float,
        heightPercent: Float,
        screenshotNumber: Int,
        isLineStart: Boolean
    ) {
        if (mediaProjection == null || virtualDisplay == null || imageReader == null) {
            Toast.makeText(context, "Screenshot not ready, please wait...", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (isCapturing) {
            Toast.makeText(context, "Please wait, capturing...", Toast.LENGTH_SHORT).show()
            return
        }

        isCapturing = true

        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            var image: Image? = null
            var bitmap: Bitmap? = null
            try {
                image = imageReader!!.acquireLatestImage()

                if (image != null) {
                    val planes = image.getPlanes()
                    val buffer = planes[0]!!.getBuffer()
                    val pixelStride = planes[0]!!.getPixelStride()
                    val rowStride = planes[0]!!.getRowStride()
                    val rowPadding = rowStride - pixelStride * screenWidth

                    bitmap = Bitmap.createBitmap(
                        screenWidth + rowPadding / pixelStride,
                        screenHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)

                    var cropX = ((xPercent / 100) * screenWidth).toInt()
                    var cropY = ((yPercent / 100) * screenHeight).toInt()
                    var cropWidth = ((widthPercent / 100) * screenWidth).toInt()
                    var cropHeight = ((heightPercent / 100) * screenHeight).toInt()

                    cropX = max(0, min(cropX, screenWidth - 1))
                    cropY = max(0, min(cropY, screenHeight - 1))
                    cropWidth = min(cropWidth, screenWidth - cropX)
                    cropHeight = min(cropHeight, screenHeight - cropY)

                    val croppedBitmap =
                        Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)

                    saveBitmap(croppedBitmap, screenshotNumber, isLineStart)

                    croppedBitmap.recycle()

                    Toast.makeText(
                        context,
                        "Screenshot saved to Pictures/SquareOverlay",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Screenshot failed: " + e.message, Toast.LENGTH_SHORT)
                    .show()
            } finally {
                if (bitmap != null) {
                    bitmap.recycle()
                }
                if (image != null) {
                    image.close()
                }
                isCapturing = false
            }
        }, 100)
    }

    fun setZoomFolder(zoomFolder: String?) {
        this.currentZoomFolder = zoomFolder
    }

    private fun saveBitmap(bitmap: Bitmap, screenshotNumber: Int, isLineStart: Boolean) {
        try {
            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "SquareOverlay")

            if (!appDir.exists()) {
                appDir.mkdirs()
            }

            // Determine target directory based on whether we're in a zoom folder
            val targetDir = if (currentZoomFolder != null) {
                File(appDir, currentZoomFolder)
            } else {
                appDir
            }

            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            val filename = String.format("%05d", screenshotNumber) + (if (isLineStart) "z" else "") + ".png"
            val file = File(targetDir, filename)

            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
        }
    }

    private fun saveBitmapToTestFolder(bitmap: Bitmap, subfolder: String, filename: String) {
        try {
            val picturesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val appDir = File(picturesDir, "SquareOverlay")
            val testDir = File(appDir, subfolder)

            if (!testDir.exists()) {
                testDir.mkdirs()
            }

            val file = File(testDir, filename)

            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
        }
    }

    fun captureTestScreenshot(
        xPercent: Float,
        yPercent: Float,
        widthPercent: Float,
        heightPercent: Float,
        subfolder: String,
        filename: String
    ) {
        if (mediaProjection == null || virtualDisplay == null || imageReader == null) {
            Toast.makeText(context, "Screenshot not ready, please wait...", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (isCapturing) {
            Toast.makeText(context, "Please wait, capturing...", Toast.LENGTH_SHORT).show()
            return
        }

        isCapturing = true

        Handler(Looper.getMainLooper()).postDelayed(Runnable {
            var image: Image? = null
            var bitmap: Bitmap? = null
            try {
                image = imageReader!!.acquireLatestImage()

                if (image != null) {
                    val planes = image.getPlanes()
                    val buffer = planes[0]!!.getBuffer()
                    val pixelStride = planes[0]!!.getPixelStride()
                    val rowStride = planes[0]!!.getRowStride()
                    val rowPadding = rowStride - pixelStride * screenWidth

                    bitmap = Bitmap.createBitmap(
                        screenWidth + rowPadding / pixelStride,
                        screenHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    bitmap.copyPixelsFromBuffer(buffer)

                    var cropX = ((xPercent / 100) * screenWidth).toInt()
                    var cropY = ((yPercent / 100) * screenHeight).toInt()
                    var cropWidth = ((widthPercent / 100) * screenWidth).toInt()
                    var cropHeight = ((heightPercent / 100) * screenHeight).toInt()

                    cropX = max(0, min(cropX, screenWidth - 1))
                    cropY = max(0, min(cropY, screenHeight - 1))
                    cropWidth = min(cropWidth, screenWidth - cropX)
                    cropHeight = min(cropHeight, screenHeight - cropY)

                    val croppedBitmap =
                        Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)

                    saveBitmapToTestFolder(croppedBitmap, subfolder, filename)

                    croppedBitmap.recycle()

                    Toast.makeText(context, "Test screenshot saved", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Screenshot failed: " + e.message, Toast.LENGTH_SHORT)
                    .show()
            } finally {
                if (bitmap != null) {
                    bitmap.recycle()
                }
                if (image != null) {
                    image.close()
                }
                isCapturing = false
            }
        }, 100)
    }

    private fun cleanup() {
        if (virtualDisplay != null) {
            virtualDisplay!!.release()
            virtualDisplay = null
        }
        if (imageReader != null) {
            imageReader!!.close()
            imageReader = null
        }
    }

    fun release() {
        cleanup()
        if (mediaProjection != null) {
            mediaProjection!!.stop()
            mediaProjection = null
        }
    }
}
