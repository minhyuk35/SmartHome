import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicBoolean;

public class SmartHomeGUI {

    private TcpServer commandServer;
    private SensorTcpServer sensorServer;
    private DoorlockServer doorlockServer;
    private EventTcpServer eventServer;

    private JLabel lblGas, lblTemp, lblDust, lblPir, lblDoorlock, lblLedStatus;
    private AtomicBoolean voiceRecording = new AtomicBoolean(false);

    // 색상 팔레트 (기존 유지)
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

        lblTemp = createSensorCard(sensorGrid, "온습도 센서", "HUMI: ---", "💧");
        lblGas = createSensorCard(sensorGrid, "가스 센서", "GAS: ---", "🔥");
        lblDust = createSensorCard(sensorGrid, "미세먼지", "DUST: ---", "💨");
        lblPir = createSensorCard(sensorGrid, "모션 감지", "PIR: ---", "🏃");
        lblDoorlock = createSensorCard(sensorGrid, "도어락", "DOOR: ---", "🚪");
        lblLedStatus = createSensorCard(sensorGrid, "조명 제어", "LED: OFF", "💡");

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

        // 🔥 [핵심 수정] 버튼 클릭 시 즉시 UI 업데이트 + 명령 전송
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

        // 얼굴/도어 버튼
        addButton(buttonPanel, "얼굴로 열기", new Color(0, 180, 0), e -> commandServer.sendCommand("REQ_FACE_UNLOCK"));
        addButton(buttonPanel, "얼굴 등록", new Color(50, 50, 50), e -> {
            commandServer.sendCommand("REGISTER_FACE");
            JOptionPane.showMessageDialog(frame, "PC 카메라를 봐주세요.\n's' 키로 저장!");
        });
        
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));
        mainContent.add(buttonPanel);
        mainContent.add(Box.createVerticalStrut(10));

        // 음성 버튼
        ModernButton btnVoice = new ModernButton("🎤 음성 인식", TOSS_BLUE);
        btnVoice.addActionListener(e -> {
            new Thread(() -> {
                try {
                    if (!voiceRecording.get()) {
                        sendVoiceCommand("START_RECORDING");
                        voiceRecording.set(true);
                        SwingUtilities.invokeLater(() -> {
                            btnVoice.setText("⏹ 인식 중지");
                            btnVoice.setBackgroundColor(TOSS_RED);
                        });
                    } else {
                        sendVoiceCommand("STOP_RECORDING");
                        voiceRecording.set(false);
                        SwingUtilities.invokeLater(() -> {
                            btnVoice.setText("🎤 음성 인식");
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

        mainContent.add(rgbPanel);

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        frame.add(scrollPane, BorderLayout.CENTER);

        setupListeners();
        frame.setVisible(true);
    }

    // 🔥 [신규] LED 상태 즉시 업데이트 함수
    private void updateLedStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            lblLedStatus.setText(status);
        });
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

    private void addButton(JPanel parent, String text, Color color, java.awt.event.ActionListener l) {
        ModernButton btn = new ModernButton(text, color);
        btn.addActionListener(l);
        parent.add(btn);
    }

    private void setupListeners() {
        sensorServer.addSensorListener((gas, temp, dust, pir) -> {
            SwingUtilities.invokeLater(() -> {
                lblGas.setText("GAS: " + gas);
                lblTemp.setText("HUMI: " + temp + "%");
                lblDust.setText("DUST: " + dust + " ㎍/m³");
                lblPir.setText("PIR: " + (pir == 1 ? "Motion" : "No Motion"));
                if(pir == 1) lblPir.setForeground(TOSS_RED);
                else lblPir.setForeground(TEXT_PRIMARY);
            });
        });

        doorlockServer.addDoorlockListener(event -> {
            SwingUtilities.invokeLater(() -> {
                lblDoorlock.setText("DOOR: " + event);
                if (event.equals("UNLOCKED")) lblDoorlock.setForeground(TOSS_BLUE);
                else if (event.equals("LOCKED")) lblDoorlock.setForeground(TEXT_PRIMARY);
                else lblDoorlock.setForeground(TOSS_RED);
                eventServer.sendEvent(event);
            });
        });

        // 음성으로 제어했을 때도 화면 바뀌게 (기존 유지)
        commandServer.addCommandListener(cmd -> {
            SwingUtilities.invokeLater(() -> {
                if (cmd.contains("LED_ON")) updateLedStatus("LED: ON");
                else if (cmd.contains("LED_OFF")) updateLedStatus("LED: OFF");
                else if (cmd.contains("RGB")) updateLedStatus("LED: RGB");
                
                // 여기서 다시 sendCommand를 하면 무한루프 돌 수 있으니 제거하거나 주의!
                // (여기서는 외부에서 온 명령을 단순히 화면에 반영만 하면 됨)
            });
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
            if (getModel().isPressed()) g2.setColor(bgColor.darker());
            else g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}