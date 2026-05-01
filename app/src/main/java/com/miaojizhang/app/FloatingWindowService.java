package com.miaojizhang.app;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class FloatingWindowService extends Service {
    private WindowManager windowManager;
    private View floatView;
    private WindowManager.LayoutParams params;
    private int startX;
    private int startY;
    private float downX;
    private float downY;
    private boolean moved;
    private int bubbleSize;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        createFloatingButton();
    }

    private void createFloatingButton() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if (windowManager == null || floatView != null) return;

        float density = getResources().getDisplayMetrics().density;
        bubbleSize = (int) (56 * density);

        TextView bubble = new TextView(this);
        bubble.setText("+");
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(30);
        bubble.setGravity(Gravity.CENTER);
        bubble.setIncludeFontPadding(false);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(18, 185, 129), Color.rgb(0, 210, 150)});
        bg.setShape(GradientDrawable.OVAL);
        bubble.setBackground(bg);
        bubble.setElevation(10 * density);
        bubble.setAlpha(0.94f);
        floatView = bubble;

        params = new WindowManager.LayoutParams();
        params.width = bubbleSize;
        params.height = bubbleSize;
        params.gravity = Gravity.TOP | Gravity.START;
        params.format = PixelFormat.TRANSLUCENT;
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        SharedPreferences sp = getSharedPreferences("miaojizhang_native", MODE_PRIVATE);
        int sw = getResources().getDisplayMetrics().widthPixels;
        int sh = getResources().getDisplayMetrics().heightPixels;
        params.x = sp.getInt("float_x", Math.max(0, sw - bubbleSize - (int)(8 * density)));
        params.y = sp.getInt("float_y", Math.max((int)(100 * density), sh / 2 - bubbleSize));

        floatView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = params.x;
                    startY = params.y;
                    downX = event.getRawX();
                    downY = event.getRawY();
                    moved = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    int dx = (int) (event.getRawX() - downX);
                    int dy = (int) (event.getRawY() - downY);
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) moved = true;
                    params.x = startX + dx;
                    params.y = startY + dy;
                    try { windowManager.updateViewLayout(floatView, params); } catch (Exception ignored) {}
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!moved) {
                        openAddRecord();
                    } else {
                        snapToEdge();
                    }
                    return true;
            }
            return false;
        });

        try {
            windowManager.addView(floatView, params);
        } catch (Exception e) {
            floatView = null;
            stopSelf();
        }
    }

    private void openAddRecord() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("open_add", true);
        startActivity(intent);
    }

    private void snapToEdge() {
        int sw = getResources().getDisplayMetrics().widthPixels;
        int sh = getResources().getDisplayMetrics().heightPixels;
        params.x = params.x + bubbleSize / 2 < sw / 2 ? 0 : sw - bubbleSize;
        if (params.y < 0) params.y = 0;
        if (params.y > sh - bubbleSize) params.y = sh - bubbleSize;
        try { windowManager.updateViewLayout(floatView, params); } catch (Exception ignored) {}
        getSharedPreferences("miaojizhang_native", MODE_PRIVATE)
                .edit()
                .putInt("float_x", params.x)
                .putInt("float_y", params.y)
                .apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (windowManager != null && floatView != null) {
            try { windowManager.removeView(floatView); } catch (Exception ignored) {}
        }
        floatView = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
