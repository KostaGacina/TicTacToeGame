package net.costa.tictactoegame

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.costa.tictactoegame.ui.theme.AudiowideFontFamily
import net.costa.tictactoegame.ui.theme.TicTacToeGameTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TicTacToeGameTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun Title() {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        text = "Tic Tac Toe Game",
        fontSize = 24.sp,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.displayLarge,
        fontFamily = AudiowideFontFamily
    )
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE) }

    // Load initial scores
    var playerAScore by remember { mutableIntStateOf(prefs.getInt("score_playerA", 0)) }
    var playerBScore by remember { mutableIntStateOf(prefs.getInt("score_playerB", 0)) }
    fun resetScores() {
        prefs.edit().apply {
            putInt("score_playerA", 0)
            putInt("score_playerB", 0)
            apply()
        }
        playerAScore = 0
        playerBScore = 0
    }

    // Refresh when returning from GameActivity or MultiplayerActivity
    DisposableEffect(Unit) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                "score_playerA" -> playerAScore = prefs.getInt(key, 0)
                "score_playerB" -> playerBScore = prefs.getInt(key, 0)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)

        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Title()
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    text = "Players Score",
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displayLarge,
                    fontFamily = AudiowideFontFamily
                )
                Row(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Player A:",
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = AudiowideFontFamily
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "$playerAScore",
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = AudiowideFontFamily
                    )
                }
                Row(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Player B:",
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = AudiowideFontFamily
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "$playerBScore",
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = AudiowideFontFamily
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(
                    onClick = {
                        context.startActivity(Intent(context, GameActivity::class.java))
                    },
                ) {
                    Text(
                        text = "Offline",
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = AudiowideFontFamily
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        context.startActivity(Intent(context, MultiplayerActivity::class.java))

                    }
                ) {
                    Text(
                        text = "Multiplayer",
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = AudiowideFontFamily
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        resetScores()
                    },
                ) {
                    Text(
                        text = "Reset Scores",
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = AudiowideFontFamily
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainScreen() {
    TicTacToeGameTheme {
        MainScreen()
    }
}
