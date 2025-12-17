package com.smarthome.web.tcp;

import com.smarthome.web.ws.WebSocketBroadcaster;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class TcpCommandServer {

    private static final int PORT = 39186;

    private final List<PrintWriter> clients = new CopyOnWriteArrayList<>();
    private final WebSocketBroadcaster broadcaster;
    private volatile boolean running = false;

    public TcpCommandServer(WebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void start() {
        if (running) return;
        running = true;
        Thread t = new Thread(this::runServer, "tcp-command-server");
        t.setDaemon(true);
        t.start();
    }

    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            broadcaster.broadcast("info", "TCP Command server listening on " + PORT);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                broadcaster.broadcast("info", "Client connected: " + clientSocket.getInetAddress());
                PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                clients.add(writer);

                Thread reader = new Thread(() -> handleClient(clientSocket, writer), "tcp-client-reader");
                reader.setDaemon(true);
                reader.start();
            }
        } catch (IOException e) {
            broadcaster.broadcast("error", "TCP Command server failed: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket, PrintWriter writer) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                String cmd = line.trim();
                if (cmd.isEmpty()) continue;
                broadcaster.broadcast("from_python", cmd);
                forwardToOthers(cmd, writer);
            }
        } catch (IOException ignored) {
        } finally {
            clients.remove(writer);
            try { clientSocket.close(); } catch (IOException ignored) {}
            broadcaster.broadcast("info", "Client disconnected");
        }
    }

    private void forwardToOthers(String cmd, PrintWriter sender) {
        for (PrintWriter out : clients) {
            if (out == sender) continue;
            try {
                out.println(cmd);
            } catch (Exception ignored) {
            }
        }
    }

    public void sendCommand(String cmd) {
        List<PrintWriter> toRemove = new ArrayList<>();
        for (PrintWriter out : clients) {
            try {
                out.println(cmd);
            } catch (Exception e) {
                toRemove.add(out);
            }
        }
        clients.removeAll(toRemove);
        broadcaster.broadcast("sent", cmd);
    }
}
