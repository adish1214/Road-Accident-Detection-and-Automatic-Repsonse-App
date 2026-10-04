package com.example.crashdetector;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class RotatingHeartbeat extends View {

    private Paint heartPaint;
    private Paint circlePaint;
    private float rotationAngle = 0f;
    private Bitmap heartBitmap;  // ✅ Add a bitmap variable

    public RotatingHeartbeat(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Dotted rotating circle
        heartPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        heartPaint.setColorFilter(new PorterDuffColorFilter(
                getResources().getColor(R.color.scanner_green_bright), PorterDuff.Mode.SRC_IN));
        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(8f);
        circlePaint.setColor(getResources().getColor(R.color.scanner_green));
        circlePaint.setPathEffect(new DashPathEffect(new float[]{25f, 25f}, 0));

        // Load heartbeat image from drawable
        heartBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.heartbeat_icon);

        startAnimation();
    }

    private void startAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(25000);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            rotationAngle = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float outerRadius = Math.min(cx, cy) - 20;

        // 🔄 Outer rotating dotted circle
        canvas.save();
        canvas.rotate(rotationAngle, cx, cy);
        canvas.drawCircle(cx, cy, outerRadius, circlePaint);
        canvas.restore();

        // 🔵 Middle static circle
        Paint middlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        middlePaint.setStyle(Paint.Style.STROKE);
        middlePaint.setStrokeWidth(6f);
        middlePaint.setColor(getResources().getColor(R.color.scanner_green_faded));
        float middleRadius = outerRadius - 40;
        canvas.drawCircle(cx, cy, middleRadius, middlePaint);

        // ❤️ Draw heartbeat image
        drawHeartbeatImage(canvas, cx, cy, middleRadius / 2);
    }

    private void drawHeartbeatImage(Canvas canvas, float cx, float cy, float size) {
        if (heartBitmap != null) {
            // Scale the bitmap to the desired size
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(heartBitmap,
                    (int)(size * 2.5), (int)(size * 2.5), true);

            // Draw it centered
            float left = cx - scaledBitmap.getWidth() / 2f;
            float top = cy - scaledBitmap.getHeight() / 2f;
            canvas.drawBitmap(scaledBitmap, left, top, heartPaint);
        }
    }
}