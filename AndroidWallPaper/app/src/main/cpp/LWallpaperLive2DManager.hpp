/**
 * Copyright(c) Live2D Inc. All rights reserved.
 *
 * Use of this source code is governed by the Live2D Open Software license
 * that can be found at https://www.live2d.com/eula/live2d-open-software-license-agreement_en.html.
 */

#pragma once

#include <CubismFramework.hpp>
#include <Math/CubismMatrix44.hpp>
#include <Type/csmVector.hpp>
#include <string>

#define CUSTOM_MODEL "custom_models"

class LWallpaperModel;

/**
* @brief サンプルアプリケーションにおいてCubismModelを管理するクラス<br>
*         モデル生成と破棄、タップイベントの処理、モデル切り替えを行う。
*
*/
class LWallpaperLive2DManager
{

public:
    /**
    * @brief   クラスのインスタンス（シングルトン）を返す。<br>
    *           インスタンスが生成されていない場合は内部でインスタンを生成する。
    *
    * @return  クラスのインスタンス
    */
    static LWallpaperLive2DManager* GetInstance(char* path = nullptr);

    /**
    * @brief   クラスのインスタンス（シングルトン）を解放する。
    *
    */
    static void ReleaseInstance();

    /**
    * @brief   現在のシーンで保持しているモデルを返す
    *
    * @return      モデルのインスタンスを返す。
    */
    LWallpaperModel* GetModel() const;

    /**
    * @brief   現在のシーンで保持しているすべてのモデルを解放する
    *
    */
    void ReleaseModel();

    /**
    * @brief   画面をドラッグしたときの処理
    *
    * @param[in]   x   画面のX座標
    * @param[in]   y   画面のY座標
    */
    void OnDrag(Csm::csmFloat32 x, Csm::csmFloat32 y) const;

    /**
    * @brief   画面をタップしたときの処理
    *
    * @param[in]   x   画面のX座標
    * @param[in]   y   画面のY座標
    */
    void OnTap(Csm::csmFloat32 x, Csm::csmFloat32 y);

    /**
    * @brief   画面を更新するときの処理
    *          モデルの更新処理および描画処理を行う
    */
    void OnUpdate() const;

    /**
     * @brief 重力加速度の値の設定
     * @param[in]   gravity   重力加速度(-9.81~9.81)
     */
    void SetGravitationalAccelerationX(float gravity);

    void SetGravitationalAcceleration(float x, float y);

    void init();

    void SetParams(bool loop_idle, bool bg, bool custom_bg, bool touch_interact, bool no_reset, float scale, float x_offset, float y_offset);

    bool isReady() { return ready; }

    Csm::csmChar* _modelDirectoryName = new Csm::csmChar[128];
    bool paramsSet = false;
    bool loopIdle = false;
//    bool useBg = false;
    bool useBg = true;
    bool customBg = false;
    bool externalModel = false;
    bool defTouchInteract = true;
    bool noReset = false;
    float scale = 1.0f;
    float gravity = 0.0f;
    float xOffset = 0.0f;
    float yOffset = 0.5f;
private:
    /**
    * @brief  コンストラクタ
    */
    LWallpaperLive2DManager(char* path = nullptr);

    /**
    * @brief  デストラクタ
    */
    virtual ~LWallpaperLive2DManager();

    /**
    * @brief ディレクトリパスの設定
    *
    * モデルのディレクトリパスを設定する
    */
    void SetAssetDirectory(const std::string& path);

    /**
    * @brief モデルの読み込み
    *
    * モデルデータの読み込み処理を行う
    *
    * @param[in] modelDirectory モデルのディレクトリ名
    */
    void LoadModel(std::string modelDirectoryName);

    Csm::CubismMatrix44*        _viewMatrix; ///< モデル描画に用いるView行列
    LWallpaperModel*  _model; ///< モデルインスタンス

    /**
    *@brief モデルデータのディレクトリ名
    * このディレクトリ名と同名の.model3.jsonを読み込む
    */
    std::string _currentModelDirectory; ///< 現在のモデルのディレクトリ

    bool ready;
};
