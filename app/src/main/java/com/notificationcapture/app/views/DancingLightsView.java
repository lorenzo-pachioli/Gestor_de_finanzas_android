package com.notificationcapture.app.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DancingLightsView extends View {

    private static final int NUM_ORBS = 6;
    private final List<LightOrb> orbs = new ArrayList<>();
    private final Random random = new Random();
    private boolean initialized = false;
    private float speedMultiplier = 3.0f;

    public DancingLightsView(Context context) {
        super(context);
        init();
    }

    public DancingLightsView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DancingLightsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setClickable(false);
        setFocusable(false);
    }

    public void setSpeedMultiplier(float speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0 && !initialized) {
            setupOrbs(w, h);
            initialized = true;
        }
    }

    private void setupOrbs(int width, int height) {
        orbs.clear();
        int[] colors = {
                Color.parseColor("#1A00A896"), // Very translucent accent_main
                Color.parseColor("#1A02C39A"), // Very translucent accent_secondary
                Color.parseColor("#1A3A6CA6")  // Very translucent blue (from start_color)
        };

        for (int i = 0; i < NUM_ORBS; i++) {
            float x = random.nextFloat() * width;
            float y = random.nextFloat() * height;
            float radius = 300 + random.nextFloat() * 400;
            int color = colors[random.nextInt(colors.length)];
            float dx = (random.nextFloat() - 0.5f) * 1.5f;
            float dy = (random.nextFloat() - 0.5f) * 1.5f;
            orbs.add(new LightOrb(x, y, radius, color, dx, dy));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (LightOrb orb : orbs) {
            orb.update(getWidth(), getHeight(), speedMultiplier);
            orb.draw(canvas);
        }

        postInvalidateOnAnimation();
    }

    private static class LightOrb {
        float x, y, radius;
        int color;
        float dx, dy;
        Paint paint;

        LightOrb(float x, float y, float radius, int color, float dx, float dy) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.color = color;
            this.dx = dx;
            this.dy = dy;
            this.paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        }

        void update(int width, int height, float speedFactor) {
            x += dx * speedFactor;
            y += dy * speedFactor;

            if (x < -radius) x = width + radius;
            if (x > width + radius) x = -radius;
            if (y < -radius) y = height + radius;
            if (y > height + radius) y = -radius;
        }

        void draw(Canvas canvas) {
            RadialGradient gradient = new RadialGradient(
                    x, y, radius,
                    color, Color.TRANSPARENT,
                    Shader.TileMode.CLAMP
            );
            paint.setShader(gradient);
            canvas.drawCircle(x, y, radius, paint);
        }
    }
}
