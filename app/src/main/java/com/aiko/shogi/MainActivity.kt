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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
    LaunchedEffect(Unit) { vm.refreshEngine() }

    // Promote dialog
    state.promoteChoice?.let { choice ->
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
        Spacer(Modifier = Modifier.height(8.dp))

        if (!state.inGame) {
            Lobby(
                state = state,
                baseUrl = baseUrl,
                onSaveUrl = onSaveUrl,
                onStart = { vm.startGame() },
                onRefreshEngine = { vm.refreshEngine() },
            )
        } else {
            GameBoard(
                state = state,
                hints = vm.hintSquares(),
                last = vm.lastMoveSquares(),
                onSquare = { r, c -> vm.onSquareTap(r, c) },
                onHandPiece = { vm.onHandPieceTap(it) },
                onResign = { vm.resign() },
            )
        }

        state.error?.let { err ->
            Spacer(Modifier = Modifier.height(8.dp))
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
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth(0.8f)) {
                Text("Start vs Aiko")
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
    Spacer(Modifier = Modifier.height(6.dp))

    // Opponent (White / Aiko) hand — top
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

    // Your (Black) hand — bottom, tappable for drops
    HandTray(
        label = "Your hand (先手) — tap piece, then square to drop",
        pieces = blackHand,
        selectedPiece = selectedHand,
        enabled = canInteract,
        onPiece = onHandPiece,
    )

    Spacer(Modifier = Modifier.height(12.dp))
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
