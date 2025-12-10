import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TcpServer {

    private int port;
    // 여러 클라이언트(IoT 장치, 음성 클라이언트 등)를 모두 브로드캐스트하기 위한 리스트
    private final List<PrintWriter> clients = Collections.synchronizedList(new ArrayList<>());

    private SensorListener sensorListener;
    private CommandListener commandListener;

    public TcpServer(int port) {
        this.port = port;
    }

    public interface SensorListener {
        void onSensorUpdate(String gas, String temp, String dust, int pir);
    }

    public void addSensorListener(SensorListener listener) {
        this.sensorListener = listener;
    }

    // 명령 수신 콜백
    public interface CommandListener {
        void onCommand(String cmd);
    }

    public void addCommandListener(CommandListener listener) {
        this.commandListener = listener;
    }

    // 서버 시작
    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[JAVA] TCP 서버 시작, 포트: " + port);

                // 클라이언트 연결을 계속 수락
                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[JAVA] 클라이언트 연결: " + clientSocket.getInetAddress());

                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                    clients.add(writer);

                    // 각 클라이언트 수신 처리
                    new Thread(() -> {
                        try {
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(clientSocket.getInputStream())
                            );

                            String line;
                            while ((line = in.readLine()) != null) {
                                System.out.println("[JAVA] 수신 (포트 " + port + "): " + line);
                                if (TcpServer.this.commandListener != null) {
                                    TcpServer.this.commandListener.onCommand(line.trim());
                                }
                            }

                            System.out.println("[JAVA] 클라이언트 연결 종료");
                        } catch (IOException e) {
                            System.err.println("[JAVA] 클라이언트 처리 오류: " + e.getMessage());
                        } finally {
                            try { clientSocket.close(); } catch (Exception ignore) {}
                            clients.remove(writer);
                        }
                    }).start();
                }

            } catch (IOException e) {
                System.err.println("[JAVA] TCP 서버 오류: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // GUI에서 서버로 명령 전송 (모든 연결된 클라이언트에 브로드캐스트)
    public void sendCommand(String cmd) {
        synchronized (clients) {
            if (clients.isEmpty()) {
                System.out.println("[JAVA] 활성화된 클라이언트가 없습니다.");
                return;
            }

            for (PrintWriter out : new ArrayList<>(clients)) {
                try {
                    out.println(cmd);
                } catch (Exception e) {
                    clients.remove(out);
                }
            }
        }
        System.out.println("[JAVA] Python/IoT로 전송: " + cmd);
    }
}
