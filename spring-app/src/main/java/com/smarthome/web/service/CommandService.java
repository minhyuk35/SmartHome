package com.smarthome.web.service;

import com.smarthome.web.tcp.TcpCommandServer;
import org.springframework.stereotype.Service;

@Service
public class CommandService {

    private final TcpCommandServer tcpCommandServer;
    private final VoiceClient voiceClient;

    public CommandService(TcpCommandServer tcpCommandServer, VoiceClient voiceClient) {
        this.tcpCommandServer = tcpCommandServer;
        this.voiceClient = voiceClient;
    }

    public void sendCommand(String cmd) {
        tcpCommandServer.sendCommand(cmd);
    }

    public void sendRgb(int r, int g, int b) {
        tcpCommandServer.sendCommand("RGB_SET " + r + " " + g + " " + b);
    }

    public void voiceStart() {
        voiceClient.send("START_RECORDING");
    }

    public void voiceStop() {
        voiceClient.send("STOP_RECORDING");
    }
}
