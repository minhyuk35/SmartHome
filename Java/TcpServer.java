import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Multi-client TCP command server (Spring replacement).
 * - Broadcasts incoming commands to other clients.
 * - Notifies GUI via CommandListener.
 */
public class TcpServer {

    private final int port;
    private final List<PrintWriter> clients = Collections.synchronizedList(new ArrayList<>());
    private CommandListener commandListener;

    public TcpServer(int port) {
        this.port = port;
    }

    public interface CommandListener {
        void onCommand(String cmd);
    }

    public void addCommandListener(CommandListener listener) {
        this.commandListener = listener;
    }

    public void start() {
        Thread t = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[JAVA] Command server listening on port " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[JAVA] Command client connected: " + clientSocket.getInetAddress());

                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                    clients.add(writer);

                    Thread reader = new Thread(() -> handleClient(clientSocket, writer), "command-reader");
                    reader.setDaemon(true);
                    reader.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "command-server");
        t.setDaemon(true);
        t.start();
    }

    private void handleClient(Socket clientSocket, PrintWriter writer) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                String cmd = line.trim();
                if (cmd.isEmpty()) continue;
                System.out.println("[JAVA] Command received: " + cmd);
                if (commandListener != null) {
                    commandListener.onCommand(cmd);
                }
                broadcast(cmd, writer);
            }
        } catch (IOException ignored) {
        } finally {
            clients.remove(writer);
            System.out.println("[JAVA] Command client disconnected");
            try { clientSocket.close(); } catch (Exception ignored) {}
        }
    }

    // Broadcast to all clients (except sender when provided)
    private void broadcast(String cmd, PrintWriter sender) {
        synchronized (clients) {
            for (PrintWriter out : new ArrayList<>(clients)) {
                try {
                    if (out != sender) out.println(cmd);
                } catch (Exception e) {
                    clients.remove(out);
                }
            }
        }
    }

    // Send command originated from GUI to every client
    public void sendCommand(String cmd) {
        broadcast(cmd, null);
    }
}
