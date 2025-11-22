package org.example.demo.server;

import org.example.demo.Game;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final FarmServer server;
    private String currentUser;
    private PrintWriter out;

    public ClientHandler(Socket socket, FarmServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            this.out = out;
            String line;
            while ((line = in.readLine()) != null) {
                handleCommand(line);
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + currentUser);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    private void handleCommand(String cmdLine) {
        String[] parts = cmdLine.split(" ");
        String command = parts[0].toUpperCase();

        try {
            switch (command) {
                case "LOGIN": // LOGIN <username>
                    if (parts.length < 2) {
                        out.println("ERROR Missing username");
                        return;
                    }
                    this.currentUser = parts[1];
                    server.getOrCreatePlayer(this.currentUser);
                    out.println("SUCCESS Logged in as " + currentUser);
                    break;

                case "PLANT": // PLANT <row> <col>
                    handlePlant(parts);
                    break;

                case "HARVEST": // HARVEST <row> <col>
                    handleHarvest(parts);
                    break;
                
                case "QUERY": // QUERY <username>
                     handleQuery(parts);
                     break;

                case "STEAL": // STEAL <target_user> <row> <col>
                     handleSteal(parts);
                     break;
                     
                default:
                    out.println("ERROR Unknown command");
            }
        } catch (Exception e) {
            out.println("ERROR " + e.getMessage());
            // Just log the error message to server console instead of full stack trace
            System.out.println("Command error (" + command + "): " + e.getMessage());
        }
    }

    private void handlePlant(String[] parts) {
        if (currentUser == null) {
            out.println("ERROR Please login first");
            return;
        }
        int row = Integer.parseInt(parts[1]);
        int col = Integer.parseInt(parts[2]);
        
        Game game = server.getPlayer(currentUser);
        game.plant(row, col); 
        out.println("SUCCESS Planted at " + row + "," + col);
    }

    private void handleHarvest(String[] parts) {
        if (currentUser == null) {
            out.println("ERROR Please login first");
            return;
        }
        int row = Integer.parseInt(parts[1]);
        int col = Integer.parseInt(parts[2]);
        
        Game game = server.getPlayer(currentUser);
        int reward = game.harvest(row, col);
        out.println("SUCCESS Harvested at " + row + "," + col + ". Gained " + reward);
    }
    
    private void handleQuery(String[] parts) {
        // If querying self (or no arg provided), query currentUser.
        // If querying others, use arg.
        String targetUser;
        if (parts.length > 1) {
            targetUser = parts[1];
        } else {
            if (currentUser == null) {
                out.println("ERROR Not logged in and no target specified");
                return;
            }
            targetUser = currentUser;
        }

        Game game = server.getPlayer(targetUser);
        if (game == null) {
            // If user doesn't exist, maybe create just to view empty? Or return error.
            out.println("ERROR User " + targetUser + " not found");
            return;
        }
        
        out.println("STATE " + game.serialize());
    }
    
    private void handleSteal(String[] parts) {
         if (currentUser == null) {
            out.println("ERROR Please login first");
            return;
        }
         if (parts.length < 4) {
             out.println("ERROR Usage: STEAL <target_user> <row> <col>");
             return;
         }
         
         String targetUser = parts[1];
         if (targetUser.equals(currentUser)) {
             out.println("ERROR Cannot steal from yourself");
             return;
         }
         
         int row = Integer.parseInt(parts[2]);
         int col = Integer.parseInt(parts[3]);
         
         Game targetGame = server.getPlayer(targetUser);
         if (targetGame == null) {
             out.println("ERROR Target user not found");
             return;
         }
         
         // Atomic steal on target
         int stolenAmount = targetGame.steal(row, col);
         
         if (stolenAmount > 0) {
             // Add to my coins
             Game myGame = server.getPlayer(currentUser);
             myGame.addCoins(stolenAmount);
             out.println("SUCCESS Stole " + stolenAmount + " from " + targetUser);
         } else {
             out.println("FAIL Could not steal (not ripe or already stolen)");
         }
    }
}
