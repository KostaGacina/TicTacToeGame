const websocket = require("websocket");
const http = require("http");

const server = http.createServer();
server.listen(process.env.PORT || 8080, () => {
  console.log(`Server listening on port ${process.env.PORT || 8080}`);
});
const wss = new websocket.server({ httpServer: server });
const clients = new Map(); // Stores active connections
const players = new Map(); // Maps clientId to player number
const playerConnections = [null, null]; // Tracks connections by player number

let game = {
  board: Array(9).fill(null),
  playerTurn: 0,
  winner: null,
  isDraw: false,
};

function resetGame() {
  game = {
    board: Array(9).fill(null),
    playerTurn: 0,
    winner: null,
    isDraw: false,
  };
}

function checkWin(board) {
  const lines = [
    [0, 1, 2],
    [3, 4, 5],
    [6, 7, 8], // rows
    [0, 3, 6],
    [1, 4, 7],
    [2, 5, 8], // columns
    [0, 4, 8],
    [2, 4, 6], // diagonals
  ];
  for (let [a, b, c] of lines) {
    if (board[a] && board[a] === board[b] && board[a] === board[c]) {
      return board[a];
    }
  }
  return null;
}

function checkDraw(board) {
  return board.every((cell) => cell !== null);
}

function broadcast(data) {
  const message = JSON.stringify(data);
  clients.forEach((client) => {
    if (client.connected) {
      client.sendUTF(message);
    }
  });
}

function reorganizePlayers() {
  // If only one player remains, make them player 0
  if (clients.size === 1) {
    const [remainingClientId] = clients.keys();
    const remainingConnection = clients.get(remainingClientId);

    // Clear existing mappings
    players.clear();
    playerConnections.fill(null);

    // Reassign as player 0
    players.set(remainingClientId, 0);
    playerConnections[0] = remainingConnection;

    console.log(`Reorganized players - ${remainingClientId} is now player 0`);

    // Notify the remaining player
    remainingConnection.sendUTF(
      JSON.stringify({
        type: "playerReassigned",
        payload: { newPlayerNumber: 0 },
      }),
    );
  }
}

wss.on("request", (req) => {
  if (clients.size >= 2) {
    req.reject(403, "Only two players are allowed.");
    return;
  }

  const connection = req.accept();
  const clientId = Math.random().toString(36).substring(2, 15);
  const playerNumber = clients.size; // 0 or 1

  clients.set(clientId, connection);
  players.set(clientId, playerNumber);
  playerConnections[playerNumber] = connection;

  console.log(`Client ${clientId} connected as player ${playerNumber}`);

  connection.sendUTF(
    JSON.stringify({
      type: "connected",
      payload: { clientId, player: playerNumber },
    }),
  );

  // Send the current game state to the newly connected player
  connection.sendUTF(JSON.stringify({ type: "updateGame", payload: game }));

  connection.on("message", (message) => {
    try {
      const data = JSON.parse(message.utf8Data);
      const { type, payload } = data;

      switch (type) {
        case "makeMove": {
          const { position, player } = payload;

          // Validate the move
          if (
            game.winner ||
            game.isDraw ||
            position < 0 ||
            position > 8 ||
            game.board[position] !== null ||
            game.playerTurn !== player
          ) {
            break;
          }

          // Process the move
          game.board[position] = player === 0 ? "X" : "O";
          game.playerTurn = 1 - player;

          // Check game state
          const winnerSymbol = checkWin(game.board);
          if (winnerSymbol) {
            game.winner = winnerSymbol;
          } else if (checkDraw(game.board)) {
            game.isDraw = true;
          }

          broadcast({ type: "updateGame", payload: game });

          if (game.winner || game.isDraw) {
            // Only broadcast gameOver, do NOT reset game immediately
            broadcast({
              type: "gameOver",
              payload: { winner: game.winner, isDraw: game.isDraw },
            });
            // The game state (game.board, game.winner, game.isDraw) will remain
            // in its finished state until a client requests a reset.
          }
          break;
        }
        case "requestGameReset": {
          // This new message type is sent by the client when "Play Again" is clicked
          if (clients.size === 2) {
            // Only allow reset if both players are connected
            resetGame();
            broadcast({ type: "updateGame", payload: game });
            console.log("Game reset requested by client.");
          } else {
            // If only one player is present and tries to reset, you might want to handle this
            // (e.g., send a message back to the client that they need an opponent)
            console.log(
              "Reset request ignored: not enough players for a new game.",
            );
          }
          break;
        }
      }
    } catch (err) {
      console.error("Invalid message: ", err);
    }
  });

  connection.on("close", () => {
    console.log(`Client ${clientId} disconnected`);

    // Remove the disconnected player
    const disconnectedPlayerNumber = players.get(clientId);
    clients.delete(clientId);
    players.delete(clientId);
    playerConnections[disconnectedPlayerNumber] = null;

    // Immediately reset the game if someone disconnects
    resetGame();
    // Notify all players that someone disconnected and the game is reset
    broadcast({
      type: "playerDisconnected",
      payload: { disconnectedPlayer: disconnectedPlayerNumber },
    });
    // Send the updated (reset) game state
    broadcast({ type: "updateGame", payload: game });

    // Reorganize players if one remains (this will also send playerReassigned)
    if (clients.size === 1) {
      reorganizePlayers();
    }
  });

  connection.on("error", (err) => {
    console.error("Connection error: ", err);
  });
});

console.log("WebSocket server started on port 8080");
//{
//  "type": "makeMove",
//  "payload": {
//    "position": 4,
//    "player": 0
//  }
//}
