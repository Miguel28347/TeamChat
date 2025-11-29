package teamchat;

/**
 * Entry point for launching the TeamChat server app.
 *
 * <p>
 * This class initializes a {@link ChatServer} instance on a fixed TCP port and
 * begins listening for incoming client connections. It contains only the
 * {@code main} method and serves as the standalone launcher for command-line
 * execution.
 * </p>
 *
 * <h2>Usage</h2>
 * <pre>
 *      java teamchat.ServerMain
 * </pre>
 *
 * <h2>Behavior</h2>
 * <ul>
 * <li>Binds the server to TCP port 5000</li>
 * <li>Creates a {@code ChatServer} instance</li>
 * <li>Starts the server's main accept loop</li>
 * </ul>
 *
 * <h2>Notes</h2>
 * <ul>
 * <li>The main logic lives inside {@link ChatServer#start()}.</li>
 * <li>Edit the port number here if needed for deployment.</li>
 * </ul>
 *
 * @author Miguel Gonzalez
 * @version 1.0
 */
public class ServerMain {

    /**
     * Program entry point. Starts the TeamChat server on port 5000.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        int port = 5000;
        ChatServer server = new ChatServer(port);
        server.start();
    }
}
