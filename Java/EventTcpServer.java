import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EventTcpServer {

    private int port;
    private PrintWriter clientOut;  // Python으로 보낼 스트림

    public EventTcpServer(int port) {
        this.port = port;
    }

    // 도어락 이벤트를 Python에 전달하는 서버
    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[JAVA] EventTcpServer 대기. 포트: " + port);

                Socket clientSocket = serverSocket.accept();
                System.out.println("[JAVA] 이벤트 클라이언트 연결: " + clientSocket.getInetAddress());

                clientOut = new PrintWriter(clientSocket.getOutputStream(), true);

                // 단일 연결 유지
                while (true) {
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 도어락 이벤트 -> Python으로 전송
    public void sendEvent(String event) {
        if (clientOut != null) {
            clientOut.println(event);
            System.out.println("[JAVA] 이벤트 전송 -> PY: " + event);
        } else {
            System.out.println("[JAVA] 이벤트 클라이언트가 연결되지 않았습니다");
        }
    }
}
