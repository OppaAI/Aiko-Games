package com.aiko.shogi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aiko.shogi.data.SfenBoard
import com.aiko.shogi.data.ServerConfig
import com.aiko.shogi.data.remote.ShogiApi
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

enum class Screen { Lobby, Rules, Game }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AikoShogiTheme {
                var baseUrl by remember { mutableStateOf(ServerConfig.get(this@MainActivity)) }
                val api = remember(baseUrl) { buildApi(baseUrl) }
                val vm: ShogiViewModel = viewModel(
                    key = baseUrl,
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return ShogiViewModel(api) as T
                        }
                    },
                )
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    ShogiApp(
                        vm = vm,
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

    private fun buildApi(baseUrl: String): ShogiApi {
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ShogiApi::class.java)
    }
}

@Composable
fun ShogiApp(
    vm: ShogiViewModel,
    baseUrl: String,
    onSaveUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.ui.collectAsState()
    var screen by remember { mutableStateOf(Screen.Lobby) }

    LaunchedEffect(Unit) {
        vm.refreshEngine()
        vm.warmupEngine()
    }

    // Keep screen in sync when a game starts / ends from ViewModel
    LaunchedEffect(state.inGame) {
        if (state.inGame) screen = Screen.Game
        else if (screen == Screen.Game) screen = Screen.Lobby
    }

    // Promote dialog (only meaningful during a game)
    state.promoteChoice?.let {
        AlertDialog(
            onDismissRequest = { vm.dismissPromote() },
            title = { Text("Promote?") },
            text = {
                Text("This piece can promote (成). Choose promote or stay as-is.")
            },
            confirmButton = {
                Button(onClick = { vm.confirmPromote(true) }) {
                    Text("成 Promote")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { vm.confirmPromote(false) }) {
                    Text("不成 Stay")
                }
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
            "Aiko Shogi ♟️",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = ShoujoText,
        )
        Text(
            "vs Aiko · USI · ${ServerConfig.displayHost(baseUrl)}",
            style = MaterialTheme.typography.bodySmall,
            color = ShoujoText.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.height(8.dp))

        when (screen) {
            Screen.Lobby -> Lobby(
                state = state,
                baseUrl = baseUrl,
                onSaveUrl = onSaveUrl,
                onStart = {
                    vm.startGame()
                    // screen flips to Game via inGame LaunchedEffect
                },
                onRules = { screen = Screen.Rules },
                onRefreshEngine = { vm.refreshEngine() },
            )
            Screen.Rules -> RulesScreen(onBack = { screen = Screen.Lobby })
            Screen.Game -> GameBoard(
                state = state,
                hints = vm.hintSquares(),
                last = vm.lastMoveSquares(),
                onSquare = { r, c -> vm.onSquareTap(r, c) },
                onHandPiece = { vm.onHandPieceTap(it) },
                onResign = { vm.resign() },
            )
        }

        state.error?.let { err ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(err, color = Color(0xFFC62828), textAlign = TextAlign.Center)
            TextButton(onClick = { vm.clearError() }) { Text("Dismiss") }
        }
    }
}

@Composable
private fun Lobby(
    state: ShogiUiState,
    baseUrl: String,
    onSaveUrl: (String) -> Unit,
    onStart: () -> Unit,
    onRules: () -> Unit,
    onRefreshEngine: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var draft by remember(baseUrl) { mutableStateOf(baseUrl) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        val eng = when (state.engineOnline) {
            true -> "YaneuraOu online ✅"
            false -> "Engine offline — casual AI"
            null -> "Checking engine…"
        }
        Text(eng, color = ShoujoText)

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
                        onRefreshEngine()
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

        if (state.loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(0.85f),
            ) {
                Text("Start vs Aiko")
            }
            OutlinedButton(
                onClick = onRules,
                modifier = Modifier.fillMaxWidth(0.85f),
            ) {
                Text("📖 Rules & How to Play")
            }
        }
        Text(
            "You move first (先手). Tap a piece or hand piece, then a square.",
            style = MaterialTheme.typography.bodySmall,
            color = ShoujoText.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun RulesScreen(onBack: () -> Unit) {
    val scroll = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scroll),
        horizontalAlignment = Alignment.Start,
    ) {
        TextButton(onClick = onBack) { Text("← Back") }

        RulesSection("What is Shogi?") {
            Text(
                "Shogi (将棋) is Japanese chess. The goal is to checkmate the opponent’s King (玉). " +
                    "Unlike Western chess, captured pieces join your hand and can be dropped back onto the board as your own.",
            )
        }

        RulesSection("Board & sides") {
            Text("• 9×9 board.\n" +
                "• You are 先手 (Black) and move first.\n" +
                "• Aiko is 後手 (White).\n" +
                "• Pieces face the opponent; promoted pieces keep their side’s color.")
        }

        RulesSection("Pieces") {
            Text(
                "歩 Pawn — one square forward; promotes to と (tokin, moves like Gold).\n" +
                    "香 Lance — any number forward; promotes to 成香 (Gold-like).\n" +
                    "桂 Knight — two forward + one side; jumps; promotes to 成桂 (Gold-like).\n" +
                    "銀 Silver — forward and all diagonals; promotes to 成銀 (Gold-like).\n" +
                    "金 Gold — forward, sides, and forward-diagonals (no back-diagonals).\n" +
                    "角 Bishop — any diagonal; promotes to 馬 (Dragon Horse: Bishop + adjacent).\n" +
                    "飛 Rook — any orthogonal; promotes to 龍 (Dragon King: Rook + adjacent).\n" +
                    "玉 King — one square any direction.",
            )
        }

        RulesSection("Promotion (成)") {
            Text(
                "The three ranks farthest from you are the promotion zone.\n" +
                    "If a piece moves into, out of, or within that zone, you may often promote.\n" +
                    "Some moves force promotion (e.g. Pawn/Lance/Knight that would have no legal move next).\n" +
                    "In this app: when both options are legal, a dialog asks 成 Promote or 不成 Stay.",
            )
        }

        RulesSection("Captures & drops (持ち駒)") {
            Text(
                "• Capture by moving onto an enemy piece; it goes to your hand.\n" +
                    "• On your turn you may either move a board piece or drop a hand piece on an empty square.\n" +
                    "• Drops use USI like B*5e (Bishop drop on 5e).\n" +
                    "• Nifu: you cannot drop a Pawn on a file that already has your unpromoted Pawn.\n" +
                    "• You cannot drop a Pawn for immediate checkmate (打ち歩詰め).\n" +
                    "• Knight/Lance/Pawn cannot be dropped where they would have no forward move.",
            )
        }

        RulesSection("How to play in this app") {
            Text(
                "1. Set your Aiko-chan server URL if needed.\n" +
                    "2. Tap Start vs Aiko.\n" +
                    "3. Board move: tap your piece → highlighted squares → tap destination.\n" +
                    "4. Drop: tap a piece in Your hand → highlighted squares → tap empty square.\n" +
                    "5. If promotion is optional, choose 成 or 不成.\n" +
                    "6. Aiko replies automatically (YaneuraOu when available).\n" +
                    "7. Resign returns to the lobby.",
            )
        }

        RulesSection("Winning") {
            Text(
                "Checkmate (詰み): the King is in check and has no legal escape, capture, or block.\n" +
                    "Other endings (stalemate / draw rules) are reported by the server when they occur.",
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
        ) {
            Text("Back to lobby")
        }
    }
}

@Composable
private fun RulesSection(title: String, body: @Composable () -> Unit) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = ShoujoText,
    )
    Spacer(modifier = Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Column {
            body()
        }
    }
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
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = ShoujoText.copy(alpha = 0.75f),
        )
        if (pieces.isEmpty()) {
            Text(
                "(empty)",
                style = MaterialTheme.typography.bodySmall,
                color = ShoujoText.copy(alpha = 0.5f),
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pieces.forEach { hp ->
                    val selected = selectedPiece == hp.symbol
                    Box(
                        modifier = Modifier
                            .background(
                                if (selected) BoardSelect else Color.White,
                                RoundedCornerShape(8.dp),
                            )
                            .border(
                                1.dp,
                                if (selected) BoardLine else Color(0x33000000),
                                RoundedCornerShape(8.dp),
                            )
                            .clickable(enabled = enabled) { onPiece(hp.symbol) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = buildString {
                                append(SfenBoard.glyphFor(hp.symbol))
                                if (hp.count > 1) append("×${hp.count}")
                            },
                            fontWeight = FontWeight.Bold,
                            color = if (hp.forBlack) PieceBlack else PieceWhite,
                            fontSize = 16.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameBoard(
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
    val canInteract = !state.loading && game.status == "playing" && game.turn == "black"

    Text(
        buildString {
            append(if (game.turn == "black") "Your turn" else "Aiko's turn")
            append(" · ")
            append(game.status)
            game.engine?.let { append(" · $it") }
        },
        fontWeight = FontWeight.Medium,
        color = ShoujoText,
    )
    game.ai_comment?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = ShoujoText.copy(alpha = 0.85f))
    }
    Spacer(modifier = Modifier.height(6.dp))

    HandTray(
        label = "Aiko's hand (後手)",
        pieces = whiteHand,
        selectedPiece = null,
        enabled = false,
        onPiece = {},
    )
    Spacer(modifier = Modifier.height(6.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(BoardWood, RoundedCornerShape(8.dp))
            .border(2.dp, BoardLine, RoundedCornerShape(8.dp))
            .padding(4.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            for (row in 0 until 9) {
                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0 until 9) {
                        val cell = grid[row][col]
                        val sel = (state.selected as? Selection.Square)?.let {
                            it.row == row && it.col == col
                        } == true
                        val hint = (row to col) in hints
                        val lastMv = (row to col) in last
                        val bg = when {
                            sel -> BoardSelect
                            hint -> BoardHint
                            lastMv -> BoardLast
                            else -> Color.Transparent
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(bg)
                                .border(0.5.dp, BoardLine)
                                .clickable(enabled = canInteract) {
                                    onSquare(row, col)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            val g = SfenBoard.glyph(cell)
                            if (g.isNotEmpty() && cell != null) {
                                val black = SfenBoard.isBlack(cell)
                                Text(
                                    text = g,
                                    fontSize = if (g.length > 1) 12.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (black) PieceBlack else PieceWhite,
                                    style = if (!black) {
                                        androidx.compose.ui.text.TextStyle(
                                            shadow = androidx.compose.ui.graphics.Shadow(
                                                color = PieceWhiteStroke,
                                                blurRadius = 1f,
                                            ),
                                        )
                                    } else androidx.compose.ui.text.TextStyle.Default,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (state.loading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x44000000)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(36.dp))
            }
        }
    }

    Spacer(modifier = Modifier.height(6.dp))

    HandTray(
        label = "Your hand (先手) — tap piece, then square to drop",
        pieces = blackHand,
        selectedPiece = selectedHand,
        enabled = canInteract,
        onPiece = onHandPiece,
    )

    Spacer(modifier = Modifier.height(12.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onResign) { Text("Resign") }
        if (game.status != "playing") {
            Text(
                when (game.status) {
                    "checkmate" -> "Checkmate!"
                    "stalemate" -> "Stalemate"
                    "draw" -> "Draw"
                    else -> game.status
                },
                fontWeight = FontWeight.Bold,
                color = ShoujoText,
            )
        }
    }
}
