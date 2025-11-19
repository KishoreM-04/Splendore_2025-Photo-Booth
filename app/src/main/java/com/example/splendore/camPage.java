package com.example.splendore;

import android.Manifest;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Size;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.OutputStream;
import java.nio.ByteBuffer;

public class camPage extends AppCompatActivity {

    private PreviewView previewView;
    private FrameOverlayView overlayView;
    private FloatingActionButton btnCapture;
    private MaterialButton btnSwitchCamera;

    private ImageCapture imageCapture;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
    private boolean isFront = false;

    // Dynamic values (screen-based)
    private int windowSize, windowLeft, windowTop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cam_page);

        previewView = findViewById(R.id.previewView);
        overlayView = findViewById(R.id.overlayView);
        btnCapture = findViewById(R.id.btnCapture);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);

        // calculate layout based on device resolution
        overlayView.post(() -> setupDynamicWindow());

        btnCapture.setOnClickListener(v -> capturePhoto());
        btnSwitchCamera.setOnClickListener(v -> switchCamera());
    }

    private void setupDynamicWindow() {

        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;
        int screenH = dm.heightPixels;

        // 1. Square = 81% of screen width
        windowSize = (int) (screenW * 0.81f);

        // 2. Center horizontally
        windowLeft = (screenW - windowSize) / 2;

        // 3. Polaroid vertical position = 21% of screen height
        windowTop = (int) (screenH * 0.21f);

        // update overlay
        overlayView.updateWindow(windowLeft, windowTop, windowSize);

        // size preview for universal perfect alignment
        ViewGroup.LayoutParams params = previewView.getLayoutParams();
        params.height = (int) (screenH * 0.70f);
        previewView.setLayoutParams(params);

        setupCamera();
    }

    private void setupCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 300);
            return;
        }

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                bindUseCases(provider);
            } catch (Exception ignored) {}
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindUseCases(ProcessCameraProvider provider) {

        Preview preview = new Preview.Builder()
                .setTargetResolution(new Size(windowSize, windowSize))
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        imageCapture = new ImageCapture.Builder()
                .setTargetResolution(new Size(windowSize, windowSize))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build();

        provider.unbindAll();
        provider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
    }

    private void switchCamera() {
        isFront = !isFront;
        cameraSelector = isFront ?
                CameraSelector.DEFAULT_FRONT_CAMERA :
                CameraSelector.DEFAULT_BACK_CAMERA;
        startCamera();
    }

    private void capturePhoto() {
        imageCapture.takePicture(ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageCapturedCallback() {
                    @Override
                    public void onCaptureSuccess(@NonNull ImageProxy image) {

                        Bitmap bmp = imageToBitmap(image);
                        image.close();

                        Bitmap finalImage = createFinalImage(bmp);
                        saveImage(finalImage);

                        Toast.makeText(camPage.this, "Saved!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private Bitmap imageToBitmap(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

        Matrix m = new Matrix();
        m.postRotate(image.getImageInfo().getRotationDegrees());
        if (isFront) m.postScale(-1, 1);

        return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), m, true);
    }

    private Bitmap createFinalImage(Bitmap photo) {

        // Final output resolution
        int outW = 1080;
        int outH = 1920;

        Bitmap result = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        // Get screen resolution used earlier
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;
        int screenH = dm.heightPixels;

        // --- SCALE FACTORS ---
        float scaleX = (float) outW / screenW;
        float scaleY = (float) outH / screenH;

        // --- CONVERT WINDOW POSITIONS ---
        int finalLeft = (int) (windowLeft * scaleX);
        int finalTop  = (int) (windowTop * scaleY);
        int finalSize = (int) (windowSize * scaleX); // ONLY X because it must stay square

        // Draw the polaroid background
        Bitmap bg = BitmapFactory.decodeResource(getResources(), R.drawable.frame_bg);
        Bitmap scaledBg = Bitmap.createScaledBitmap(bg, outW, outH, true);
        canvas.drawBitmap(scaledBg, 0, 0, null);

        // Scale user's captured photo into the square
        Bitmap scaledPhoto = Bitmap.createScaledBitmap(photo, finalSize, finalSize, true);

        // Draw it inside the square
        canvas.drawBitmap(scaledPhoto, finalLeft, finalTop, null);

        return result;
    }

    private void saveImage(Bitmap bitmap) {
        try {
            String filename = "Splendore_" + System.currentTimeMillis() + ".jpg";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/SplendoreFrame");

            Uri uri = getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            OutputStream out = getContentResolver().openOutputStream(uri);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 96, out);
            out.close();

        } catch (Exception ignored) {}
    }
}
