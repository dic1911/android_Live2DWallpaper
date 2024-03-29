/**
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

#include "LWallpaperPal.hpp"
#include <stdio.h>
#include <stdlib.h>
#include <stdarg.h>
#include <sys/stat.h>
#include <time.h>
#include <iostream>
#include <fstream>
#include <GLES2/gl2.h>
#include <android/log.h>
#include <Model/CubismMoc.hpp>
#include "LWallpaperDefine.hpp"
#include "JniBridgeC.hpp"

using std::endl;
using namespace Csm;
using namespace std;
using namespace LWallpaperDefine;

double LWallpaperPal::s_currentFrame = 0.0;
double LWallpaperPal::s_lastFrame = 0.0;
double LWallpaperPal::s_deltaTime = 0.0;

csmByte* LWallpaperPal::LoadFileAsBytes(const string filePath, csmSizeInt* outSize, bool fromFiles)
{
    const char* path = filePath.c_str();

    // file buffer
    char* buf;
    if (fromFiles) {
        buf = JniBridgeC::LoadFileFromFilesAsBytesFromJava(path, outSize);
    } else {
        buf = JniBridgeC::LoadFileAsBytesFromJava(path, outSize);
    }

    return reinterpret_cast<csmByte*>(buf);
}

void LWallpaperPal::ReleaseBytes(csmByte* byteData)
{
    delete[] byteData;
}

csmFloat32  LWallpaperPal::GetDeltaTime()
{
    return static_cast<csmFloat32>(s_deltaTime);
}

void LWallpaperPal::UpdateTime()
{
    s_currentFrame = GetSystemTime();
    s_deltaTime = s_currentFrame - s_lastFrame;
    s_lastFrame = s_currentFrame;
}

void LWallpaperPal::PrintLog(const csmChar* format, ...)
{
    va_list args;
    csmChar buf[256];
    va_start(args, format);
    __android_log_vprint(ANDROID_LOG_DEBUG, "NativePrint", format, args);    // 標準出力でレンダリング
    std::cerr << buf << std::endl;
    va_end(args);
}

void LWallpaperPal::PrintMessage(const csmChar* message)
{
    PrintLog("%s", message);
}

double LWallpaperPal::GetSystemTime()
{
    struct timespec res;
    clock_gettime(CLOCK_MONOTONIC, &res);
    return (res.tv_sec + res.tv_nsec * 1e-9);
}
