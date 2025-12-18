import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SensorTcpServer {

    private final int port;

    // Python/Jupyter -> Java
    private SensorListener sensorListener;

    public SensorTcpServer(int port) {
        this.port = port;
    }

    // GUI에 센서 값 전달을 위한 콜백
    public interface SensorListener {
        void onSensorUpdate(String gas, String humidity, String dustPm10, int pir);
    }

    public void addSensorListener(SensorListener listener) {
        this.sensorListener = listener;
    }

    public void start() {
        Thread t = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[JAVA] Sensor server listening on port " + port);
                while (true) {
                    try (Socket clientSocket = serverSocket.accept()) {
                        System.out.println("[JAVA] Sensor client connected: " + clientSocket.getInetAddress());
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        System.out.println("[JAVA] Sensor client error: " + e.getMessage());
                    } finally {
                        System.out.println("[JAVA] Sensor client disconnected");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }, "sensor-server");
        t.setDaemon(true);
        t.start();
    }

    private void handleClient(Socket clientSocket) throws IOException {
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream(), StandardCharsets.UTF_8)
        )) {
            String line;
            while ((line = in.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("SENSOR")) {
                    parseSensorPacket(trimmed);
                }
            }
        }
    }

    private void parseSensorPacket(String msg) {
        try {
            Map<String, String> fields = parseFields(msg);
            String gas = fields.getOrDefault("GAS", "---");
            String humi = fields.getOrDefault("HUMI", "---");
            String pm10 = fields.getOrDefault("PM10", "---");
            int pir = parseInt(fields.get("PIR"));
            if (sensorListener != null) {
                sensorListener.onSensorUpdate(gas, humi, pm10, pir);
            }
        } catch (Exception e) {
            System.out.println("[JAVA] 센서 파싱 오류: " + e);
        }
    }

    private static Map<String, String> parseFields(String msg) {
        Map<String, String> fields = new HashMap<>();
        String[] tokens = msg.split("\\s+");
        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];
            int idx = token.indexOf('=');
            if (idx <= 0 || idx == token.length() - 1) continue;
            String key = token.substring(0, idx).toUpperCase();
            String value = token.substring(idx + 1);
            fields.put(key, value);
        }
        return fields;
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
}
