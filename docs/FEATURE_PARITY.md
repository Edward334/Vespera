# SPlayer 功能对等矩阵

本文档用于核对 Vespera 是否完整复现 SPlayer。基准为 `SPlayer-Dev/SPlayer` 的 `dev` 分支，审计日期为 2026-08-12。

状态定义：

- `完成`：存在真实实现，并有自动化测试或三端运行证据。
- `部分`：仅覆盖部分数据、交互或平台，不计入完整复现。
- `未实现`：没有实现，或只有静态界面/状态占位。

正式版发布门槛：下列必需项不得存在 `部分` 或 `未实现`，且 Android、Linux、iOS/iPadOS 构建与核心流程测试全部通过。

## 页面与内容

| 功能 | 原版证据 | 当前状态 | 验证要求 |
|---|---|---:|---|
| 在线首页、个性推荐 | `views/Home/HomeOnline.vue` | 部分 | 游客与登录用户均可加载、刷新、播放 |
| 本地首页 | `views/Home/HomeLocal.vue` | 未实现 | 离线状态完整可用 |
| 搜索默认页、热搜、历史、建议 | `components/Search/*` | 部分 | 已有热搜、进程内历史、建议与防抖；缺历史持久化和 UI 自动化测试 |
| 搜索歌曲 | `views/Search/songs.vue` | 部分 | 已有真实结果、分页、播放与下载；缺筛选和完整菜单 |
| 搜索歌单、歌手、专辑、视频、播客 | `views/Search/*` | 部分 | 六类接口解析与分页测试已通过，歌单详情已接通；其余详情和操作缺失 |
| 发现歌单、排行榜、歌手、新歌新碟 | `views/Discover/*` | 未实现 | 分类、分页与详情 |
| 歌手详情：歌曲、专辑、视频 | `views/Artist/*` | 未实现 | 收藏、分页、全部播放 |
| 专辑详情 | `views/List/album.vue` | 未实现 | 元数据、歌曲、收藏 |
| 歌单详情 | `views/List/playlist.vue` | 部分 | 已有元数据、完整曲目、详情导航、全部播放和收藏操作；缺排序与完整菜单，写操作待真号验证 |
| 歌曲百科、乐谱、回忆坐标 | `views/Song/wiki.vue` | 未实现 | 数据加载与异常态 |
| 每日推荐 | `views/DailySongs.vue` | 部分 | 需登录态和“不感兴趣” |
| 喜欢的歌曲 | `views/List/liked.vue` | 部分 | 列表、添加/取消红心与即时回写已实现并通过 Mock；缺持久缓存与真实账号验证 |
| 收藏：歌单、专辑、歌手、视频、播客 | `views/Like/*` | 部分 | 五类原生接口及列表/取消操作已实现；缺部分详情导航、分页和真实账号验证 |
| 云盘列表与播放 | `views/Cloud.vue` | 部分 | 列表可读；上传、删除、纠正缺失 |
| 云盘上传、删除、歌曲纠正 | `api/cloud.ts` | 未实现 | 上传进度、失败恢复、纠正与删除 |
| 播客分类、热门、详情与节目 | `views/Radio/*` | 未实现 | 分类、订阅、节目播放 |
| MV/视频详情与播放 | `views/Video.vue` | 未实现 | 视频地址、清晰度、全屏 |
| 评论列表 | `views/Comment.vue` | 部分 | 推荐/热度/时间排序、热门评论、游标分页和楼中楼已实现并有真实匿名接口证据；缺 UI 视觉验证 |
| 评论点赞、抱一抱、回复 | `api/comment.ts` | 部分 | 原生接口、中文 MD3 交互和状态回写已实现并通过 Mock；待真实登录账号验证 |
| 最近播放 | `views/History.vue` | 部分 | 当前仅进程内歌曲列表，未持久化/分类 |
| 403/404/500 与空状态 | `views/Status/*` | 未实现 | 导航和错误恢复 |

## 账号与在线服务

| 功能 | 原版证据 | 当前状态 | 验证要求 |
|---|---|---:|---|
| 应用内直连网易云，不依赖外置 API | `electron/server/netease/*` | 部分 | 搜索已直连；全接口与异常策略待覆盖 |
| 二维码登录与自动轮询 | `Modal/Login/LoginQRCode.vue` | 部分 | 已实现代码与会话捕获，待真机扫码验证 |
| 手机号验证码登录 | `Modal/Login/LoginPhone.vue` | 部分 | 已实现接口与 UI，待真实账号验证 |
| Cookie/UID 登录兼容 | `Modal/Login/LoginCookie.vue`、`LoginUID.vue` | 未实现 | 导入、校验、脱敏与持久化 |
| 登录会话持久化与退出 | `stores/data.ts` | 部分 | 三端存储已接入，待重启验证 |
| 用户资料、等级、收藏计数 | `api/user.ts` | 部分 | 当前只有基础昵称/头像/ID |
| 播放记录上报 | `api/user.ts::scrobble` | 未实现 | 播放阈值与去重 |
| Last.fm 连接与 Scrobble | `api/lastfm.ts` | 未实现 | OAuth、Now Playing、Scrobble |

## 播放器

| 功能 | 原版证据 | 当前状态 | 验证要求 |
|---|---|---:|---|
| 播放、暂停、跳转、音量 | `core/player/*` | 部分 | 基础实现；缓冲、错误、完成事件缺失 |
| 上一首、下一首、播放队列 | `SongManager.ts` | 部分 | 歌单可替换队列、切歌按需解析音源；缺队列编辑、拖动、插播、移除 |
| 顺序、单曲循环、列表循环、随机 | `PlayModeManager.ts` | 部分 | 缺随机与完整结束行为 |
| 播放速度 | `Modal/ChangeRate.vue` | 部分 | 基础速度切换，未持久化 |
| AB 循环 | `Modal/ABLoop.vue` | 部分 | 基础状态机，待音频结束边界验证 |
| 自动关闭 | `Modal/AutoClose.vue` | 部分 | 基础计时器，无播完当前歌曲等模式 |
| 均衡器 | `Modal/Equalizer.vue` | 未实现 | 当前只有 UI 状态，没有作用于音频 |
| 淡入淡出 | `AudioEffectManager.ts` | 未实现 | 设置开关目前不影响音频 |
| Automix | `core/automix/*` | 未实现 | 分析、调度与过渡 |
| ReplayGain | `AudioEffectManager.ts` | 未实现 | Track/Album 增益 |
| 音质切换与预取 | `useQualityControl.ts` | 未实现 | 全音质、降级与下一曲预取 |
| Personal FM | `PlayerComponents/PersonalFM.vue` | 未实现 | FM、垃圾桶、下一首 |
| 媒体会话、系统快捷键 | `MediaSessionManager.ts` | 未实现 | Android/iOS/Linux 系统控制 |
| 防休眠、任务栏进度、托盘 | `electron/main/*` | 未实现 | 平台能力验证 |
| 音乐频谱 | `PlayerSpectrum.vue` | 未实现 | 实时 FFT 与性能测试 |
| 动态封面 | `PlayerCover.vue` | 未实现 | 支持歌曲与静态回退 |

## 歌词

| 功能 | 原版证据 | 当前状态 | 验证要求 |
|---|---|---:|---|
| 普通 LRC 解析与同步 | `utils/lyric/parseLrc.ts` | 完成 | 单元测试通过 |
| YRC 逐字歌词 | `utils/lyric/lyricParser.ts` | 部分 | 已解析逐字时间；需真实数据与边界样本 |
| 逐字填充、行缩放、自动滚动 | `components/AMLL/LyricPlayer.vue` | 部分 | 已有 Compose 动效；需截图与帧稳定性验证 |
| 翻译与罗马音 | `PlayerLyric/*` | 部分 | 已合并显示；缺交换、字号与开关设置 |
| TTML 歌词 | `parseTTML.ts` | 未实现 | 解析背景歌词、对唱、逐字时间 |
| QQ 音乐歌词回退 | `parseQrc.ts` | 未实现 | 匹配与回退优先级 |
| 背景歌词、对唱布局 | `AMLyric.vue` | 未实现 | 左右声部与背景行 |
| 歌词点击跳转 | `PlayerLyric/*` | 完成 | Compose 行点击跳转已实现 |
| 歌词偏移、进度调词 | `Setting/config/lyric.ts` | 未实现 | 正负偏移与持久化 |
| 歌词复制、导出 LRC/YRC/ASS | `Modal/CopyLyrics.vue` | 未实现 | 编码、翻译/罗马音选项 |
| 桌面歌词 | `views/DesktopLyric/*` | 未实现 | Linux 窗口与移动端合理降级 |
| 任务栏歌词、macOS 状态栏歌词 | `windows/taskbar-lyric/*` | 未实现 | 平台支持或明确不可用说明 |

## 本地、下载与流媒体

| 功能 | 原版证据 | 当前状态 | 验证要求 |
|---|---|---:|---|
| 本地目录扫描 | `LocalMusicService.ts` | 部分 | 当前浅层扫描，无标签、递归和增量索引 |
| 歌曲/歌手/专辑/文件夹/歌单分类 | `views/Local/*` | 未实现 | 五种视图与检索排序 |
| 标签、封面与歌词读取 | `MusicMetadataService.ts` | 未实现 | 常见格式与编码测试 |
| 本地标签和封面编辑 | `SongInfoEditor.vue` | 未实现 | 安全写回与备份 |
| 本地歌单创建、编辑、排序 | `LocalMusicDB.ts` | 未实现 | 持久化与迁移 |
| 下载队列、并发、暂停/重试 | `DownloadService.ts` | 未实现 | 生命周期与失败恢复 |
| 歌曲、封面、歌词、元数据下载 | `Setting/config/local.ts` | 部分 | 当前仅单文件保存，选项均缺失 |
| 下载命名与目录策略 | `Setting/config/local.ts` | 未实现 | 冲突和非法字符处理 |
| Jellyfin | `api/streaming/jellyfin.ts` | 未实现 | 登录、浏览、搜索、播放、歌词 |
| Emby | `api/streaming/emby.ts` | 未实现 | 登录、浏览、搜索、播放、歌词 |
| Subsonic/Navidrome | `api/streaming/subsonic.ts` | 未实现 | 多服务器、自动连接与播放 |

## Material 3 与适配

| 功能 | 原版证据 | 当前状态 | 验证要求 |
|---|---|---:|---|
| Material 3 组件与语义色 | 用户要求 | 部分 | 已使用 MD3，仍是固定浅色与硬编码主题 |
| Light/Dark/Auto | `appearance.ts::themeMode` | 未实现 | 三端系统主题切换 |
| 动态取色与全站着色 | `utils/color.ts` | 未实现 | 封面取色、对比度与缓存 |
| 手机、平板、桌面自适应 | `useMobile.ts` | 部分 | 底栏/侧栏已有，详情页布局未完成 |
| 键盘、焦点、无障碍 | `keyboard.ts` | 未实现 | 快捷键、屏幕阅读器、触控目标 |
| 中文界面 | 用户要求 | 部分 | 当前主要文案为中文，错误与全部设置待统一 |

## 设置核对

原版设置由以下七组配置定义：

- `src/components/Setting/config/general.ts`
- `appearance.ts`
- `play.ts`
- `lyric.ts`
- `local.ts`
- `network.ts`
- `keyboard.ts`

审计共识别 150 余个设置键。当前仅有主题占位、翻译开关占位、自动关闭基础状态、均衡器 UI 状态等少量实现，因此设置整体状态为 `未实现`。后续每个设置项必须同时具备：可操作控件、持久化、实际行为、默认值迁移和适用平台说明；仅显示控件不算完成。

## 发布核对

| 交付项 | 当前状态 | 备注 |
|---|---:|---|
| Linux pacman 包 | 部分 | 本地应用目录可生成；CI 尚未全绿 |
| Android APK | 部分 | 本地 Release APK 构建成功；尚未完成真机流程测试 |
| iOS/iPadOS IPA | 未实现 | CI 因错误的跨平台依赖失败，正在修复；unsigned IPA 仍需后续签名 |
| GitHub 正式 Release | 未实现 | `v1.0.0` workflow 失败，没有可用 Release；正式版前不会宣称完成 |
