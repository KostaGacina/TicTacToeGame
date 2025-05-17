package net.costa.tictactoegame

import android.app.AlertDialog
import android.app.GameState
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.costa.tictactoegame.ui.theme.AudiowideFontFamily
import net.costa.tictactoegame.ui.theme.TicTacToeGameTheme
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject
import java.nio.file.WatchEvent
//192.168.1.19
class MultiplayerActivity : ComponentActivity() {
    private var webSocket: WebSocket?= null
    private val serverUrl = "ws://192.168.1.19:8080"
    private var clientId by mutableStateOf("")
    private var playerNumber by mutableIntStateOf(-1)
    private var gameState by mutableStateOf(List(9) {null as String?})
    private var isMyTurn by mutableStateOf(false)
    private var statusText by mutableStateOf("Connecting to server...")
    private var showGameOverDialog by mutableStateOf(false)
    private var gameOverMessage by mutableStateOf("")
    private var winner by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val client = OkHttpClient()
        val request = Request.Builder().url(serverUrl).build()
        val listener = object: WebSocketListener(){
            override fun onOpen(webSocket: WebSocket, response: Response) {
                runOnUiThread { statusText = "Connected to server!" }
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                runOnUiThread {
                    try {
                        val json = JSONObject(text)
                        when (json.getString("type")){
                            "connected" -> {
                                clientId = json.getJSONObject("payload").getString("clientId")
                                playerNumber = json.getJSONObject("payload").getInt("player")
                                statusText = "You are Player ${if (playerNumber==0) "X" else "O"}."
                                isMyTurn = playerNumber == 0
                            }
                            "updateGame" ->{
                                val board = json.getJSONObject("payload").getJSONArray("board")
                                val turn = json.getJSONObject("payload").getInt("playerTurn")
                                val newGameState = mutableListOf<String?>()
                                for (i in 0 until board.length()){
                                    newGameState.add(board.getString(i).takeIf { it != "null" })
                                }
                                gameState = newGameState
                                isMyTurn = turn == playerNumber
                                if (json.getJSONObject("payload").getString("winner") != "null"){
                                    winner = json.getJSONObject("payload").getString("winner")
                                    gameOverMessage = "${if (winner == (if(playerNumber == 0) "X" else "O")) "You" else winner} won!"
                                    showGameOverDialog = true
                                } else if (json.getJSONObject("payload").getBoolean("isDraw")){
                                    gameOverMessage = "It's a draw!"
                                    showGameOverDialog = true
                                }else{
                                    statusText = if (isMyTurn) "Your turn." else "${if(turn == 0) "X" else "O"}'s turn."
                                    winner = null
                                }
                            }
                            "gameOver" -> {
                                val winnerSymbol = json.getJSONObject("payload").optString("winner")
                                val isDraw = json.getJSONObject("payload").optBoolean("isDraw")
                                if(!winnerSymbol.isNullOrEmpty() && winnerSymbol != "null"){
                                    gameOverMessage = "${if(winnerSymbol == (if(playerNumber == 0) "X" else "O")) "You" else winnerSymbol} won!"
                                }else if (isDraw){
                                    gameOverMessage = "It's a draw!"
                                    winner = null
                                }
                                showGameOverDialog = true
                            }
                        }
                    }catch (e: Exception){
                        e.printStackTrace()
                    }
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                println("Received message (bytes): ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                println("Closing: $code $reason")
                this@MultiplayerActivity.webSocket = null
                runOnUiThread {
                    statusText = "Disconnected from server."
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                println("Closing: $code $reason")
                this@MultiplayerActivity.webSocket = null
                runOnUiThread {
                    statusText = "Disconnected from server."
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                t.printStackTrace()
                this@MultiplayerActivity.webSocket = null
                runOnUiThread {
                    statusText = "Connection failed: ${t.message}"
                }
            }
        }
        webSocket = client.newWebSocket(request, listener)
        client.dispatcher.executorService.shutdown()

        setContent {
            TicTacToeGameTheme {
                MultiplayerGameScreen(
                    gameState = gameState,
                    statusText = statusText,
                    isMyTurn = isMyTurn,
                    onMakeMove = {position ->
                        Log.d("MultiplayerGameScreen", "onMakeMove called with position: $position")
                        if (isMyTurn && gameState[position] == null && winner == null && !showGameOverDialog){
                            val move = JSONObject().apply {
                                put("type", "makeMove")
                                put("payload", JSONObject().apply{
                                    put("position", position)
                                    put("player", playerNumber)
                                })
                            }.toString()
                            webSocket?.send(move)
                        }
                    },
                    onExit = {
                        webSocket?.close(1000, "User initiated close")
                        setResult(RESULT_OK)
                        finish()
                    },
                    showGameOverDialog = showGameOverDialog,
                    gameOverMessage = gameOverMessage,
                    onResetGame = {
                        val resetMessage = JSONObject().apply {
                            put("type", "resetGame")
                        }.toString()
                        webSocket?.send(resetMessage)
                        showGameOverDialog = false
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket?.close(1000, "Activity destroyed")
    }
}

@Composable
fun MultiplayerGameScreen(
    gameState: List<String?>,
    statusText: String,
    isMyTurn: Boolean,
    onMakeMove: (Int) -> Unit,
    onExit: () -> Unit,
    showGameOverDialog: Boolean,
    gameOverMessage: String,
    onResetGame:() -> Unit
){
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Title()
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = AudiowideFontFamily,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (row in 0..2){
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0..2){
                            val position = row*3 + col
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(enabled = isMyTurn && gameState[position] == null && !showGameOverDialog){
                                        onMakeMove(position)
                                    }
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ){
                                val content = gameState[position]
                                if(content == "X"){
                                    Icon(
                                        painter = painterResource(R.drawable.x),
                                        contentDescription = "X",
                                        modifier = Modifier.size(64.dp)
                                    )
                                }else if (content == "O"){
                                    Icon(
                                        painter = painterResource(R.drawable.o),
                                        contentDescription = "X",
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }//end of box
                        }
                    }//end of row
                }
            }
        }
        Button(
            onClick = onExit,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 32.dp)
        ) {
            Text(
                text = "Exit Game",
                style = MaterialTheme.typography.displaySmall,
                fontFamily = AudiowideFontFamily
            )
        }

        if (showGameOverDialog){
            AlertDialog(
                onDismissRequest = {},
                title = {Text("Game Over")},
                text = {Text(gameOverMessage)},
                confirmButton = {
                    Button(onClick = onResetGame) {
                        Text("Play Again")
                    }
                },
                dismissButton = {
                    Button(onClick = onExit) {
                        Text("Exit")
                    }
                }
            )
        }
    }
}




















