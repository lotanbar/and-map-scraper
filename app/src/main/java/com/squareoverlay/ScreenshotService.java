package com.squareoverlay;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;

public class ScreenshotService {

    private Context context;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;

    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    private boolean isCapturing = false;

    public ScreenshotService(Context context) {
        this.context = context;
        this.projectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
    }

    public Intent createScreenCaptureIntent() {
        return projectionManager.createScreenCaptureIntent();
    }

    public void startProjection(int resultCode, Intent data) {
        release();

        mediaProjection = projectionManager.getMediaProjection(resultCode, data);

        mediaProjection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                cleanup();
            }
        }, null);

        imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "SquareOverlayCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                null
        );
    }

    public void captureScreenshot(float xPercent, float yPercent, float widthPercent, float heightPercent, int screenshotNumber, boolean isLineStart) {
        if (mediaProjection == null || virtualDisplay == null || imageReader == null) {
            Toast.makeText(context, "Screenshot not ready, please wait...", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isCapturing) {
            Toast.makeText(context, "Please wait, capturing...", Toast.LENGTH_SHORT).show();
            return;
        }

        isCapturing = true;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Image image = null;
            Bitmap bitmap = null;

            try {
                image = imageReader.acquireLatestImage();

                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * screenWidth;

                    bitmap = Bitmap.createBitmap(
                            screenWidth + rowPadding / pixelStride,
                            screenHeight,
                            Bitmap.Config.ARGB_8888
                    );
                    bitmap.copyPixelsFromBuffer(buffer);

                    int cropX = (int) ((xPercent / 100) * screenWidth);
                    int cropY = (int) ((yPercent / 100) * screenHeight);
                    int cropWidth = (int) ((widthPercent / 100) * screenWidth);
                    int cropHeight = (int) ((heightPercent / 100) * screenHeight);

                    cropX = Math.max(0, Math.min(cropX, screenWidth - 1));
                    cropY = Math.max(0, Math.min(cropY, screenHeight - 1));
                    cropWidth = Math.min(cropWidth, screenWidth - cropX);
                    cropHeight = Math.min(cropHeight, screenHeight - cropY);

                    Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight);

                    saveBitmap(croppedBitmap, screenshotNumber, isLineStart);

                    croppedBitmap.recycle();

                    Toast.makeText(context, "Screenshot saved to Pictures/SquareOverlay", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(context, "Screenshot failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            } finally {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                if (image != null) {
                    image.close();
                }
                isCapturing = false;
            }
        }, 100);
    }

    private void saveBitmap(Bitmap bitmap, int screenshotNumber, boolean isLineStart) {
        try {
            File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File appDir = new File(picturesDir, "SquareOverlay");

            if (!appDir.exists()) {
                appDir.mkdirs();
            }

            String filename = screenshotNumber + (isLineStart ? "z" : "") + ".png";
            File file = new File(appDir, filename);

            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
        }
    }

    private void cleanup() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }
    }

    public void release() {
        cleanup();
        if (mediaProjection != null) {
            mediaProjection.stop();
            mediaProjection = null;
        }
    }
}
