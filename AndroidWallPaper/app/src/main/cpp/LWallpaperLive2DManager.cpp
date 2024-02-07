/**
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

#include "LWallpaperLive2DManager.hpp"
#include <chrono>
#include <string>
#include <GLES2/gl2.h>
#include <Rendering/CubismRenderer.hpp>
#include "LWallpaperPal.hpp"
#include "LWallpaperDefine.hpp"
#include "LWallpaperDelegate.hpp"
#include "LWallpaperModel.hpp"
#include "LWallpaperView.hpp"
#include "JniBridgeC.hpp"

using namespace Csm;
using namespace LWallpaperDefine;
using namespace std;

namespace {
    LWallpaperLive2DManager* s_instance = nullptr;
    extern char* _modelDirectoryName;
    unsigned int _unused_size;
}

LWallpaperLive2DManager* LWallpaperLive2DManager::GetInstance(char* path)
{
    std::lock_guard<std::mutex> guard(init_mutex);


    if (!s_instance)
    {
        char* model_name = JniBridgeC::LoadFileFromFilesAsBytesFromJava("model", &_unused_size);
        CubismLogDebug("030: loading %s | isExternal: %d", model_name, (std::string(model_name).find(CUSTOM_MODEL) != std::string::npos));
        s_instance = new LWallpaperLive2DManager(model_name);
    } else if (!path && !s_instance->ready) {
        CubismLogDebug("030 - LWallpaperLive2DManager::GetInstance - init cuz we prob haven't");
        s_instance->init();
    }
//    else if (path && s_instance && (strcmp(path, s_instance->_modelDirectoryName) != 0)) {
        // TODO: force reload model
//        ReleaseInstance();
//        s_instance = new LWallpaperLive2DManager(path);

//        s_instance->_modelDirectoryName = path;
//        s_instance->LoadModel(path);
//    }

    return s_instance;
}

void LWallpaperLive2DManager::ReleaseInstance()
{
    if (s_instance)
    {
        delete s_instance;
    }

    s_instance = nullptr;
}

LWallpaperLive2DManager::LWallpaperLive2DManager(char* path)
{
    if (path) {
        strcpy(_modelDirectoryName, path);
        LWallpaperPal::PrintLog("030 - path? %s", _modelDirectoryName);
    }
    this->externalModel = (std::string(_modelDirectoryName).find(CUSTOM_MODEL) != std::string::npos);

    init();
}

void LWallpaperLive2DManager::init() {
    if (ready) {
        CubismLogWarning("030 - attempted to init after we're ready, aborting");
        return;
    }
    LoadModel(_modelDirectoryName);
}

LWallpaperLive2DManager::~LWallpaperLive2DManager()
{
    ReleaseModel();
}

void LWallpaperLive2DManager::ReleaseModel()
{
    delete _model;
}

LWallpaperModel* LWallpaperLive2DManager::GetModel() const
{
    std::unique_lock<std::mutex> guard(init_mutex);
    bool wait = started_c.wait_for(guard, std::chrono::seconds(5), [this]{return this->ready;});
    if (!wait) LWallpaperPal::PrintLog("030 - LWallpaperLive2DManager::GetModel wait timeout");

    return _model;
}

void LWallpaperLive2DManager::OnDrag(csmFloat32 x, csmFloat32 y) const
{
    LWallpaperModel* model = GetModel();
    model->SetDragging(x, y);
}

void LWallpaperLive2DManager::OnTap(csmFloat32 x, csmFloat32 y)
{
    if (_model->HitTest(HitAreaNameHead, x, y))
    {
        _model->SetRandomExpression();
    }
    else if (_model->HitTest(HitAreaNameBody, x, y))
    {
        _model->Reset(true);
        _model->StartRandomMotionWithOption(MotionGroupTapBody, PriorityNormal);
    }
}

void LWallpaperLive2DManager::OnUpdate() const
{
    int width = LWallpaperDelegate::GetInstance()->GetWindowWidth();
    int height = LWallpaperDelegate::GetInstance()->GetWindowHeight();

    CubismMatrix44 projection;

    LWallpaperModel* model = GetModel();

    if (model->GetModel()->GetCanvasWidth() > 1.0f && width < height)
    {
        // 横に長いモデルを縦長ウィンドウに表示する際モデルの横サイズでscaleを算出する
        model->GetModelMatrix()->SetWidth(2.0f);
        projection.Scale(scale, scale * static_cast<float>(width) / static_cast<float>(height));
    }
    else
    {
        projection.Scale(static_cast<float>(height) / static_cast<float>(width) * scale, scale);
    }

    // 必要があればここで乗算 - Note. the matrix was never modified, prob unused.
//    if (_viewMatrix)
//    {
//        projection.MultiplyByMatrix(_viewMatrix);
//    }

    // モデル1体描画前コール
    LWallpaperDelegate::GetInstance()->GetView()->PreModelDraw(*model);

    model->Update();
    model->Draw(projection);///< 参照渡しなのでprojectionは変質する

    // モデル1体描画前コール
    LWallpaperDelegate::GetInstance()->GetView()->PostModelDraw(*model);
}

void LWallpaperLive2DManager::SetAssetDirectory(const std::string &path)
{
    _currentModelDirectory = path;
}

void LWallpaperLive2DManager::LoadModel(std::string modelDirectoryName)
{
    // unused?
//    if (!_viewMatrix) {
//        _viewMatrix = new CubismMatrix44();
//    }

    // モデルのディレクトリを指定
    SetAssetDirectory(LWallpaperDefine::ResourcesPath + modelDirectoryName + "/");
    CubismLogDebug("030-loadmodel - addr %p", this);
    CubismLogInfo("030 - LoadModel() - %s | %s | isExternal: %d", _currentModelDirectory.c_str(), modelDirectoryName.c_str(), externalModel);

    // モデルデータの新規生成
    _model = new LWallpaperModel(modelDirectoryName, _currentModelDirectory, externalModel);

    // モデルデータの読み込み及び生成とセットアップを行う
    static_cast<LWallpaperModel*>(_model)->SetupModel();
    _model->StartRandomMotionWithOption(MotionGroupIdle, 2, nullptr);
    ready = true;
    CubismLogDebug("030 - LoadModel finished");
}

void LWallpaperLive2DManager::SetGravitationalAccelerationX(float gravity)
{
    this->gravity = gravity;
}

void LWallpaperLive2DManager::SetGravitationalAcceleration(float x, float y)
{
    if(_model)
    {
        _model->SetGravitationalAcceleration(x, y);
    }
}

void LWallpaperLive2DManager::SetParams(bool loop_idle, bool bg, bool custom_bg, bool def_touch_interact, bool no_reset, float target_scale, float x_offset, float y_offset) {
    paramsSet = true;
    loopIdle = loop_idle;
    useBg = bg;
    customBg = custom_bg;
    defTouchInteract = def_touch_interact;
    noReset = no_reset;
    scale = target_scale;
    xOffset = x_offset;
    yOffset = y_offset;
}


