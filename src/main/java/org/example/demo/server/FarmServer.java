package org.example.demo.server;

import org.example.demo.Game;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FarmServer {
    private static final int PORT = 8888;
    // 存储所有在线或离线玩家的游戏状态：Map<Username, GameInstance>
    // 这里为了简单，直接把原来的 Game 类当作单个玩家的状态容器
    private final Map<String, Game> playerStates = new ConcurrentHashMap<>();
    private final ExecutorService threadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        new FarmServer().start();
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Farm Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                // 为每个客户端启动一个处理线程
                threadPool.execute(new ClientHandler(clientSocket, this));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized Game getOrCreatePlayer(String username) {
        return playerStates.computeIfAbsent(username, k -> new Game());
    }
    
    public Game getPlayer(String username) {
        return playerStates.get(username);
    }
}

