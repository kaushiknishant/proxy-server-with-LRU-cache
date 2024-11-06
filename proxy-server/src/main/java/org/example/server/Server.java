package org.example.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Server implements Runnable {

    private final Socket clientSocket;

    public Server(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            // Read the request line
            String requestLine = in.readLine();
            if (requestLine != null && !requestLine.isEmpty()) {
                System.out.println("Request: " + requestLine);

                // Extract the URL from the request
                String[] parts = requestLine.split(" ");
                if (parts.length > 1) {
                    String urlPath = parts[1];
                    String targetUrl = urlPath.substring(1); // Remove leading '/'
                    System.out.println("Extracted URL: " + targetUrl);

                    // Forward request to target server
                    forwardRequest(targetUrl, out, in);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void forwardRequest(String targetUrl, PrintWriter out, BufferedReader in) throws IOException {
        int port;
        if (targetUrl.startsWith("https://")) {
            port = 443;
            targetUrl = targetUrl.substring(8); // Remove 'https://'
        } else if (targetUrl.startsWith("http://")) {
            port = 80;
            targetUrl = targetUrl.substring(7); // Remove 'http://'
        } else {
            out.println("HTTP/1.1 400 Bad Request");
            return;
        }

        // Split host and path correctly
        String host;
        String path;
        int slashIndex = targetUrl.indexOf('/');
        if (slashIndex != -1) {
            host = targetUrl.substring(0, slashIndex);
            path = targetUrl.substring(slashIndex); // Get everything after the host
        } else {
            host = targetUrl;
            path = "/"; // Default path if none specified
        }

        // Connect to the target server
        try (Socket serverSocket = new Socket(host, port);
             PrintWriter serverOut = new PrintWriter(serverSocket.getOutputStream(), true);
             BufferedReader serverIn = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()))) {
            // Send HTTP GET request to the target server
            String getRequest = "GET " + path + " HTTP/1.1\r\n" +
                    "Host: " + host + "\r\n" +
                    "Connection: close\r\n\r\n";
            serverOut.print(getRequest);
            serverOut.flush();

//            System.out.println(serverIn.readLine());
            // Read response from the target server and send it back to client
            String responseLine;
            while ((responseLine = serverIn.readLine()) != null) {
                System.out.println(responseLine);
                out.println(responseLine);
                out.flush();
            }

            // Ensure we close output stream after sending all data

        } catch (IOException e) {
            e.printStackTrace();
            out.println("HTTP/1.1 502 Bad Gateway");
        }
    }
}