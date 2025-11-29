package teamchat;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Full Swing GUI client for the TeamChat messaging system.
 * <p>
 * This class manages
 * <ul>
 * <li>Connecting to a {@link teamchat.ChatServer}</li>
 * <li>Sending chat messages and command</li>
 * <li>Displaying incoming messages</li>
 * <li>Thread-safe UI updates via SwingUtilities</li>
 * </ul>
 * </p>
 */
public class ChatClientGUI extends JFrame {

// UI COMPONENTS
    // Hostname input field (the default is "localhost").
    private JTextField mHostField;
    // Port input field (the default is 5000).
    private JTextField mPortField;
    // Usernme input field.
    private JTextField mUsernameField;
    // Button that initializes connection to the server.
    private JButton mConnectButton;
    // Button that initializes disconnects from the server.
    private JButton mDisconnectButton;

    // Text area showing chat history.
    private JTextArea mChatArea;
    // Text field for typing outgoing messages.
    private JTextField mInputField;
    // Button that sends message
    private JButton mSendButton;

// NETWORK STATE
    // Whether the client is connected to the server.
    private volatile boolean mConnected = false;
    // Actice TCP socket connection to the server.
    private Socket mSocket;
    // Reads messages sent by the server.
    private BufferedReader mServerIn;
    // Sends messages to the server. 
    private PrintWriter mServerOut;
    // Background reader thread that listens for server output.
    private Thread mReaderThread;

    /**
     * Constructs the chat GUI window and initializes components. Also sets up
     * event handlers for connecting, disconnecting, and sending messages
     */
    public ChatClientGUI() {
        super("TeamChat GUI Client");

        initUI();
        setupEventHandlers();
    }

// UI SETUP
    /**
     * Creates and arranges all Swing components using GridBagLayout,
     * BorderLayout, and JScrollPane for the chat window.
     */
    private void initUI() {
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.gridy = 0;

        // Host
        gbc.gridx = 0;
        topPanel.add(new JLabel("Host:"), gbc);

        mHostField = new JTextField("localhost", 10);
        gbc.gridx = 1;
        topPanel.add(mHostField, gbc);

        // Port
        gbc.gridx = 2;
        topPanel.add(new JLabel("Port:"), gbc);

        mPortField = new JTextField("5000", 5);
        gbc.gridx = 3;
        topPanel.add(mPortField, gbc);

        // Username
        gbc.gridx = 4;
        topPanel.add(new JLabel("Username:"), gbc);

        mUsernameField = new JTextField("User" + (int) (Math.random() * 1000), 10);
        gbc.gridx = 5;
        topPanel.add(mUsernameField, gbc);

        // Buttons
        mConnectButton = new JButton("Connect");
        mDisconnectButton = new JButton("Disconnect");
        mDisconnectButton.setEnabled(false);

        gbc.gridx = 6;
        topPanel.add(mConnectButton, gbc);
        gbc.gridx = 7;
        topPanel.add(mDisconnectButton, gbc);

        // Chat display
        mChatArea = new JTextArea(20, 60);
        mChatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(mChatArea);

        // Chat area
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        mInputField = new JTextField();
        mSendButton = new JButton("Send");

        bottomPanel.add(mInputField, BorderLayout.CENTER);
        bottomPanel.add(mSendButton, BorderLayout.EAST);

        // Main layout
        setLayout(new BorderLayout(5, 5));
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Frame config
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null); // center on screen
    }

    /**
     * Registers event listeners for
     * <ul>
     * <li>Connect button</li>
     * <li>Disconnect button</li>
     * <li>Send button and Enter key</li>
     * </ul>
     */
    private void setupEventHandlers() {
        // Connect button
        mConnectButton.addActionListener(this::onConnectClicked);

        // Disconnect button
        mDisconnectButton.addActionListener(e -> disconnect());

        // Send button
        mSendButton.addActionListener(this::onSendClicked);

        // Enter key in input field = send
        mInputField.addActionListener(this::onSendClicked);
    }

// UI HELPERS
    /**
     * Appends text to the chat display area on the EDT.
     *
     * @param msg text to append
     */
    private void appendToChat(String msg) {
        SwingUtilities.invokeLater(() -> {
            mChatArea.append(msg + "\n");
            mChatArea.setCaretPosition(mChatArea.getDocument().getLength());
        });
    }

// CONNECTION
    /**
     * Handles the Connect button click.
     *
     * @param e the ACtionEvent triggered by the button
     */
    private void onConnectClicked(ActionEvent e) {
        if (mConnected) {
            appendToChat("[CLIENT] Already connected.");
            return;
        }
        connectToServer();
    }

    /**
     * Attempts to connect to the ChatServer using host/port fields. Launches a
     * background thread to listen for server messages.
     */
    private void connectToServer() {
        String host = mHostField.getText().trim();
        String portText = mPortField.getText().trim();
        int port;

        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ex) {
            appendToChat("[CLIENT] Invalid port: " + portText);
            return;
        }

        try {
            mSocket = new Socket(host, port);
            mServerIn = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
            mServerOut = new PrintWriter(mSocket.getOutputStream(), true);
            mConnected = true;

            appendToChat("[CLIENT] Connected to " + host + ":" + port);

            // auto-send nickname
            String username = mUsernameField.getText().trim();
            if (!username.isEmpty()) {
                mServerOut.println("/nick " + username);
            }

            // Reader thread
            mReaderThread = new Thread(() -> {
                try {
                    String line;
                    while ((line = mServerIn.readLine()) != null) {
                        appendToChat(line);
                    }
                } catch (IOException e) {
                    appendToChat("[CLIENT] Connection closed.");
                } finally {
                    mConnected = false;
                    SwingUtilities.invokeLater(() -> {
                        mConnectButton.setEnabled(true);
                        mDisconnectButton.setEnabled(false);
                    });
                }
            });
            mReaderThread.setDaemon(true);
            mReaderThread.start();

            mConnectButton.setEnabled(false);
            mDisconnectButton.setEnabled(true);

        } catch (IOException e) {
            appendToChat("[CLIENT] Failed to connect: " + e.getMessage());
        }
    }

    /**
     * Sends a "/quit" to the server, closes socket, updates UI buttons, and
     * logs the disconnect event.
     */
    private void disconnect() {
        if (!mConnected) {
            return;
        }

        mConnected = false;

        try {
            if (mServerOut != null) {
                // tell server we are quitting
                mServerOut.println("/quit");
                mServerOut.flush();
            }
        } catch (Exception ignored) {
        }

        try {
            if (mSocket != null && !mSocket.isClosed()) {
                mSocket.close();
            }
        } catch (IOException ignored) {
        }

        appendToChat("[CLIENT] Disconnected.");

        mConnectButton.setEnabled(true);
        mDisconnectButton.setEnabled(false);
    }

// SEND MESSAGE
    /**
     * Sends a chat message or command to the server. Automatically handles
     * special commands like /quit.
     *
     * @param e event triggered by Send button or Enter key.
     */
    private void onSendClicked(ActionEvent e) {
        if (!mConnected || mServerOut == null) {
            appendToChat("[CLIENT] Not connected.");
            return;
        }

        String text = mInputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }

        mServerOut.println(text);

        if ("/quit".equalsIgnoreCase(text)) {
            disconnect();
        }

        mInputField.setText("");
    }
}
