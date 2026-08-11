# Vespera

Vespera 是 SPlayer 的 Compose Multiplatform 重写版，采用 Material 3，目标平台为 Android、Linux Desktop 和 iOS/iPadOS。代码和状态模型共享，平台层只负责窗口与网络/音频实现。

本项目参考 [SPlayer-Dev/SPlayer](https://github.com/SPlayer-Dev/SPlayer) 的功能范围，并兼容 [api-enhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced) 接口；歌词展示参考 [applemusic-like-lyrics](https://github.com/amll-dev/applemusic-like-lyrics)。本项目以 AGPL-3.0-only 发布。

## 功能

- 网易云音乐搜索、歌单、每日歌曲、云盘、历史和二维码登录（`MusicApi` 可替换为 api-enhanced 服务）
- 播放队列、播放/暂停、音量、循环模式、上一首/下一首、AB Loop、变速、均衡器和自动关闭状态机
- LRC 逐行同步歌词模型，支持 Apple Music-like lyrics 渲染入口
- 评论列表、点赞计数、二维码登录、下载、歌单资料库和 Material 3 自适应导航
- Desktop AppImage/Deb/RPM 与 Android APK；macOS CI 生成 iOS/iPadOS unsigned IPA 包

## UI 结构

```text
+--------------------------------------------------+
| Vespera                                  [gear] |
+--------------------------------------------------+
| Home / Search / Library / Comments / Settings    |
|                                                  |
|  content                                         |
|                                                  |
|  [cover] Song title       [pause] [repeat]       |
+--------------------------------------------------+
```

## 构建

需要 JDK 17。执行 `./gradlew test`、`./gradlew assembleRelease`，Linux 使用 `./gradlew packageReleaseDistributionForCurrentOS`。推送 `v1.0.0` 标签后，`.github/workflows/release.yml` 会上传 Linux、pacman 包、APK 和 unsigned IPA 产物。iOS 真机发布仍需 Apple Developer 签名证书和 provisioning profile；unsigned IPA 不能直接当作 App Store 签名包。
