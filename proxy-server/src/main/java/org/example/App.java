package org.example;

import com.sun.net.httpserver.HttpServer;
import org.example.server.Server;

import java.io.IOException;
import java.net.InetSocketAddress;

import static java.lang.System.*;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8080), 0);
        Server handler =  new Server();
        httpServer.createContext("/",handler);
        httpServer.start();

        out.println("Server started on port 8080");
    }
}
