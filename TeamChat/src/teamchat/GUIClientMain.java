package teamchat;

import javax.swing.SwingUtilities;

/**
 * Entry Point for launching into the TeamChat GUI.
 * <p>
 * This class ensures that the {@link ChatClientGUI} is created and executed on
 * the Swing Event Dispatch Thread, which is required for all thread-safe Swing
 * UI operations.
 * </p>
 */
public class GUIClientMain {

    /**
     * Starts the TeamChat GUI client.
     * <p>
     * This method creates the UI using
     * {@link SwingUtilities#invokeLater(Runnable)} so that the GUI is
     * constructed on the Event Dispatch Thread.
     *
     * @param args command-line arguments (unused)
     * </p>
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChatClientGUI gui = new ChatClientGUI();
            gui.setVisible(true);
        });
    }
}
