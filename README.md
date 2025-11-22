# QQ Farm Demo (Networked Version)

CS209A Assignment 2 - Multiplayer QQ Farm

A networked multiplayer farm game using **Java Socket** for communication and **JavaFX** for the graphical interface. This project has been refactored from a single-player demo into a full Client-Server (C/S) architecture.

## 📋 Features

- **Client-Server Architecture**: 
  - Centralized server maintains the state of all players.
  - Clients communicate via TCP Sockets using a custom text-based protocol.
- **Multiplayer Support**: 
  - Multiple clients can connect simultaneously.
  - Each player has their own farm state (Coins, Plots).
- **Core Gameplay**:
  - **Plant**: Spend coins to plant crops (takes 10s to mature).
  - **Harvest**: Collect ripe crops for profit.
  - **Steal**: Visit other players' farms and steal 25% of their crop yield.
- **Concurrency & Thread Safety**:
  - Server uses `ConcurrentHashMap` and `synchronized` methods to handle concurrent requests (e.g., multiple players stealing the same crop at the same time).
- **Real-time Updates**:
  - Clients automatically refresh the view every second to show crop growth progress.

## 🛠 Environment

- **Java JDK**: 17 or higher
- **Maven**: 3.8+
- **JavaFX**: 17.0.6 (Dependencies managed via Maven)

## 🚀 How to Run

### 1. Start the Server
Run the `FarmServer` class first. It will listen on port **8888**.

```bash
# Run via IDE or Maven
src/main/java/org/example/demo/server/FarmServer.java
```

### 2. Start Clients
Run the `Application` class. You can launch multiple instances to simulate different players.

```bash
src/main/java/org/example/demo/Application.java
```

1. Upon launching, a dialog will ask for a **Username** (e.g., `Alice`).
2. The main game window will appear.
3. Launch another instance and log in as `Bob`.

### 3. How to Play

- **My Farm**: 
  - Click an empty plot -> Click **Plant** (Cost: 5 coins).
  - Wait 10 seconds for the crop to change from `Growing` to `Ripe`.
  - Click the ripe crop -> Click **Harvest** (Reward: 12 coins).

- **Visit & Steal**:
  - In the top bar, enter a friend's name (e.g., `Alice`) and click **Go**.
  - You will see Alice's farm status.
  - If Alice has ripe crops, select a plot and click **Steal**.
  - You will gain coins, and Alice's crop yield will decrease.
  - Click **Home** to return to your own farm.

## 📡 Communication Protocol

The client and server communicate using simple text commands:

| Command | Format | Description |
|---------|--------|-------------|
| **LOGIN** | `LOGIN <username>` | Register/Login to the server. |
| **PLANT** | `PLANT <row> <col>` | Plant a crop at (r, c). |
| **HARVEST**| `HARVEST <row> <col>` | Harvest a crop at (r, c). |
| **STEAL** | `STEAL <target> <r> <c>`| Steal from target user. |
| **QUERY** | `QUERY [username]` | Get farm state of self or others. |

**Server Response (State)**:
`STATE <coins>|<cell_0_0>,<cell_0_1>...`
- Example: `STATE 45|RIPE:4,GROWING:0,EMPTY,EMPTY...`

## 📂 Project Structure

- `org.example.demo.server`
  - `FarmServer`: Entry point for the server.
  - `ClientHandler`: Handles individual client connections.
- `org.example.demo`
  - `Application`: Entry point for the JavaFX client.
  - `Controller`: Handles UI events and refreshes.
  - `NetworkClient`: Manages socket connection and background listening.
  - `Game`: Shared logic class (now used primarily on Server).
