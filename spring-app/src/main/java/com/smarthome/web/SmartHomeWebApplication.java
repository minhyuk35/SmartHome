package com.smarthome.web;

import com.smarthome.web.tcp.SensorTcpServer;
import com.smarthome.web.tcp.DoorEventTcpServer;
import com.smarthome.web.tcp.TcpCommandServer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SmartHomeWebApplication implements CommandLineRunner {

    private final TcpCommandServer commandServer;
    private final SensorTcpServer sensorTcpServer;
    private final DoorEventTcpServer doorEventTcpServer;

    public SmartHomeWebApplication(
            TcpCommandServer commandServer,
            SensorTcpServer sensorTcpServer,
            DoorEventTcpServer doorEventTcpServer
    ) {
        this.commandServer = commandServer;
        this.sensorTcpServer = sensorTcpServer;
        this.doorEventTcpServer = doorEventTcpServer;
    }

    public static void main(String[] args) {
        SpringApplication.run(SmartHomeWebApplication.class, args);
    }

    @Override
    public void run(String... args) {
        commandServer.start(); // commands to/from Python
        sensorTcpServer.start(); // sensor feed from Python
        doorEventTcpServer.start(); // door events (LOCKED/UNLOCKED) from RPi
    }
}
