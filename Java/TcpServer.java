import java.io.*;
import java.net.*;

public class TcpServer {

    private int port;
    private PrintWriter clientOut;

    private SensorListener sensorListener;

    public TcpServer(int port) {
        this.port = port;
    }

    public interface SensorListener {
        void onSensorUpdate(String gas, String temp, String dust, int pir);
    }

    public void addSensorListener(SensorListener listener) {
        this.sensorListener = listener;
    }

    // 서버 시작
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[JAVA] TCP 서버 시작, 포트: " + port);

            Socket clientSocket = serverSocket.accept();
            System.out.println("[JAVA] 클라이언트 연결됨: " + clientSocket.getInetAddress());

            clientOut = new PrintWriter(clientSocket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream())
            );

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[JAVA] 클라이언트로부터 수신: " + line);	
            }

            System.out.println("[JAVA] 클라이언트 연결 종료");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    // GUI에서 서버로 명령 전송
    public void sendCommand(String cmd) {
        if (clientOut != null) {
            clientOut.println(cmd);
            System.out.println("[JAVA] → Python 으로 전송: " + cmd);
        } else {
            System.out.println("[JAVA] 아직 클라이언트가 연결되지 않았어요.");
        }
    }
}
