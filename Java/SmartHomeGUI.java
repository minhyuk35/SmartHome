import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SmartHomeGUI {

    private final TcpServer commandServer;
    private final SensorTcpServer sensorServer;
    private final DoorlockServer doorlockServer;

    private JLabel lblGas, lblTemp, lblDust, lblPir, lblDoorlock, lblLedStatus;
    private final AtomicBoolean voiceRecording = new AtomicBoolean(false);
    private final List<JComponent> gatedControls = new ArrayList<>();
    private boolean unlockedOnce = false;
    private long lastPirTriggerMs = 0L;
    private static final long PIR_COOLDOWN_MS = 5000;

    // 🔥 [다크 모드 팔레트]
    private static final Color BG_COLOR = new Color(30, 30, 40);       
    private static final Color CARD_COLOR = new Color(45, 45, 55);     
    private static final Color TEXT_WHITE = new Color(255, 255, 255);  
    private static final Color TEXT_GRAY = new Color(170, 170, 190);   

    // ✨ 포인트 컬러 (형광)
    private static final Color NEON_BLUE = new Color(50, 150, 255);
    private static final Color NEON_RED = new Color(255, 80, 80);
    private static final Color NEON_GREEN = new Color(0, 220, 130);
    private static final Color NEON_YELLOW = new Color(255, 200, 50);
    private static final Color NEON_PURPLE = new Color(180, 100, 255);
    
    // 버튼 색상
    private static final Color BTN_OFF_BG = new Color(70, 70, 80);
    private static final Color BTN_TEXT_ON = new Color(20, 20, 30);
    private static final Color BTN_TEXT_OFF = new Color(240, 240, 240);

    // 폰트 (가독성 중심)
    private static final Font FONT_TITLE = new Font("Arial Black", Font.BOLD, 28);
    private static final Font FONT_VALUE = new Font("Verdana", Font.BOLD, 22);
    private static final Font FONT_LABEL = new Font("맑은 고딕", Font.BOLD, 15);
    private static final Font FONT_BTN = new Font("맑은 고딕", Font.BOLD, 15);

    public SmartHomeGUI(TcpServer commandServer, SensorTcpServer sensorServer, DoorlockServer doorlockServer) {
        this.commandServer = commandServer;
        this.sensorServer = sensorServer;
        this.doorlockServer = doorlockServer;
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
        JFrame frame = new JFrame("Smart Home Controller");
        frame.setSize(520, 950);
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(BG_COLOR);

        JPanel mainContent = new JPanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(BG_COLOR);
        mainContent.setBorder(new EmptyBorder(30, 30, 30, 30));

        // 1. 헤더
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        
        JLabel titleLabel = new JLabel("MY HOME");
        titleLabel.setFont(FONT_TITLE);
        titleLabel.setForeground(TEXT_WHITE);
        
        JLabel subtitleLabel = new JLabel("● SYSTEM ONLINE");
        subtitleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        subtitleLabel.setForeground(NEON_GREEN);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(subtitleLabel, BorderLayout.EAST);
        mainContent.add(headerPanel);
        mainContent.add(Box.createVerticalStrut(30));

        // 2. 센서 모니터링
        JLabel labelSection1 = new JLabel("MONITORING");
        labelSection1.setFont(new Font("Arial", Font.BOLD, 14));
        labelSection1.setForeground(TEXT_GRAY);
        labelSection1.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(labelSection1);
        mainContent.add(Box.createVerticalStrut(10));

        JPanel sensorGrid = new JPanel(new GridLayout(3, 2, 15, 15));
        sensorGrid.setBackground(BG_COLOR);
        sensorGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 🔥 [아이콘 제거] 텍스트와 컬러 바로만 승부!
        lblTemp = createSensorCard(sensorGrid, "Humidity", "--- %", NEON_BLUE);
        lblGas = createSensorCard(sensorGrid, "Gas Level", "---", NEON_RED);
        lblDust = createSensorCard(sensorGrid, "Fine Dust", "---", NEON_YELLOW);
        lblPir = createSensorCard(sensorGrid, "Motion", "---", NEON_PURPLE);
        lblDoorlock = createSensorCard(sensorGrid, "Door Lock", "Locked", NEON_GREEN);
        lblLedStatus = createSensorCard(sensorGrid, "Light", "OFF", TEXT_WHITE);

        sensorGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 360));
        mainContent.add(sensorGrid);
        mainContent.add(Box.createVerticalStrut(40));

        // 3. 컨트롤 패널
        JLabel labelSection2 = new JLabel("CONTROLS");
        labelSection2.setFont(new Font("Arial", Font.BOLD, 14));
        labelSection2.setForeground(TEXT_GRAY);
        labelSection2.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(labelSection2);
        mainContent.add(Box.createVerticalStrut(10));

        JPanel buttonPanel = new JPanel(new GridLayout(5, 2, 12, 12));
        buttonPanel.setBackground(BG_COLOR);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // 버튼
        addButton(buttonPanel, "LED ON", NEON_BLUE, BTN_TEXT_ON, e -> {
            updateLedStatus("ON");
            commandServer.sendCommand("LED_ON");
        });
        addButton(buttonPanel, "LED OFF", BTN_OFF_BG, BTN_TEXT_OFF, e -> {
            updateLedStatus("OFF");
            commandServer.sendCommand("LED_OFF");
        });

        addButton(buttonPanel, "FAN ON", NEON_BLUE, BTN_TEXT_ON, e -> commandServer.sendCommand("FAN_ON"));
        addButton(buttonPanel, "FAN OFF", BTN_OFF_BG, BTN_TEXT_OFF, e -> commandServer.sendCommand("FAN_OFF"));

        addButton(buttonPanel, "SLEEP MODE", new Color(130, 100, 255), BTN_TEXT_ON, e -> commandServer.sendCommand("LIGHT_SLEEP"));
        addButton(buttonPanel, "WARM MODE", new Color(255, 170, 50), BTN_TEXT_ON, e -> commandServer.sendCommand("LIGHT_WARM"));

        addButton(buttonPanel, "RGB ON", new Color(255, 80, 180), BTN_TEXT_ON, e -> {
            updateLedStatus("RGB");
            commandServer.sendCommand("RGB_ON");
        });
        addButton(buttonPanel, "RGB OFF", BTN_OFF_BG, BTN_TEXT_OFF, e -> {
            updateLedStatus("OFF");
            commandServer.sendCommand("RGB_OFF");
        });

        // 보안
        addButton(buttonPanel, "Face Unlock", NEON_GREEN, BTN_TEXT_ON, e -> commandServer.sendCommand("REQ_FACE_UNLOCK"), false);
        addButton(buttonPanel, "Register Face", new Color(100, 100, 100), BTN_TEXT_OFF, e -> {
            commandServer.sendCommand("REGISTER_FACE");
            JOptionPane.showMessageDialog(frame, "카메라를 봐주세요.\n's' 키를 누르면 저장됩니다.");
        }, false);

        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 320));
        mainContent.add(buttonPanel);
        mainContent.add(Box.createVerticalStrut(15));

        // 4. 음성 인식 버튼
        ModernButton btnVoice = new ModernButton("Voice Command", NEON_BLUE, BTN_TEXT_ON);
        btnVoice.addActionListener(e -> {
            new Thread(() -> {
                try {
                    if (!voiceRecording.get()) {
                        sendVoiceCommand("START_RECORDING");
                        voiceRecording.set(true);
                        SwingUtilities.invokeLater(() -> {
                            btnVoice.setText("Listening...");
                            btnVoice.setColors(NEON_RED, BTN_TEXT_ON);
                        });
                    } else {
                        sendVoiceCommand("STOP_RECORDING");
                        voiceRecording.set(false);
                        SwingUtilities.invokeLater(() -> {
                            btnVoice.setText("Voice Command");
                            btnVoice.setColors(NEON_BLUE, BTN_TEXT_ON);
                        });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();
        });
        
        JPanel voicePanel = new JPanel(new GridLayout(1, 1));
        voicePanel.setBackground(BG_COLOR);
        voicePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        voicePanel.add(btnVoice);
        mainContent.add(voicePanel);
        mainContent.add(Box.createVerticalStrut(30));

        // 5. RGB 슬라이더
        JPanel rgbPanel = new RoundPanel();
        rgbPanel.setLayout(new BoxLayout(rgbPanel, BoxLayout.Y_AXIS));
        rgbPanel.setBackground(CARD_COLOR);
        rgbPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        rgbPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        rgbPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        JLabel rgbTitle = new JLabel("RGB Color Picker");
        rgbTitle.setFont(new Font("Arial", Font.BOLD, 14));
        rgbTitle.setForeground(TEXT_GRAY);
        rgbPanel.add(rgbTitle);
        rgbPanel.add(Box.createVerticalStrut(15));

        JPanel sliderBox = new JPanel(new GridLayout(3, 1, 5, 10));
        sliderBox.setBackground(CARD_COLOR);
        
        JSlider sR = createDarkSlider(0, 100, 0, NEON_RED);
        JSlider sG = createDarkSlider(0, 100, 0, NEON_GREEN);
        JSlider sB = createDarkSlider(0, 100, 0, NEON_BLUE);
        
        sliderBox.add(sR); sliderBox.add(sG); sliderBox.add(sB);
        rgbPanel.add(sliderBox);
        
        rgbPanel.add(Box.createVerticalStrut(15));
        ModernButton btnApply = new ModernButton("Apply Color", BTN_OFF_BG, BTN_TEXT_OFF);
        btnApply.setPreferredSize(new Dimension(100, 40));
        btnApply.addActionListener(e -> {
            updateLedStatus("RGB");
            commandServer.sendCommand("RGB_SET " + sR.getValue() + " " + sG.getValue() + " " + sB.getValue());
        });
        
        registerControl(sR); registerControl(sG); registerControl(sB); registerControl(btnApply);
        rgbPanel.add(btnApply);
        mainContent.add(rgbPanel);

        JScrollPane scrollPane = new JScrollPane(mainContent);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        frame.add(scrollPane, BorderLayout.CENTER);

        setupListeners();
        frame.setVisible(true);
    }

    // --- Helper Methods ---

    private ModernButton addButton(JPanel panel, String text, Color bgColor, Color textColor, ActionListener action) {
        return addButton(panel, text, bgColor, textColor, action, true);
    }

    private ModernButton addButton(JPanel panel, String text, Color bgColor, Color textColor, ActionListener action, boolean gated) {
        ModernButton btn = new ModernButton(text, bgColor, textColor);
        btn.addActionListener(action);
        if (gated) registerControl(btn);
        panel.add(btn);
        return btn;
    }

    private JSlider createDarkSlider(int min, int max, int val, Color trackColor) {
        JSlider slider = new JSlider(min, max, val);
        slider.setBackground(CARD_COLOR);
        slider.setUI(new DarkSliderUI(slider, trackColor));
        return slider;
    }

    private void updateLedStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            lblLedStatus.setText(status);
            if(status.contains("ON")) lblLedStatus.setForeground(NEON_YELLOW);
            else if(status.contains("RGB")) lblLedStatus.setForeground(NEON_PURPLE);
            else lblLedStatus.setForeground(TEXT_WHITE);
        });
    }

    // 🔥 [수정] 아이콘 제거, 텍스트 배치 최적화
    private JLabel createSensorCard(JPanel parent, String title, String initVal, Color accentColor) {
        RoundPanel card = new RoundPanel();
        card.setLayout(new BorderLayout());
        card.setBackground(CARD_COLOR);
        card.setBorder(new EmptyBorder(20, 25, 20, 25)); // 여백 좀 더 확보

        // 왼쪽: 제목
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_LABEL);
        titleLbl.setForeground(TEXT_GRAY);
        
        // 오른쪽: 값 (크게)
        JLabel valueLbl = new JLabel(initVal);
        valueLbl.setFont(FONT_VALUE);
        valueLbl.setForeground(TEXT_WHITE);
        valueLbl.setHorizontalAlignment(SwingConstants.RIGHT);

        // 하단: 컬러 바 (포인트)
        JPanel bar = new JPanel();
        bar.setBackground(accentColor);
        bar.setPreferredSize(new Dimension(parent.getWidth(), 4)); // 전체 너비
        
        // 레이아웃 배치
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(Box.createVerticalStrut(10));
        card.add(valueLbl, BorderLayout.CENTER);
        card.add(bar, BorderLayout.SOUTH);

        parent.add(card);
        return valueLbl;
    }

    private void setupListeners() {
        sensorServer.addSensorListener((gas, temp, dust, pir) -> {
            SwingUtilities.invokeLater(() -> {
                lblGas.setText(gas);
                lblTemp.setText(temp + "%");
                lblDust.setText(dust + " ug");
                lblPir.setText(pir == 1 ? "DETECTED" : "SAFE");
                lblPir.setForeground(pir == 1 ? NEON_RED : TEXT_WHITE);
                try {
                    int gasVal = Integer.parseInt(gas);
                    int dustVal = Integer.parseInt(dust);
                    lblGas.setForeground(gasVal > 500 ? NEON_RED : TEXT_WHITE);
                    lblDust.setForeground(dustVal > 80 ? NEON_RED : (dustVal > 30 ? NEON_YELLOW : TEXT_WHITE));
                } catch (NumberFormatException e) { }
            });
            handlePirAuth(pir);
        });

        doorlockServer.addDoorlockListener(this::handleDoorEvent);
        commandServer.addCommandListener(this::handleIncomingCommand);
    }

    private void handlePirAuth(int pir) {
        if (pir != 1) return;
        if (unlockedOnce) return;
        long now = System.currentTimeMillis();
        if (now - lastPirTriggerMs < PIR_COOLDOWN_MS) return;
        lastPirTriggerMs = now;
        commandServer.sendCommand("PROMPT_AUTH");
        commandServer.sendCommand("REQ_FACE_UNLOCK");
    }

    private void handleIncomingCommand(String rawCmd) {
        String cmd = rawCmd.trim();
        if (cmd.startsWith("RGB_SET")) { updateLedStatus("RGB"); return; }
        if (isDoorEvent(cmd)) { handleDoorEvent(cmd); }
        else {
            switch (cmd) {
                case "LED_ON": updateLedStatus("ON"); break;
                case "LED_OFF": updateLedStatus("OFF"); break;
                case "RGB_ON": updateLedStatus("RGB"); break;
                case "RGB_OFF": updateLedStatus("OFF"); break;
            }
        }
    }

    private boolean isDoorEvent(String cmd) {
        String upper = cmd.toUpperCase();
        return upper.equals("UNLOCK") || upper.equals("UNLOCKED") || upper.equals("LOCKED") || upper.equals("ALERT_FAIL_3");
    }

    private void handleDoorEvent(String event) {
        String normalized = event.toUpperCase().equals("UNLOCK") ? "UNLOCKED" : event.toUpperCase();
        SwingUtilities.invokeLater(() -> {
            lblDoorlock.setText(normalized);
            if (normalized.equals("UNLOCKED")) {
                lblDoorlock.setForeground(NEON_GREEN);
                if (!unlockedOnce) { unlockedOnce = true; setControlsEnabled(true); }
            } else if (normalized.equals("LOCKED")) { lblDoorlock.setForeground(TEXT_WHITE);
            } else { lblDoorlock.setForeground(NEON_RED); }
        });
    }

    private void sendVoiceCommand(String msg) throws Exception {
        java.net.Socket s = new java.net.Socket("127.0.0.1", 40191);
        java.io.PrintWriter out = new java.io.PrintWriter(s.getOutputStream(), true);
        out.println(msg);
        out.close(); s.close();
    }

    // --- Custom UI Classes ---

    static class RoundPanel extends JPanel {
        public RoundPanel() { setOpaque(false); }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class ModernButton extends JButton {
        private Color bgColor;
        private Color textColor;
        private boolean isHovered = false;
        
        public ModernButton(String text, Color bgColor, Color textColor) {
            super(text);
            this.bgColor = bgColor;
            this.textColor = textColor;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(textColor);
            setFont(FONT_BTN);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
            });
        }

        public void setColors(Color bg, Color text) {
            this.bgColor = bg;
            this.textColor = text;
            setForeground(text);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            Color drawColor = bgColor;
            if (getModel().isPressed()) {
                drawColor = bgColor.darker();
            } else if (isHovered) {
                drawColor = new Color(
                    Math.min(255, bgColor.getRed() + 30),
                    Math.min(255, bgColor.getGreen() + 30),
                    Math.min(255, bgColor.getBlue() + 30)
                );
            }
            g2.setColor(drawColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class DarkSliderUI extends BasicSliderUI {
        private final Color trackColor;
        public DarkSliderUI(JSlider b, Color trackColor) { super(b); this.trackColor = trackColor; }
        @Override
        public void paintTrack(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle t = trackRect;
            g2.setColor(new Color(60, 60, 80));
            g2.fillRoundRect(t.x, t.y + (t.height / 2) - 3, t.width, 6, 6, 6);
            int fillW = xPositionForValue(slider.getValue()) - t.x;
            g2.setColor(trackColor);
            g2.fillRoundRect(t.x, t.y + (t.height / 2) - 3, fillW, 6, 6, 6);
        }
        @Override
        public void paintThumb(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TEXT_WHITE);
            g2.fillOval(thumbRect.x, thumbRect.y + (thumbRect.height / 2) - 9, 18, 18);
            g2.setColor(trackColor.darker());
            g2.setStroke(new BasicStroke(2));
            g2.drawOval(thumbRect.x+1, thumbRect.y + (thumbRect.height / 2) - 8, 16, 16);
        }
    }
}