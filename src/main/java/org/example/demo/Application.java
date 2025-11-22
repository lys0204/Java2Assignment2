package org.example.demo;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;

import java.util.Optional;


public class Application extends javafx.application.Application {

    private NetworkClient client;

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Login Dialog
        TextInputDialog dialog = new TextInputDialog("player1");
        dialog.setTitle("QQ Farm Login");
        dialog.setHeaderText("Connect to Server");
        dialog.setContentText("Please enter your username:");

        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return; // User cancelled
        }
        String username = result.get();

        // 2. Connect to Server
        client = new NetworkClient();
        try {
            client.connect("localhost", 8888);
            client.sendLogin(username);
        } catch (Exception e) {
            System.err.println("Could not connect to server: " + e.getMessage());
            // Ideally show an alert here
            return;
        }

        // 3. Load UI
        FXMLLoader loader = new FXMLLoader(Application.class.getResource("board.fxml"));
        Parent root = loader.load();

        Controller controller = loader.getController();
        controller.init(client, username);

        Scene scene = new Scene(root);
        stage.setTitle("QQ Farm - " + username);
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            controller.shutdown();
            client.close();
            System.exit(0);
        });
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
