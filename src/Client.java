import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import javax.swing.JOptionPane;

public class Client extends JFrame {
    private static final int SERVER_PORT = 8888;
    private void showGameWindow() {
        setVisible(false);
        setVisible(true);
    }
    private String showIPInput() {
        return JOptionPane.showInputDialog(this, "Введите IP адрес сервера:");
    }
    private JButton[][] buttons;
    private char currentPlayer;
    private PrintWriter serverWriter;
    private Scanner serverReader;
    private boolean gameEnded;
    public Client() {
        setTitle("Tic-Tac-Toe");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(300, 300);
        setLayout(new GridLayout(3, 3));

        buttons = new JButton[3][3];
        currentPlayer = 'X';
        serverWriter = null;
        serverReader = null;
        gameEnded = false;

        initializeButtons();
        connectToServer();

        setVisible(true);
    }
    private void initializeButtons() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                JButton button = new JButton();
                button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 48));
                button.addActionListener(new ButtonClickListener(i, j));
                buttons[i][j] = button;
                add(button);
            }
        }
    }
    private void connectToServer() {
        try {
            String serverIP = showIPInput();
            Socket socket = new Socket(serverIP, SERVER_PORT);
            serverWriter = new PrintWriter(socket.getOutputStream(), true);
            serverReader = new Scanner(socket.getInputStream());

            Thread serverThread = new Thread(new ServerListener());
            serverThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void makeMove(int row, int col) {
        serverWriter.println(row + " " + col);
    }
    private void processMessage(String message) {
        if (message.startsWith("MOVE")) {
            String[] parts = message.split(" ");
            int row = Integer.parseInt(parts[1]);
            int col = Integer.parseInt(parts[2]);
            char player = parts[3].charAt(0);

            buttons[row][col].setText(String.valueOf(player));
            buttons[row][col].setEnabled(false);
        } else if (message.startsWith("WIN") && !gameEnded) {
            String[] parts = message.split(" ");
            char player = parts[1].charAt(0);

            JOptionPane.showMessageDialog(this, "Player " + player + " wins!");
            resetBoard();

            if (currentPlayer == 'X') {
                currentPlayer = 'O';
            } else {
                currentPlayer = 'X';
            }
        } else if (message.startsWith("START")) {
            showGameWindow();
        } else if (message.startsWith("LOSE")) {
            gameEnded = true;
        } else {
            JOptionPane.showMessageDialog(this, message);
        }
    }
    private void resetBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(true);
            }
        }
        gameEnded = false;
    }
    private class ButtonClickListener implements ActionListener {
        private int row;
        private int col;

        public ButtonClickListener(int row, int col) {
            this.row = row;
            this.col = col;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            makeMove(row, col);
        }
    }
    private class ServerListener implements Runnable {
        @Override
        public void run() {
            while (true) {
                if (serverReader.hasNextLine()) {
                    String message = serverReader.nextLine();
                    processMessage(message);
                }
            }
        }
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Client());
    }
}