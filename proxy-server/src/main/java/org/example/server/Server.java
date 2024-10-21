package org.example.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class Server implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("GET".equals(exchange.getRequestMethod())) {
            String requestURI = exchange.getRequestURI().toString();
            String urlToRedirect = requestURI.substring(1); // Remove leading "/"

            if (urlToRedirect.startsWith("http://") || urlToRedirect.startsWith("https://")) {
                exchange.getResponseHeaders().add("Location", urlToRedirect);
                exchange.sendResponseHeaders(302, -1);
            } else {
                String response = "Invalid URL format. Please provide a valid URL.";
                exchange.sendResponseHeaders(400, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
        }
    }
}
