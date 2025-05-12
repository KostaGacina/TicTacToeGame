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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import java.lang.reflect.Array.getInt


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TicTacToeGameTheme {
                ScaffoldExample()
            }
        }
    }
}

@Composable
fun Title(){
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

@Preview
@Composable
fun ScaffoldExample() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE) }

    // Load initial scores
    var playerAScore by remember { mutableIntStateOf(prefs.getInt("score_playerA", 0)) }
    var playerBScore by remember { mutableIntStateOf(prefs.getInt("score_playerB", 0)) }
    fun resetScores(){
        prefs.edit().apply(){
            putInt("score_playerA", 0)
            putInt("score_playerB", 0)
            apply()
        }
        playerAScore = 0
        playerBScore = 0
    }

    // Refresh when returning from GameActivity
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
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
            Row{
                Text(
                    modifier = Modifier
                        .padding(start = 70.dp),
                    text = "Player A:",
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.displayLarge,
                    fontFamily = AudiowideFontFamily
                )
                Text(
                    modifier = Modifier
                        .padding(start = 130.dp),
                    text = "$playerAScore",
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.displayLarge,
                    fontFamily = AudiowideFontFamily
                )
            }
            Row {
                Text(
                    modifier = Modifier
                        .padding(start = 70.dp),
                    text = "Player B:",
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.displayLarge,
                    fontFamily = AudiowideFontFamily
                )
                Text(
                    modifier = Modifier
                        .padding(start = 130.dp),
                    text = "$playerBScore",
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.displayLarge,
                    fontFamily = AudiowideFontFamily
                )
            }
            Column (
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ){
                Button(
                    onClick = {
                        context.startActivity(Intent(context, GameActivity::class.java))
                    },
                ) {
                    Text(
                        text="Start Game",
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = AudiowideFontFamily
                    )
                }
                Button(
                    onClick = {
                        resetScores()
                    },
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Text(
                        text="Reset Scores",
                        style = MaterialTheme.typography.displaySmall,
                        fontFamily = AudiowideFontFamily
                    )
                }
            }
        }
    }
}
