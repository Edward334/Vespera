# Vespera

Vespera 是正在开发中的 SPlayer Compose Multiplatform 重写版，采用 Material 3，目标平台为 Android、Linux Desktop 和 iOS/iPadOS。

本项目以 [SPlayer-Dev/SPlayer](https://github.com/SPlayer-Dev/SPlayer) `dev` 分支的功能范围为迁移基准，网易云协议实现参考 [api-enhanced](https://github.com/neteasecloudmusicapienhanced/api-enhanced)，歌词交互参考 [applemusic-like-lyrics](https://github.com/amll-dev/applemusic-like-lyrics)。本项目以 AGPL-3.0-only 发布。

> 当前版本尚未达到完整复现或正式发布标准。逐项进度、原版证据和验证门槛见 [SPlayer 功能对等矩阵](docs/FEATURE_PARITY.md)。没有实现或只有界面占位的功能不会标记为完成。

## 功能

- 应用内直连网易云接口，不要求用户部署额外服务；已接入搜索、基础歌单、每日歌曲、云盘和登录会话
- 播放队列、播放/暂停、音量、循环模式、上一首/下一首、AB Loop、变速、均衡器和自动关闭状态机
- LRC/YRC 解析、逐字填充、自动滚动、翻译与罗马音基础渲染
- 评论列表、点赞计数、二维码登录、下载、歌单资料库和 Material 3 自适应导航
- Android、Linux Desktop、iOS/iPadOS 工程与发布流水线（尚未全部验证通过）

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

需要 JDK 17。执行 `./gradlew desktopTest`、`./gradlew assembleRelease`，Linux 使用 `./gradlew createDistributable`。只有功能矩阵通过后才会创建正式 Release。iOS 真机发布仍需 Apple Developer 签名证书和 provisioning profile；unsigned IPA 不能直接当作 App Store 签名包。
