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
    // 동기화된 리스트로 여러 클라이언트(PC, 주피터) 안전하게 관리
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
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[JAVA] 명령 서버(TcpServer) 시작, 포트: " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    // 클라이언트 접속 시 로그 출력
                    System.out.println("[JAVA] 명령 클라이언트 접속: " + clientSocket.getInetAddress());

                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                    clients.add(writer);

                    new Thread(() -> {
                        try {
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(clientSocket.getInputStream())
                            );
                            String line;
                            while ((line = in.readLine()) != null) {
                                System.out.println("[JAVA] 명령 수신: " + line);
                                if (commandListener != null) {
                                    commandListener.onCommand(line.trim());
                                }
                            }
                        } catch (IOException e) {
                            // 연결 끊김 처리
                        } finally {
                            clients.remove(writer);
                            System.out.println("[JAVA] 클라이언트 연결 해제됨");
                            try { clientSocket.close(); } catch (Exception ignored) {}
                        }
                    }).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // [핵심] 모든 연결된 기기(PC, 주피터)에 명령 전송
    public void sendCommand(String cmd) {
        synchronized (clients) {
            for (PrintWriter out : new ArrayList<>(clients)) {
                try {
                    out.println(cmd);
                    // System.out.println("[JAVA] 전송 -> " + cmd); // 로그 필요하면 주석 해제
                } catch (Exception e) {
                    clients.remove(out);
                }
            }
        }
    }
}