package com.example.splendore;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class camPage extends AppCompatActivity {

    private static final int REQUEST_PERMISSION = 101;
    private PreviewView previewView;
    private FrameOverlayView overlayView;
    private FloatingActionButton btnCapture;
    private MaterialButton btnSwitchCamera;
    private ImageCapture imageCapture;
    private CameraSelector cameraSelector;
    private boolean isFrontCamera = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam_page);

        initViews();
        checkPermissionsAndStartCamera();
    }

    private void initViews() {
        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        btnCapture = findViewById(R.id.btnCapture);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);

        btnCapture.setOnClickListener(v -> capturePhoto());
        btnSwitchCamera.setOnClickListener(v -> switchCamera());
    }

    private void checkPermissionsAndStartCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraUseCases(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        cameraSelector = isFrontCamera ?
                CameraSelector.DEFAULT_FRONT_CAMERA :
                CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            cameraProvider.unbindAll();
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Toast.makeText(this, "Camera binding failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void switchCamera() {
        isFrontCamera = !isFrontCamera;
        startCamera();
    }

    private void capturePhoto() {
        if (imageCapture == null) return;

        btnCapture.setEnabled(false);

        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {
                        Bitmap photoBitmap = imageProxyToBitmap(image);
                        image.close();

                        if (photoBitmap != null) {
                            Bitmap framedBitmap = createFramedImage(photoBitmap);
                            saveImage(framedBitmap);
                        }

                        btnCapture.setEnabled(true);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Toast.makeText(camPage.this,
                                "Capture failed: " + exception.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        btnCapture.setEnabled(true);
                    }
                });
    }

    private Bitmap imageProxyToBitmap(ImageProxy image) {
        ImageProxy.PlaneProxy[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        Bitmap bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        // Rotate if needed
        Matrix matrix = new Matrix();
        matrix.postRotate(image.getImageInfo().getRotationDegrees());

        if (isFrontCamera) {
            matrix.postScale(-1, 1, bitmap.getWidth() / 2f, bitmap.getHeight() / 2f);
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
                bitmap.getHeight(), matrix, true);
    }

    private Bitmap createFramedImage(Bitmap photo) {
        int width = 1080;
        int height = 1920;

        // Circle position - must match FrameOverlayView
        int centerX = width / 2;
        int centerY = height / 2 - 100;
        int radius = (int) (Math.min(width, height) / 2.6);

        // Step 1: Scale camera bitmap so that it covers the whole screen
        Bitmap scaledPhoto = scaleCenterCrop(photo, width, height);

        // Step 2: Crop the EXACT circle area
        Bitmap circleCrop = Bitmap.createBitmap(radius * 2, radius * 2, Bitmap.Config.ARGB_8888);
        Canvas cropCanvas = new Canvas(circleCrop);

        // Create circular mask
        Paint mask = new Paint(Paint.ANTI_ALIAS_FLAG);
        mask.setColor(Color.BLACK);
        cropCanvas.drawCircle(radius, radius, radius, mask);

        // Apply SRC_IN mode (keeps only inside circle)
        mask.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        cropCanvas.drawBitmap(
                scaledPhoto,
                -(centerX - radius),
                -(centerY - radius),
                mask
        );

        // Step 3: Draw final result with background frame
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        canvas.drawRGB(255, 255, 255);

        // Draw circular cropped bitmap in place
        canvas.drawBitmap(circleCrop, centerX - radius, centerY - radius, null);

        // Draw your frame (border, background)
        overlayView.drawFrameOnCanvas(canvas, width, height, circleCrop);

        return result;
    }


    private Bitmap scaleCenterCrop(Bitmap source, int targetWidth, int targetHeight) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        float xScale = (float) targetWidth / sourceWidth;
        float yScale = (float) targetHeight / sourceHeight;
        float scale = Math.max(xScale, yScale);

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        int scaledWidth = Math.round(sourceWidth * scale);
        int scaledHeight = Math.round(sourceHeight * scale);

        int left = (scaledWidth - targetWidth) / 2;
        int top = (scaledHeight - targetHeight) / 2;

        Bitmap scaled = Bitmap.createBitmap(source, 0, 0, sourceWidth, sourceHeight, matrix, true);
        return Bitmap.createBitmap(scaled, left, top, targetWidth, targetHeight);
    }

    private void saveImage(Bitmap bitmap) {
        String filename = "FramedPhoto_" +
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date()) + ".jpg";

        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/PhotoFrame");
        values.put(MediaStore.Images.Media.IS_PENDING, 1);

        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        if (uri != null) {
            try {
                OutputStream outputStream = getContentResolver().openOutputStream(uri);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                outputStream.close();

                // Make image visible in gallery
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                getContentResolver().update(uri, values, null, null);

                Toast.makeText(this, "Saved to Gallery!", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}