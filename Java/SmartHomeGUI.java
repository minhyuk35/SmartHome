import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SmartHomeGUI {

    private final TcpServer commandServer;
    private final SensorTcpServer sensorServer;
    private final DoorlockServer doorlockServer;
    private final EventTcpServer eventServer;

    private JLabel lblGas, lblTemp, lblDust, lblPir, lblDoorlock, lblLedStatus;
    private final AtomicBoolean voiceRecording = new AtomicBoolean(false);
    private final List<JComponent> gatedControls = new ArrayList<>();
    private boolean unlockedOnce = false;

    // 색상 팔레트
    private static final Color BG_COLOR = new Color(242, 244, 246);
    private static final Color CARD_COLOR = new Color(255, 255, 255);
    private static final Color TEXT_PRIMARY = new Color(25, 31, 40);
    private static final Color TEXT_SECONDARY = new Color(139, 149, 161);
    private static final Color TOSS_BLUE = new Color(49, 130, 246);
    private static final Color TOSS_RED = new Color(255, 80, 80);

    public SmartHomeGUI(TcpServer commandServer, SensorTcpServer sensorServer, DoorlockServer doorlockServer, EventTcpServer eventServer) {
        this.commandServer = commandServer;
        this.sensorServer = sensorServer;
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
        JFrame frame = new JFrame("Toss Style Smart Home");
        frame.setSize(480, 850);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(BG_COLOR);

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(BG_COLOR);
        mainContent.setBorder(new EmptyBorder(20, 20, 20, 20));

        // 헤더
        JLabel titleLabel = new JLabel("Smart Home Monitor");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(titleLabel);
        mainContent.add(Box.createVerticalStrut(20));

        // 센서 그리드
        JPanel sensorGrid = new JPanel(new GridLayout(3, 2, 15, 15));
        sensorGrid.setBackground(BG_COLOR);
        sensorGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblTemp = createSensorCard(sensorGrid, "온습도 센서", "HUMI: ---", "\uD83D\uDCA7");
        lblGas = createSensorCard(sensorGrid, "가스 센서", "GAS: ---", "\uD83D\uDD25");
        lblDust = createSensorCard(sensorGrid, "미세먼지", "DUST: ---", "\uD83D\uDCA8");
        lblPir = createSensorCard(sensorGrid, "모션 감지", "PIR: ---", "\uD83C\uDFC3");
        lblDoorlock = createSensorCard(sensorGrid, "도어락", "DOOR: ---", "\uD83D\uDEAA");
        lblLedStatus = createSensorCard(sensorGrid, "조명 제어", "LED: OFF", "\uD83D\uDCA1");

        sensorGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));
        mainContent.add(sensorGrid);
        mainContent.add(Box.createVerticalStrut(30));

        // 버튼 패널
        JLabel controlLabel = new JLabel("Control Panel");
        controlLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        controlLabel.setForeground(TEXT_PRIMARY);
        controlLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(controlLabel);
        mainContent.add(Box.createVerticalStrut(15));

        JPanel buttonPanel = new JPanel(new GridLayout(5, 2, 10, 10));
        buttonPanel.setBackground(BG_COLOR);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 조명/가전 제어
        addButton(buttonPanel, "LED ON", TOSS_BLUE, e -> {
            updateLedStatus("LED: ON");
            commandServer.sendCommand("LED_ON");
        });
        addButton(buttonPanel, "LED OFF", new Color(200, 200, 200), e -> {
            updateLedStatus("LED: OFF");
            commandServer.sendCommand("LED_OFF");
        });
        addButton(buttonPanel, "FAN ON", TOSS_BLUE, e -> commandServer.sendCommand("FAN_ON"));
        addButton(buttonPanel, "FAN OFF", new Color(200, 200, 200), e -> commandServer.sendCommand("FAN_OFF"));
        addButton(buttonPanel, "SLEEP MODE", new Color(100, 100, 150), e -> commandServer.sendCommand("LIGHT_SLEEP"));
        addButton(buttonPanel, "WARM MODE", new Color(255, 180, 50), e -> commandServer.sendCommand("LIGHT_WARM"));
        addButton(buttonPanel, "RGB ON", new Color(255, 100, 200), e -> {
            updateLedStatus("LED: RGB");
            commandServer.sendCommand("RGB_ON");
        });
        addButton(buttonPanel, "RGB OFF", new Color(200, 200, 200), e -> {
            updateLedStatus("LED: OFF");
            commandServer.sendCommand("RGB_OFF");
        });

        // 잠금 상태에서도 접근 가능한 버튼
        addButton(buttonPanel, "얼굴로 열기", new Color(0, 180, 0), e -> commandServer.sendCommand("REQ_FACE_UNLOCK"), false);
        addButton(buttonPanel, "얼굴 등록", new Color(50, 50, 50), e -> {
            commandServer.sendCommand("REGISTER_FACE");
            JOptionPane.showMessageDialog(frame, "PC 카메라를 봐주세요.\n's' 키로 저장!");
        }, false);

        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        mainContent.add(buttonPanel);
        mainContent.add(Box.createVerticalStrut(10));

        // 음성 버튼
        ModernButton btnVoice = new ModernButton("\uD83C\uDFA4 음성 인식", TOSS_BLUE);
        btnVoice.addActionListener(e -> {
            new Thread(() -> {
                try {
                    if (!voiceRecording.get()) {
                        sendVoiceCommand("START_RECORDING");
                        voiceRecording.set(true);
                        SwingUtilities.invokeLater(() -> {
                            btnVoice.setText("\u23F9 인식 중지");
                            btnVoice.setBackgroundColor(TOSS_RED);
                        });
                    } else {
                        sendVoiceCommand("STOP_RECORDING");
                        voiceRecording.set(false);
                        SwingUtilities.invokeLater(() -> {
                            btnVoice.setText("\uD83C\uDFA4 음성 인식");
                            btnVoice.setBackgroundColor(TOSS_BLUE);
                        });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });

        JPanel voicePanel = new JPanel(new GridLayout(1, 1));
        voicePanel.setBackground(BG_COLOR);
        voicePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        voicePanel.add(btnVoice);
        mainContent.add(voicePanel);
        mainContent.add(Box.createVerticalStrut(20));

        // RGB 슬라이더
        JPanel rgbPanel = new RoundPanel();
        rgbPanel.setLayout(new BoxLayout(rgbPanel, BoxLayout.Y_AXIS));
        rgbPanel.setBackground(CARD_COLOR);
        rgbPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        rgbPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rgbPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        JLabel rgbTitle = new JLabel("RGB Control");
        rgbTitle.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        rgbPanel.add(rgbTitle);
        rgbPanel.add(Box.createVerticalStrut(10));

        JPanel sliderBox = new JPanel(new GridLayout(1, 3, 5, 5));
        sliderBox.setBackground(CARD_COLOR);
        JSlider sR = new JSlider(0, 100, 0); sR.setBackground(CARD_COLOR);
        JSlider sG = new JSlider(0, 100, 0); sG.setBackground(CARD_COLOR);
        JSlider sB = new JSlider(0, 100, 0); sB.setBackground(CARD_COLOR);
        sliderBox.add(sR); sliderBox.add(sG); sliderBox.add(sB);
        rgbPanel.add(sliderBox);

        ModernButton btnApply = new ModernButton("APPLY COLOR", new Color(50, 50, 50));
        btnApply.setPreferredSize(new Dimension(100, 30));
        btnApply.addActionListener(e -> {
            updateLedStatus("LED: RGB");
            commandServer.sendCommand("RGB_SET " + sR.getValue() + " " + sG.getValue() + " " + sB.getValue());
        });
        rgbPanel.add(Box.createVerticalStrut(10));
        rgbPanel.add(btnApply);

        // RGB 제어는 인증 이후 사용 가능
        registerControl(sR);
        registerControl(sG);
        registerControl(sB);
        registerControl(btnApply);

        mainContent.add(rgbPanel);

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        frame.add(scrollPane, BorderLayout.CENTER);

        setupListeners();
        frame.setVisible(true);
    }

    private ModernButton addButton(JPanel panel, String text, Color color, ActionListener action) {
        return addButton(panel, text, color, action, true);
    }

    private ModernButton addButton(JPanel panel, String text, Color color, ActionListener action, boolean gated) {
        ModernButton btn = new ModernButton(text, color);
        btn.addActionListener(action);
        if (gated) {
            registerControl(btn);
        }
        panel.add(btn);
        return btn;
    }

    private void updateLedStatus(String status) {
        SwingUtilities.invokeLater(() -> lblLedStatus.setText(status));
    }

    private JLabel createSensorCard(JPanel parent, String title, String initVal, String icon) {
        RoundPanel card = new RoundPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(CARD_COLOR);
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel titleLbl = new JLabel(icon + " " + title);
        titleLbl.setFont(new Font("맑은 고딕", Font.PLAIN, 12));
        titleLbl.setForeground(TEXT_SECONDARY);
        JLabel valueLbl = new JLabel(initVal);
        valueLbl.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        valueLbl.setForeground(TEXT_PRIMARY);
        valueLbl.setHorizontalAlignment(SwingConstants.RIGHT);
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLbl, BorderLayout.CENTER);
        parent.add(card);
        return valueLbl;
    }

    private void setupListeners() {
        sensorServer.addSensorListener((gas, temp, dust, pir) -> {
            SwingUtilities.invokeLater(() -> {
                lblGas.setText("GAS: " + gas);
                lblTemp.setText("HUMI: " + temp + "%");
                lblDust.setText("DUST: " + dust + " ug/m^3");
                lblPir.setText("PIR: " + (pir == 1 ? "Motion" : "No Motion"));
                lblPir.setForeground(pir == 1 ? TOSS_RED : TEXT_PRIMARY);
            });
        });

        doorlockServer.addDoorlockListener(this::handleDoorEvent);

        // 음성/외부 명령으로 상태가 바뀔 때도 UI 동기화
        commandServer.addCommandListener(this::handleIncomingCommand);
    }

    private void handleIncomingCommand(String rawCmd) {
        String cmd = rawCmd.trim();
        if (cmd.startsWith("RGB_SET")) {
            updateLedStatus("LED: RGB");
            return;
        }
        switch (cmd) {
            case "LED_ON":
                updateLedStatus("LED: ON");
                return;
            case "LED_OFF":
                updateLedStatus("LED: OFF");
                return;
            case "RGB_ON":
                updateLedStatus("LED: RGB");
                return;
            case "RGB_OFF":
                updateLedStatus("LED: OFF");
                return;
            default:
                break;
        }

        if (isDoorEvent(cmd)) {
            handleDoorEvent(cmd);
        }
    }

    private boolean isDoorEvent(String cmd) {
        String upper = cmd.toUpperCase();
        return upper.equals("UNLOCK") || upper.equals("UNLOCKED") || upper.equals("LOCKED") || upper.equals("ALERT_FAIL_3");
    }

    private String normalizeDoorEvent(String event) {
        String upper = event.toUpperCase();
        if (upper.equals("UNLOCK")) return "UNLOCKED";
        return upper;
    }

    private void handleDoorEvent(String event) {
        String normalized = normalizeDoorEvent(event);
        SwingUtilities.invokeLater(() -> {
            lblDoorlock.setText("DOOR: " + normalized);
            if (normalized.equals("UNLOCKED")) {
                lblDoorlock.setForeground(TOSS_BLUE);
                if (!unlockedOnce) {
                    unlockedOnce = true;
                    setControlsEnabled(true);
                }
            } else if (normalized.equals("LOCKED")) {
                lblDoorlock.setForeground(TEXT_PRIMARY);
            } else {
                lblDoorlock.setForeground(TOSS_RED);
            }
            eventServer.sendEvent(normalized);
        });
    }

    private void sendVoiceCommand(String msg) throws Exception {
        java.net.Socket s = new java.net.Socket("127.0.0.1", 40191);
        java.io.PrintWriter out = new java.io.PrintWriter(s.getOutputStream(), true);
        out.println(msg);
        out.close();
        s.close();
    }

    static class RoundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
            g2.dispose();
            super.paintComponent(g);
        }
        public RoundPanel() { setOpaque(false); }
    }

    static class ModernButton extends JButton {
        private Color bgColor;
        public ModernButton(String text, Color color) {
            super(text);
            this.bgColor = color;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("맑은 고딕", Font.BOLD, 14));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        public void setBackgroundColor(Color c) { this.bgColor = c; repaint(); }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isPressed() ? bgColor.darker() : bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
