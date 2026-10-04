package com.example.crashdetector;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class GridView extends View {

    private Paint paint;
    private float offset = 0f;
    private float spacing = 57f;

    public GridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setColor(Color.argb(55, 0, 255, 255)); // Visible test color
        paint.setStrokeWidth(1.3f);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.STROKE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        for (float x = -spacing + offset; x < width; x += spacing) {
            canvas.drawLine(x, 0, x, height, paint);
        }

        for (float y = -spacing + offset; y < height; y += spacing) {
            canvas.drawLine(0, y, width, y, paint);
        }

    }

    private void startAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, spacing);
        animator.setDuration(7500);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());

        animator.addUpdateListener(animation -> {
            offset = (float) animation.getAnimatedValue();

        });

        animator.start();
    }
}