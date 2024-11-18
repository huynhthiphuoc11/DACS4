package da_ltm_test;

import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private String clientIP;
    private server_ui ui;

    public ClientHandler(Socket clientSocket, String clientIP, server_ui ui) {
        this.clientSocket = clientSocket;
        this.clientIP = clientIP;
        this.ui = ui;
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream())
        ) {
            // Nhận username từ client
            String username = reader.readLine();
            ui.appendMessage("Client connected: " + clientIP + " with username: " + username);
            ui.appendMessage("Login successful for user: " + username);

            // Xử lý các lệnh từ client
            String message;
            while ((message = reader.readLine()) != null) {
                if (message.startsWith("UPLOAD")) {
                    String fileName = message.split(" ")[1]; // Lấy tên file
                    receiveFile(fileName, dataInputStream);  // Nhận file từ client
                } else if (message.startsWith("DOWNLOAD")) {
                    String filePath = message.substring(9).trim(); // Lấy đường dẫn file
                    sendFile("D:\\" + filePath, dataOutputStream); // Gửi file tới client
                } else if (message.startsWith("LIST")) {
                    String directoryPath = message.substring(5).trim(); // Lấy đường dẫn thư mục
                    listDirectoryContents(directoryPath, writer);       // Gửi danh sách thư mục
                } else {
                    ui.appendMessage("Message from " + username + ": " + message);
                    writer.println("Server received: " + message); // Phản hồi lại client
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            ui.appendMessage("Error handling client " + clientIP + ": " + ex.getMessage());
        } finally {
            try {
                clientSocket.close();
                ui.appendMessage("Client " + clientIP + " disconnected.");
            } catch (IOException ex) {
                Logger.getLogger(ClientHandler.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    // Phương thức gửi file tới client
    private void sendFile(String filePath, DataOutputStream dos) {
        if (dos == null) {
            ui.appendMessage("DataOutputStream is null. Cannot send file.");
            return;
        }

        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
                long fileSize = file.length();
                dos.writeLong(fileSize); // Gửi kích thước file
                dos.flush();

                ui.appendMessage("Sending file: " + file.getName() + ", Size: " + fileSize);

                byte[] buffer = new byte[4096];
                int bytesRead;
                long totalSent = 0;

                while ((bytesRead = bis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                    dos.flush();
                    totalSent += bytesRead;
                }

                ui.appendMessage("File " + file.getName() + " sent successfully. Total bytes sent: " + totalSent);
            } catch (IOException e) {
                ui.appendMessage("Error sending file: " + e.getMessage());
            }
        } else {
            try {
                ui.appendMessage("File not found: " + filePath);
                dos.writeLong(0); // Gửi kích thước 0 để báo lỗi cho client
                dos.flush();
            } catch (IOException e) {
                ui.appendMessage("Error notifying client about missing file: " + e.getMessage());
            }
        }
    }

    // Phương thức nhận file từ client
    private void receiveFile(String fileName, DataInputStream dataInputStream) {
        File file = new File("D:\\" + fileName);

        try {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs(); // Tạo thư mục nếu chưa tồn tại
            }

            long fileSize = dataInputStream.readLong(); // Đọc kích thước file
            if (fileSize <= 0) {
                ui.appendMessage("File size invalid or file not found on client.");
                return;
            }

            ui.appendMessage("Receiving file: " + fileName + ", Size: " + fileSize);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                long totalRead = 0;
                int bytesRead;

                while (totalRead < fileSize && (bytesRead = dataInputStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalRead += bytesRead;
                }

                if (totalRead == fileSize) {
                    ui.appendMessage("File " + fileName + " received successfully. Total bytes received: " + totalRead);
                } else {
                    ui.appendMessage("File " + fileName + " received partially. Expected: " + fileSize + ", Received: " + totalRead);
                }
            }
        } catch (IOException e) {
            ui.appendMessage("Error receiving file: " + e.getMessage());
        }
    }

    // Phương thức gửi danh sách thư mục tới client
    private void listDirectoryContents(String directoryPath, PrintWriter writer) {
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        writer.println("DIR: " + file.getName());
                    } else {
                        writer.println("FILE: " + file.getName());
                    }
                }
                writer.println("END_OF_LIST"); // Đánh dấu kết thúc danh sách
                ui.appendMessage("Sent directory contents of: " + directoryPath);
            } else {
                writer.println("ERROR: Unable to list contents of directory.");
            }
        } else {
            writer.println("ERROR: Directory not found.");
        }
    }
}
