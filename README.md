# TeamChat – Multi-Client Java Chat System (Console + GUI)

**Author:** Miguel Gonzalez  
**Tech:** Java, Sockets, Threads, Swing, NetBeans / Ant

TeamChat is a small but realistic multi-client chat system.  
It has:

- A **multi-threaded TCP server** ("ChatServer")  
- A **console client** ("ChatClient")  
- A **Swing GUI client** ("ChatClientGUI")  

It supports **chat rooms**, **nicknames**, and **private messages**, and is designed to be easy to run on any machine with Java.

## Features

- ✅ **Multi-threaded server**
  - One `ClientHandler` thread per connected client
  - Shared state protected with synchronized collections and ConcurrentHashMap

- ✅ **Chat rooms**
  - Each client has a `room` (default: `lobby`)
  - `/join roomName` moves you between rooms
  - Broadcasts are scoped to a single room

- ✅ **Nicknames**
  - `/nick yourName` sets the users nickname
  - Server maintains a `Map<String, ClientHandler>` nickname → live connection

- ✅ **Private messages**
  - `/w user message` or `/pm user message`
  - Server routes point-to-point messages without exposing socket details

- ✅ **Two client front-ends**
  - **Console client** (`ClientMain` / `ChatClient`) – good for debugging / scripts
  - **GUI client** (`GUIClientMain` / `ChatClientGUI`) – Swing interface with:
    - Host / port / username fields
    - Connect / Disconnect buttons
    - Scrollable chat window
    - Text input with “Send” button

## Architecture

### Server (`ChatServer` + `ClientHandler`)

- `ChatServer`:
  - Listens on a TCP port via `ServerSocket`
  - For each new connection:
    - Wraps the socket in a `ClientHandler`
    - Starts a new `Thread` for that client
- Tracks:
  - `Set<ClientHandler> mClients` – all connected clients (for room broadcasts)
  - `Map<String, ClientHandler> mClientsByNick` – nickname → client (for private messages)

- `ClientHandler`:
  - Owns a single client socket
  - Reads line-based commands:
    - `/nick name`
    - `/join room`
    - `/w user message` / `/pm user message`
    - `/quit`
    - any other line → broadcast to the current room
  - Delegates routing logic back to `ChatServer` (broadcast / private)

### Clients

- **Console client** (`ChatClient`, `ClientMain`)
  - Single TCP connection to the server
  - Background thread reads server messages and prints to `stdout`
  - Main thread reads from `stdin` and forwards to server
  - Supports all commands: `/nick`, `/join`, `/w`, `/pm`, `/quit`

- **Swing GUI client** (`ChatClientGUI`, `GUIClientMain`)
  - Built with Swing (`JFrame`, `JTextArea`, `JTextField`, `JButton`, etc.)
  - Uses `SwingUtilities.invokeLater` to ensure thread-safe UI updates
  - Background reader thread listens for server messages and appends to chat area
  - UI controls:
    - Host, port, username inputs
    - Connect / Disconnect buttons
    - Text input + Send button / Enter key

---

## How to Run

### Prerequisites

- Java 8+ (JDK)
- Java IDE
- (Optional) NetBeans (project created as a NetBeans project)

### 1. Start the server (command line)

- Once downloaded, open the project in your Java IDE
- Open ServerMain.java
- Right click and run, you should see the output window say "[SERVER] Listening on port 5000".
<img width="3440" height="1440" alt="Screenshot 2025-11-29 024803" src="https://github.com/user-attachments/assets/a76db1c8-ed37-4288-8c29-d6097d26f108" />
- Then navigate to GUIClientMain.java and right click and run the file. You will see a new window open with Host, Port and Username. You will also have two buttons (Connect and Disconnect).
<img width="3440" height="1380" alt="Screenshot 2025-11-29 025229" src="https://github.com/user-attachments/assets/1bf21604-cb59-488d-ba93-842aa22fc995" />
- Create as many of these users as you want.

- You have the option to change chat rooms, choose nicknames and send private messages. Directions on how to do all these are provided once you connect.
<img width="3440" height="1440" alt="Screenshot 2025-11-29 025834" src="https://github.com/user-attachments/assets/dbc94660-5c6d-4b2f-8805-fcc592cd8745" />



