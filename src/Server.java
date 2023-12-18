import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class Server {
    private static final int PORT = 8888;
    private static final int BOARD_SIZE = 3;

    private char[][] board;
    private char currentPlayer;
    private PrintWriter[] playerWriters;
    private int numPlayers;

    public Server() {
        board = new char[BOARD_SIZE][BOARD_SIZE];
        currentPlayer = 'X';
        playerWriters = new PrintWriter[2];
        numPlayers = 0;
        initializeBoard();
    }

    private void initializeBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            for (int j = 0; j < BOARD_SIZE; j++) {
                board[i][j] = '-';
            }
        }
    }

    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running on port " + PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New player connected");

                if (numPlayers >= 2) {
                    System.out.println("Maximum number of players reached. Rejecting connection.");
                    socket.close();
                    continue;
                }

                playerWriters[numPlayers] = new PrintWriter(socket.getOutputStream(), true);
                numPlayers++;

                Thread playerThread = new Thread(new PlayerHandler(socket, numPlayers));
                playerThread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String message) {
        for (int i = 0; i < numPlayers; i++) {
            playerWriters[i].println(message);
        }
    }

    private synchronized boolean makeMove(int row, int col, char player) {
        if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
            return false;
        }

        if (board[row][col] == '-') {
            board[row][col] = player;
            return true;
        }

        return false;
    }

    private boolean checkWinCondition(char player) {
        for (int i = 0; i < BOARD_SIZE; i++) {
            // Check rows
            if (board[i][0] == player && board[i][1] == player && board[i][2] == player) {
                return true;
            }

            // Check columns
            if (board[0][i] == player && board[1][i] == player && board[2][i] == player) {
                return true;
            }
        }

        // Check diagonals
        if (board[0][0] == player && board[1][1] == player && board[2][2] == player) {
            return true;
        }
        if (board[0][2] == player && board[1][1] == player && board[2][0] == player) {
            return true;
        }

        return false;
    }

    private class PlayerHandler implements Runnable {
        private Socket socket;
        private int playerId;
        private Scanner playerReader;
        private PrintWriter playerWriter;

        public PlayerHandler(Socket socket, int playerId) {
            this.socket = socket;
            this.playerId = playerId;
        }

        @Override
        public void run() {
            try {
                playerReader = new Scanner(socket.getInputStream());
                playerWriter = playerWriters[playerId - 1];

                playerWriter.println("Welcome to Tic-Tac-Toe! You are player " + playerId);

                while (true) {
                    int row = playerReader.nextInt();
                    int col = playerReader.nextInt();

                    boolean validMove = makeMove(row, col, currentPlayer);

                    if (validMove) {
                        broadcastMessage("MOVE " + row + " " + col + " " + currentPlayer);

                        if (checkWinCondition(currentPlayer)) {
                            broadcastMessage("WIN " + currentPlayer);
                            break;
                        }

                        currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
                    } else {
                        playerWriter.println("Invalid move. Try again.");
                    }
                }

                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.run();
    }
}