package org.example.server;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ServerTest {

    private static HttpServer httpServer;

    @BeforeAll
    public static void setUp() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
        httpServer.createContext("/", new Server());
        httpServer.start();
    }

    @AfterAll
    public static void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    void testRedirectValidUrl() throws IOException {
        String validUrl = "https://www.geeksforgeeks.org/";
        URL url = new URL("http://localhost:8080/" + validUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        assertEquals(302, connection.getResponseCode());

        String redirectLocation = connection.getHeaderField("Location");
        assertEquals(validUrl, redirectLocation);
    }

    @Test
    void testInvalidUrlFormat() throws IOException {
        URL url = new URL("http://localhost:8080/invalid-url");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        assertEquals(400, connection.getResponseCode());
    }

    @Test
    void testMethodNotAllowed() throws IOException {
        URL url = new URL("http://localhost:8080/");

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        assertEquals(405, connection.getResponseCode());
    }
}