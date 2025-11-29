package teamchat;

import java.io.IOException;

/**
 * Entry point for running a chat client.
 * <p>
 * This class constructs a {@link ChatClient} instance with a default host and
 * port, then starts it's blocking I/O loop. It is useful for testing the server
 * without Swing GUI.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 *      java teamchat.ClientMain
 * </pre>
 *
 * <h2>Responsibilities</h2>
 * <ul>
 * <li>Defines default hostname and port</li>
 * <li>Starts a {@link ChatClient} in console mode</li>
 * <li>Handles basic I/O exceptions from the client</li>
 * </ul>
 *
 * @author Miguel Gonzalez
 * @version 1.0
 */
public class ClientMain {

    /**
     * Entry point for the console client.
     *
     * @param args command-line argument (unused)
     */
    public static void main(String[] args) {
        String host = "localhost";
        int port = 5000;

        ChatClient client = new ChatClient(host, port);
        try {
            client.start();
        } catch (IOException e) {
            System.err.println("[CLIENT MAIN] Error: " + e.getMessage());
        }
    }
}
