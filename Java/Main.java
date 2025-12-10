public class Main {

    public static void main(String[] args) {

        int COMMAND_PORT = 39186; 
        int SENSOR_PORT  = 39187; 
        int DOOR_PORT    = 39188;  
        int EVENT_PORT   = 39189;  

        // 1) 서버 객체 생성
        TcpServer commandServer = new TcpServer(COMMAND_PORT);
        SensorTcpServer sensorServer = new SensorTcpServer(SENSOR_PORT);
        DoorlockServer doorlockServer = new DoorlockServer(DOOR_PORT);
        EventTcpServer eventServer = new EventTcpServer(EVENT_PORT); 

        // 2) GUI 생성 
        SmartHomeGUI gui = new SmartHomeGUI(commandServer, sensorServer, doorlockServer, eventServer);
        gui.showWindow();

        // 3) 서버 실행
        new Thread(() -> commandServer.start()).start();
        new Thread(() -> sensorServer.start()).start();
        new Thread(() -> eventServer.start()).start();
        new Thread(() -> doorlockServer.start()).start();

        System.out.println("[JAVA] 메인 초기화 완료");
    }
}
