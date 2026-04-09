package com.loadbalancer.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;
        String message = args.length > 2 ? args[2] : "Hello from client";

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Connected to " + host + ":" + port);
            out.println(message);
            out.println(); // blank line signals end of request
            socket.shutdownOutput(); // signal EOF so the worker knows we're done sending

            String response;
            while ((response = in.readLine()) != null) {
                System.out.println("Response: " + response);
            }
        }
    }
}
