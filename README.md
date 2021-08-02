# Cubism Android Wallpaper Sample

Live2D Cubism 4 Editor で出力したモデルを表示する LiveWallpaper アプリケーションのサンプルです。

Cubism Native Framework および Live2D Cubism Core と組み合わせて使用します。

利用には事前に下記のダウンロードが必要です

* AndroidStudio
* Android SDK
* Android SDK Build-Tools
* Android NDK
* CMake


## ライセンス

本 SDK を使用する前に[ライセンス](LICENSE.md)をご確認ください。


## 注意事項

本 SDK を使用する前に [注意事項](NOTICE.md)をご確認ください。

## ディレクトリ構成

```
.
├─ Core             # Live2D Cubism Core が含まれるディレクトリ
├─ Framework        # レンダリングやアニメーション機能などのソースコードが含まれるディレクトリ
├─ thirdParty       # サードパーティ製のヘッダーが含まれるディレクトリ
└─ AndroidWallPaper # Android Studio プロジェクト
```


## Cubism Native Framework

モデルを表示、操作するための各種機能を提供します。

当リポジトリには Cubism Native Framework は同梱されていません。

ダウンロードするには[こちら](https://www.live2d.com/download/cubism-sdk/download-native/)のページを参照ください。
ダウンロードした Zip ファイルの中身を当リポジトリの `Framework` ディレクトリにコピーしてください。


## Live2D Cubism Core for Native

Cubism Native Frameworkを利用するために必要なライブラリです。

当リポジトリには Live2D Cubism Core for Native は同梱されていません。

ダウンロードするには[こちら](https://www.live2d.com/download/cubism-sdk/download-native/)のページを参照ください。
ダウンロードした Zip ファイルの中身を当リポジトリの `Core` ディレクトリにコピーしてください。


## SDKマニュアル

[Cubism SDK Manual](https://docs.live2d.com/cubism-sdk-manual/top/)


## 変更履歴

当リポジトリの変更履歴については [CHANGELOG.md](CHANGELOG.md) を参照ください。


## 開発環境

| 開発ツール | バージョン |
| --- | --- |
| Android Studio | 4.2.1 |
| CMake | 3.18.1 |

### Thirdparty

| サードパーティ | バージョン |
| -------------- | ---------- |
| [stb_image.h]  | 2.23       |
| [GLWallpaperService.java]  | 0.9.2       |

### Android

| Android SDK tools | バージョン |
| --- | --- |
| Android NDK | 22.1.7171670 |
| Android SDK | 30.0.3 |


## 動作確認環境

| プラットフォーム | バージョン |
| --- | --- |
| Windows 10 | 20H2 |

### Android

| バージョン | デバイス | Tegra |
| --- | --- | --- |
| 11 | Pixel 3a | |
| 7.1.1 | Nexus 9 | ✔︎ |