package com.example.splendore;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class FrameOverlayView extends View {

    private Bitmap bgImage;
    private Paint overlayPaint, clearPaint;

    // Dynamic values from camPage
    public int windowSize = 0;
    public int windowLeft = 0;
    public int windowTop = 0;

    private float cornerRadius = 40f;

    public FrameOverlayView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    private void init() {
        bgImage = BitmapFactory.decodeResource(getResources(), R.drawable.frame_bg);

        overlayPaint = new Paint();
        overlayPaint.setColor(0xA0000000);

        clearPaint = new Paint();
        clearPaint.setAntiAlias(true);
        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    public void updateWindow(int left, int top, int size) {
        this.windowLeft = left;
        this.windowTop = top;
        this.windowSize = size;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int W = getWidth();
        int H = getHeight();

        Bitmap scaled = Bitmap.createScaledBitmap(bgImage, W, H, true);
        canvas.drawBitmap(scaled, 0, 0, null);

        canvas.drawRect(0, 0, W, H, overlayPaint);

        RectF rect = new RectF(windowLeft, windowTop, windowLeft + windowSize, windowTop + windowSize);

        Path p = new Path();
        p.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CCW);

        canvas.drawPath(p, clearPaint);
    }

    public void drawFrameOnCanvas(Canvas canvas, Bitmap userPhoto, int finalLeft, int finalTop, int finalSize) {

        int W = canvas.getWidth();
        int H = canvas.getHeight();

        Bitmap scaled = Bitmap.createScaledBitmap(bgImage, W, H, true);
        canvas.drawBitmap(scaled, 0, 0, null);

        RectF rect = new RectF(finalLeft, finalTop, finalLeft + finalSize, finalTop + finalSize);

        Bitmap cropped = Bitmap.createScaledBitmap(userPhoto,
                finalSize, finalSize, true);

        int save = canvas.save();

        Path clip = new Path();
        clip.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CCW);
        canvas.clipPath(clip);

        canvas.drawBitmap(cropped, rect.left, rect.top, null);

        canvas.restoreToCount(save);
    }
}
