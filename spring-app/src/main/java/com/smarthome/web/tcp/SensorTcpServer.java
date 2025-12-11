package com.smarthome.web.tcp;

import com.smarthome.web.ws.WebSocketBroadcaster;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensorTcpServer {

    private static final int PORT = 39187;
    private final WebSocketBroadcaster broadcaster;
    private volatile boolean running = false;

    public SensorTcpServer(WebSocketBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void start() {
        if (running) return;
        running = true;
        Thread t = new Thread(this::runServer, "tcp-sensor-server");
        t.setDaemon(true);
        t.start();
    }

    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            broadcaster.broadcast("info", "Sensor server listening on " + PORT);
            while (running) {
                Socket client = serverSocket.accept();
                Thread reader = new Thread(() -> readLoop(client), "sensor-reader");
                reader.setDaemon(true);
                reader.start();
            }
        } catch (Exception e) {
            broadcaster.broadcast("error", "Sensor server error: " + e.getMessage());
        }
    }

    private void readLoop(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.startsWith("SENSOR")) {
                    Map<String, Object> data = parse(line);
                    broadcaster.broadcast("sensor", data);
                }
            }
        } catch (Exception ignored) {
        } finally {
            try { client.close(); } catch (Exception ignored) {}
        }
    }

    private Map<String, Object> parse(String msg) {
        Map<String, Object> map = new HashMap<>();
        try {
            String[] parts = msg.split(" ");
            map.put("gas", parts[1].split("=")[1]);
            map.put("temp", parts[3].split("=")[1]);
            map.put("dust", parts[7].split("=")[1]);
            map.put("pir", Integer.parseInt(parts[8].split("=")[1]));
        } catch (Exception e) {
            map.put("raw", msg);
        }
        return map;
    }
}
