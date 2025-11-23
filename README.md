# QQ Farm (Networked Version)

**Course**: Java Assignment 2  
**Assignment**: A2 - Multiplayer QQ Farm

## 1. Project Overview

This project implements a networked multiplayer version of the QQ Farm game using a Client-Server architecture. It demonstrates key Java concepts including Socket programming, Multithreading, Thread Safety (Concurrency Control), and JavaFX GUI development.

The system consists of a centralized server that maintains the state of all players and multiple thin clients that communicate with the server via TCP sockets.

## 2. Features

*   **Client-Server Architecture**: Separation of game logic (Server) and user interface (Client).
*   **Multiplayer Interaction**: Multiple clients can connect simultaneously. Players can visit other farms and steal crops.
*   **Concurrency Control**: Uses `synchronized` blocks and `ConcurrentHashMap` to ensure thread safety, preventing race conditions during concurrent steal attempts.
*   **Resilience**: Handles network disconnections gracefully with a reconnect mechanism.
*   **Real-time Updates**: Clients automatically poll the server to reflect crop growth and state changes.

## 3. Environment Requirements

*   **JDK**: Java 17 or higher
*   **Build Tool**: Maven 3.8+
*   **GUI Framework**: JavaFX 17.0.6

## 4. How to Run

### Step 1: Start the Server
Run the `FarmServer` class first. It listens on port **8888**.

```bash
# Main Class: org.example.demo.server.FarmServer
```

### Step 2: Start Clients
Run the `Application` class. You can launch multiple instances.

```bash
# Main Class: org.example.demo.Application
```

1.  Enter a **Username** (e.g., "Alice") in the login dialog.
2.  The game board will appear.
3.  Start another instance and login as "Bob" to test interactions.

### Step 3: Gameplay Instructions
*   **My Farm**: Select an empty plot and click **Plant**. Wait 10 seconds for it to ripen, then click **Harvest**.
*   **Visit & Steal**: Enter a friend's name in the top bar and click **Go**. Select a ripe crop and click **Steal**.
*   **Reconnect**: If the server crashes, click the **Reconnect** button to restore the session.

## 5. Protocol Description

The communication uses a simple line-based text protocol over TCP.

**Request Format**: `COMMAND [ARG1] [ARG2] ...`

| Command | Arguments | Description |
| :--- | :--- | :--- |
| `LOGIN` | `username` | Register or login to the server. |
| `PLANT` | `row` `col` | Plant a crop at the specified coordinates. |
| `HARVEST` | `row` `col` | Harvest a crop at the specified coordinates. |
| `STEAL` | `target_user` `row` `col` | Steal crop from a target player. |
| `QUERY` | `[username]` | Query farm state (self or others). |

**Response Format**:
*   Success: `SUCCESS <message>`
*   Error: `ERROR <message>`
*   State Data: `STATE <coins>|<cell_0_0>,<cell_0_1>...`

## 6. Project Structure

*   `org.example.demo.server`
    *   `FarmServer`: Server entry point, thread pool management.
    *   `ClientHandler`: Handles individual client connections (Runnable).
*   `org.example.demo`
    *   `Application`: JavaFX client entry point.
    *   `Controller`: GUI logic and event handling.
    *   `NetworkClient`: Socket management and background listening.
    *   `Game`: Shared data model and logic (used by Server).
