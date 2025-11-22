package org.example.demo;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class Game {

    public enum PlotState {EMPTY, GROWING, RIPE}

    private static final int ROWS = 4;
    private static final int COLS = 4;
    private static final int PLANT_COST = 5;
    private static final int MAX_YIELD = 4; // 4 units = 100%
    private static final int UNIT_REWARD = 3; // 4 units * 3 = 12 total
    private static final int STEAL_REWARD = 3; // 25% of total (12) is 3

    private final PlotState[][] board = new PlotState[ROWS][COLS];
    private final int[][] cropYield = new int[ROWS][COLS];
    
    // Shared scheduler for all game instances to save threads
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "crop-growth-pool");
        thread.setDaemon(true);
        return thread;
    });

    private int coins = 40;

    public Game() {
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                board[r][c] = PlotState.EMPTY;
                cropYield[r][c] = 0;
            }
        }
    }

    public synchronized int getCoins() {
        return coins;
    }

    public synchronized PlotState getState(int row, int col) {
        return board[row][col];
    }

    public synchronized void plant(int row, int col) {
        if (board[row][col] != PlotState.EMPTY) {
            throw new IllegalStateException("Plot occupied");
        }
        if (coins < PLANT_COST) {
            throw new IllegalStateException("Not enough coins");
        }
        coins -= PLANT_COST;
        board[row][col] = PlotState.GROWING;
        cropYield[row][col] = 0;

        // Simulate growth finishing after 10 seconds (as per requirements)
        scheduler.schedule(() -> {
            synchronized (Game.this) {
                if (board[row][col] == PlotState.GROWING) {
                    board[row][col] = PlotState.RIPE;
                    cropYield[row][col] = MAX_YIELD; // Set to 100% yield
                }
            }
        }, 10, TimeUnit.SECONDS);
    }

    public synchronized int harvest(int row, int col) {
        if (board[row][col] != PlotState.RIPE) {
            throw new IllegalStateException("Crop not ripe");
        }
        int yield = cropYield[row][col];
        int reward = yield * UNIT_REWARD;
        
        board[row][col] = PlotState.EMPTY;
        cropYield[row][col] = 0;
        coins += reward;
        return reward;
    }


    public synchronized int steal(int row, int col) {
        if (board[row][col] != PlotState.RIPE) {
            return 0; // Can only steal ripe crops
        }
        if (cropYield[row][col] <= 1) {
             return 0; // Too little yield left to steal (e.g. < 25%)? Or maybe allow until 0?
             // Requirement: "atomic update prevents over-stealing".
             // Let's say we need at least 1 unit to steal. 
        }
        
        // Thief takes 1 unit (25%)
        cropYield[row][col] -= 1;
        return STEAL_REWARD;
        }

    public synchronized void addCoins(int amount) {
        coins += amount;
    }

    public int getRows() {
        return ROWS;
    }

    public int getCols() {
        return COLS;
    }

    // Serialize state for network transmission
    // Format: COINS|R0C0_STATE:YIELD,R0C1_STATE:YIELD...
    public synchronized String serialize() {
        StringBuilder sb = new StringBuilder();
        sb.append(coins).append("|");
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                sb.append(board[r][c]).append(":").append(cropYield[r][c]);
                if (c < COLS - 1 || r < ROWS - 1) sb.append(",");
            }
        }
        return sb.toString();
    }
}
