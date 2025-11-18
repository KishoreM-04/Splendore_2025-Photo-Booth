package com.example.splendore;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;

public class FrameOverlayView extends View {

    private Paint overlayPaint;
    private Paint clearCirclePaint;
    private Paint borderPaint;
    private Bitmap bgImage;

    public FrameOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {

        // Background image from drawable
        bgImage = BitmapFactory.decodeResource(getResources(), R.drawable.frame_bg);


        // Dim overlay
        overlayPaint = new Paint();
        overlayPaint.setColor(Color.argb(160, 0, 0, 0));
        overlayPaint.setStyle(Paint.Style.FILL);

        // Clear-circle paint
        clearCirclePaint = new Paint();
        clearCirclePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        clearCirclePaint.setAntiAlias(true);

        // Circle border paint (reduced thickness)
        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(5);   // Reduced from 8
        borderPaint.setAntiAlias(true);

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        // Draw background image stretched to view size
        if (bgImage != null) {
            Bitmap scaled = Bitmap.createScaledBitmap(bgImage, width, height, true);
            canvas.drawBitmap(scaled, 0, 0, null);
        }

        // Draw dim overlay
        canvas.drawRect(0, 0, width, height, overlayPaint);

        int centerX = width / 2;
        int centerY = height / 2 - 100;
        int radius = (int) (Math.min(width, height) / 2.6);

        // Create transparent circular area
        canvas.drawCircle(centerX, centerY, radius, clearCirclePaint);

        // Draw white circle border
        canvas.drawCircle(centerX, centerY, radius, borderPaint);
    }

    public void drawFrameOnCanvas(Canvas canvas, int width, int height, Bitmap userPhoto) {

        // Draw background
        if (bgImage != null) {
            Bitmap scaledBg = Bitmap.createScaledBitmap(bgImage, width, height, true);
            canvas.drawBitmap(scaledBg, 0, 0, null);
        }

        int centerX = width / 2;
        int centerY = height / 2 + 30;
        int radius = (int)(Math.min(width, height) / 2.6f);

        // --- CUT HOLE ---
        Paint clearPaint = new Paint();
        clearPaint.setAntiAlias(true);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawCircle(centerX, centerY, radius, clearPaint);

        // --- DRAW USER PHOTO AS PERFECT CIRCLE ---
        if (userPhoto != null) {

            Bitmap scaled = Bitmap.createScaledBitmap(
                    userPhoto,
                    radius * 2,
                    radius * 2,
                    true
            );

            int save = canvas.save();

            Path clipPath = new Path();
            clipPath.addCircle(centerX, centerY, radius, Path.Direction.CCW);
            canvas.clipPath(clipPath);

            canvas.drawBitmap(
                    scaled,
                    centerX - radius,
                    centerY - radius,
                    null
            );

            canvas.restoreToCount(save);
        }

        // --- DRAW BORDER ---
        Paint border = new Paint();
        border.setColor(Color.WHITE);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(5);
        border.setAntiAlias(true);

        canvas.drawCircle(centerX, centerY, radius, border);
    }


}
