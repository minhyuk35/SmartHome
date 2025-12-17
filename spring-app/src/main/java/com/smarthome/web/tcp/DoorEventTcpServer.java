package com.smarthome.web.tcp;

import com.smarthome.web.ws.WebSocketBroadcaster;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

@Component
public class DoorEventTcpServer {

    // Python main.py는 DOOR_EVENT_PORT = 39189로 접속하므로 맞춰준다.
    private static final int PORT = 39189;
    private final WebSocketBroadcaster broadcaster;
    private volatile boolean running = false;

    public DoorEventTcpServer(WebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void start() {
        if (running) return;
        running = true;
        Thread t = new Thread(this::runServer, "tcp-door-event-server");
        t.setDaemon(true);
        t.start();
    }

    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            broadcaster.broadcast("info", "Door event server listening on " + PORT);
            while (running) {
                Socket client = serverSocket.accept();
                Thread reader = new Thread(() -> readLoop(client), "door-event-reader");
                reader.setDaemon(true);
                reader.start();
            }
        } catch (Exception e) {
            broadcaster.broadcast("error", "Door event server error: " + e.getMessage());
        }
    }

    private void readLoop(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                String evt = line.trim();
                if (!evt.isEmpty()) {
                    broadcaster.broadcast("door_event", evt);
                }
            }
        } catch (Exception ignored) {
        } finally {
            try { client.close(); } catch (Exception ignored) {}
        }
    }
}
