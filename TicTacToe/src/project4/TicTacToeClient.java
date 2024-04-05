package project4;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import javafx.application.Platform;

public class TicTacToeClient extends Application {

    private DataOutputStream toServer = null;
    private DataInputStream fromServer = null;
    private int clientId;
    private boolean isTurn = false;//tracks which players turn it is

    //global javaFX elements so multiple methods can access them
    private Scene scene;
    private TextArea ta = new TextArea();

    @Override
    public void start(Stage primaryStage) {

        //makes a 3x3 gridpane of buttons
        GridPane ticTacToeGrid = createTicTacToeGrid();

        VBox topVBox = new VBox(10);
        topVBox.getChildren().addAll(ticTacToeGrid);

        BorderPane mainPane = new BorderPane();       
        
        mainPane.setCenter(ta);
        mainPane.setTop(topVBox);

        scene = new Scene(mainPane, 450, 300);
        primaryStage.setTitle("Tic-Tac-Toe Client");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        ta.setEditable(false);

        //when the GUI window is closed, this makes sure the client stops running
        primaryStage.setOnCloseRequest(event -> {
            Platform.exit();
            System.exit(0);
        });

        try {
            //socket to connect to the server
            Socket socket = new Socket("localhost", 8000);

            //input stream to receive data from the server
            fromServer = new DataInputStream(socket.getInputStream());

            //output stream to send data to the server
            toServer = new DataOutputStream(socket.getOutputStream());

            //get client id from the server. If its 1, they play first
            clientId = fromServer.readInt();
            if (clientId == 1) {
                isTurn = true;
            }
            ta.appendText("Welcome player " + clientId + '\n');
            
            if(clientId == 1)
                ta.appendText("You are 'X'" + '\n');
            else
                ta.appendText("You are 'O'" + '\n');

            //continuously receive messages from the server
            new Thread(() -> {
                try {
                    while (true) {

                        String message = fromServer.readUTF();

                        //if the server broadcasts 'switch', then players swap turns
                        if ("switch".equals(message)) {
                            isTurn = !isTurn;
                        } else if (message.charAt(1) == ',') {
                            //if ',' at index 1, its a message saying which sqaure was clicked and by who
                            String[] parts = message.split(",");
                            int row = Integer.parseInt(parts[0]);
                            int col = Integer.parseInt(parts[1]);
                            int player = Integer.parseInt(parts[2]);

                            handleButton(row, col, player);

                        } else {
                            //print message to text area
                            ta.appendText(message + '\n');
                        }

                        if ("GAME OVER! RE-LAUNCH TO PLAY AGAIN".equals(message)) {
                            //disable the grid from being clicked and close connection
                            ticTacToeGrid.setDisable(true);
                            fromServer.close();
                            toServer.close();
                        }

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException ex) {
            ta.appendText(ex.toString() + '\n');
        }
    }

    private GridPane createTicTacToeGrid() {
        GridPane gridPane = new GridPane();
        gridPane.setAlignment(Pos.CENTER);
        //adjust gap between buttons
        gridPane.setPadding(new Insets(4));
        gridPane.setHgap(1);
        gridPane.setVgap(1);
        int buttonNum = 0;//index of button within 3x3 grid

        // Create buttons
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Button button = new Button();
                button.setId("button_" + buttonNum++);
                button.setMinSize(50, 50);

                // Create effectively final variables for row and col
                final int finalRow = row;
                final int finalCol = col;

                // Add an action handler for the button
                button.setOnAction(e -> handleButton(finalRow, finalCol, 0));

                //add button as child to gridPane
                gridPane.add(button, col, row);
            }
        }

        return gridPane;
    }

    private void handleButton(int row, int col, int client) {

        if (client == 0) {//send to all players
            try {
                // Send the information to the server
                if (isTurn) {
                    toServer.writeUTF(row + "," + col);
                } else {
                    ta.appendText("Not your turn!" + '\n');
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {//send to specific player

            /*
            The button grid is indexed like:
            
            0 1 2
            3 4 5
            6 7 8
            
            and this formula gets the index using the row and column
             */
            int index = row * 3 + col;
            Button button = (Button) scene.lookup("#button_" + index);

            //Platform.runLater needed to change javaFX elements
            Platform.runLater(() -> {

                if (client == 1) {//player 1 = X
                    button.setStyle("-fx-text-fill: red; -fx-font-size: 20;");
                    button.setText("X");

                } else {//player 2 = O
                    button.setStyle("-fx-text-fill: blue; -fx-font-size: 20;");
                    button.setText("O");
                }
            });
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
