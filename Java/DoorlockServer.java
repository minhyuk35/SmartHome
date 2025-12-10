import java.io.*;
import java.net.*;

public class DoorlockServer {

    private int port;
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
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("[JAVA] Doorlock 서버 시작, 포트: " + port);

                while (true) {
                    Socket client = serverSocket.accept();
                    System.out.println("[JAVA] 도어락 연결됨: " + client.getInetAddress());

                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(client.getInputStream())
                    );

                    String line = in.readLine();
                    if (line != null) {
                        System.out.println("[JAVA] 도어락 이벤트 수신: " + line);

                        if (listener != null) listener.onDoorlockEvent(line);
                    }

                    client.close();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
