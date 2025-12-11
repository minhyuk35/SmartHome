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
    // 연결된 모든 기기(PC, 주피터 등)를 관리하는 명단 (동기화 리스트)
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
                System.out.println("[JAVA] 명령 서버(TcpServer) 시작됨, 포트: " + port);

                while (true) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[JAVA] 새 기기 접속: " + clientSocket.getInetAddress());

                    // 접속한 기기에게 보낼 편지지를 만들어서 명단에 추가
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
                    clients.add(writer);

                    // 각 기기마다 담당자를 붙여서(스레드) 말을 듣게 함
                    new Thread(() -> {
                        try {
                            BufferedReader in = new BufferedReader(
                                    new InputStreamReader(clientSocket.getInputStream())
                            );
                            String line;
                            while ((line = in.readLine()) != null) {
                                String receivedCmd = line.trim();
                                System.out.println("[JAVA] 명령 수신: " + receivedCmd);
                                
                                // 1. GUI 화면한테 알려주기 (글자 바꾸라고)
                                if (commandListener != null) {
                                    commandListener.onCommand(receivedCmd);
                                }

                                // 2. 🔥 [핵심] 연결된 모든 기기에게 소문내기 (Broadcast)
                                // PC가 보낸 UNLOCK을 여기서 주피터한테 전달합니다!
                                broadcast(receivedCmd);
                            }
                        } catch (IOException e) {
                            // 연결 끊김
                        } finally {
                            clients.remove(writer);
                            System.out.println("[JAVA] 기기 연결 해제됨: " + clientSocket.getInetAddress());
                            try { clientSocket.close(); } catch (Exception ignored) {}
                        }
                    }).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // GUI 버튼으로 명령 보낼 때
    public void sendCommand(String cmd) {
        System.out.println("[JAVA] GUI 전송 -> " + cmd);
        broadcast(cmd);
    }

    // [핵심 함수] 명단에 있는 모든 기기에게 메시지 전송
    private void broadcast(String msg) {
        synchronized (clients) {
            for (PrintWriter out : new ArrayList<>(clients)) {
                try {
                    out.println(msg);
                } catch (Exception e) {
                    clients.remove(out);
                }
            }
        }
    }
}