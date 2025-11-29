package teamchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core server for TeamChat app.
 * 
 * <p>
 * This class accepts incoming TCP connections, and creates a dedicated
 * {@link ClientHandler} for each client, and routes messages between users.
 * </p>
 * 
 * <h2>Features</h2>
 * <ul>
 *      <li>Room based broadcasts</li>
 *      <li>Nicknames via {@code /nick}</li>
 *      <li>Room switching via {@code /join}</li>
 *      <li>Disconnect via {@code /quit}</li>
 *      <li>Private messages via {@code /w or /pm user message}</li>
 * </ul>
 * 
 * <h2>Threading Model</h2>
 * <ul>
 *      <li>One accepts loop in {@link #start()} listens on the server socket.</li>
 *      <li>Each client runs in its own {@link Thread} via {@link ClientHandler}.</li>
 *      <li>Shared collections use synchronization / concurrent maps for safety.</li>
 * </ul>
 * 
 * @author Miguel Gonzalez
 * @version 1.0
 */

public class ChatServer {
    
    // TCP port that the server will bind and listen to.
    private final int mPort;

    // All connected clients used for room-wide broadcasts
    private final Set<ClientHandler> mClients =
            Collections.synchronizedSet(new HashSet<>());

    // Nickname -> ClientHandler (for private messages)
    private final Map<String, ClientHandler> mClientsByNick =
            new ConcurrentHashMap<>();
    
    /**
     * Creates a new {@code ChatServer} bound to the specified TCP port.
     * 
     * @param port the TCP port number to listen on
     */
    public ChatServer(int port) {
        this.mPort = port;
    }

    /**
     * Starts the main accept loop.
     * 
     * <p>
     * This method blocks indefinitely: it opens a {@link ServerSocket},
     * waits for incoming connections, and makes a {@link ClientHandler} in a 
     * new {@link Thread} for each accepted client. 
     * <p>
     * On unrecoverable errors, an error message and stack trace are printed to
     * standard output, and the server quits. 
     * </p>
     */
    public void start() {
        System.out.println("[SERVER] Listening on port " + mPort);

        try (ServerSocket serverSocket = new ServerSocket(mPort)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[SERVER] Client connected: " + socket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(this, socket);
                addClient(handler);

                Thread t = new Thread(handler);
                t.start();
            }
        } catch (IOException e) {
            System.out.println("[SERVER] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Registers a newly connected client with the server.
     * <p>
     * This method adds the handler to {@#mClients} so that the client can
     * receive room broadcasts. Nickname map is not updated here. Clients start
     * as {@code "Guest"} and are only added to {@link #mClientsByNick} after
     * they successfully issue a {@code /nick} command.
     * </p>
     * 
     * @param handler the client handler to add.
     */

    void addClient(ClientHandler handler) {
        mClients.add(handler);
    }
    
    /**
     *  Unregisters a disconnected client from all server tracking structures.
     * <p>
     * Removes the handler from {@link #mClients}, and if the client had a 
     * non-null nickname, also removes from mapping. {@link #mClientsByNick}.
     * </p>
     * 
     * @param handler the client handler to remove.  
     */

    void removeClient(ClientHandler handler) {
        mClients.remove(handler);
        String nick = handler.getNickname();
        if (nick != null) {
            mClientsByNick.remove(nick);
        }
    }

    /**
     * Updates the internal nickname mapping when a client changes their nick.
     *
     * <p>
     * Removes the old nickname (if present) from {@link #mClientsByNick} and
     * registers the new nickname, if it is non-null and not blank.
     * </p>
     *
     * @param oldNick  previous nickname (may be {@code null})
     * @param newNick  new nickname to associate with this client
     * @param handler  the client handler associated with the nickname
     */
    void updateNickname(String oldNick, String newNick, ClientHandler handler) {
        if (oldNick != null) {
            mClientsByNick.remove(oldNick);
        }
        if (newNick != null && !newNick.isBlank()) {
            mClientsByNick.put(newNick, handler);
        }
    }

    /**
     * Broadcasts a chat message to all clients in the sender's room.
     *
     * <p>
     * The final delivered message is tagged with room and nickname:
     * {@code [room] nickname: message}.
     * </p>
     *
     * @param chatText the raw chat text sent by the user
     * @param from     the client handler that originated the message
     */
    
    void broadcast(String chatText, ClientHandler from) {
        String room = from.getRoom();
        String nickname = from.getNickname();
        String tagged = "[" + room + "] " + nickname + ": " + chatText;

        synchronized (mClients) {
            for (ClientHandler ch : mClients) {
                if (room.equals(ch.getRoom())) {
                    ch.send(tagged);
                }
            }
        }

        System.out.println("[SERVER/BROADCAST " + room + "] " + nickname + ": " + chatText);
    }

    /**
     * Sends a server informational message to a single client.
     *
     * <p>
     * The message is automatically prefixed with {@code "[SERVER]"} on
     * delivery to the client.
     * </p>
     *
     * @param to  the client handler to receive the message.
     * @param msg the message body (without prefix).
     */
    
    void sendServerMessage(ClientHandler to, String msg) {
        to.send("[SERVER] " + msg);
    }
    
    /**
     * Sends a private message from one user to another by nickname.
     * <p>
     * If the target user is online and known, the server delivers a message
     * {@code "[PM from fromNick] message"}.
     * </p>
     * 
     * @param fromNick nickname of the sender.
     * @param toNick of the intended recipient.
     * @param message the private message text.
     * @return {@code true} if the recipient was found and message delivered.
     *         {@code false} if user DNE.
     */
    
    boolean sendPrivate(String fromNick, String toNick, String message) {
        ClientHandler target = mClientsByNick.get(toNick);
        if (target == null) {
            return false;
        }
        target.send("[PM from " + fromNick + "] " + message);
        return true;
    }

//  Per-client handler
    
    /**
     * Handles all I/O for a single connected client.
     * 
     * <p>
     * Each {@code ClientHandler} runs its own thread. Its in charge of reading
     * line based commands from the socket, parsing chat commands, and 
     * delegating to the {@link ChatServer} for broadcasting or private 
     * messaging. 
     * </p>
     * 
     * <h2>Supported Commands</h2>
     * <ul>
     *      <li>{@code /nick name} sets or changes nickname.</li>
     *      <li>{@code /join room} switches to a different chat room.</li>
     *      <li>{@code /w user message} send a private message.</li>
     *      <li>{@code /pm user message} sends private message.</li>
     *      <li>{@code /quit} disconnect from server.</li>
     *      <li>Any other text is broadcast to the current room.</li>
     * </ul>
     */
    
    static class ClientHandler implements Runnable {

        private final ChatServer server;
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        
        private String room = "lobby";       // default room until /join 
        private String nickname = "Guest";   // default until /nick is set
        
        /**
         * Constructs a new client handler bound to a specific socket and server.
         * 
         * @param server the parent instance {@link ChatServer} .
         * @param socket the established client {@link Socket}.
         */

        ClientHandler(ChatServer server, Socket socket) {
            this.server = server;
            this.socket = socket;
        }
        
        /**
         * Returns the current room name for this client.
         * 
         * @return the room name. 
         */

        String getRoom() {
            return room;
        }
        
        /**
         * Returns the current nickname for this client.
         * 
         * @return the nickname string (never{@code null}).
         */

        String getNickname() {
            return nickname;
        }

        /**
         * Sends a single line of text to the client
         * <p>
         * This method is thread-safe with respect of usage in typical chat
         * patterns. It checks that the output stream is initialized and writes
         * the line followed by a newline.
         * </p>
         * 
         * @param msg the message to send.
         */

        void send(String msg) {
            if (out != null) {
                out.println(msg);
            }
        }
        
        /**
         * Main execution loop for the client handler thread. 
         * 
         * <p>
         * Initializes I/O streams, prints server usage instructions, then 
         * repeatedly reads lines from the client until exited or connection
         * is lost. On exit, the client is is no unregistered from the server, 
         * and the socket is closed. 
         * </p>
         */

        @Override
        public void run() {
            try {
                in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                send("[SERVER] Welcome to TeamChat!");
                send("[SERVER] You are in room: " + room);
                send("[SERVER] Use /nick yourName to set your nickname.");
                send("[SERVER] Use /join roomName to switch rooms, /quit to exit.");
                send("[SERVER] Use /w user message or /pm user message for private messages.");

                String line;
                while ((line = in.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }

                    // /quit
                    if (line.equalsIgnoreCase("/quit")) {
                        break;
                    }

                    // /nick yourName
                    if (line.startsWith("/nick")) {
                        String[] parts = line.split("\\s+", 2);
                        if (parts.length < 2 || parts[1].trim().isEmpty()) {
                            send("[SERVER] Usage: /nick yourName");
                        } else {
                            String old = nickname;
                            String newNick = parts[1].trim();
                            nickname = newNick;
                            server.updateNickname(old, newNick, this);
                            send("[SERVER] Nickname set to '" + nickname + "'.");
                            System.out.println("[SERVER] Client " + old + " is now " + nickname);
                        }
                        continue;
                    }

                    // /join roomName
                    if (line.startsWith("/join")) {
                        String[] parts = line.split("\\s+", 2);
                        if (parts.length < 2 || parts[1].trim().isEmpty()) {
                            send("[SERVER] Usage: /join roomName");
                        } else {
                            String newRoom = parts[1].trim();
                            String oldRoom = room;
                            room = newRoom;
                            send("[SERVER] Switched from '" + oldRoom + "' to '" + room + "'.");
                        }
                        continue;
                    }

                    // /w user message OR /pm user message
                    if (line.startsWith("/w ") || line.startsWith("/pm ")) {
                        String[] parts = line.split("\\s+", 3);
                        if (parts.length < 3) {
                            send("[SERVER] Usage: /w user message");
                        } else {
                            String targetUser = parts[1];
                            String msg = parts[2];
                            boolean ok = server.sendPrivate(nickname, targetUser, msg);
                            if (!ok) {
                                send("[SERVER] User '" + targetUser + "' not found.");
                            } else {
                                send("[PM to " + targetUser + "] " + msg);
                            }
                        }
                        continue;
                    }

                    // Normal chat text -> broadcast in current room
                    server.broadcast(line, this);
                }
            } catch (IOException e) {
                System.out.println("[SERVER] Client error: " + e.getMessage());
            } finally {
                server.removeClient(this);
                try {
                    socket.close();
                } catch (IOException ignored) {}
                System.out.println("[SERVER] Client disconnected: " + socket.getRemoteSocketAddress());
            }
        }
    }
}
