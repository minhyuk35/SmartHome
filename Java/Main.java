public class Main {

    public static void main(String[] args) {

        int COMMAND_PORT = 39186;
        int SENSOR_PORT  = 39187;
        int DOOR_PORT    = 39189; // Python/Jupyter door-event feed

        // 1) 서버 객체 생성
        TcpServer commandServer = new TcpServer(COMMAND_PORT);
        SensorTcpServer sensorServer = new SensorTcpServer(SENSOR_PORT);
        DoorlockServer doorlockServer = new DoorlockServer(DOOR_PORT);

        // 2) GUI 생성
        SmartHomeGUI gui = new SmartHomeGUI(commandServer, sensorServer, doorlockServer);
        gui.showWindow();

        // 3) 서버 실행
        commandServer.start();
        sensorServer.start();
        doorlockServer.start();

        System.out.println("[JAVA] 메인 초기화 완료");
    }
}
