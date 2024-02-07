/*
 * Copyright(c) Live2D Inc. All rights reserved.
 * <p>
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d.wp;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ConfigurationInfo;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import net.rbgrn.android.glwallpaperservice.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class LiveWallpaperService extends GLWallpaperService {

    private static LiveWallpaperService instance;
    private Live2DWallpaperEngine engine;
    private BroadcastReceiver receiver;

    public static List<String> forceLoopIdle = Arrays.asList("Fubuki");

    private boolean started = false;

    public static LiveWallpaperService getInstance() {
        return instance;
    }

    public Live2DWallpaperEngine getEngine() {
        return engine;
    }

    public LiveWallpaperService() {
        super();
        instance = this;
    }

    public Engine onCreateEngine() {
        Live2DWallpaperEngine engine = new Live2DWallpaperEngine();

        receiver = new Live2DReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
//        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS); // Android API 31から非推奨
//        filter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        // TODO: fix crash (receiver leak)
        try {
            this.registerReceiver(receiver, filter);
        } catch (Exception ignored) {}
        this.engine = engine;
        return engine;
    }

    @Override
    public void onDestroy() {
        try {
            this.unregisterReceiver(receiver);
        } catch (Exception ignored) {}
        super.onDestroy();
    }

    public class Live2DWallpaperEngine extends GLEngine {

        public Live2DGLRenderer renderer;

        private class SetParamThread extends Thread {
            String model;
            boolean loop_idle;
            boolean use_bg;
            boolean custom_bg;
            boolean touch_interact;
            boolean no_reset;
            int model_scale;
            int x_offset;
            int y_offset;
            public SetParamThread(String model, boolean loop_idle, boolean use_bg, boolean custom_bg, boolean touch_interact, boolean no_reset, int model_scale, int x_offset, int y_offset) {
                this.model = model;
                this.loop_idle = loop_idle;
                this.use_bg = use_bg;
                this.custom_bg = custom_bg;
                this.touch_interact = touch_interact;
                this.no_reset = no_reset;
                this.model_scale = model_scale;
                this.x_offset = x_offset;
                this.y_offset = y_offset;
            }
            @Override
            public void run() {
                super.run();
                try {
                    Thread.sleep(300);
                    JniBridgeJava.SetParam(model, loop_idle, use_bg, custom_bg, touch_interact, no_reset, model_scale, x_offset, y_offset);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public Live2DWallpaperEngine() {
            super();

            JniBridgeJava.SetContext(getApplicationContext());
            JniBridgeJava.nativeOnStart();
            SharedPreferences prefs = getPrefs();
            String model = prefs.getString("model", "kanade_normal_0101");
            boolean loop_idle = prefs.getBoolean("loop_idle_motion", false) || forceLoopIdle.contains(model);
            boolean use_bg = prefs.getBoolean("use_background", false);
            boolean custom_bg = prefs.getBoolean("custom_background", false);
            boolean no_reset = prefs.getBoolean("no_reset", false);
            boolean touch_interact = prefs.getBoolean("default_touch_interaction", true) && !(loop_idle || no_reset);
            int model_scale = Integer.parseInt(Objects.requireNonNull(prefs.getString("model_scale", "100")));
            int x_offset = Integer.parseInt(Objects.requireNonNull(prefs.getString("x_offset", "0")));
            int y_offset = Integer.parseInt(Objects.requireNonNull(prefs.getString("y_offset", "0")));
            new SetParamThread(model, loop_idle, use_bg, custom_bg, touch_interact, no_reset, model_scale, x_offset, y_offset).start();
            started = true;
        }

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);

            // Check if the system supports OpenGL ES 2.0.
            // システムがOpenGL ES 2.0に対応しているかのチェック
            final ActivityManager activityManager =
                    (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            final ConfigurationInfo configurationInfo =
                    activityManager.getDeviceConfigurationInfo();
            final boolean supportsEs2 =
                    configurationInfo.reqGlEsVersion >= 0x20000;

            if (supportsEs2)
            {
                // Request an OpenGL ES 2.0 compatible context.
                // OpenGL ES 2.0互換のコンテキストを要求
                setEGLContextClientVersion(2);

                // On Honeycomb+ devices, this improves the performance when
                // leaving and resuming the live wallpaper.
                // Honeycomb以降のデバイスでは、ライブ壁紙を終了して再開した際のパフォーマンスが向上します。
                setPreserveEGLContextOnPause(true);

                // Set the renderer.
                // レンダラーの設定
                renderer = new Live2DGLRenderer(getApplicationContext());
                setRenderer(renderer);
            }
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            float pointX = event.getX();
            float pointY = event.getY();
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    JniBridgeJava.nativeOnTouchesBegan(pointX, pointY);
                    break;
                case MotionEvent.ACTION_UP:
                    JniBridgeJava.nativeOnTouchesEnded(pointX, pointY);
                    break;
                case MotionEvent.ACTION_MOVE:
                    JniBridgeJava.nativeOnTouchesMoved(pointX, pointY);
                    break;
            }
        }

        public void onDestroy() {
            super.onDestroy();
            JniBridgeJava.nativeOnDestroy();
            try {
                unregisterReceiver(receiver);
            } catch (Exception ignored) {}
            if (renderer != null) {
                renderer.release();
            }
            renderer = null;
        }

        private SharedPreferences getPrefs() {
            return getApplicationContext().getSharedPreferences("com.live2d.wp_preferences", Context.MODE_PRIVATE);
        }
    }
}
