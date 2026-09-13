package com.aiko.games

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiko.games.data.model.KoiCard
import com.aiko.games.data.model.KoiYaku
import com.aiko.games.data.SfenBoard
import com.aiko.games.data.ServerConfig
import com.aiko.games.data.remote.GoApi
import com.aiko.games.data.remote.KoiKoiApi
import com.aiko.games.data.remote.ShogiApi
import com.aiko.games.ui.GoBoardView
import com.aiko.games.ui.GoUiState
import com.aiko.games.ui.GoViewModel
import com.aiko.games.ui.HanafudaBack
import com.aiko.games.ui.HanafudaCard
import com.aiko.games.ui.KoiKoiViewModel
import com.aiko.games.ui.KoiUiState
import com.aiko.games.ui.KomaImages
import com.aiko.games.ui.monthLabel
import com.aiko.games.ui.Selection
import com.aiko.games.ui.ShogiUiState
import com.aiko.games.ui.ShogiViewModel
import com.aiko.games.ui.theme.AikoGamesTheme
import com.aiko.games.ui.theme.BoardHint
import com.aiko.games.ui.theme.BoardLast
import com.aiko.games.ui.theme.BoardLine
import com.aiko.games.ui.theme.BoardSelect
import com.aiko.games.ui.theme.BoardWood
import com.aiko.games.ui.theme.CuteCoral
import com.aiko.games.ui.theme.CuteMint
import com.aiko.games.ui.theme.CuteSky
import com.aiko.games.ui.theme.PieceBlack
import com.aiko.games.ui.theme.PieceWhite
import com.aiko.games.ui.theme.PieceWhiteStroke
import com.aiko.games.ui.theme.ShoujoAccent
import com.aiko.games.ui.theme.ShoujoPalePink
import com.aiko.games.ui.theme.ShoujoPink
import com.aiko.games.ui.theme.ShoujoSoftPink
import com.aiko.games.ui.theme.ShoujoText
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

enum class Screen {
    Lobby,
    Preferences,
    ShogiRules,
    GoRules,
    KoikoiRules,
    GuideSelection,
    ShogiGame,
    GoGame,
    KoikoiGame,
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AikoGamesTheme {
                // Baked in at build time from AIKO_PUBLIC_BASE_URL
                // (see local.properties → BuildConfig). No in-app override.
                val baseUrl = remember { ServerConfig.get() }
                val retrofit = remember(baseUrl) { buildRetrofit(baseUrl) }
                val shogiApi = remember(retrofit) { retrofit.create(ShogiApi::class.java) }
                val goApi = remember(retrofit) { retrofit.create(GoApi::class.java) }
                val koiApi = remember(retrofit) { retrofit.create(KoiKoiApi::class.java) }

                val shogiVm: ShogiViewModel = viewModel(
                    key = "shogi-$baseUrl",
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return ShogiViewModel(shogiApi) as T
                        }
                    },
                )
                val goVm: GoViewModel = viewModel(
                    key = "go-$baseUrl",
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return GoViewModel(goApi) as T
                        }
                    },
                )
                val koiVm: KoiKoiViewModel = viewModel(
                    key = "koi-$baseUrl",
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return KoiKoiViewModel(koiApi) as T
                        }
                    },
                )

                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    GamesApp(
                        shogiVm = shogiVm,
                        goVm = goVm,
                        koiVm = koiVm,
                        baseUrl = baseUrl,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }

    private fun buildRetrofit(baseUrl: String): Retrofit {
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
}

@Composable
fun GamesApp(
    shogiVm: ShogiViewModel,
    goVm: GoViewModel,
    koiVm: KoiKoiViewModel,
    baseUrl: String,
    modifier: Modifier = Modifier,
) {
    val shogi by shogiVm.ui.collectAsState()
    val go by goVm.ui.collectAsState()
    val koi by koiVm.ui.collectAsState()
    var screen by remember { mutableStateOf(Screen.Lobby) }

    LaunchedEffect(baseUrl) {
        shogiVm.refreshEngine()
        shogiVm.warmupEngine()
        goVm.refreshEngine()
        goVm.warmupEngine()
        koiVm.refreshEngine()
        koiVm.warmupEngine()
    }

    // Auto-enter the game when a start succeeds. Manual Back keeps the
    // session alive in the VM so Lobby can offer Resume.
    LaunchedEffect(shogi.inGame) {
        if (shogi.inGame) screen = Screen.ShogiGame
        else if (screen == Screen.ShogiGame) screen = Screen.Lobby
    }
    LaunchedEffect(go.inGame) {
        if (go.inGame) screen = Screen.GoGame
        else if (screen == Screen.GoGame) screen = Screen.Lobby
    }
    LaunchedEffect(koi.inGame) {
        if (koi.inGame) screen = Screen.KoikoiGame
        else if (screen == Screen.KoikoiGame) screen = Screen.Lobby
    }

    shogi.promoteChoice?.let {
        AlertDialog(
            onDismissRequest = { shogiVm.dismissPromote() },
            title = { Text("Promote?") },
            text = { Text("This piece can promote (成). Choose promote or stay as-is.") },
            confirmButton = {
                Button(onClick = { shogiVm.confirmPromote(true) }) { Text("成 Promote") }
            },
            dismissButton = {
                OutlinedButton(onClick = { shogiVm.confirmPromote(false) }) { Text("不成 Stay") }
            },
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ShoujoSoftPink)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (screen) {
            Screen.Lobby -> Lobby(
                shogi = shogi,
                go = go,
                koi = koi,
                baseUrl = baseUrl,
                onStartShogi = { shogiVm.startGame() },
                onResumeShogi = { screen = Screen.ShogiGame },
                onStartGo = { goVm.startGame() },
                onResumeGo = { screen = Screen.GoGame },
                onStartKoi = { koiVm.startGame() },
                onResumeKoi = { screen = Screen.KoikoiGame },
                onGuides = { screen = Screen.GuideSelection },
                onPreferences = { screen = Screen.Preferences },
            )
            Screen.Preferences -> PreferencesScreen(
                shogi = shogi,
                go = go,
                koi = koi,
                baseUrl = baseUrl,
                onShogiDifficulty = { shogiVm.setDifficulty(it) },
                onGoDifficulty = { goVm.setDifficulty(it) },
                onKoiDifficulty = { koiVm.setDifficulty(it) },
                onShogiSide = { shogiVm.setSide(it) },
                onGoSide = { goVm.setSide(it) },
                onGoSize = { goVm.setBoardSize(it) },
                onToggleGoHints = { goVm.toggleHints() },
                onKoiMonths = { koiVm.setMonths(it) },
                onBack = { screen = Screen.Lobby },
            )
            Screen.ShogiRules -> ShogiRulesScreen(onBack = { screen = Screen.GuideSelection })
            Screen.GoRules -> GoRulesScreen(onBack = { screen = Screen.GuideSelection })
            Screen.KoikoiRules -> KoikoiRulesScreen(onBack = { screen = Screen.GuideSelection })
            Screen.GuideSelection -> GuideSelectionScreen(
                onShogi = { screen = Screen.ShogiRules },
                onGo = { screen = Screen.GoRules },
                onKoi = { screen = Screen.KoikoiRules },
                onBack = { screen = Screen.Lobby }
            )
            Screen.ShogiGame -> ShogiGameBoard(
                state = shogi,
                hints = shogiVm.hintSquares(),
                last = shogiVm.lastMoveSquares(),
                onSquare = { r, c -> shogiVm.onSquareTap(r, c) },
                onHandPiece = { shogiVm.onHandPieceTap(it) },
                onResign = { shogiVm.resign() },
                onBackToLobby = { screen = Screen.Lobby },
            )
            Screen.GoGame -> GoGameBoard(
                state = go,
                hints = goVm.legalHintSquares(),
                last = goVm.lastMoveSquare(),
                onTap = { r, c -> goVm.onIntersectionTap(r, c) },
                onPass = { goVm.pass() },
                onResign = { goVm.resign() },
                onToggleHints = { goVm.toggleHints() },
                onBackToLobby = { screen = Screen.Lobby },
            )
            Screen.KoikoiGame -> KoikoiGameBoard(
                state = koi,
                onHandTap = { koiVm.onHandTap(it) },
                onFieldTap = { koiVm.onFieldTap(it) },
                onFlipTake = { koiVm.answerFlip(it) },
                onDecide = { stop -> koiVm.decide(stop) },
                onResign = { koiVm.resign() },
                onBackToLobby = { screen = Screen.Lobby },
            )
        }

        val labeledErrors = buildList {
            shogi.error?.let { add("Shogi" to it) }
            go.error?.let { add("Go" to it) }
            koi.error?.let { add("Koi-Koi" to it) }
        }
        if (labeledErrors.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            ErrorCard(
                errors = labeledErrors,
                onDismiss = {
                    shogiVm.clearError()
                    goVm.clearError()
                    koiVm.clearError()
                },
            )
        }
    }
}

// ---------------------------------------------------------------- Lobby ---

@Composable
private fun Lobby(
    shogi: ShogiUiState,
    go: GoUiState,
    koi: KoiUiState,
    baseUrl: String,
    onStartShogi: () -> Unit,
    onResumeShogi: () -> Unit,
    onStartGo: () -> Unit,
    onResumeGo: () -> Unit,
    onStartKoi: () -> Unit,
    onResumeKoi: () -> Unit,
    onGuides: () -> Unit,
    onPreferences: () -> Unit,
) {
    val scroll = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll),
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            shape = RoundedCornerShape(40.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_launcher),
                contentDescription = "Aiko",
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Crop,
            )
        }

        Text(
            "Aiko Games 🎮",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
            color = ShoujoAccent,
        )
        Text(
            "Let's play together!",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = ShoujoText.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(8.dp))

        // --- Cute game menu (Lingo-style cards) ---
        if (shogi.loading) {
            LoadingRow(label = "Starting Shogi…")
        } else if (shogi.inGame && shogi.game != null) {
            val engine = shogi.game.engine
            val sub1 = "Resume vs Aiko" + (if (engine != null) " via $engine" else "")
            val sub2 = "${sideLabel(shogi.side)} · ${shogi.difficulty.cap()}"
            CuteGameCard(icon = "♟️", iconRes = KomaImages.handDrawable('P', true), title = "Shogi 将棋", subtitle1 = sub1, subtitle2 = sub2, color = CuteMint, onClick = onResumeShogi)
        } else {
            val engine = if (shogi.engineOnline == true) "YaneuraOu" else null
            val sub1 = "New Game vs Aiko" + (if (engine != null) " via $engine" else "")
            val sub2 = "${sideLabel(shogi.side)} · ${shogi.difficulty.cap()}"
            CuteGameCard(icon = "♟️", iconRes = KomaImages.handDrawable('P', true), title = "Shogi 将棋", subtitle1 = sub1, subtitle2 = sub2, color = CuteMint, onClick = onStartShogi)
        }

        if (go.loading) {
            LoadingRow(label = "Starting Go… (server may take a moment)")
        } else if (go.inGame && go.game != null) {
            val engine = go.game.engine
            val sub1 = "Resume vs Aiko" + (if (engine != null) " via $engine" else "")
            val sub2 = "${sideLabel(go.side)} · ${go.boardSize}×${go.boardSize} · ${go.difficulty.cap()}"
            CuteGameCard(icon = "⚫", title = "Go 囲碁", subtitle1 = sub1, subtitle2 = sub2, color = CuteSky, onClick = onResumeGo)
        } else {
            val engine = if (go.engineOnline == true) "KataGo" else null
            val sub1 = "New Game vs Aiko" + (if (engine != null) " via $engine" else "")
            val sub2 = "${sideLabel(go.side)} · ${go.boardSize}×${go.boardSize} · ${go.difficulty.cap()}"
            CuteGameCard(icon = "⚫", title = "Go 囲碁", subtitle1 = sub1, subtitle2 = sub2, color = CuteSky, onClick = onStartGo)
        }

        if (koi.loading) {
            LoadingRow(label = "Dealing hanafuda… 🌸")
        } else if (koi.inGame && koi.game != null) {
            val sub1 = "Resume vs Aiko"
            val sub2 = "${koi.difficulty.cap()} · ${koi.months} months"
            CuteGameCard(icon = "🎴", title = "Koi-Koi こいこい", subtitle1 = sub1, subtitle2 = sub2, color = ShoujoPink, onClick = onResumeKoi)
        } else {
            val sub1 = "New Game vs Aiko"
            val sub2 = "${koi.difficulty.cap()} · ${koi.months} months"
            CuteGameCard(icon = "🎴", title = "Koi-Koi こいこい", subtitle1 = sub1, subtitle2 = sub2, color = ShoujoPink, onClick = onStartKoi)
        }

        val busy = shogi.loading || go.loading || koi.loading
        if (!busy) {
            OutlinedButton(
                onClick = onGuides,
                modifier = Modifier.fillMaxWidth(0.9f),
            ) { Text("📖 Game Guides ♡") }

            OutlinedButton(
                onClick = onPreferences,
                modifier = Modifier.fillMaxWidth(0.9f),
            ) { Text("⚙️ Preferences ♡") }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text(
                "Credits:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = ShoujoText,
            )
            Text(
                "Shogi Engine: YaneuraOu (github.com/yaneurao/YaneuraOu)",
                style = MaterialTheme.typography.bodySmall,
                color = ShoujoText.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
            Text(
                "Go Engine: KataGo (github.com/lightvector/KataGo)",
                style = MaterialTheme.typography.bodySmall,
                color = ShoujoText.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
            Text(
                "Shogi Art: sunfish-shogi (sunfish-shogi.github.io)",
                style = MaterialTheme.typography.bodySmall,
                color = ShoujoText.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
            Text(
                "Koi-Koi Art: Louie Mantia (CC BY-SA 4.0)",
                style = MaterialTheme.typography.bodySmall,
                color = ShoujoText.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CuteGameCard(
    icon: String,
    iconRes: Int? = null,
    title: String,
    subtitle1: String,
    subtitle2: String,
    color: Color,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(84.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (iconRes != null) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(icon, fontSize = 32.sp)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = ShoujoText)
                Text(text = subtitle1, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ShoujoText.copy(alpha = 0.7f))
                Text(text = subtitle2, fontSize = 11.sp, fontWeight = FontWeight.Normal, color = ShoujoText.copy(alpha = 0.6f))
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("▶", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = ShoujoText.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun LoadingRow(label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.size(8.dp))
        Text(label, color = ShoujoText)
    }
}

// ---------------------------------------------------------- Preferences ---

@Composable
private fun PreferencesScreen(
    shogi: ShogiUiState,
    go: GoUiState,
    koi: KoiUiState,
    baseUrl: String,
    onShogiDifficulty: (String) -> Unit,
    onGoDifficulty: (String) -> Unit,
    onKoiDifficulty: (String) -> Unit,
    onShogiSide: (String) -> Unit,
    onGoSide: (String) -> Unit,
    onGoSize: (Int) -> Unit,
    onToggleGoHints: () -> Unit,
    onKoiMonths: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val scroll = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().verticalScroll(scroll),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Lobby") }
            Spacer(modifier = Modifier.weight(1f))
            Text("Preferences", fontWeight = FontWeight.Bold, color = ShoujoText)
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(64.dp))
        }

        PrefsCard(title = "♟️ Shogi") {
            PrefsLabel("Difficulty (default: Medium)")
            DifficultyRow(
                levels = shogi.difficulties.ifEmpty { listOf("easy", "medium", "hard") },
                selected = shogi.difficulty,
                onPick = onShogiDifficulty,
            )
            Spacer(modifier = Modifier.height(6.dp))
            PrefsLabel("Your side")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("black" to "先手 Black", "white" to "後手 White").forEach { (side, label) ->
                    if (shogi.side == side) {
                        Button(onClick = {}, enabled = false) { Text(label) }
                    } else {
                        OutlinedButton(onClick = { onShogiSide(side) }) { Text(label) }
                    }
                }
            }
        }

        PrefsCard(title = "⚫ Go") {
            PrefsLabel("Difficulty (default: Medium)")
            DifficultyRow(
                levels = go.difficulties.ifEmpty { listOf("easy", "medium", "hard") },
                selected = go.difficulty,
                onPick = onGoDifficulty,
            )
            Spacer(modifier = Modifier.height(6.dp))
            PrefsLabel("Your side")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("black" to "Black ⚫", "white" to "White ⚪").forEach { (side, label) ->
                    if (go.side == side) {
                        Button(onClick = {}, enabled = false) { Text(label) }
                    } else {
                        OutlinedButton(onClick = { onGoSide(side) }) { Text(label) }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            PrefsLabel("Board size")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                go.availableSizes.forEach { n ->
                    if (go.boardSize == n) {
                        Button(onClick = {}, enabled = false) { Text("${n}×$n") }
                    } else {
                        OutlinedButton(onClick = { onGoSize(n) }) { Text("${n}×$n") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Move hints", color = ShoujoText, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.size(8.dp))
                Switch(checked = go.showHints, onCheckedChange = { onToggleGoHints() })
            }
        }

        PrefsCard(title = "🌸 Koi-Koi") {
            PrefsLabel("Difficulty (default: Medium)")
            DifficultyRow(
                levels = koi.difficulties.ifEmpty { listOf("easy", "medium", "hard") },
                selected = koi.difficulty,
                onPick = onKoiDifficulty,
            )
            Spacer(modifier = Modifier.height(6.dp))
            PrefsLabel("Match length")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                koi.availableMonths.forEach { n ->
                    if (koi.months == n) {
                        Button(onClick = {}, enabled = false) { Text("$n mo") }
                    } else {
                        OutlinedButton(onClick = { onKoiMonths(n) }) { Text("$n mo") }
                    }
                }
            }
            Text(
                "Months per match — 3 quick, 6 classic-short, 12 full year.",
                style = MaterialTheme.typography.bodySmall,
                color = ShoujoText.copy(alpha = 0.7f),
            )
        }

        Button(onClick = onBack, modifier = Modifier.fillMaxWidth(0.9f)) {
            Text("Done")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun PrefsCard(title: String, body: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.85f)),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = ShoujoText)
            body()
        }
    }
}

@Composable
private fun PrefsLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = ShoujoText)
}

@Composable
private fun DifficultyRow(levels: List<String>, selected: String, onPick: (String) -> Unit) {
    // Always Easy | Medium | Hard so Medium sits in the middle.
    val order = listOf("easy", "medium", "hard")
    val ordered = levels.sortedBy { order.indexOf(it.lowercase()).takeIf { i -> i >= 0 } ?: 99 }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ordered.forEach { level ->
            if (selected == level) {
                Button(onClick = {}, enabled = false) { Text(level.cap()) }
            } else {
                OutlinedButton(onClick = { onPick(level) }) { Text(level.cap()) }
            }
        }
    }
}

private fun String.cap(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

private fun sideLabel(side: String): String = if (side == "white") "後手 White" else "先手 Black"

private fun goSideLabel(side: String): String = if (side == "white") "White ⚪" else "Black ⚫"

// ----------------------------------------------------------------- Rules ---
@Composable
private fun GuideSelectionScreen(
    onShogi: () -> Unit,
    onGo: () -> Unit,
    onKoi: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Back", color = ShoujoAccent, fontWeight = FontWeight.Bold) }
            Spacer(modifier = Modifier.weight(1f))
            Text("Game Guides", fontWeight = FontWeight.ExtraBold, color = ShoujoText)
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(64.dp))
        }

        CuteGameCard(icon = "♟️", iconRes = KomaImages.handDrawable('P', true), title = "Shogi Guide", subtitle1 = "Learn the soul of chess", subtitle2 = "Sente/Gote · Pieces · Drops", color = CuteMint, onClick = onShogi)
        CuteGameCard(icon = "⚫", title = "Go Guide", subtitle1 = "Master the board", subtitle2 = "Rules · Capture · Ko", color = CuteSky, onClick = onGo)
        CuteGameCard(icon = "🎴", title = "Koi-Koi Guide", subtitle1 = "Play with the seasons", subtitle2 = "Hanafuda · Yaku · Match", color = ShoujoPink, onClick = onKoi)
    }
}

@Composable
private fun ShogiRulesScreen(onBack: () -> Unit) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(scroll),
        horizontalAlignment = Alignment.Start,
    ) {
        TextButton(onClick = onBack) { Text("← Back") }
        RulesSection("Objective — checkmate (詰み)") {
            Text(
                "Checkmate the opponent's king (Black 玉, White 王): attack it so it has " +
                    "no legal escape. In check you must get out — move the king, capture " +
                    "the attacker, or block the attack (a drop can block or capture too).",
            )
        }
        RulesSection("Board, sides & setup") {
            Text(
                "9×9 board, 81 squares. 先手 Black (sente) moves first, 後手 White (gote) " +
                    "second — pick your side in Preferences.\n" +
                    "Each side starts with: back rank 香桂銀金玉金銀桂香 " +
                    "(lance-knight-silver-gold-king-gold-silver-knight-lance), " +
                    "second rank 飛 rook + 角 bishop, third rank 9 pawns (歩).",
            )
        }
        RulesSection("How each piece moves") {
            Text(
                "• 歩 Pawn: 1 square straight forward.\n" +
                    "• 香 Lance: any distance straight forward, no jumping.\n" +
                    "• 桂 Knight: 2 forward + 1 sideways (jumps; forward only).\n" +
                    "• 銀 Silver: 1 square diagonally or straight forward.\n" +
                    "• 金 Gold: 1 square orthogonal or diagonally forward (not back-diagonal).\n" +
                    "• 角 Bishop: any distance diagonally.\n" +
                    "• 飛 Rook: any distance orthogonal.\n" +
                    "• 玉/王 King: 1 square any direction.",
            )
        }
        RulesSection("Promotion (成り)") {
            Text(
                "Pawn, lance, knight, silver, bishop and rook can promote when they move " +
                    "into, out of, or within the far 3 ranks (gold and king never promote).\n" +
                    "• と/成香/成桂/成銀 move as gold.\n" +
                    "• 馬 Horse = bishop + 1 orthogonal. 龍 Dragon = rook + 1 diagonal.\n" +
                    "• Forced: pawn/lance reaching the last rank and knights reaching the " +
                    "last two ranks MUST promote (else they'd have no moves).\n" +
                    "In this app a dialog asks 成 Promote / 不成 Stay when it's optional.",
            )
        }
        RulesSection("Drops (持ち駒) — the soul of shogi") {
            Text(
                "Captured pieces switch sides: drop one onto any empty square as your " +
                    "move. Illegal drops:\n" +
                    "• 二歩 nifu: two of your unpromoted pawns on the same file.\n" +
                    "• Pawn/lance on the last rank, knight on the last two ranks " +
                    "(no legal move from there).\n" +
                    "• 打ち歩詰め: dropping a pawn to deliver instant checkmate.",
            )
        }
        RulesSection("Endings & time") {
            Text(
                "Win by checkmate, opponent's resignation, or flag (clock). Fourfold " +
                    "repetition is normally a draw.\n" +
                    "Default control here: 60 min main time + 60s byoyomi per move. " +
                    "The per-move clock ticks down from 60s each turn and turns red with 15s left.",
            )
        }
        RulesSection("How to play here") {
            Text(
                "Tap a piece → glowing destinations → tap to move. Use the hand tray " +
                    "for drops. Aiko replies via YaneuraOu when the server has it, " +
                    "otherwise casual legal moves.",
            )
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text("Back to lobby")
        }
    }
}

@Composable
private fun GoRulesScreen(onBack: () -> Unit) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(scroll),
        horizontalAlignment = Alignment.Start,
    ) {
        TextButton(onClick = onBack) { Text("← Back") }
        RulesSection("Objective — surround territory") {
            Text(
                "Surround empty points (territory) with your stones, and capture enemy " +
                    "stones. Most points wins.\n" +
                    "Note: this app's server does not auto-score — after two consecutive " +
                    "passes it marks the game finished and you judge the result on the board.",
            )
        }
        RulesSection("Board, stones & first move") {
            Text(
                "Stones go on intersections (not squares). Black plays first.\n" +
                    "9×9 (fast, best for learning), 13×13, or 19×19 (standard) — " +
                    "change it in Preferences before starting.",
            )
        }
        RulesSection("Liberties & capture") {
            Text(
                "Orthogonally connected stones of one colour form a group. Its liberties " +
                    "are adjacent empty points. Play the last liberty of an enemy group " +
                    "and the whole group is captured and removed. Your own stones show " +
                    "captured counts at the top of the board screen.",
            )
        }
        RulesSection("Illegal moves: suicide & ko") {
            Text(
                "• Suicide is illegal: no placing a stone that would leave its own group " +
                    "with zero liberties (unless it captures first).\n" +
                    "• Simple ko: you may not immediately recapture a single-stone ko — " +
                    "play elsewhere first, then you may recapture.",
            )
        }
        RulesSection("Life & death in one minute") {
            Text(
                "A group with two separate inner eyes can never be captured (alive). " +
                    "A group with at most one eye can be killed. Groups that neither " +
                    "side can attack without dying are alive in seki — an advanced topic, " +
                    "just keep playing and watch your liberties.",
            )
        }
        RulesSection("Ending, passing & resigning") {
            Text(
                "Pass when no useful move remains (Pass button). Two consecutive passes " +
                    "end the game (status: finished). Resign anytime with Resign if the " +
                    "position is hopeless — no shame, even pros resign.",
            )
        }
        RulesSection("How to play here") {
            Text(
                "1. Pick board size, side, and difficulty in Preferences.\n" +
                    "2. Tap Go (囲碁) vs Aiko.\n" +
                    "3. Tap an empty intersection (toggle hints on/off anytime).\n" +
                    "4. Aiko replies via KataGo if configured on the server; " +
                    "otherwise a casual legal move.",
            )
        }
        RulesSection("Engine note") {
            Text(
                "Backend: pure-Python rules + optional KataGo GTP. " +
                    "GNU Go is a lighter alternative for future server wiring.",
            )
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text("Back to lobby")
        }
    }
}

@Composable
private fun RulesSection(title: String, body: @Composable () -> Unit) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ShoujoText)
    Spacer(modifier = Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) { Column { body() } }
}

// ------------------------------------------------------------ Shared UI ---

@Composable
private fun ErrorCard(
    errors: List<Pair<String, String>>,
    onDismiss: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            errors.forEach { (game, msg) ->
                Text("$game: $msg", color = Color(0xFFC62828), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(4.dp))
            }
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}

/** Aiko's line always in a speech-style box — never bare text. */
@Composable
private fun AikoCommentBox(comment: String?) {
    if (comment.isNullOrBlank()) return
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = ShoujoPalePink),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Text("🌸", fontSize = 22.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    "Aiko",
                    fontWeight = FontWeight.Bold,
                    color = ShoujoText,
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    comment,
                    color = ShoujoText,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun InfoCard(lines: List<String>) {
    if (lines.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.75f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            lines.forEach {
                Text(it, color = ShoujoText, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ----------------------------------------------------------------- Shogi ---

@Composable
private fun HandTray(
    label: String,
    pieces: List<com.aiko.games.data.HandPiece>,
    selectedPiece: Char?,
    enabled: Boolean,
    isOpponent: Boolean,
    onPiece: (Char) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = ShoujoText.copy(alpha = 0.75f))
        if (pieces.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(42.dp), contentAlignment = Alignment.CenterStart) {
                Text("(empty)", style = MaterialTheme.typography.bodySmall, color = ShoujoText.copy(alpha = 0.5f))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pieces.forEach { hp ->
                    val selected = selectedPiece == hp.symbol
                    val res = KomaImages.handDrawable(hp.symbol, hp.forBlack)
                    Box(
                        modifier = Modifier
                            .background(if (selected) BoardSelect else PieceWhite, RoundedCornerShape(8.dp))
                            .border(
                                1.dp,
                                if (selected) BoardLine
                                else if (hp.forBlack) Color(0xFF3E2723) else Color(0xFFC62828),
                                RoundedCornerShape(8.dp),
                            )
                            .clickable(enabled = enabled) { onPiece(hp.symbol) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (res != null) {
                                // Bundled offline wood piece (hitomoji_wood).
                                Image(
                                    painter = painterResource(res),
                                    contentDescription = "${hp.symbol} (${if (hp.forBlack) "black" else "white"})",
                                    modifier = Modifier.size(36.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                Text(
                                    SfenBoard.glyphFor(hp.symbol),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (hp.forBlack) PieceBlack else PieceWhiteStroke,
                                )
                            }
                            if (hp.count > 1) {
                                Text(
                                    "×${hp.count}",
                                    fontWeight = FontWeight.Bold,
                                    color = ShoujoText,
                                    fontSize = 14.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (isOpponent) {
            Text(
                "Red border = gote (white) pieces",
                style = MaterialTheme.typography.bodySmall,
                color = ShoujoText.copy(alpha = 0.45f),
            )
        }
    }
}

@Composable
private fun ShogiGameBoard(
    state: ShogiUiState,
    hints: Set<Pair<Int, Int>>,
    last: Set<Pair<Int, Int>>,
    onSquare: (Int, Int) -> Unit,
    onHandPiece: (Char) -> Unit,
    onResign: () -> Unit,
    onBackToLobby: () -> Unit,
) {
    val game = state.game ?: return
    val grid = remember(game.sfen) { SfenBoard.parseGrid(game.sfen) }
    val blackHand = remember(game.sfen) { SfenBoard.blackHand(game.sfen) }
    val whiteHand = remember(game.sfen) { SfenBoard.whiteHand(game.sfen) }
    val selectedHand = (state.selected as? Selection.Hand)?.piece
    val userSide = game.side.ifBlank { "black" }
    val canInteract = !state.loading && game.status == "playing" && game.turn == userSide
    val youLabel = if (userSide == "black") "先手 (Black ⚫)" else "後手 (White ⚪)"
    val aikoLabel = if (userSide == "black") "後手 (White ⚪)" else "先手 (Black ⚫)"
    // Your pieces are your own colour; opponent's are the other colour.
    val yourHand = if (userSide == "black") blackHand else whiteHand
    val aikoHand = if (userSide == "black") whiteHand else blackHand

    // Clock logic
    val sfenKey = game.sfen + "|" + (game.last_move ?: "")
    val (youMain, aikoMain) = if (userSide == "black") game.clock_black_ms to game.clock_white_ms else game.clock_white_ms to game.clock_black_ms
    val limitMs = if (game.byoyomi_ms != null && game.byoyomi_ms > 0) game.byoyomi_ms else null
    val snapshotAt = remember(sfenKey) { System.currentTimeMillis() }
    var now by remember(sfenKey) { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sfenKey, game.status, game.turn) {
        if (game.status != "playing") return@LaunchedEffect
        while (true) {
            delay(250L)
            now = System.currentTimeMillis()
        }
    }
    val moveElapsed = (now - snapshotAt).coerceAtLeast(0L)
    val moveLeft = limitMs?.let { (it - moveElapsed).coerceAtLeast(0L) }
    val moverIsYou = game.turn == userSide

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBackToLobby) {
                Text("← Lobby", color = ShoujoAccent, fontWeight = FontWeight.Bold)
            }
            Text(
                if (game.turn == userSide) "Your turn" else "Aiko's turn",
                fontWeight = FontWeight.Bold,
                color = ShoujoText,
            )
            Button(
                onClick = onResign,
                colors = ButtonDefaults.buttonColors(containerColor = ShoujoAccent)
            ) {
                Text("Resign", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        InfoCard(
            lines = buildList {
                add("You: $youLabel · Aiko: $aikoLabel · ${game.status}")
                if (youMain != null || aikoMain != null) {
                    add("Main: You: ${formatClock(youMain)} · Aiko: ${formatClock(aikoMain)}")
                }
                if (game.status == "playing" && moveLeft != null) {
                    add("Timer: ⏱ ${if (moverIsYou) "Your move" else "Aiko's move"}: ${formatClock(moveLeft)} left")
                } else if (game.status == "playing") {
                    add("Timer: ⏱ This move: ${formatClock(moveElapsed)}")
                }
                if (game.status != "playing") add(statusLine(game.status))
            },
        )
        Spacer(modifier = Modifier.height(6.dp))
        AikoCommentBox(game.ai_comment)
        Spacer(modifier = Modifier.height(6.dp))
        HandTray(
            label = "Aiko's hand ($aikoLabel)",
            pieces = aikoHand,
            selectedPiece = null,
            enabled = false,
            isOpponent = true,
            onPiece = {},
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(458f / 500f)
                .background(BoardWood, RoundedCornerShape(8.dp))
                .border(2.dp, BoardLine, RoundedCornerShape(8.dp)),
        ) {
            Image(
                painter = painterResource(KomaImages.BOARD_LIGHT_RES),
                contentDescription = "Shogi board",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
            )
            Column(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                for (row in 0 until 9) {
                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        for (col in 0 until 9) {
                            val cell = grid[row][col]
                            val sel = (state.selected as? Selection.Square)?.let { it.row == row && it.col == col } == true
                            val bg = when {
                                sel -> BoardSelect.copy(alpha = 0.55f)
                                (row to col) in hints -> BoardHint.copy(alpha = 0.45f)
                                (row to col) in last -> BoardLast.copy(alpha = 0.45f)
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                                    .background(bg)
                                    .clickable(enabled = canInteract) { onSquare(row, col) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (cell != null) {
                                    val isBlack = KomaImages.isBlackSide(cell)
                                    // Bundled offline wood piece — orientation shows the side
                                    // (sente points up, gote points down).
                                    KomaImages.drawableFor(cell)?.let { res ->
                                        Image(
                                            painter = painterResource(res),
                                            contentDescription = SfenBoard.glyph(cell) +
                                                if (isBlack) " (black)" else " (white)",
                                            modifier = Modifier.fillMaxSize(0.92f),
                                            contentScale = ContentScale.Fit,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (state.loading) {
                Box(Modifier.fillMaxSize().background(Color(0x44000000)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        HandTray(
            label = "Your hand ($youLabel)",
            pieces = yourHand,
            selectedPiece = selectedHand,
            enabled = canInteract,
            isOpponent = false,
            onPiece = onHandPiece,
        )
        // Row with Lobby and Resign moved to top
    }
}

private fun formatClock(ms: Long?): String {
    if (ms == null) return "—"
    val totalSec = (ms.coerceAtLeast(0) / 1000).toInt()
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

private fun statusLine(status: String): String = when (status) {
    "checkmate" -> "Checkmate — game over"
    "resigned" -> "Resigned — game over"
    "timeout" -> "Flag — out of time ⌛"
    "draw", "stalemate" -> "Draw — game over"
    "finished" -> "Finished — game over"
    else -> status
}

// -------------------------------------------------------------------- Go ---

@Composable
private fun GoGameBoard(
    state: GoUiState,
    hints: Set<Pair<Int, Int>>,
    last: Pair<Int, Int>?,
    onTap: (Int, Int) -> Unit,
    onPass: () -> Unit,
    onResign: () -> Unit,
    onToggleHints: () -> Unit,
    onBackToLobby: () -> Unit,
) {
    val game = state.game ?: return
    val userSide = game.side.ifBlank { "black" }
    val canInteract = !state.loading && game.status == "playing" && game.turn == userSide
    val youLabel = if (userSide == "black") "Black ⚫" else "White ⚪"

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        TextButton(onClick = onBackToLobby) {
            Text("← Lobby", color = ShoujoAccent, fontWeight = FontWeight.Bold)
        }
        Text(
            if (game.turn == userSide) "Your turn ($youLabel)" else "Aiko's turn",
            fontWeight = FontWeight.Bold,
            color = ShoujoText,
        )
        Button(
            onClick = onResign,
            colors = ButtonDefaults.buttonColors(containerColor = ShoujoAccent)
        ) {
            Text("Resign", color = Color.White)
        }
    }

        InfoCard(
            lines = buildList {
                add("${game.size}×${game.size} · ${game.status}${game.engine?.let { " · $it" } ?: ""}")
                add("Captures — You/Aiko context: B taken ${game.captured_black} · W taken ${game.captured_white} · Moves: ${game.moves.size}")
                game.last_move?.let { add("Last: $it") }
                if (game.status != "playing") add(statusLine(game.status))
            },
        )
        Spacer(modifier = Modifier.height(6.dp))
        AikoCommentBox(game.ai_comment)
        Spacer(modifier = Modifier.height(8.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            GoBoardView(
                size = game.size,
                stones = game.stones,
                hints = hints,
                lastMove = last,
                enabled = canInteract,
                onTap = onTap,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.loading) {
                Box(
                    Modifier
                        .matchParentSize()
                        .background(Color(0x44000000)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onPass, enabled = canInteract) { Text("Pass") }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Hints", style = MaterialTheme.typography.bodySmall, color = ShoujoText)
                Spacer(modifier = Modifier.size(4.dp))
                Switch(checked = state.showHints, onCheckedChange = { onToggleHints() })
            }
        }
    }
}

// ---------------------------------------------------------------- Koi-Koi ---

@Composable
private fun YakuChip(yaku: KoiYaku) {
    Box(
        modifier = Modifier
            .background(Color(0xFFFFF3E0), RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("${yaku.jp} ${yaku.points}", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = ShoujoText)
    }
}

@Composable
private fun CapturedLine(label: String, cards: List<KoiCard>, yaku: List<KoiYaku>) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = ShoujoText.copy(alpha = 0.75f))
            Spacer(modifier = Modifier.size(6.dp))
            val counts = cards.groupingBy { it.kind }.eachCount()
            val order = listOf("hikari", "tane", "tan-poetry", "tan-blue", "tan-red", "kasu")
            val icons = mapOf("hikari" to "🌟", "tane" to "🦋", "tan-poetry" to "📝", "tan-blue" to "💙", "tan-red" to "🎀", "kasu" to "🍂")
            Text(
                if (counts.isEmpty()) "(empty)"
                else order.filter { counts[it] ?: 0 > 0 }.joinToString(" ") { "${icons[it]}${counts[it]}" },
                style = MaterialTheme.typography.bodySmall,
                color = ShoujoText.copy(alpha = 0.8f),
            )
        }
        if (yaku.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                yaku.forEach { YakuChip(it) }
            }
        }
    }
}

@Composable
private fun KoikoiGameBoard(
    state: KoiUiState,
    onHandTap: (Int) -> Unit,
    onFieldTap: (Int) -> Unit,
    onFlipTake: (List<Int>) -> Unit,
    onDecide: (Boolean) -> Unit,
    onResign: () -> Unit,
    onBackToLobby: () -> Unit,
) {
    val game = state.game ?: return
    val pending = game.pending
    val deciding = pending?.kind == "decision"
    val flipping = pending?.kind == "flip"
    val canPlay = !state.loading && game.status == "playing" && game.turn == "you" && pending == null
    val selected = state.selectedHand
    val takeSets = selected?.let { hid ->
        state.plays.firstOrNull { it.hand.id == hid }
            ?.takes?.map { opt -> opt.map { c -> c.id } }
    } ?: emptyList()
    val glowIds = takeSets.flatten().toSet()
    val flipIds = pending?.options?.flatten()?.map { it.id }?.toSet() ?: emptySet()
    val youTotal = game.totals["you"] ?: 0
    val aikoTotal = game.totals["aiko"] ?: 0

    if (deciding) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("🌸 Yaku! ${pending.new_yaku.joinToString(", ") { it.name }}") },
            text = {
                Text(
                    "Bank ${pending.would_score} pts now (×${pending.multiplier}), " +
                        "or call koi-koi and continue — stakes double, but if Aiko " +
                        "finishes next, SHE scores double!",
                )
            },
            confirmButton = {
                Button(onClick = { onDecide(true) }) { Text("🛑 Stop +${pending.would_score}") }
            },
            dismissButton = {
                OutlinedButton(onClick = { onDecide(false) }) { Text("🌸 Koi-koi!") }
            },
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBackToLobby) {
                Text("← Lobby", color = ShoujoAccent, fontWeight = FontWeight.Bold)
            }
            Text(
                "🌸 Month ${game.month}/${game.months}",
                fontWeight = FontWeight.ExtraBold,
                color = ShoujoText,
            )
            Button(
                onClick = onResign,
                colors = ButtonDefaults.buttonColors(containerColor = ShoujoAccent)
            ) {
                Text("Resign", color = Color.White)
            }
        }

        InfoCard(
            lines = buildList {
                add("You $youTotal pts · Aiko $aikoTotal pts · ${if (game.oya == "you") "you deal" else "Aiko deals"} · ${game.status}")
                if (game.status == "finished") {
                    add(
                        when (game.winner) {
                            "you" -> "🏆 You win the match! おめでとう!"
                            "aiko" -> "Aiko wins the match 🌸 Good game!"
                            else -> "Draw — good game! 🌸"
                        },
                    )
                }
            },
        )
        game.round_result?.let { r ->
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CuteMint),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    when {
                        r.reason == "resigned" -> "🏳️ ${if (r.winner == "you") "Aiko" else "You"} resigned"
                        r.winner == "draw" -> "🤝 Month drawn — no points"
                        r.winner == "you" -> "🌸 You take the month +${r.points} (${r.base}×${r.multiplier})!"
                        else -> "🌸 Aiko takes the month +${r.points} (${r.base}×${r.multiplier})"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    color = ShoujoText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        AikoCommentBox(game.ai_comment)
        Spacer(modifier = Modifier.height(6.dp))

        // Aiko's side: hand backs + captured.
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🌸", fontSize = 18.sp)
            repeat(game.hand_aiko_count) { HanafudaBack(width = 30.dp) }
        }
        Spacer(modifier = Modifier.height(4.dp))
        CapturedLine(label = "Aiko's collection", cards = game.cap_aiko, yaku = game.yaku_aiko)
        Spacer(modifier = Modifier.height(6.dp))

        // Field (table).
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20).copy(alpha = 0.85f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (game.field.isEmpty()) {
                    Text("✨ table clear! ✨", color = Color.White, fontWeight = FontWeight.Bold)
                }
                game.field.chunked(4).forEach { rowCards ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowCards.forEach { c ->
                            val glow = c.id in glowIds || c.id in flipIds
                            val active = (canPlay && selected != null && c.id in glowIds) ||
                                (flipping && c.id in flipIds)
                            HanafudaCard(
                                card = c,
                                width = 58.dp,
                                highlighted = glow,
                                dimmed = (selected != null || flipping) && !glow,
                                onClick = if (active) {
                                    { if (flipping) onFlipTake(listOf(c.id)) else onFieldTap(c.id) }
                                } else null,
                            )
                        }
                    }
                }
            }
        }

        if (flipping) {
            Spacer(modifier = Modifier.height(4.dp))
            val flipName = pending.flip?.let { f ->
                val (kanji, flower) = monthLabel(f.month)
                "$flower $kanji"
            } ?: "a card"
            Text(
                "🎴 Deck flipped $flipName — tap a glowing card!",
                fontWeight = FontWeight.Bold,
                color = ShoujoText,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(modifier = Modifier.height(6.dp))

        // Your hand.
        Text("Your hand ♡", style = MaterialTheme.typography.labelMedium, color = ShoujoText.copy(alpha = 0.75f))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            game.hand_you.forEach { c ->
                HanafudaCard(
                    card = c,
                    width = 64.dp,
                    selected = selected == c.id,
                    dimmed = !canPlay,
                    onClick = if (canPlay) {
                        { onHandTap(c.id) }
                    } else null,
                )
            }
        }
        if (canPlay) {
            val hint = if (selected == null) "Pick a card ♡" else "Tap a glowing field card ✨"
            Text(hint, style = MaterialTheme.typography.bodySmall, color = ShoujoText.copy(alpha = 0.8f))
        } else if (game.status == "playing" && game.turn != "you" && !state.loading) {
            Text("Aiko is thinking… 💭", style = MaterialTheme.typography.bodySmall, color = ShoujoText.copy(alpha = 0.8f))
        }
        if (state.loading) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Aiko is playing… 🌸", color = ShoujoText)
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        CapturedLine(label = "Your collection", cards = game.cap_you, yaku = game.yaku_you)
    }
}

@Composable
private fun KoikoiRulesScreen(onBack: () -> Unit) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(scroll),
        horizontalAlignment = Alignment.Start,
    ) {
        TextButton(onClick = onBack) { Text("← Back") }
        RulesSection("What is Koi-Koi? 🌸") {
            Text(
                "Koi-koi (こいこい) is the classic 2-player hanafuda card game. " +
                    "Match cards by month, collect scoring combos (yaku), then press your " +
                    "luck: bank the points or call koi-koi and play on for double!",
            )
        }
        RulesSection("Cards & months") {
            Text(
                "48 cards: 12 months × 4. Kinds: 🌟 Hikari bright (5) · 🦋 Tane animals (9) · " +
                    "🎀 Tanzaku ribbons (10: poetry 📝, blue 💙, plain ❤️) · 🍂 Kasu chaff (24).\n" +
                    "A match = 8 cards dealt each + 8 on the table. Months per match " +
                    "are set in Preferences (3 / 6 / 12).",
            )
        }
        RulesSection("Your turn: play, then flip") {
            Text(
                "1. Play one hand card. Same-month table card? Take both! Two choices? " +
                    "Pick one. Three on the table? Sweep all four! No match? Your card " +
                    "joins the table.\n" +
                    "2. Flip the deck's top card — same matching, same taking.\n" +
                    "In this app: tap a hand card (glowing = matches), tap again to place " +
                    "when nothing matches; tap a glowing card when the deck flip offers two.",
            )
        }
        RulesSection("Yaku & points 🏆") {
            Text(
                "• 五光 Five Brights 10 · 四光 Four Brights (no rain) 8 · " +
                    "雨四光 Rainy Four 7 · 三光 Three Brights (no rain) 5\n" +
                    "• 月見酒 Moon+cup 5 · 花見酒 Curtain+cup 5 · 猪鹿蝶 Boar-Deer-Butterfly 5\n" +
                    "• 赤短 Poetry ribbons 5 · 青短 Blue ribbons 5\n" +
                    "• Tane 5+ = 1 (+1 each extra) · Ribbons 5+ = 1 (+1 each extra) · " +
                    "Chaff 10+ = 1 (+1 each extra)",
            )
        }
        RulesSection("Koi-koi or stop? 🛑") {
            Text(
                "Completing a yaku pauses the game: 🛑 Stop banks the points, or 🌸 " +
                    "Koi-koi continues with stakes ×2 (again and again!). Danger: if Aiko " +
                    "finishes after your koi-koi, SHE scores double. Empty hands with no " +
                    "stop = higher yaku total takes the month (dealer wins ties).",
            )
        }
        RulesSection("Months, totals & dealer") {
            Text(
                "Round winner's points add to their match total; winner deals (oya) next " +
                    "month and plays first. Highest total after all months wins the match. " +
                    "You deal the first month ♡",
            )
        }
        RulesSection("How Aiko plays 🤖") {
            Text(
                "No external engine exists for Koi-Koi like YaneuraOu/KataGo — Aiko uses " +
                    "a built-in brain (greedy captures, yaku hunting, koi-koi courage by " +
                    "difficulty). Always ready, even offline from engines. Easy sometimes " +
                    "blunders and calls wild koi-kois; hard rarely forgives.",
            )
        }
        RulesSection("Card art 🎴") {
            Text(
                "Hanafuda card art by Louie Mantia, CC BY-SA 4.0, via Wikimedia Commons. " +
                    "Thank you for the beautiful deck! 🌸",
            )
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text("Back to lobby ♡")
        }
    }
}
