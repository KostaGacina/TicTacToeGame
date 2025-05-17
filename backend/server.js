const websocket = require("websocket");
const http = require("http");

const server = http.createServer();
server.listen(8080);
const wss = new websocket.server({ httpServer: server });
const clients = new Map();
const players = new Map();
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
    [6, 7, 8],
    [0, 3, 6],
    [1, 4, 7],
    [2, 5, 8],
    [0, 4, 8],
    [2, 4, 6],
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

wss.on("request", (req) => {
  if (clients.size >= 2) {
    req.reject(403, "Only two players are allowed.");
    return;
  }

  const connection = req.accept();
  const clientId = Math.random().toString(36).substring(2, 15);
  const playerNumber = clients.size;

  clients.set(clientId, connection);
  players.set(clientId, playerNumber);

  console.log(`Client ${clientId} connected as player ${playerNumber}`);

  connection.sendUTF(
    JSON.stringify({
      type: "connected",
      payload: { clientId, player: playerNumber },
    }),
  );

  connection.on("message", (message) => {
    try {
      const data = JSON.parse(message.utf8Data);
      const { type, payload } = data;

      switch (type) {
        case "makeMove": {
          const { position, player } = payload;

          if (
            game.winner ||
            game.isDraw ||
            game.board[position] !== null ||
            game.playerTurn !== player
          )
            break;

          game.board[position] = player === 0 ? "X" : "O";
          game.playerTurn = 1 - player;

          const winnerSymbol = checkWin(game.board);
          if (winnerSymbol) {
            game.winner = winnerSymbol;
          } else if (checkDraw(game.board)) {
            game.isDraw = true;
          }

          broadcast({ type: "updateGame", payload: game });

          if (game.winner || game.isDraw) {
            setTimeout(() => {
              broadcast({
                type: "gameOver",
                payload: { winner: game.winner, isDraw: game.isDraw },
              });
              resetGame();
              broadcast({ type: "updateGame", payload: game });
            }, 1000);
          }
          break;
        }
      }
    } catch (err) {
      console.error("Invalid message: ", err);
    }
  });

  connection.on("close", () => {
    clients.delete(clientId);
    players.delete(clientId);
    console.log(`Client ${clientId} disconnected`);
    resetGame();
    broadcast({ type: "updateGame", payload: game });
  });

  connection.on("error", (err) => {
    console.error("connection error: ", err);
  });
});

console.log("WebSocket server started on port 8080");
