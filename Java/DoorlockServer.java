import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Door event server (matches Spring's port). Receives door events and broadcasts to connected clients.
 */
public class DoorlockServer {

    private final int port;
    private final List<PrintWriter> clients = Collections.synchronizedList(new ArrayList<>());
    private DoorlockListener listener;

    public DoorlockServer(int port) {
        this.port = port;
    }

    public interface DoorlockListener {
        void onDoorlockEvent(String event);
    }

    public void addDoorlockListener(DoorlockListener l) {
        this.listener = l;
    }

    public void start() {
        Thread t = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[JAVA] Door event server started, port: " + port);

                while (true) {
                    Socket client = serverSocket.accept();
                    System.out.println("[JAVA] Door event client connected: " + client.getInetAddress());
                    PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
                    clients.add(writer);

                    Thread reader = new Thread(() -> handleClient(client, writer), "door-event-reader");
                    reader.setDaemon(true);
                    reader.start();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "door-event-server");
        t.setDaemon(true);
        t.start();
    }

    private void handleClient(Socket client, PrintWriter writer) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                String evt = line.trim();
                if (evt.isEmpty()) continue;
                System.out.println("[JAVA] Door event received: " + evt);
                if (listener != null) listener.onDoorlockEvent(evt);
                broadcast(evt, writer);
            }
        } catch (Exception ignored) {
        } finally {
            clients.remove(writer);
            try { client.close(); } catch (Exception ignored) {}
            System.out.println("[JAVA] Door event client disconnected");
        }
    }

    private void broadcast(String evt, PrintWriter sender) {
        synchronized (clients) {
            for (PrintWriter out : new ArrayList<>(clients)) {
                try {
                    if (out != sender) out.println(evt);
                } catch (Exception e) {
                    clients.remove(out);
                }
            }
        }
    }

    // Allow manual broadcasting if we ever need to trigger events locally
    public void broadcast(String evt) {
        broadcast(evt, null);
    }
}
