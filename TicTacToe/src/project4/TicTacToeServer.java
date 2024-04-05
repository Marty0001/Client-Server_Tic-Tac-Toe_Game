package project4;

import java.io.*;
import java.net.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TicTacToeServer {

    private int connectedClients = 0;
    private static Map<Integer, DataOutputStream> clientStreams = new HashMap<>();
    private boolean gameOver = false;
    private int[][] board = {//keep track of open spaces and check for win/tie
        {0, 0, 0},
        {0, 0, 0},
        {0, 0, 0}
    };

    public static void main(String[] args) {
        new TicTacToeServer().startServer();
    }

    public void startServer() {
        try {
            //create a server socket
            ServerSocket serverSocket = new ServerSocket(8000);
            System.out.println("Tic-Tac-Toe server started at " + new Date());

            //look for clients until theres 2 players
            while (connectedClients < 2) {
                //listen for connection
                Socket socket = serverSocket.accept();

                connectedClients++;

                System.out.println("Starting thread for client " + connectedClients
                        + " at " + new Date());

                System.out.println("Client " + connectedClients + " connected");

                new Thread(new HandleClient(socket, connectedClients)).start();
            }

            //close the server socket after 2 players connect
            serverSocket.close();
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    class HandleClient implements Runnable {

        private Socket socket;
        private int clientId;

        /** Construct a thread */
        public HandleClient(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        /** Run a thread */
        public void run() {
            try {
                // Create data input and output streams
                DataInputStream inputFromClient = new DataInputStream(
                        socket.getInputStream());
                DataOutputStream outputToClient = new DataOutputStream(
                        socket.getOutputStream());

                // Inform the client about their assigned identifier
                outputToClient.writeInt(clientId);
                
                // Add the client's output stream to the map
                clientStreams.put(clientId, outputToClient);

                if (clientId == 2) {
                    broadcast(clientId, "Player 2 connected!");
                    broadcast(0, "GAME READY");
                }
                
                // Continuously serve the client
                while (!gameOver) {

                    //get row and col from the client
                    String message = inputFromClient.readUTF();

                    //convert message to row and col
                    String[] parts = message.split(",");
                    int row = Integer.parseInt(parts[0]);
                    int col = Integer.parseInt(parts[1]);

                    // if the chosen square is open and theres 2 players
                    if (board[row][col] == 0 && connectedClients == 2) {
                        board[row][col] = clientId;//if square is open, set that sqaure equal to the players client id

                        String chosenSquare = "Player " + clientId + ": (" + row + ", " + col + ")";

                        System.out.println(chosenSquare);

                        broadcast(clientId, chosenSquare);//display what sqaure the player picked
                        broadcast(0, "switch");//tell each client to switch turns
                        broadcast(0, row + "," + col + "," + clientId);//tell each client what sqaure to display x or o 

                        if (checkWin()) {
                            broadcast(0, "PLAYER " + clientId + " WINS!");
                            System.out.println("PLAYER " + clientId + " WINS!");
                            gameOver = true;
                        } else if (catsGame()) {
                            broadcast(0, "NO OPEN SPACES LEFT, ITS A DRAW!");
                            System.out.println("NO OPEN SPACES LEFT, ITS A DRAW!");
                            gameOver = true;
                        }
                    } else {
                        if (connectedClients < 2) {//case for if player 1 clicks square before player 2 connects
                            broadcast(0, "Waiting for another player...");
                        } else {
                            outputToClient.writeUTF("Choose an open Square!");// chosen square is already filled
                        }
                    }
                }

                broadcast(0, "GAME OVER! RE-LAUNCH TO PLAY AGAIN");
                System.out.println("GAME OVER!");
     
            } catch (IOException e) {
                // Handle the IOException that occurs when the client disconnects
                System.out.println("Client " + clientId + " disconnected.");
                connectedClients--; // Decrement the client count

                // Remove the client's output stream from the map
                clientStreams.remove(clientId);

                //close socket if no more clients are connected
                try {
                    socket.close();
                    return;
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private boolean checkWin() {

        //check each row for 3 in a row, also make sure its not 0 so game doesnt immidiatly end
        for (int row = 0; row < 3; row++) {
            if (board[row][0] != 0 && board[row][0] == board[row][1] && board[row][0] == board[row][2]) {
                return true;
            }
        }

        //check each column for 3 in a row
        for (int col = 0; col < 3; col++) {
            if (board[0][col] != 0 && board[0][col] == board[1][col] && board[0][col] == board[2][col]) {
                return true;
            }
        }

        //check diagonals
        if ((board[1][1] != 0 && board[1][1] == board[0][0] && board[1][1] == board[2][2])
                || (board[1][1] != 0 && board[1][1] == board[0][2] && board[1][1] == board[2][0])) {
            return true;
        }

        return false;
    }

    //tie, no open spaces left
    private boolean catsGame() {
        for (int[] row : board) {
            for (int value : row) {
                //if there is a 0, its an open square and not a tie yet
                if (value == 0) {
                    return false;
                }
            }
        }
        //if no 0's are found, all sqaures are filled and its a tie
        return true;
    }

    // Broadcast a message to all connected clients 
    //sender = 0 means send to all, anything else means it will not send to that client
    private void broadcast(int sender, String message) {
        for (Map.Entry<Integer, DataOutputStream> clients : clientStreams.entrySet()) {
            //only send message to clients who aren't the sender
            //This if statement is really only for displaying what the other player picked
            if (sender != clients.getKey()) {
                try {
                    DataOutputStream clientStream = clients.getValue();

                    //send message to client
                    clientStream.writeUTF(message);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
