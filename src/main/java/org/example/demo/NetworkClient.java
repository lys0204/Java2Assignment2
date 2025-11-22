package org.example.demo;

import javafx.application.Platform;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class NetworkClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private volatile boolean running = false;

    // Callbacks for UI updates
    private Consumer<String> onStateReceived;
    private Consumer<String> onMessageReceived;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        running = true;

        // Start listener thread
        Thread listener = new Thread(this::listen);
        listener.setDaemon(true);
        listener.start();
    }

    public void setOnStateReceived(Consumer<String> onStateReceived) {
        this.onStateReceived = onStateReceived;
    }

    public void setOnMessageReceived(Consumer<String> onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    private void listen() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                final String msg = line;
                Platform.runLater(() -> processMessage(msg));
            }
        } catch (IOException e) {
            if (running) {
                Platform.runLater(() -> {
                    if (onMessageReceived != null) onMessageReceived.accept("Connection lost: " + e.getMessage());
                });
            }
        }
    }

    private void processMessage(String msg) {
        if (msg.startsWith("STATE ")) {
            if (onStateReceived != null) {
                onStateReceived.accept(msg.substring(6));
            }
        } else {
            if (onMessageReceived != null) {
                onMessageReceived.accept(msg);
            }
        }
    }

    public void sendLogin(String username) {
        send("LOGIN " + username);
    }

    public void sendPlant(int row, int col) {
        send("PLANT " + row + " " + col);
    }

    public void sendHarvest(int row, int col) {
        send("HARVEST " + row + " " + col);
    }
    
    public void sendSteal(String target, int row, int col) {
        send("STEAL " + target + " " + row + " " + col);
    }

    public void sendQuery(String targetUser) {
        if (targetUser == null || targetUser.isEmpty()) {
            send("QUERY");
        } else {
            send("QUERY " + targetUser);
        }
    }

    private void send(String cmd) {
        if (out != null) {
            out.println(cmd);
        }
    }

    public void close() {
        running = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            // ignore
        }
    }
}

