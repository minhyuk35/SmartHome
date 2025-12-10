import javax.swing.*;
import java.awt.*;

public class SmartHomeGUI {

    private TcpServer commandServer;
    private SensorTcpServer sensorServer;
    private DoorlockServer doorlockServer;
    private EventTcpServer eventServer;

    private JLabel lblDoorlock;

    private JLabel lblGas, lblTemp, lblDust, lblPir;

    public SmartHomeGUI(TcpServer commandServer, SensorTcpServer sensorServer, DoorlockServer doorlockServer, EventTcpServer eventServer) {
        this.commandServer = commandServer;
        this.sensorServer  = sensorServer;
        this.doorlockServer = doorlockServer;
        this.eventServer = eventServer;
    }

    public void showWindow() {

        JFrame frame = new JFrame("Smart Home TCP Test");
        frame.setSize(550, 650);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ================================
        // 🔥 상단 센서 패널
        // ================================
        JPanel sensorPanel = new JPanel();
        sensorPanel.setLayout(new GridLayout(3, 2));
        sensorPanel.setBorder(BorderFactory.createTitledBorder("Sensor Status"));

        lblGas  = new JLabel("GAS: ---",  SwingConstants.CENTER);
        lblTemp = new JLabel("HUMI: ---", SwingConstants.CENTER);
        lblDust = new JLabel("DUST: ---", SwingConstants.CENTER);
        lblPir  = new JLabel("PIR: ---",  SwingConstants.CENTER);
        lblDoorlock = new JLabel("DOOR: ---", SwingConstants.CENTER);

        Font f = new Font("맑은 고딕", Font.BOLD, 16);
        lblGas.setFont(f);
        lblTemp.setFont(f);
        lblDust.setFont(f);
        lblPir.setFont(f);
        lblDoorlock.setFont(f);

        sensorPanel.add(lblGas);
        sensorPanel.add(lblTemp);
        sensorPanel.add(lblDust);
        sensorPanel.add(lblPir);
        sensorPanel.add(lblDoorlock);

        frame.add(sensorPanel, BorderLayout.NORTH);


        // ================================
        // 🔥 버튼 패널
        // ================================
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(4, 2, 10, 10));

        Dimension btnSize = new Dimension(150, 50);

        JButton btnLedOn = new JButton("LED ON");
        JButton btnLedOff = new JButton("LED OFF");
        JButton btnSleep  = new JButton("LIGHT SLEEP");
        JButton btnWarm   = new JButton("LIGHT WARM");
        JButton btnFanOn  = new JButton("FAN ON");
        JButton btnFanOff = new JButton("FAN OFF");
        JButton btnRgbWhite = new JButton("RGB WHITE");
        JButton btnRgbOff   = new JButton("RGB OFF");

        JButton[] btns = {
            btnLedOn, btnLedOff, btnSleep, btnWarm,
            btnFanOn, btnFanOff, btnRgbWhite, btnRgbOff
        };

        for (JButton b : btns) {
            b.setPreferredSize(btnSize);
            b.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        }

        btnLedOn.addActionListener(e -> commandServer.sendCommand("LED_ON"));
        btnLedOff.addActionListener(e -> commandServer.sendCommand("LED_OFF"));
        btnSleep.addActionListener(e -> commandServer.sendCommand("LIGHT_SLEEP"));
        btnWarm.addActionListener(e -> commandServer.sendCommand("LIGHT_WARM"));
        btnFanOn.addActionListener(e -> commandServer.sendCommand("FAN_ON"));
        btnFanOff.addActionListener(e -> commandServer.sendCommand("FAN_OFF"));
        btnRgbWhite.addActionListener(e -> commandServer.sendCommand("RGB_ON"));
        btnRgbOff.addActionListener(e -> commandServer.sendCommand("RGB_OFF"));

        for (JButton b : btns) buttonPanel.add(b);

        frame.add(buttonPanel, BorderLayout.CENTER);

        // ================================
        // 🎨 RGB 슬라이더
        // ================================
        JPanel rgbPanel = new JPanel();
        rgbPanel.setLayout(new GridLayout(4, 1));
        rgbPanel.setBorder(BorderFactory.createTitledBorder("RGB Color Control"));

        JSlider sliderR = new JSlider(0, 100, 0);
        JSlider sliderG = new JSlider(0, 100, 0);
        JSlider sliderB = new JSlider(0, 100, 0);

        rgbPanel.add(new JLabel("Red"));
        rgbPanel.add(sliderR);
        rgbPanel.add(new JLabel("Green"));
        rgbPanel.add(sliderG);
        rgbPanel.add(new JLabel("Blue"));
        rgbPanel.add(sliderB);

        JButton btnApplyColor = new JButton("APPLY COLOR");
        rgbPanel.add(btnApplyColor);

        btnApplyColor.addActionListener(e -> {
            int r = sliderR.getValue();
            int g = sliderG.getValue();
            int b = sliderB.getValue();

            commandServer.sendCommand("RGB_SET " + r + " " + g + " " + b);
        });

        frame.add(rgbPanel, BorderLayout.SOUTH);

        // ================================
        // ⭐ 센서 업데이트 리스너
        // ================================
        sensorServer.addSensorListener((gas, temp, dust, pir) -> {
            SwingUtilities.invokeLater(() -> {
                lblGas.setText("GAS: " + gas);
                lblTemp.setText("HUMI: " + temp + "%");
                lblDust.setText("DUST: " + dust + " ㎍/m³");
                lblPir.setText("PIR: " + (pir == 1 ? "Motion" : "No Motion"));
            });
        });

        // ================================
        // ⭐ 도어락 이벤트 리스너
        // ================================
        doorlockServer.addDoorlockListener(event -> {
            SwingUtilities.invokeLater(() -> {
                lblDoorlock.setText("DOOR: " + event);

                if (event.equals("UNLOCKED")) {
                    lblDoorlock.setBackground(Color.GREEN);
                } else if (event.equals("LOCKED")) {
                    lblDoorlock.setBackground(Color.RED);
                } else if (event.equals("ALERT_FAIL_3")) {
                    lblDoorlock.setBackground(Color.ORANGE);
                }

                lblDoorlock.setOpaque(true);
                eventServer.sendEvent(event);
            });
        });

        frame.setVisible(true);
    }
}
