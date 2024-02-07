/*
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

package com.live2d.wp;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class JniBridgeJava {

    private static final String LIBRARY_NAME = "Live2DWallpaper";
    private static Context _context;

    static {
        System.loadLibrary(LIBRARY_NAME);
    }

    // Native -----------------------------------------------------------------

    public static native void nativeOnStart();

    public static native void nativeOnDestroy();

    public static native void nativeOnSurfaceCreated();

    public static native void nativeOnSurfaceChanged(int width, int height);

    public static native void nativeOnDrawFrame();

    public static native void nativeOnTouchesBegan(float pointX, float pointY);

    public static native void nativeOnTouchesEnded(float pointX, float pointY);

    public static native void nativeOnTouchesMoved(float pointX, float pointY);

    public static native void nativeStartRandomMotion();
    public static native void nativeStartIdleMotion();

    public static native void nativeStartMotion(int index);

    public static native void nativeSetClearColor(float r, float g, float b);

    public static native void SetBackGroundSpriteAlpha(float a);

    public static native void SetGravitationalAccelerationX(float gravity);

    public static native void SetParam(String model, boolean loop_idle, boolean use_bg, boolean custom_bg, boolean touch_interact, boolean no_reset, int scale, int xoffset, int yoffset);

    // Java -----------------------------------------------------------------

    public static void SetContext(Context context) {
        _context = context;
    }

    public static byte[] LoadFile(String filePath) {
        InputStream fileData = null;
        try {
            fileData = _context.getAssets().open(filePath);
            int fileSize = fileData.available();
            byte[] fileBuffer = new byte[fileSize];
            fileData.read(fileBuffer, 0, fileSize);
            return fileBuffer;
        } catch (IOException e) {
            Log.e("030-load", String.format("error occurred while trying to load '%s': %s, stack: %s", filePath, e.getMessage(), Arrays.toString(e.getStackTrace())));
            return null;
        } finally {
            try {
                if (fileData != null) {
                    fileData.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static byte[] LoadFileFromFiles(String filePath) {
        FileInputStream fileData = null;
        File filesDir = _context.getFilesDir();
        try {
            fileData = new FileInputStream(filesDir + "/" + filePath);
            int fileSize = fileData.available();
            byte[] fileBuffer = new byte[fileSize];
            fileData.read(fileBuffer, 0, fileSize);
            return fileBuffer;
        } catch (FileNotFoundException ex) {
            // fallback
            Log.e("030-load", String.format("%s wasn't found in %s, dir content:", filePath, filesDir.getAbsolutePath()));
            File[] files = filesDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    Log.d("030-load", f.getAbsolutePath());
                }
            }
            return LoadFile(filePath);
        } catch (IOException e) {
            Log.e("030-loadFromFiles", String.format("error occurred while trying to load '%s', folder content: %s, stack: %s", filePath, Arrays.toString(filesDir.listFiles()), Arrays.toString(e.getStackTrace())));
            return null;
        } finally {
            try {
                if (fileData != null) {
                    fileData.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
