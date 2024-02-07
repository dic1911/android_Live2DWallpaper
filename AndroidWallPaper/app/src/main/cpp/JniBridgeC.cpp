/**
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

#include <jni.h>
#include "JniBridgeC.hpp"
#include "LWallpaperDelegate.hpp"
#include "LWallpaperPal.hpp"
#include <unistd.h>
#include <chrono>

using namespace Csm;

static JavaVM* g_JVM; // JavaVM is valid for all threads, so just save it globally
static jclass  g_JniBridgeJavaClass;
static jmethodID g_LoadFileMethodId;
static jmethodID g_LoadFileFromFilesMethodId;

JNIEnv* GetEnv()
{
    JNIEnv* env = NULL;
    g_JVM->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6);
    return env;
}

// The VM calls JNI_OnLoad when the native library is loaded
jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved)
{
    g_JVM = vm;

    JNIEnv *env;
    if (vm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_1_6) != JNI_OK)
    {
        return JNI_ERR;
    }

    jclass clazz = env->FindClass("com/live2d/wp/JniBridgeJava");
    g_JniBridgeJavaClass = reinterpret_cast<jclass>(env->NewGlobalRef(clazz));
    g_LoadFileMethodId = env->GetStaticMethodID(g_JniBridgeJavaClass, "LoadFile", "(Ljava/lang/String;)[B");
    g_LoadFileFromFilesMethodId = env->GetStaticMethodID(g_JniBridgeJavaClass, "LoadFileFromFiles", "(Ljava/lang/String;)[B");

    return JNI_VERSION_1_6;
}

void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
    JNIEnv *env = GetEnv();
    env->DeleteGlobalRef(g_JniBridgeJavaClass);
}

char* JniBridgeC::LoadFileAsBytesFromJava(const char* filePath, unsigned int* outSize)
{
    CubismLogDebug("030 - loading file: %s", filePath);
    JNIEnv *env = GetEnv();

    // ファイルロード
    jbyteArray obj = (jbyteArray)env->CallStaticObjectMethod(g_JniBridgeJavaClass, g_LoadFileMethodId, env->NewStringUTF(filePath));

    if (obj == nullptr) {
        return LoadFileFromFilesAsBytesFromJava(filePath, outSize);
    }

    *outSize = static_cast<unsigned int>(env->GetArrayLength(obj));

    char* buffer = new char[*outSize];
    env->GetByteArrayRegion(obj, 0, *outSize, reinterpret_cast<jbyte *>(buffer));

    return buffer;
}

char* JniBridgeC::LoadFileFromFilesAsBytesFromJava(const char* filePath, unsigned int* outSize)
{
    JNIEnv *env = GetEnv();

    // ファイルロード
    jbyteArray obj = (jbyteArray)env->CallStaticObjectMethod(g_JniBridgeJavaClass, g_LoadFileFromFilesMethodId, env->NewStringUTF(filePath));
    *outSize = static_cast<unsigned int>(env->GetArrayLength(obj));

    char* buffer = new char[*outSize];
    env->GetByteArrayRegion(obj, 0, *outSize, reinterpret_cast<jbyte *>(buffer));

    return buffer;
}

extern "C"
{
    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_nativeOnStart(JNIEnv *env, jclass type)
    {
        std::lock_guard<std::mutex> guard(init_mutex);
        LWallpaperDelegate::GetInstance()->OnStart();
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_nativeOnPause(JNIEnv *env, jclass type)
    {
        LWallpaperDelegate::GetInstance()->OnPause();
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_nativeOnStop(JNIEnv *env, jclass type)
    {
        LWallpaperDelegate::GetInstance()->OnStop();
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_nativeOnDestroy(JNIEnv *env, jclass type)
    {
        LWallpaperDelegate::GetInstance()->OnDestroy();
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_nativeOnSurfaceCreated(JNIEnv *env, jclass type)
    {
        // 第一問 1.1
        LWallpaperDelegate::GetInstance()->OnSurfaceCreate();
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_nativeOnSurfaceChanged(JNIEnv *env, jclass type, jint width, jint height)
    {
        LWallpaperPal::PrintLog("030 - surface: %d x %d", width, height);
        std::unique_lock<std::mutex> guard(init_mutex);
        bool wait = started_c.wait_for(guard, std::chrono::seconds(5), []{return LWallpaperLive2DManager::GetInstance()->isReady();});
        if (!wait) LWallpaperPal::PrintLog("030 - nativeOnSurfaceChanged wait timeout", wait);

        // 第一問 1.2
        LWallpaperDelegate::GetInstance()->OnSurfaceChanged(width, height);
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_nativeOnDrawFrame(JNIEnv *env, jclass type)
    {
        std::unique_lock<std::mutex> guard(init_mutex);
        bool wait = started_c.wait_for(guard, std::chrono::seconds(5), []{return LWallpaperLive2DManager::GetInstance()->isReady();});
        if (!wait) LWallpaperPal::PrintLog("030 - nativeOnDrawFrame wait timeout", wait);
        // 第一問 1.3
        LWallpaperDelegate::GetInstance()->Run();
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_nativeOnTouchesBegan(JNIEnv *env, jclass type, jfloat pointX, jfloat pointY)
    {
        LWallpaperDelegate::GetInstance()->OnTouchBegan(pointX, pointY);
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_nativeOnTouchesEnded(JNIEnv *env, jclass type, jfloat pointX, jfloat pointY)
    {
        LWallpaperDelegate::GetInstance()->OnTouchEnded(pointX, pointY);
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_nativeOnTouchesMoved(JNIEnv *env, jclass type, jfloat pointX, jfloat pointY)
    {
        LWallpaperDelegate::GetInstance()->OnTouchMoved(pointX, pointY);
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_nativeStartRandomMotion(JNIEnv *env, jclass type)
    {
        // 第二問 2.1
        LWallpaperDelegate::GetInstance()->StartRandomMotion();
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_nativeStartMotion(JNIEnv *env, jclass type, jint index)
    {
        LWallpaperDelegate::GetInstance()->StartMotion(index);
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_nativeSetClearColor(JNIEnv *env, jclass clazz, jfloat r, jfloat g, jfloat b)
    {
        LWallpaperDelegate::GetInstance()->SetClearColor(r, g, b);
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_SetBackGroundSpriteAlpha(JNIEnv *env, jclass clazz, jfloat a)
    {
        LWallpaperDelegate::GetInstance()->SetBackGroundSpriteAlpha(a);
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_SetGravitationalAccelerationX(JNIEnv *env, jclass clazz, jfloat gravity)
    {
        LWallpaperDelegate::GetInstance()->SetGravitationalAccelerationX(gravity);
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_SetGravitationalAcceleration(JNIEnv *env, jclass clazz, jfloat x, jfloat y)
    {
        LWallpaperDelegate::GetInstance()->SetGravitationalAcceleration(x, y);
    }

    JNIEXPORT void JNICALL
    Java_com_live2d_wp_JniBridgeJava_SetParam(JNIEnv *env, jclass clazz, jstring model, jboolean loop_idle, jboolean use_bg, jboolean custom_bg, jboolean touch_interact, jboolean no_reset, jint scale, jint xoffset, jint yoffset)
    {
        const char *model_str = env->GetStringUTFChars(model, 0);
        const bool li = (loop_idle == JNI_TRUE);
        const bool bg = (use_bg == JNI_TRUE);
        const bool cbg = (custom_bg == JNI_TRUE);
        const bool ti = (touch_interact == JNI_TRUE);
        const bool nr = (no_reset == JNI_TRUE);
//        usleep(200);
//        std::lock_guard<std::mutex> guard(init_mutex);

//        std::unique_lock<std::mutex> guard(init_mutex);
//        bool result = started_c.wait_for(guard, std::chrono::seconds(1), [] {return delegate_ready;});

//        LWallpaperPal::PrintLog("030 - wait: %d", result);

        std::unique_lock<std::mutex> guard(init_mutex);
        bool wait = started_c.wait_for(guard, std::chrono::seconds(5), []{return LWallpaperLive2DManager::GetInstance()->isReady();});
        if (!wait) LWallpaperPal::PrintLog("030 - SetParam wait timeout", wait);

        auto* instance = LWallpaperLive2DManager::GetInstance();
        if (!instance) {
            LWallpaperPal::PrintLog("030 - ignoring SetParam data cus wp is null");
            return;
        }
        LWallpaperPal::PrintLog("030 - Wallpaper ready? %d", instance->isReady());
        LWallpaperPal::PrintLog("030 - Setting model: %s | loop idle: %d | use bg: %d | custom bg: %d | touch: %d", model_str, li, bg, cbg, ti);
//        LWallpaperDelegate* delegate = LWallpaperDelegate::GetInstance();
//        delegate->SetParam(const_cast<char*>(model_str), li, bg, cbg, ti, (scale / 100.0f), (xoffset / 100.0f), (yoffset / 100.0f));
        LWallpaperLive2DManager::GetInstance(const_cast<char*>(model_str))->SetParams(li, bg, cbg, ti, nr, (scale / 100.0f), (xoffset / 100.0f), (yoffset / 100.0f));
    }
}
