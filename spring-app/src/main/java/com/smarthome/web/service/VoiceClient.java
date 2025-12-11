package com.smarthome.web.service;

import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.net.Socket;

@Component
public class VoiceClient {

    private static final String HOST = "127.0.0.1";
    private static final int PORT = 40191;

    public void send(String msg) {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(msg);
        } catch (Exception ignored) {
        }
    }
}
