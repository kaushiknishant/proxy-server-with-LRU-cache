package org.example.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;

import org.example.cache.LRUCache;
import org.example.cache.CacheElement;

import javax.net.ssl.SSLSocketFactory;

public class Server implements Runnable {
    private static final int CACHE_SIZE = 200 * (1 << 20); // 200MB cache size
    private static final int MAX_ELEMENT_SIZE = 10 * (1 << 20); // 10MB max element size
    private static final LRUCache cache = new LRUCache(CACHE_SIZE);
    private final Socket clientSocket;

    public Server(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                sendErrorResponse(out, "HTTP/1.1 400 Bad Request\r\n\r\nEmpty request received");
                return;
            }

            System.out.println("Request: " + requestLine);

            // Extract the URL from the request
            String[] parts = requestLine.split(" ");
            if (parts.length < 3) {
                sendErrorResponse(out, "HTTP/1.1 400 Bad Request\r\n\r\nInvalid request format");
                return;
            }

            String urlPath = parts[1];
            // Ignore favicon.ico requests
            if (urlPath.contains("favicon.ico")) {
                sendErrorResponse(out, "HTTP/1.1 404 Not Found\r\n\r\nFavicon not supported");
                return;
            }

            String targetUrl = urlPath.substring(1); // Remove leading '/'
            System.out.println("Extracted URL: " + targetUrl);

            if (targetUrl.isEmpty()) {
                sendErrorResponse(out, "HTTP/1.1 400 Bad Request\r\n\r\nEmpty URL");
                return;
            }

            // Forward request to target server
            forwardRequest(targetUrl, out, in);

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

    private void sendErrorResponse(PrintWriter out, String response) {
        out.println(response);
        out.flush();
    }

    private void forwardRequest(String targetUrl, PrintWriter out, BufferedReader in) throws IOException {
        String host;
        String path;
        int port;

        // Parse URL properly
        if (targetUrl.startsWith("https://")) {
            port = 443;
            host = targetUrl.substring(8); // Remove 'https://'
        } else if (targetUrl.startsWith("http://")) {
            port = 80;
            host = targetUrl.substring(7); // Remove 'http://'
        } else {
            port = 80;
            host = targetUrl;
        }

        // Extract path from host
        int slashIndex = host.indexOf('/');
        if (slashIndex != -1) {
            path = host.substring(slashIndex);
            host = host.substring(0, slashIndex);
        } else {
            path = "/";
        }

        System.out.println("Connecting to - Host: " + host + ", Path: " + path + ", Port: " + port);

        String cacheKey = host + path;
        byte[] cachedResponse;
        
        synchronized (cache) {
            CacheElement element = cache.find(cacheKey);
            if (element != null) {
                // Check if element size exceeds max allowed
                if (element.getData().length > MAX_ELEMENT_SIZE) {
                    cache.remove(cacheKey);
                    cachedResponse = null;
                } else {
                    cachedResponse = element.getData();
                    // Update access time
                    element.setLastAccessTime(Instant.now());
                }
            } else {
                cachedResponse = null;
            }
        }

        if (cachedResponse != null) {
            // Serve from cache with proper headers
            System.out.println("Serving from cache: " + cacheKey);
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html");
            out.println("Content-Length: " + cachedResponse.length);
            out.println("X-Cache: HIT");
            out.println("Connection: close");
            out.println();  // Empty line between headers and body
            out.println(new String(cachedResponse));
            out.flush();
            return;
        }

        // Connect to the target server
        try {
            Socket serverSocket;
            if (port == 443) {
                // Use SSLSocket for HTTPS
                SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                serverSocket = sslSocketFactory.createSocket(host, port);
            } else {
                // Use regular Socket for HTTP
                serverSocket = new Socket(host, port);
            }

            try (PrintWriter serverOut = new PrintWriter(serverSocket.getOutputStream(), true);
                 BufferedReader serverIn = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()))) {
                
                // Modified request format
                String getRequest = String.format("GET %s HTTP/1.1\r\nHost: %s\r\nConnection: close\r\n\r\n", 
                                               path, host);

                serverOut.print(getRequest);
                serverOut.flush();

                // Read the response headers first
                String statusLine = serverIn.readLine();

                StringBuilder headerBuilder = new StringBuilder();
                String line;
                String location = null;

                // Read headers
                while ((line = serverIn.readLine()) != null && !line.isEmpty()) {
                    headerBuilder.append(line).append("\r\n");
                    if (line.toLowerCase().startsWith("location:")) {
                        location = line.substring(9).trim();
                    }
                }

                // Handle redirects (301, 302, 307, 308)
                if (statusLine != null && (statusLine.contains(" 301 ") || 
                                         statusLine.contains(" 302 ") || 
                                         statusLine.contains(" 307 ") || 
                                         statusLine.contains(" 308 ")) && 
                    location != null) {
                    System.out.println("Redirecting to: " + location);
                    forwardRequest(location, out, in);  // Recursive call with new URL
                    return;
                }

                // Forward the original response if not redirecting
                out.println(statusLine);
                out.println(headerBuilder.toString());
                out.println();  // Empty line between headers and body
                out.flush();

                // Forward the response body
                StringBuilder responseBuilder = new StringBuilder();
                responseBuilder.append(statusLine).append("\r\n")
                             .append(headerBuilder.toString())
                             .append("\r\n");

                while ((line = serverIn.readLine()) != null) {
                    responseBuilder.append(line).append("\r\n");
                    out.println(line);
                    out.flush();
                }

                // Modified caching logic
                if (responseBuilder.length() > 0) {
                    byte[] responseBytes = responseBuilder.toString().getBytes();
                    if (responseBytes.length <= MAX_ELEMENT_SIZE) {
                        synchronized (cache) {
                            // Remove old entries if needed
                            while (cache.getCurrentSize() + responseBytes.length > CACHE_SIZE) {
                                cache.removeLeastRecentlyUsed();
                            }
                            cache.addCacheElement(responseBytes, responseBytes.length, cacheKey, Instant.now());
                        }
                    }
                }
            } finally {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            sendErrorResponse(out, "HTTP/1.1 502 Bad Gateway\r\n\r\nError connecting to target server");
        }
    }
}