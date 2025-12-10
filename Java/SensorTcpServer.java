import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class SensorTcpServer {

    private int port;

    // Python -> Java : 센서값 전달 리스너
    private SensorListener sensorListener;

    public SensorTcpServer(int port) {
        this.port = port;
    }

    // GUI에 센서 값 전달을 위한 콜백
    public interface SensorListener {
        void onSensorUpdate(String gas, String temp, String dust, int pir);
    }

    public void addSensorListener(SensorListener listener) {
        this.sensorListener = listener;
    }

    // =============== 센서 수신 서버 ===============
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[JAVA] SensorServer 대기, 포트: " + port);

            // Python 센서 스트림과 지속 연결
            Socket clientSocket = serverSocket.accept();
            System.out.println("[JAVA] 센서 클라이언트 연결: " + clientSocket.getInetAddress());

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream(), "UTF-8")
            );

            String line;

            while ((line = in.readLine()) != null) {
                if (line.startsWith("SENSOR")) {
                    parseSensorPacket(line);
                }
            }

            System.out.println("[JAVA] 센서 연결 종료");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // SENSOR 메시지 파싱
    private void parseSensorPacket(String msg) {
        try {
            // 예: SENSOR GAS=123 METHAN=1 TEMP=25.50 HUMI=36.70 PM1=7 PM25=5 PM10=8 PIR=0
            String[] p = msg.split(" ");

            String gas  = p[1].split("=")[1];
            String temp = p[3].split("=")[1];
            String dust = p[7].split("=")[1];
            int pir = Integer.parseInt(p[8].split("=")[1]);

            if (sensorListener != null) {
                sensorListener.onSensorUpdate(gas, temp, dust, pir);
            }

        } catch (Exception e) {
            System.out.println("[JAVA] 센서 파싱 오류: " + e);
        }
    }
}
