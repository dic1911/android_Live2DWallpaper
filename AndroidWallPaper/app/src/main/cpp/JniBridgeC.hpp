/**
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

#pragma once
#include <condition_variable>
#include <mutex>

namespace {
    std::mutex init_mutex;
    std::condition_variable_any started_c;
    bool delegate_ready = false;
}

/**
* @brief Jni Bridge Class
*/
class JniBridgeC
{
public:
    /**
    * @brief Javaからファイル読み込み
    */
    static char* LoadFileAsBytesFromJava(const char* filePath, unsigned int* outSize);
    static char* LoadFileFromFilesAsBytesFromJava(const char* filePath, unsigned int* outSize);
};
