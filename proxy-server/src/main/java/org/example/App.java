package org.example;

import org.example.server.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Server!
 */
public class App {
    private static final int PORT = 8080;
    private static final int MAX_THREADS = 10;

    public static void main(String[] args) {
        ExecutorService threadPool = Executors.newFixedThreadPool(MAX_THREADS);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Proxy server is running on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                threadPool.execute(new Server(clientSocket));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
