import java.io.*;
import java.net.*;

public class EventTcpServer {

    private int port;
    private PrintWriter clientOut;  // 파이썬 #2로 보낼 스트림

    public EventTcpServer(int port) {
        this.port = port;
    }

    // 파이썬에서 이벤트 받는 서버 시작
    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[JAVA] EventTcpServer 시작. 포트: " + port);

                Socket clientSocket = serverSocket.accept();
                System.out.println("[JAVA] 파이썬 이벤트 클라이언트 연결됨");

                clientOut = new PrintWriter(clientSocket.getOutputStream(), true);

                // 이 서버는 보내기만 하므로 읽을 필요 없음
                while (true) {
                    Thread.sleep(1000);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 💥 도어락 이벤트를 파이썬으로 전송
    public void sendEvent(String event) {
        if (clientOut != null) {
            clientOut.println(event);
            System.out.println("[JAVA] 이벤트 전송 → PY: " + event);
        } else {
            System.out.println("[JAVA] 이벤트 클라이언트 없음");
        }
    }
}
