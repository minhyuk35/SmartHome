import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SmartHomeGUI {

    private TcpServer commandServer;
    private SensorTcpServer sensorServer;
    private DoorlockServer doorlockServer;
    private EventTcpServer eventServer;

    private JLabel lblDoorlock;

    private JLabel lblGas, lblTemp, lblDust, lblPir;
    private JLabel lblLedStatus;
    private AtomicBoolean voiceRecording = new AtomicBoolean(false);
    private final List<JComponent> gatedControls = new ArrayList<>();
    private boolean unlockedOnce = false;

    public SmartHomeGUI(TcpServer commandServer, SensorTcpServer sensorServer, DoorlockServer doorlockServer, EventTcpServer eventServer) {
        this.commandServer = commandServer;
        this.sensorServer  = sensorServer;
        this.doorlockServer = doorlockServer;
        this.eventServer = eventServer;
    }

    private void registerControl(JComponent component) {
        component.setEnabled(unlockedOnce);
        gatedControls.add(component);
    }

    private void setControlsEnabled(boolean enabled) {
        for (JComponent component : gatedControls) {
            component.setEnabled(enabled);
        }
    }

    public void showWindow() {

        JFrame frame = new JFrame("Smart Home TCP Test");
        frame.setSize(550, 650);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // ================================
        // 상단 센서 패널
        // ================================
        JPanel sensorPanel = new JPanel();
        sensorPanel.setLayout(new GridLayout(3, 2));
        sensorPanel.setBorder(BorderFactory.createTitledBorder("Sensor Status"));

        lblGas  = new JLabel("GAS: ---",  SwingConstants.CENTER);
        lblTemp = new JLabel("HUMI: ---", SwingConstants.CENTER);
        lblDust = new JLabel("DUST: ---", SwingConstants.CENTER);
        lblPir  = new JLabel("PIR: ---",  SwingConstants.CENTER);
        lblDoorlock = new JLabel("DOOR: ---", SwingConstants.CENTER);
        lblLedStatus = new JLabel("LED: OFF", SwingConstants.CENTER);

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
        sensorPanel.add(lblLedStatus);

        frame.add(sensorPanel, BorderLayout.NORTH);


        // ================================
        // 중단 버튼 패널
        // ================================
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(5, 2, 10, 10));

        Dimension btnSize = new Dimension(150, 50);

        JButton btnLedOn = new JButton("LED ON");
        JButton btnLedOff = new JButton("LED OFF");
        JButton btnSleep  = new JButton("LIGHT SLEEP");
        JButton btnWarm   = new JButton("LIGHT WARM");
        JButton btnFanOn  = new JButton("FAN ON");
        JButton btnFanOff = new JButton("FAN OFF");
        JButton btnRgbWhite = new JButton("RGB WHITE");
        JButton btnRgbOff   = new JButton("RGB OFF");
        JButton btnVoice  = new JButton("🎤 음성 인식");

        JButton[] btns = {
            btnLedOn, btnLedOff, btnSleep, btnWarm,
            btnFanOn, btnFanOff, btnRgbWhite, btnRgbOff,
            btnVoice
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
        
        // 음성 인식 토글 버튼 (누르면 START/STOP 전송)
        btnVoice.addActionListener(e -> {
            new Thread(() -> {
                try {
                    if (!voiceRecording.get()) {
                        java.net.Socket s = new java.net.Socket("127.0.0.1", 40191);
                        java.io.PrintWriter out = new java.io.PrintWriter(s.getOutputStream(), true);
                        out.println("START_RECORDING");
                        out.close();
                        s.close();
                        voiceRecording.set(true);
                        SwingUtilities.invokeLater(() -> btnVoice.setText("⏹ 음성 인식 중지"));
                        System.out.println("[JAVA] 음성 인식 시작 요청 전송");
                    } else {
                        java.net.Socket s = new java.net.Socket("127.0.0.1", 40191);
                        java.io.PrintWriter out = new java.io.PrintWriter(s.getOutputStream(), true);
                        out.println("STOP_RECORDING");
                        out.close();
                        s.close();
                        voiceRecording.set(false);
                        SwingUtilities.invokeLater(() -> btnVoice.setText("🎤 음성 인식"));
                        System.out.println("[JAVA] 음성 인식 중지 요청 전송");
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, "Python 서버 연결 실패\n" + ex.getMessage(), "에러", JOptionPane.ERROR_MESSAGE);
                }
            }).start();
        });

        for (JButton b : btns) buttonPanel.add(b);

        frame.add(buttonPanel, BorderLayout.CENTER);

        // ================================
        // 하단 RGB 슬라이더
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

        for (JButton b : btns) {
            registerControl(b);
        }
        registerControl(sliderR);
        registerControl(sliderG);
        registerControl(sliderB);
        registerControl(btnApplyColor);

        frame.add(rgbPanel, BorderLayout.SOUTH);

        // ================================
        // 센서 서버 데이터 업데이트
        // ================================
        sensorServer.addSensorListener((gas, temp, dust, pir) -> {
            SwingUtilities.invokeLater(() -> {
                lblGas.setText("GAS: " + gas);
                lblTemp.setText("HUMI: " + temp + "%");
                lblDust.setText("DUST: " + dust + " ㎍/m³");
                lblPir.setText("PIR: " + (pir == 1 ? "Motion" : "No Motion"));
            });
        });

        // TcpServer에서 받는 명령을 GUI 상태에 반영
        commandServer.addCommandListener(cmd -> {
            System.out.println("[JAVA-GUI] TcpServer 명령 수신: " + cmd);

            SwingUtilities.invokeLater(() -> {
                String c = cmd.trim();
                if (c.equalsIgnoreCase("LED_ON")) {
                    lblLedStatus.setText("LED: ON");
                    lblLedStatus.setOpaque(true);
                    lblLedStatus.setBackground(Color.GREEN);
                } else if (c.equalsIgnoreCase("LED_OFF")) {
                    lblLedStatus.setText("LED: OFF");
                    lblLedStatus.setOpaque(true);
                    lblLedStatus.setBackground(Color.LIGHT_GRAY);
                } else if (c.startsWith("RGB_SET") || c.equalsIgnoreCase("RGB_ON")) {
                    lblLedStatus.setText("LED: RGB");
                    lblLedStatus.setOpaque(true);
                    lblLedStatus.setBackground(Color.MAGENTA);
                }
            });

            // 음성(STT) 등에서 들어온 명령을 IoT 클라이언트에도 브로드캐스트
            commandServer.sendCommand(cmd);
        });

        // 도어락 이벤트 리스너
        doorlockServer.addDoorlockListener(event -> {
            SwingUtilities.invokeLater(() -> {
                lblDoorlock.setText("DOOR: " + event);

                if (event.equals("UNLOCKED")) {
                    lblDoorlock.setBackground(Color.GREEN);
                    if (!unlockedOnce) {
                        unlockedOnce = true;
                        setControlsEnabled(true);
                    }
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
