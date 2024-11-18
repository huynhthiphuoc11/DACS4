package da_ltm_test;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerThread implements Runnable {
    private int port;
    private server_ui ui;
    private ServerSocket serverSocket;
    private boolean running;

    public ServerThread(int port, server_ui ui) {
        this.port = port;
        this.ui = ui;
        this.running = true;
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(port);
            ui.appendMessage("Server started on port: " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                String clientIP = clientSocket.getInetAddress().getHostAddress();
                ui.appendMessage("New connection from: " + clientIP);

                // Pass the socket, client IP, and server_ui instance to the ClientHandler
                ClientHandler clientHandler = new ClientHandler(clientSocket, clientIP, ui);
                new Thread(clientHandler).start();
            }
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            ui.appendMessage("Error: " + ex.getMessage());
        } finally {
            stopServer();
        }
    }


    public void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            ui.appendMessage("Server stopped.");
        } catch (IOException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            ui.appendMessage("Error stopping server: " + ex.getMessage());
        }
    }
}
