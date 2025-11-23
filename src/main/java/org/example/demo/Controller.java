package org.example.demo;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import java.util.regex.Pattern;

public class Controller {

    @FXML
    private GridPane gameBoard;
    @FXML
    private Label coinsLabel;
    @FXML
    private Label currentUserLabel;
    @FXML
    private TextField friendField;

    @FXML
    private Button plantButton;
    @FXML
    private Button harvestButton;
    @FXML
    private Button stealButton;
    @FXML
    private Button reconnectButton;

    private NetworkClient client;
    private String myUsername;
    private String viewingUser; // Currently viewing farm owner
    
    private ToggleButton[][] cells;
    private int selectedRow = -1;
    private int selectedCol = -1;
    private Timeline refreshTimeline;

    private static final int ROWS = 4;
    private static final int COLS = 4;
    
    private String currentCoins = "0"; // Store coins locally to avoid overwrite by message
    private String currentStatus = "Ready.";

    public void init(NetworkClient client, String username) {
        this.client = client;
        this.myUsername = username;
        this.viewingUser = username;
        
        currentUserLabel.setText(username);
        
        createBoard();
        setupNetworkCallbacks();
        startAutoRefresh();
        
        // Initial query to get my own state
        client.sendQuery(myUsername);
        updateButtonStates();
    }

    private void setupNetworkCallbacks() {
        client.setOnStateReceived(this::updateBoardFromState);
        client.setOnMessageReceived(this::updateStatus);
        client.setOnConnectionLost(this::handleConnectionLost);
    }
    
    private void handleConnectionLost() {
        updateStatus("CONNECTION LOST!");
        plantButton.setDisable(true);
        harvestButton.setDisable(true);
        stealButton.setDisable(true);
        reconnectButton.setDisable(false); // Enable reconnect button
    }
    
    @FXML
    private void handleReconnect() {
        updateStatus("Attempting to reconnect...");
        reconnectButton.setDisable(true);
        
        new Thread(() -> {
            try {
                client.connect(client.getHost(), client.getPort());
                // Re-login
                client.sendLogin(myUsername);
                // Re-query
                client.sendQuery(viewingUser);
                
                // Back to UI thread to update
                javafx.application.Platform.runLater(() -> {
                    updateStatus("Reconnected successfully!");
                    updateButtonStates();
                });
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    updateStatus("Reconnect failed: " + e.getMessage());
                    reconnectButton.setDisable(false); // Retry allowed
                });
            }
        }).start();
    }
    
    private void startAutoRefresh() {
        refreshTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (client != null && client.isConnected() && viewingUser != null) {
                 client.sendQuery(viewingUser);
            }
        }));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    private void createBoard() {
        gameBoard.getChildren().clear();
        cells = new ToggleButton[ROWS][COLS];
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                ToggleButton cell = new ToggleButton();
                cell.setPrefSize(60, 60);
                cell.getStyleClass().add("plot-button");
                int r = row;
                int c = col;
                cell.setOnAction(event -> {
                    selectedRow = r;
                    selectedCol = c;
                    refreshSelection();
                    updateStatus("Selected (" + r + "," + c + ")");
                });
                gameBoard.add(cell, col, row);
                cells[row][col] = cell;
            }
        }
    }

    private void refreshSelection() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                cells[row][col].setSelected(row == selectedRow && col == selectedCol);
            }
        }
    }

    // State Format: COINS|STATE:YIELD,STATE:YIELD...
    private void updateBoardFromState(String stateStr) {
        try {
            String[] parts = stateStr.split(Pattern.quote("|"));
            this.currentCoins = parts[0];
            String[] plots = parts[1].split(",");

            refreshInfoLabel();

            int idx = 0;
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    if (idx < plots.length) {
                        updateCell(cells[row][col], plots[idx]);
                    }
                    idx++;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Error parsing state: " + e.getMessage());
        }
    }

    private void updateCell(ToggleButton cell, String plotData) {
        // plotData format: STATE:YIELD
        String[] info = plotData.split(":");
        String state = info[0];
        int yield = info.length > 1 ? Integer.parseInt(info[1]) : 0;

        cell.getStyleClass().removeAll("state-empty", "state-growing", "state-ripe");
        
        String text = switch (state) {
            case "EMPTY" -> "Empty";
            case "GROWING" -> "Growing";
            case "RIPE" -> "Ripe\n(" + (yield * 25) + "%)";
            default -> state;
        };
        
        cell.setText(text);

        switch (state) {
            case "EMPTY" -> cell.getStyleClass().add("state-empty");
            case "GROWING" -> cell.getStyleClass().add("state-growing");
            case "RIPE" -> cell.getStyleClass().add("state-ripe");
        }
    }

    private void updateStatus(String message) {
        this.currentStatus = message;
        refreshInfoLabel();
    }
    
    private void refreshInfoLabel() {
        coinsLabel.setText("Coins: " + currentCoins + " | Viewing: " + viewingUser + " | " + currentStatus);
    }

    @FXML
    private void handlePlant() {
        if (!ensureSelection()) return;
        if (!isMyFarm()) {
            updateStatus("Can only plant on your own farm!");
            return;
        }
        client.sendPlant(selectedRow, selectedCol);
        updateStatus("Planting..."); 
    }

    @FXML
    private void handleHarvest() {
        if (!ensureSelection()) return;
         if (!isMyFarm()) {
            updateStatus("Can only harvest your own farm!");
            return;
        }
        client.sendHarvest(selectedRow, selectedCol);
        updateStatus("Harvesting...");
    }

    @FXML
    private void handleSteal() {
        if (!ensureSelection()) return;
        if (isMyFarm()) {
            updateStatus("Cannot steal from yourself!");
            return;
        }
        client.sendSteal(viewingUser, selectedRow, selectedCol);
        updateStatus("Attempting to steal...");
    }
    
    @FXML
    private void handleVisit() {
        String friend = friendField.getText().trim();
        if (friend.isEmpty()) return;
        
        viewingUser = friend;
        client.sendQuery(friend);
        updateButtonStates();
        updateStatus("Visiting " + friend);
    }
    
    @FXML
    private void handleBackHome() {
        viewingUser = myUsername;
        client.sendQuery(myUsername);
        updateButtonStates();
        updateStatus("Back home");
    }
    
    private void updateButtonStates() {
        boolean isHome = isMyFarm();
        plantButton.setDisable(!isHome);
        harvestButton.setDisable(!isHome);
        stealButton.setDisable(isHome);
    }
    
    private boolean isMyFarm() {
        return myUsername != null && myUsername.equals(viewingUser);
    }

    private boolean ensureSelection() {
        if (selectedRow < 0 || selectedCol < 0) {
            updateStatus("Select a plot first.");
            return false;
        }
        return true;
    }

    public void shutdown() {
        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
        if (client != null) {
            client.close();
        }
    }
}
