package net.costa.tictactoegame

import android.annotation.SuppressLint
import android.os.Bundle
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import net.costa.tictactoegame.ui.theme.AudiowideFontFamily
import net.costa.tictactoegame.ui.theme.TicTacToeGameTheme

class GameActivity : ComponentActivity() {
    private fun updateScore(player: String) {
        val prefs = getSharedPreferences("game_prefs", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putInt(player, prefs.getInt(player, 0) + 1)
        editor.apply() // Or editor.commit() if you need immediate write
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TicTacToeGameTheme {
                TicTacToeGameScreen(
                    onExit = {
                        setResult(RESULT_OK)
                        finish()
                    },
                    onPlayerWin = { winner ->
                        updateScore(if (winner == 0) "score_playerA" else "score_playerB")
                    }
                )
            }
        }
    }
}

@SuppressLint("RememberReturnType")
@Composable
fun TicTacToeGameScreen(onExit: () -> Unit, onPlayerWin: (Int) -> Unit) {
    var gameActive by remember { mutableStateOf(true) }
    var activePlayer by remember {mutableIntStateOf(0)} // 0 = X, 1 = O
    var gameState by remember { mutableStateOf(List(9) { 2 }) } // 2 = empty, 0 = X, 1 = O
    var statusText by remember { mutableStateOf("X's Turn - Tap to play") }
    var counter by remember {mutableIntStateOf(0)}

    // Win positions
    val winPositions = listOf(
        listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8), // rows
        listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8), // columns
        listOf(0, 4, 8), listOf(2, 4, 6)  // diagonals
    )


    fun playerTap(position: Int) {
        if (!gameActive) {
            // Reset game
            gameActive = true
            activePlayer = 0
            gameState = List(9) { 2 }
            statusText = "X's Turn - Tap to play"
            counter = 0
            return
        }

        // If the tapped position is empty
        if (gameState[position] == 2) {
            counter++

            // Check if it's the last box
            if (counter == 9) {
                gameActive = false
            }

            // Update game state
            val newGameState = gameState.toMutableList()
            newGameState[position] = activePlayer
            gameState = newGameState

            // Switch player
            activePlayer = if (activePlayer == 0) 1 else 0
            statusText = if (activePlayer == 0) "X's Turn - Tap to play" else "O's Turn - Tap to play"

            // Check for win if enough moves have been made
            if (counter > 4) {
                var winner: Int? = null

                for (winPos in winPositions) {
                    if (gameState[winPos[0]] == gameState[winPos[1]] &&
                        gameState[winPos[1]] == gameState[winPos[2]] &&
                        gameState[winPos[0]] != 2) {
                        winner = gameState[winPos[0]]
                        break
                    }
                }

                if (winner != null) {
                    statusText = if (winner == 0) "X has won" else "O has won"
                    gameActive = false
                    // Here you would call updateScore() if you want to keep score
                    onPlayerWin(winner)
                } else if (counter == 9) {
                    statusText = "Match Draw"
                    gameActive = false
                }
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Game Content (Top Part)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Title()

            // Game Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (row in 0..2) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0..2) {
                            val position = row * 3 + col
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { playerTap(position) }
                                    .background(Color.LightGray),
                                contentAlignment = Alignment.Center
                            ) {
                                when (gameState[position]) {
                                    0 -> Icon(
                                        painter = painterResource(R.drawable.x),
                                        contentDescription = "X",
                                        modifier = Modifier.size(64.dp)
                                    )
                                    1 -> Icon(
                                        painter = painterResource(R.drawable.o),
                                        contentDescription = "O",
                                        modifier = Modifier.size(64.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.displaySmall,
            fontFamily = AudiowideFontFamily,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // Exit Button (Bottom Part)
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
    }
}