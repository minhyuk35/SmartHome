package com.smarthome.web.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthome.web.service.CommandService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ControlWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final CommandService commandService;
    private final WebSocketBroadcaster broadcaster;

    public ControlWebSocketHandler(CommandService commandService, WebSocketBroadcaster broadcaster) {
        this.commandService = commandService;
        this.broadcaster = broadcaster;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        broadcaster.register(session);
        broadcaster.broadcast("info", "Web client connected");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JsonNode root = mapper.readTree(message.getPayload());
            String type = root.path("type").asText("");

            switch (type) {
                case "command" -> {
                    String cmd = root.path("command").asText("");
                    if (!cmd.isBlank()) commandService.sendCommand(cmd);
                }
                case "rgb" -> {
                    int r = root.path("r").asInt(0);
                    int g = root.path("g").asInt(0);
                    int b = root.path("b").asInt(0);
                    commandService.sendRgb(r, g, b);
                }
                case "voice" -> {
                    String action = root.path("action").asText("");
                    if ("start".equalsIgnoreCase(action)) commandService.voiceStart();
                    else if ("stop".equalsIgnoreCase(action)) commandService.voiceStop();
                }
                default -> broadcaster.broadcast("warn", "Unknown message type: " + type);
            }
        } catch (Exception e) {
            broadcaster.broadcast("error", "Failed to handle message: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        broadcaster.unregister(session);
    }
}
