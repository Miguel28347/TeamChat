package teamchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Console based chat client for the TeamChat system.
 * <p>
 * This class establishes a TCPa connection to a {@link ChatServer}, relays user
 * input from the terminal to the server, prints any received messages directly
 * to stdout. Useful for debugging, automation, and non-GUI operations.
 * </p>
 * <h2>Features</h2>
 * <ul>
 * <li>Connects to a server at a specified host and port</li>
 * <li>Runs a background thread to receive server messages</li>
 * <li>Reads user input from standard input</li>
 * <li>Supports all server commands such as /nick, /join, w/, /quit</li>
 * <li>Automatically terminates on server disconnect or /quit</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>
 *      ChatClient client = new ChatClient("localhost", 5000);
 *      client.start();
 * </pre>
 *
 * <h2>Threading Model</h2>
 * <ul>
 * <li>Main thread handles keyboard input</li>
 * <li>Background thread continuously processes server messages</li>
 * </ul>
 *
 * @author Miguel Gonzalez
 * @version 1.0
 */
public class ChatClient {

    // Hostname or IP address of the chat server.
    private final String host;
    // Port number the chat server is listening on.
    private final int port;

    /**
     * Creates a new ChatClient bound to a given host and port
     *
     * @param host the server hostname (i.e., "localhost")
     * @param port the server port number (i.e., 5000)
     */
    public ChatClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Starts client, connecting to the server and relaying stdin/ stdout.
     * <p>
     * This method blocks until the user enter "/quit" or the button
     * "Disconnect" is pressed. Then the method returns and the client exits.
     * </p>
     *
     * @throws IOException if the network connection fails or streams cannot be
     * created
     */
    public void start() throws IOException {
        try (
                Socket socket = new Socket(host, port); BufferedReader serverIn = new BufferedReader(
                        new InputStreamReader(socket.getInputStream())); PrintWriter serverOut = new PrintWriter(socket.getOutputStream(), true); BufferedReader consoleIn = new BufferedReader(
                        new InputStreamReader(System.in))) {
            System.out.println("[CLIENT] Connected to " + host + ":" + port);

            // Background thread to continouslt read server messages
            Thread readerThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = serverIn.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    System.out.println("[CLIENT] Connection closed.");
                }
            });
            readerThread.setDaemon(true);
            readerThread.start();

            // Main loop that reads from console and sends to server
            String userInput;
            while ((userInput = consoleIn.readLine()) != null) {
                serverOut.println(userInput);
                if (userInput.equalsIgnoreCase("/quit")) {
                    break;
                }
            }
        }
    }
}
