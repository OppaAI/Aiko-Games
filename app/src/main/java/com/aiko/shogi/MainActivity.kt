package com.aiko.shogi

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aiko.shogi.data.SfenBoard
import com.aiko.shogi.data.ServerConfig
import com.aiko.shogi.data.remote.GoApi
import com.aiko.shogi.data.remote.ShogiApi
import com.aiko.shogi.ui.GoBoardView
import com.aiko.shogi.ui.GoUiState
import com.aiko.shogi.ui.GoViewModel
import com.aiko.shogi.ui.KomaImages
import com.aiko.shogi.ui.Selection
import com.aiko.shogi.ui.ShogiUiState
import com.aiko.shogi.ui.ShogiViewModel
import com.aiko.shogi.ui.theme.AikoShogiTheme
import com.aiko.shogi.ui.theme.BoardHint
import com.aiko.shogi.ui.theme.BoardLast
import com.aiko.shogi.ui.theme.BoardLine
import com.aiko.shogi.ui.theme.BoardSelect
import com.aiko.shogi.ui.theme.BoardWood
import com.aiko.shogi.ui.theme.PieceBlack
import com.aiko.shogi.ui.theme.PieceWhite
import com.aiko.shogi.ui.theme.PieceWhiteStroke
import com.aiko.shogi.ui.theme.ShoujoSoftPink
import com.aiko.shogi.ui.theme.ShoujoText
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

enum class Screen {
    Lobby,
    ShogiRules,
    GoRules,
    ShogiGame,
    GoGame,
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AikoShogiTheme {
                var baseUrl by remember { mutableStateOf(ServerConfig.get(this@MainActivity)) }
                val retrofit = remember(baseUrl) { buildRetrofit(baseUrl) }
                val shogiApi = remember(retrofit) { retrofit.create(ShogiApi::class.java) }
                val goApi = remember(retrofit) { retrofit.create(GoApi::class.java) }

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

                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    GamesApp(
                        shogiVm = shogiVm,
                        goVm = goVm,
                        baseUrl = baseUrl,
                        onSaveUrl = { raw ->
                            baseUrl = ServerConfig.set(this@MainActivity, raw)
                        },
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
    baseUrl: String,
    onSaveUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shogi by shogiVm.ui.collectAsState()
    val go by goVm.ui.collectAsState()
    var screen by remember { mutableStateOf(Screen.Lobby) }

    LaunchedEffect(baseUrl) {
        shogiVm.refreshEngine()
        shogiVm.warmupEngine()
        goVm.refreshEngine()
        goVm.warmupEngine()
    }

    LaunchedEffect(shogi.inGame) {
        if (shogi.inGame) screen = Screen.ShogiGame
        else if (screen == Screen.ShogiGame) screen = Screen.Lobby
    }
    LaunchedEffect(go.inGame) {
        if (go.inGame) screen = Screen.GoGame
        else if (screen == Screen.GoGame) screen = Screen.Lobby
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
        Text(
            "Aiko Games ♟️",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ShoujoText,
        )
        Text(
            "vs Aiko · ${ServerConfig.displayHost(baseUrl)}",
            style = MaterialTheme.typography.bodySmall,
            color = ShoujoText.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(8.dp))

        when (screen) {
            Screen.Lobby -> Lobby(
                shogi = shogi,
                go = go,
                baseUrl = baseUrl,
                onSaveUrl = onSaveUrl,
                onStartShogi = { shogiVm.startGame() },
                onStartGo = { goVm.startGame() },
                onShogiRules = { screen = Screen.ShogiRules },
                onGoRules = { screen = Screen.GoRules },
                onRefresh = {
                    shogiVm.refreshEngine()
                    goVm.refreshEngine()
                },
                onShogiDifficulty = { shogiVm.setDifficulty(it) },
                onGoDifficulty = { goVm.setDifficulty(it) },
                onShogiSide = { shogiVm.setSide(it) },
                onGoSide = { goVm.setSide(it) },
                onGoSize = { goVm.setBoardSize(it) },
                onToggleGoHints = { goVm.toggleHints() },
            )
            Screen.ShogiRules -> ShogiRulesScreen(onBack = { screen = Screen.Lobby })
            Screen.GoRules -> GoRulesScreen(onBack = { screen = Screen.Lobby })
            Screen.ShogiGame -> ShogiGameBoard(
                state = shogi,
                hints = shogiVm.hintSquares(),
                last = shogiVm.lastMoveSquares(),
                onSquare = { r, c -> shogiVm.onSquareTap(r, c) },
                onHandPiece = { shogiVm.onHandPieceTap(it) },
                onResign = { shogiVm.resign() },
            )
            Screen.GoGame -> GoGameBoard(
                state = go,
                hints = goVm.legalHintSquares(),
                last = goVm.lastMoveSquare(),
                onTap = { r, c -> goVm.onIntersectionTap(r, c) },
                onPass = { goVm.pass() },
                onResign = { goVm.resign() },
                onToggleHints = { goVm.toggleHints() },
            )
        }

        val errors = listOfNotNull(shogi.error, go.error).distinct()
        errors.forEach { e ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(e, color = Color(0xFFC62828), textAlign = TextAlign.Center)
        }
        if (errors.isNotEmpty()) {
            TextButton(onClick = {
                shogiVm.clearError()
                goVm.clearError()
            }) { Text("Dismiss") }
        }
    }
}

@Composable
private fun Lobby(
    shogi: ShogiUiState,
    go: GoUiState,
    baseUrl: String,
    onSaveUrl: (String) -> Unit,
    onStartShogi: () -> Unit,
    onStartGo: () -> Unit,
    onShogiRules: () -> Unit,
    onGoRules: () -> Unit,
    onRefresh: () -> Unit,
    onShogiDifficulty: (String) -> Unit,
    onGoDifficulty: (String) -> Unit,
    onShogiSide: (String) -> Unit,
    onGoSide: (String) -> Unit,
    onGoSize: (Int) -> Unit,
    onToggleGoHints: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(baseUrl) { mutableStateOf(baseUrl) }
    val scroll = rememberScrollState()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll),
    ) {
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
            buildString {
                append("Shogi: ")
                append(
                    when (shogi.engineOnline) {
                        true -> "YaneuraOu ✅"
                        false -> "casual"
                        null -> "…"
                    },
                )
                append("  ·  Go: ")
                append(
                    when (go.engineOnline) {
                        true -> "KataGo ✅"
                        false -> "casual"
                        null -> "…"
                    },
                )
            },
            color = ShoujoText,
            style = MaterialTheme.typography.bodySmall,
        )

        if (editing) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text("Aiko-chan URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    if (ServerConfig.isValid(draft)) {
                        onSaveUrl(draft)
                        editing = false
                        onRefresh()
                    }
                }) { Text("Save") }
                OutlinedButton(onClick = { editing = false; draft = baseUrl }) {
                    Text("Cancel")
                }
            }
        } else {
            TextButton(onClick = { editing = true }) {
                Text("🔗 Server: ${ServerConfig.displayHost(baseUrl)}")
            }
        }

        Text("Shogi difficulty", style = MaterialTheme.typography.labelLarge, color = ShoujoText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            shogi.difficulties.forEach { level ->
                val selected = shogi.difficulty == level
                if (selected) {
                    Button(onClick = {}, enabled = false) { Text(level.replaceFirstChar { it.uppercase() }) }
                } else {
                    OutlinedButton(onClick = { onShogiDifficulty(level) }) { Text(level.replaceFirstChar { it.uppercase() }) }
                }
            }
        }

        Text("Shogi side (you play)", style = MaterialTheme.typography.labelLarge, color = ShoujoText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("black" to "先手 Black", "white" to "後手 White").forEach { (side, label) ->
                if (shogi.side == side) {
                    Button(onClick = {}, enabled = false) { Text(label) }
                } else {
                    OutlinedButton(onClick = { onShogiSide(side) }) { Text(label) }
                }
            }
        }

        Text("Go difficulty", style = MaterialTheme.typography.labelLarge, color = ShoujoText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            go.difficulties.forEach { level ->
                val selected = go.difficulty == level
                if (selected) {
                    Button(onClick = {}, enabled = false) { Text(level.replaceFirstChar { it.uppercase() }) }
                } else {
                    OutlinedButton(onClick = { onGoDifficulty(level) }) { Text(level.replaceFirstChar { it.uppercase() }) }
                }
            }
        }

        Text("Go side (you play)", style = MaterialTheme.typography.labelLarge, color = ShoujoText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("black" to "Black ⚫", "white" to "White ⚪").forEach { (side, label) ->
                if (go.side == side) {
                    Button(onClick = {}, enabled = false) { Text(label) }
                } else {
                    OutlinedButton(onClick = { onGoSide(side) }) { Text(label) }
                }
            }
        }

        Text("Go board size", style = MaterialTheme.typography.labelLarge, color = ShoujoText)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            go.availableSizes.forEach { n ->
                if (go.boardSize == n) {
                    Button(onClick = {}, enabled = false) { Text("${n}×$n") }
                } else {
                    OutlinedButton(onClick = { onGoSize(n) }) { Text("${n}×$n") }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Go move hints", style = MaterialTheme.typography.labelLarge, color = ShoujoText)
            Switch(checked = go.showHints, onCheckedChange = { onToggleGoHints() })
        }

        if (shogi.loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Starting Shogi…", color = ShoujoText)
            }
        } else {
            Button(
                onClick = onStartShogi,
                modifier = Modifier.fillMaxWidth(0.9f),
            ) { Text("Shogi (将棋) vs Aiko") }
        }

        if (go.loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("Starting Go…", color = ShoujoText)
            }
        } else {
            Button(
                onClick = onStartGo,
                modifier = Modifier.fillMaxWidth(0.9f),
            ) { Text("Go (囲碁) vs Aiko") }
        }

        if (!shogi.loading && !go.loading) {
            OutlinedButton(
                onClick = onShogiRules,
                modifier = Modifier.fillMaxWidth(0.9f),
            ) { Text("📖 Shogi Rules") }

            OutlinedButton(
                onClick = onGoRules,
                modifier = Modifier.fillMaxWidth(0.9f),
            ) { Text("📖 Go Rules") }
        }

        Text(
            "Engines run on Aiko-chan. Jetson Orin Nano: casual AI is fine; " +
                "optional YaneuraOu (Shogi) / KataGo (Go) when configured.",
            style = MaterialTheme.typography.bodySmall,
            color = ShoujoText.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
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
        RulesSection("What is Shogi?") {
            Text(
                "Shogi (将棋) is Japanese chess. Checkmate the King (玉). " +
                    "Captured pieces join your hand and can be dropped as your own.",
            )
        }
        RulesSection("Board & sides") {
            Text("9×9 board. Pick your side on the lobby: 先手 (Black, first) or 後手 (White, second).")
        }
        RulesSection("Pieces") {
            Text(
                "歩 Pawn · 香 Lance · 桂 Knight · 銀 Silver · 金 Gold · " +
                    "角 Bishop · 飛 Rook · 玉 King. Many promote (成) in the far three ranks.",
            )
        }
        RulesSection("How to play here") {
            Text(
                "Tap piece → destination. Hand tray for drops. Optional promote dialog. " +
                    "Clock shows remaining time when the server enables it. " +
                    "Aiko uses YaneuraOu when available, else casual moves.",
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
        RulesSection("What is Go?") {
            Text(
                "Go (囲碁 / 碁) is the classic territory game. Place stones on intersections. " +
                    "Surround empty area and capture by removing the opponent’s last liberty.",
            )
        }
        RulesSection("Board sizes") {
            Text("9×9 (fast), 13×13, or 19×19 (standard). This app defaults to 9×9.")
        }
        RulesSection("Basics") {
            Text(
                "• Black plays first.\n" +
                    "• Capture groups with no liberties.\n" +
                    "• Simple ko: cannot immediately recapture the single-stone ko point.\n" +
                    "• Two consecutive passes end the game (server status: finished).\n" +
                    "• Suicide moves are illegal.",
            )
        }
        RulesSection("How to play here") {
            Text(
                "1. Pick board size, side, and difficulty on the lobby.\n" +
                    "2. Tap Go (囲碁) vs Aiko.\n" +
                    "3. Tap an empty intersection (toggle hints on/off anytime).\n" +
                    "4. Pass when neither side wants to play.\n" +
                    "5. Aiko replies via KataGo if configured on the server; " +
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

@Composable
private fun HandTray(
    label: String,
    pieces: List<com.aiko.shogi.data.HandPiece>,
    selectedPiece: Char?,
    enabled: Boolean,
    onPiece: (Char) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = ShoujoText.copy(alpha = 0.75f))
        if (pieces.isEmpty()) {
            Text("(empty)", style = MaterialTheme.typography.bodySmall, color = ShoujoText.copy(alpha = 0.5f))
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pieces.forEach { hp ->
                    val selected = selectedPiece == hp.symbol
                    val url = KomaImages.handUrl(hp.symbol, hp.forBlack)
                    Box(
                        modifier = Modifier
                            .background(if (selected) BoardSelect else PieceWhite, RoundedCornerShape(8.dp))
                            .border(1.dp, if (selected) BoardLine else Color(0x33000000), RoundedCornerShape(8.dp))
                            .clickable(enabled = enabled) { onPiece(hp.symbol) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (url != null) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = hp.symbol.toString(),
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
) {
    val game = state.game ?: return
    val grid = remember(game.sfen) { SfenBoard.parseGrid(game.sfen) }
    val blackHand = remember(game.sfen) { SfenBoard.blackHand(game.sfen) }
    val whiteHand = remember(game.sfen) { SfenBoard.whiteHand(game.sfen) }
    val selectedHand = (state.selected as? Selection.Hand)?.piece
    val userSide = game.side.ifBlank { "black" }
    val canInteract = !state.loading && game.status == "playing" && game.turn == userSide
    val youLabel = if (userSide == "black") "先手 (Black)" else "後手 (White)"

    Text(
        buildString {
            append(if (game.turn == userSide) "Your turn" else "Aiko's turn")
            append(" · you: $youLabel")
            append(" · "); append(game.status)
            game.engine?.let { append(" · $it") }
        },
        fontWeight = FontWeight.Medium,
        color = ShoujoText,
    )
    ClockRow(
        blackMs = game.clock_black_ms,
        whiteMs = game.clock_white_ms,
        byoyomiMs = game.byoyomi_ms,
    )
    game.last_move?.let {
        Text(
            "Last: $it",
            style = MaterialTheme.typography.bodySmall,
            color = ShoujoText.copy(alpha = 0.7f),
        )
    }
    if (game.status != "playing") {
        Text(
            statusLine(game.status),
            fontWeight = FontWeight.Bold,
            color = ShoujoText,
        )
    }
    game.ai_comment?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = ShoujoText.copy(alpha = 0.85f))
    }
    Spacer(modifier = Modifier.height(6.dp))
    HandTray("Aiko's hand (後手)", whiteHand, null, false) {}
    Spacer(modifier = Modifier.height(6.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(458f / 500f)
            .background(BoardWood, RoundedCornerShape(8.dp))
            .border(2.dp, BoardLine, RoundedCornerShape(8.dp)),
    ) {
        AsyncImage(
            model = KomaImages.BOARD_LIGHT,
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
                            val url = KomaImages.url(cell)
                            if (url != null) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = SfenBoard.glyph(cell),
                                    modifier = Modifier.fillMaxSize(0.92f),
                                    contentScale = ContentScale.Fit,
                                )
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
    HandTray("Your hand (先手)", blackHand, selectedHand, canInteract, onHandPiece)
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedButton(onClick = onResign) { Text("Resign") }
}

@Composable
private fun ClockRow(blackMs: Long?, whiteMs: Long?, byoyomiMs: Long?) {
    if (blackMs == null && whiteMs == null) return
    Text(
        buildString {
            append("⏱ You: ${formatClock(blackMs)} · Aiko: ${formatClock(whiteMs)}")
            if (byoyomiMs != null && byoyomiMs > 0) append(" · byoyomi ${byoyomiMs / 1000}s")
        },
        style = MaterialTheme.typography.bodySmall,
        color = ShoujoText.copy(alpha = 0.8f),
    )
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

@Composable
private fun GoGameBoard(
    state: GoUiState,
    hints: Set<Pair<Int, Int>>,
    last: Pair<Int, Int>?,
    onTap: (Int, Int) -> Unit,
    onPass: () -> Unit,
    onResign: () -> Unit,
    onToggleHints: () -> Unit,
) {
    val game = state.game ?: return
    val userSide = game.side.ifBlank { "black" }
    val canInteract = !state.loading && game.status == "playing" && game.turn == userSide
    val youLabel = if (userSide == "black") "Black ⚫" else "White ⚪"

    Text(
        buildString {
            append(if (game.turn == userSide) "Your turn ($youLabel)" else "Aiko's turn")
            append(" · ${game.size}×${game.size} · "); append(game.status)
            game.engine?.let { append(" · $it") }
        },
        fontWeight = FontWeight.Medium,
        color = ShoujoText,
    )
    Text(
        "Captures — B: ${game.captured_black}  W: ${game.captured_white} · Moves: ${game.moves.size}",
        style = MaterialTheme.typography.bodySmall,
        color = ShoujoText.copy(alpha = 0.8f),
    )
    game.last_move?.let {
        Text(
            "Last: $it",
            style = MaterialTheme.typography.bodySmall,
            color = ShoujoText.copy(alpha = 0.7f),
        )
    }
    game.ai_comment?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = ShoujoText.copy(alpha = 0.85f))
    }
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
        OutlinedButton(onClick = onResign) { Text("Resign") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Hints", style = MaterialTheme.typography.bodySmall, color = ShoujoText)
            Spacer(modifier = Modifier.size(4.dp))
            Switch(checked = state.showHints, onCheckedChange = { onToggleHints() })
        }
    }
    if (game.status != "playing") {
        Spacer(modifier = Modifier.height(4.dp))
        Text(statusLine(game.status), fontWeight = FontWeight.Bold, color = ShoujoText)
    }
}
