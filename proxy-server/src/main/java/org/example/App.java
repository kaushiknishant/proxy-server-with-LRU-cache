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
            
            // Add shutdown hook to clean up resources
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                threadPool.shutdown();
                System.out.println("Server shutting down...");
            }));
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected from " + clientSocket.getInetAddress());
                try {
                    threadPool.execute(new Server(clientSocket));
                } catch (Exception e) {
                    System.err.println("Error handling client connection: " + e.getMessage());
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            threadPool.shutdown();
        }
    }
}
