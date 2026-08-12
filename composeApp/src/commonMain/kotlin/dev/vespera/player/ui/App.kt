@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package dev.vespera.player.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.vespera.player.data.*
import dev.vespera.player.download.DownloadManager
import dev.vespera.player.data.HistoryRepository
import dev.vespera.player.lyrics.*
import dev.vespera.player.model.*
import dev.vespera.player.player.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import io.github.alexzhirkevich.qrose.rememberQrCodePainter

private val navigationItems = AppDestination.entries

@Composable
fun VesperaApp(
    api: MusicApi,
    player: PlayerController = remember { PlayerController(PlatformAudioEngine()) },
) {
    var destination by remember { mutableStateOf(AppDestination.HOME) }
    var nowPlaying by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf(emptyList<Song>()) }
    val history = remember { HistoryRepository() }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val playbackFeatures = remember { PlaybackFeatures(scope) }
    val playSong: (Song) -> Unit = { song ->
        scope.launch {
            runCatching { requireNotNull(api.streamUrl(song.id)) { "当前账号或音源无法播放这首歌曲" } }
                .onSuccess { val resolved = song.copy(streamUrl = it); player.play(resolved); history.record(resolved) }
                .onFailure { error = it.message ?: "无法获取音频地址" }
        }
    }
    val downloadSong: (Song) -> Unit = { song -> scope.launch { runCatching { DownloadManager(api).download(song) }.onSuccess { error = "已保存到 $it" }.onFailure { error = it.message ?: "下载失败" } } }

    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF6750A4), secondary = Color(0xFF006C4C))) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val expanded = maxWidth >= 720.dp
            Row(Modifier.fillMaxSize()) {
                if (expanded) AppNavigationRail(destination) { destination = it; nowPlaying = false }
                Scaffold(
                    modifier = Modifier.weight(1f),
                    topBar = { AppTopBar { destination = AppDestination.SETTINGS; nowPlaying = false } },
                    bottomBar = {
                        Column {
                            PlayerBar(player, onExpand = { nowPlaying = true })
                            if (!expanded) AppNavigationBar(destination) { destination = it; nowPlaying = false }
                        }
                    },
                    snackbarHost = { error?.let { message -> Snackbar { Text(message) }; LaunchedEffect(message) { error = null } } },
                ) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding)) {
                        if (nowPlaying) {
                            NowPlayingScreen(api, player, onClose = { nowPlaying = false })
                        } else when (destination) {
                            AppDestination.HOME -> HomeScreen(songs, playSong, downloadSong) {
                                scope.launch { runCatching { api.dailySongs() }.onSuccess { songs = it }.onFailure { error = it.message } }
                            }
                            AppDestination.SEARCH -> SearchScreen(query, songs, { value ->
                                query = value
                                scope.launch { runCatching { api.search(value) }.onSuccess { songs = it }.onFailure { error = it.message } }
                            }, playSong, downloadSong)
                            AppDestination.LIBRARY -> LibraryScreen(api, history.songs.collectAsState().value, playSong, downloadSong, onError = { error = it })
                            AppDestination.COMMENTS -> CommentsScreen(api, player.state.collectAsState().value.current, onError = { error = it })
                            AppDestination.SETTINGS -> SettingsScreen(api, playbackFeatures)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTopBar(onSettings: () -> Unit) = CenterAlignedTopAppBar(
    title = { Text("Vespera", fontWeight = FontWeight.SemiBold) },
    actions = { IconButton(onSettings) { Icon(Icons.Default.Settings, "设置") } },
)

@Composable
private fun AppNavigationBar(selected: AppDestination, onSelect: (AppDestination) -> Unit) = NavigationBar {
    navigationItems.forEach { item ->
        NavigationBarItem(selected == item, { onSelect(item) }, { Icon(item.icon(), item.label) }, label = { Text(item.label) })
    }
}

@Composable
private fun AppNavigationRail(selected: AppDestination, onSelect: (AppDestination) -> Unit) = NavigationRail {
    Spacer(Modifier.height(12.dp))
    navigationItems.forEach { item ->
        NavigationRailItem(selected == item, { onSelect(item) }, { Icon(item.icon(), item.label) }, label = { Text(item.label) })
    }
}

private fun AppDestination.icon() = when (this) {
    AppDestination.HOME -> Icons.Default.Home
    AppDestination.SEARCH -> Icons.Default.Search
    AppDestination.LIBRARY -> Icons.Default.LibraryMusic
    AppDestination.COMMENTS -> Icons.Default.Forum
    AppDestination.SETTINGS -> Icons.Default.Settings
}

private fun RepeatMode.label() = when (this) { RepeatMode.OFF -> "不循环"; RepeatMode.ALL -> "列表循环"; RepeatMode.ONE -> "单曲循环" }
private fun LoginStatus.label() = when (this) { LoginStatus.WAITING -> "等待扫码"; LoginStatus.SCANNED -> "已扫码，请确认"; LoginStatus.AUTHORIZED -> "登录成功"; LoginStatus.EXPIRED -> "二维码已过期"; LoginStatus.ERROR -> "登录失败" }

@Composable
private fun HomeScreen(songs: List<Song>, onPlay: (Song) -> Unit, onDownload: (Song) -> Unit, onLoad: () -> Unit) {
    LaunchedEffect(Unit) { onLoad() }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("晚上好", style = MaterialTheme.typography.headlineMedium) }
        item { Text("随心推荐", style = MaterialTheme.typography.titleLarge) }
        items(songs, key = Song::id) { SongRow(it, onPlay, onDownload) }
    }
}

@Composable
private fun SearchScreen(query: String, songs: List<Song>, onQuery: (String) -> Unit, onPlay: (Song) -> Unit, onDownload: (Song) -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        OutlinedTextField(query, onQuery, Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, placeholder = { Text("搜索歌曲、歌手和歌单") })
        Spacer(Modifier.height(16.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(songs, key = Song::id) { SongRow(it, onPlay, onDownload) } }
    }
}

@Composable
private fun LibraryScreen(api: MusicApi, history: List<Song>, onPlay: (Song) -> Unit, onDownload: (Song) -> Unit, onError: (String) -> Unit) {
    var playlists by remember { mutableStateOf(emptyList<Playlist>()) }
    var cloud by remember { mutableStateOf(emptyList<Song>()) }
    var local by remember { mutableStateOf(emptyList<Song>()) }
    var tab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(api) { runCatching { api.playlists() }.onSuccess { playlists = it }.onFailure { onError(it.message.orEmpty()) } }
    Column(Modifier.fillMaxSize()) {
        Text("我的音乐库", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(20.dp))
        PrimaryTabRow(tab) { listOf("歌单", "云盘", "历史", "本地").forEachIndexed { index, label -> Tab(tab == index, { tab = index; when (index) { 1 -> scope.launch { runCatching { cloud = api.cloudSongs() }.onFailure { onError(it.message.orEmpty()) } }; 3 -> local = LocalMusicScanner.scan() } }, text = { Text(label) }) } }
        when (tab) {
            0 -> LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { items(playlists, key = Playlist::id) { ListItem(headlineContent = { Text(it.name) }, supportingContent = { Text("${it.trackCount} 首歌曲") }, leadingContent = { Icon(Icons.Default.QueueMusic, null) }) } }
            1 -> SongList(cloud, onPlay, onDownload)
            2 -> SongList(history, onPlay, onDownload)
            else -> SongList(local, onPlay, onDownload)
        }
    }
}

@Composable private fun SongList(songs: List<Song>, onPlay: (Song) -> Unit, onDownload: (Song) -> Unit) = LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(songs, key = Song::id) { SongRow(it, onPlay, onDownload) } }
@Composable private fun EmptyFeature(title: String, message: String) { Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.FolderOpen, null, Modifier.size(48.dp)); Spacer(Modifier.height(12.dp)); Text(title, style = MaterialTheme.typography.titleLarge); Text(message, style = MaterialTheme.typography.bodyMedium) } }

@Composable
private fun CommentsScreen(api: MusicApi, song: Song?, onError: (String) -> Unit) {
    var comments by remember { mutableStateOf(emptyList<Comment>()) }
    var videos by remember { mutableStateOf(emptyList<Video>()) }
    LaunchedEffect(song?.id) { if (song != null) runCatching { api.comments(song.id) }.onSuccess { comments = it }.onFailure { onError(it.message.orEmpty()) } }
    CommentList(song, comments)
}

@Composable
private fun CommentList(song: Song?, comments: List<Comment>, modifier: Modifier = Modifier) = LazyColumn(modifier, contentPadding = PaddingValues(20.dp)) {
    item { Text("评论", style = MaterialTheme.typography.headlineMedium); Text(song?.name ?: "选择歌曲后查看评论", style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.height(12.dp)) }
    items(comments, key = Comment::id) { ListItem(headlineContent = { Text(it.user) }, supportingContent = { Text(it.content) }, trailingContent = { Text("${it.likedCount} 赞") }) }
}

@Composable
private fun SettingsScreen(api: MusicApi, playback: PlaybackFeatures) {
    var crossfade by remember { mutableStateOf(false) }
    var lyricTranslation by remember { mutableStateOf(true) }
    var dynamicColor by remember { mutableStateOf(true) }
    var autoCloseMinutes by remember { mutableFloatStateOf(0f) }
    val equalizer by playback.equalizer.collectAsState()
    LazyColumn(contentPadding = PaddingValues(20.dp)) {
        item { Text("设置", style = MaterialTheme.typography.headlineMedium) }
        item { SettingsSwitch("淡入淡出", "在相邻歌曲间平滑过渡", crossfade) { crossfade = it } }
        item { SettingsSwitch("歌词翻译", "在同步歌词下显示翻译", lyricTranslation) { lyricTranslation = it } }
        item { SettingsSwitch("动态配色", "从专辑封面提取界面颜色", dynamicColor) { dynamicColor = it } }
        item { Text("自动关闭：${autoCloseMinutes.toInt()} 分钟", style = MaterialTheme.typography.bodyLarge); Slider(autoCloseMinutes, { autoCloseMinutes = it; if (it > 0) playback.setAutoClose(it.toInt()) {} else playback.cancelAutoClose() }, valueRange = 0f..120f, steps = 11) }
        item { SettingsSwitch("均衡器", "使用 10 段频段调节音色", equalizer.enabled) { playback.setEqualizerEnabled(it) } }
        items(equalizer.bands.size) { index -> Slider(equalizer.bands[index], { playback.setBand(index, it) }, Modifier.fillMaxWidth(), valueRange = -12f..12f) }
        item { HorizontalDivider(); Spacer(Modifier.height(16.dp)); LoginPanel(api) }
        item { ListItem(headlineContent = { Text("均衡器") }, supportingContent = { Text("10 段均衡器与预设") }, leadingContent = { Icon(Icons.Default.Equalizer, null) }) }
        item { ListItem(headlineContent = { Text("下载") }, supportingContent = { Text("音质、保存目录与并发任务") }, leadingContent = { Icon(Icons.Default.Download, null) }) }
        item { ListItem(headlineContent = { Text("关于 Vespera") }, supportingContent = { Text("开发版本 0.2.0") }) }
    }
}

@Composable
private fun LoginPanel(api: MusicApi) {
    var qr by remember(api) { mutableStateOf<LoginQr?>(null) }
    var status by remember(api) { mutableStateOf("尚未登录") }
    var loading by remember(api) { mutableStateOf(false) }
    var method by remember { mutableIntStateOf(0) }
    var countryCode by remember { mutableStateOf("86") }
    var phone by remember { mutableStateOf("") }
    var captcha by remember { mutableStateOf("") }
    val session by api.session.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(api) { api.refreshAccount() }
    LaunchedEffect(qr?.key) {
        val key = qr?.key ?: return@LaunchedEffect
        while (true) {
            delay(1500)
            val attempt = runCatching { api.checkLoginQr(key) }
            if (attempt.isFailure) {
                status = attempt.exceptionOrNull()?.message.orEmpty()
                return@LaunchedEffect
            }
            val result = attempt.getOrThrow()
            status = result.message.ifBlank { result.status.label() }
            if (result.status in setOf(LoginStatus.AUTHORIZED, LoginStatus.EXPIRED, LoginStatus.ERROR)) break
        }
    }

    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.Start) {
        Text("网易云账号", style = MaterialTheme.typography.titleMedium)
        if (session.loggedIn) {
            val profile = requireNotNull(session.profile)
            ListItem(
                headlineContent = { Text(profile.nickname) },
                supportingContent = { Text("网易云 ID ${profile.id}") },
                leadingContent = { profile.avatarUrl?.let { AsyncImage(it, null, Modifier.size(48.dp)) } ?: Icon(Icons.Default.AccountCircle, null) },
                trailingContent = { OutlinedButton(onClick = { scope.launch { api.logout() } }) { Text("退出") } },
            )
            return@Column
        }
        SecondaryTabRow(method) {
            Tab(method == 0, { method = 0 }, text = { Text("扫码登录") })
            Tab(method == 1, { method = 1 }, text = { Text("手机号登录") })
        }
        Spacer(Modifier.height(12.dp))
        if (method == 0) {
            Text(status, style = MaterialTheme.typography.bodyMedium)
            qr?.payload?.let { payload ->
                Image(rememberQrCodePainter(payload), "网易云登录二维码", Modifier.size(220.dp).align(Alignment.CenterHorizontally))
            }
            Button(
                enabled = !loading,
                onClick = {
                    scope.launch {
                        loading = true
                        runCatching { api.createLoginQr() }
                            .onSuccess { qr = it; status = "请使用网易云音乐扫码，应用会自动确认" }
                            .onFailure { status = it.message.orEmpty() }
                        loading = false
                    }
                },
            ) { Icon(Icons.Default.QrCode2, null); Spacer(Modifier.width(8.dp)); Text(if (qr == null) "生成二维码" else "刷新二维码") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(countryCode, { countryCode = it.filter(Char::isDigit) }, Modifier.width(96.dp), label = { Text("区号") }, singleLine = true)
                OutlinedTextField(phone, { phone = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("手机号") }, singleLine = true)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(captcha, { captcha = it.filter(Char::isDigit) }, Modifier.weight(1f), label = { Text("验证码") }, singleLine = true)
                OutlinedButton(
                    enabled = phone.isNotBlank() && !loading,
                    onClick = { scope.launch { loading = true; status = runCatching { if (api.sendPhoneCaptcha(phone, countryCode)) "验证码已发送" else "验证码发送失败" }.getOrElse { it.message.orEmpty() }; loading = false } },
                ) { Text("获取验证码") }
            }
            Button(
                enabled = phone.isNotBlank() && captcha.isNotBlank() && !loading,
                onClick = { scope.launch { loading = true; status = runCatching { api.loginWithPhone(phone, captcha, countryCode); "登录成功" }.getOrElse { it.message.orEmpty() }; loading = false } },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("登录") }
        }
    }
}

@Composable
private fun SettingsSwitch(title: String, summary: String, checked: Boolean, onChecked: (Boolean) -> Unit) = ListItem(
    headlineContent = { Text(title) }, supportingContent = { Text(summary) }, trailingContent = { Switch(checked, onChecked) },
)

@Composable
private fun SongRow(song: Song, onPlay: (Song) -> Unit, onDownload: ((Song) -> Unit)? = null) = ListItem(
    modifier = Modifier.clickable { onPlay(song) },
    headlineContent = { Text(song.name) },
    supportingContent = { Text(song.artists.joinToString(" · ")) },
    trailingContent = { Row { onDownload?.let { action -> IconButton({ action(song) }) { Icon(Icons.Default.Download, "下载") } }; IconButton({ onPlay(song) }) { Icon(Icons.Default.PlayArrow, "播放") } } },
    leadingContent = { if (song.coverUrl != null) AsyncImage(song.coverUrl, null, Modifier.size(48.dp)) else Icon(Icons.Default.Album, null) },
)

@Composable
private fun PlayerBar(player: PlayerController, onExpand: () -> Unit) {
    val state by player.state.collectAsState()
    val current = state.current ?: return
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand)) {
        Row(Modifier.heightIn(min = 64.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(current.name, fontWeight = FontWeight.Medium); Text(current.artists.joinToString(" · "), style = MaterialTheme.typography.bodySmall) }
            IconButton(player::toggle) { Icon(if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow, "播放或暂停") }
            IconButton(player::cycleRepeat) { Icon(Icons.Default.Repeat, state.repeat.label()) }
        }
    }
}

@Composable
private fun NowPlayingScreen(api: MusicApi, player: PlayerController, onClose: () -> Unit) {
    val state by player.state.collectAsState()
    val song = state.current ?: return
    var tab by remember { mutableIntStateOf(0) }
    var lyric by remember { mutableStateOf(LyricBundle()) }
    var comments by remember { mutableStateOf(emptyList<Comment>()) }
    var videos by remember { mutableStateOf(emptyList<Video>()) }
    var rate by remember { mutableFloatStateOf(1f) }
    var abStage by remember { mutableIntStateOf(0) }
    val abLoop = remember { AbLoopController() }
    val abState by abLoop.state.collectAsState()
    LaunchedEffect(song.id) {
        runCatching { lyric = api.lyrics(song.id) }
        runCatching { comments = api.comments(song.id) }
        if (song.mvId > 0) runCatching { videos = api.videos(song.mvId) }
    }
    LaunchedEffect(state.playing, song.id) { while (state.playing) { delay(250); player.syncPosition() } }
    LaunchedEffect(state.positionMs, abState) { abLoop.nextPosition(state.positionMs)?.let(player::seek) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) { IconButton(onClose) { Icon(Icons.Default.KeyboardArrowDown, "收起播放页") }; Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { Text(song.name, fontWeight = FontWeight.SemiBold); Text(song.artists.joinToString(" · "), style = MaterialTheme.typography.bodySmall) }; Spacer(Modifier.width(48.dp)) }
        PrimaryTabRow(tab) { listOf("歌词", "播放队列", "评论", "视频").forEachIndexed { index, label -> Tab(tab == index, { tab = index }, text = { Text(label) }) } }
        when (tab) {
            0 -> LyricsView(lyric, state.positionMs, { player.seek(it) }, Modifier.fillMaxSize().padding(horizontal = 24.dp))
            1 -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp)) { items(state.queue, key = Song::id) { SongRow(it, player::play) } }
            2 -> CommentList(song, comments, Modifier.fillMaxSize())
            else -> VideoList(videos)
        }
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Slider(state.positionMs.toFloat(), { player.seek(it.toLong()) }, valueRange = 0f..song.durationMs.coerceAtLeast(1).toFloat())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = { rate = if (rate >= 2f) 0.5f else rate + 0.25f; player.setRate(rate) }, label = { Text("速度 ${rate}x") })
                AssistChip(onClick = { when (abStage) { 0 -> { abLoop.setStart(state.positionMs); abStage = 1 }; 1 -> { abLoop.setEnd(state.positionMs); abStage = 2 }; else -> { abLoop.clear(); abStage = 0 } } }, label = { Text(when (abStage) { 0 -> "设置 A 点"; 1 -> "设置 B 点"; else -> "AB 循环中" }) })
                if (abStage == 2) TextButton({ abLoop.clear(); abStage = 0 }) { Text("清除 AB") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { IconButton(player::cycleRepeat) { Icon(Icons.Default.Repeat, state.repeat.label()) }; IconButton(player::previous) { Icon(Icons.Default.SkipPrevious, "上一首") }; IconButton(player::toggle, Modifier.size(64.dp)) { Icon(if (state.playing) Icons.Default.PauseCircle else Icons.Default.PlayCircle, "播放或暂停", Modifier.size(48.dp)) }; IconButton(player::next) { Icon(Icons.Default.SkipNext, "下一首") } }
        }
    }
}

@Composable private fun VideoList(videos: List<Video>) = if (videos.isEmpty()) EmptyFeature("暂无视频", "这首歌曲没有可用的音乐视频") else LazyColumn(contentPadding = PaddingValues(20.dp)) { items(videos, key = Video::id) { ListItem(headlineContent = { Text(it.title) }, supportingContent = { Text(it.url.orEmpty()) }, leadingContent = { Icon(Icons.Default.VideoLibrary, null) }) } }

@Composable
fun LyricsView(bundle: LyricBundle, positionMs: Long, onSeek: (Long) -> Unit, modifier: Modifier = Modifier) {
    val lines = remember(bundle) { LyricParser.parse(bundle) }
    val active = lines.activeIndex(positionMs)
    val listState = rememberLazyListState()
    LaunchedEffect(active) { if (active >= 0) listState.animateScrollToItem((active - 2).coerceAtLeast(0)) }
    LazyColumn(modifier, state = listState, contentPadding = PaddingValues(vertical = 120.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        items(lines.size) { index ->
            AppleLyricLine(lines[index], positionMs, index == active) { onSeek(lines[index].startMs) }
        }
    }
}

@Composable
private fun AppleLyricLine(line: LyricLine, positionMs: Long, active: Boolean, onClick: () -> Unit) {
    val scale by androidx.compose.animation.core.animateFloatAsState(if (active) 1.04f else 1f)
    Column(
        Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (active) 1f else 0.55f }.clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            line.words.forEach { word -> AppleLyricWord(word, positionMs, active) }
        }
        line.translation?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (active) 0.82f else 0.55f)) }
        line.romanization?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (active) 0.72f else 0.45f)) }
    }
}

@Composable
private fun AppleLyricWord(word: LyricWord, positionMs: Long, active: Boolean) {
    val progress = when {
        positionMs <= word.startMs -> 0f
        positionMs >= word.endMs -> 1f
        else -> (positionMs - word.startMs).toFloat() / (word.endMs - word.startMs).coerceAtLeast(1)
    }
    val style = if (active) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium
    Box {
        Text(word.text, style = style, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            word.text,
            style = style,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.drawWithContent {
                clipRect(right = size.width * progress) { this@drawWithContent.drawContent() }
            },
        )
    }
}
