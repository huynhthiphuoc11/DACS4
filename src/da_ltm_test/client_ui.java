package da_ltm_test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class client_ui extends JFrame {
    private JTextField ipField;
    private JTextField portField;
    private JLabel statusLabel;
    private String username;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private InputStream inBinary;
    private JTree fileTree;
    private JTree localFileTree;
    private String selectedRemoteFilePath;
    private String selectedLocalFilePath;
    private JTextArea uploadStatusArea;
    private JProgressBar uploadProgressBar;
    private DefaultTreeModel remoteTreeModel;
    private DefaultTreeModel localTreeModel;
    private JButton connectButton;
    private JButton newFolderButton;
    private JButton newFileButton;
    private JButton uploadButton;
    private JButton downloadButton;
    private JButton editButton;
    private JButton deleteButton;
    private JButton refreshButton;

    public client_ui(String username) {
        this.username = username;
        initializeUI();
        setupActionListeners();
    }

    private void initializeUI() {
        setTitle("File Transfer Protocol - Client");
        JPanel mainPanel = new JPanel(new BorderLayout());

        // Top Panel
        mainPanel.add(createTopPanel(), BorderLayout.NORTH);

        // Center Panel with Trees and Buttons
        mainPanel.add(createCenterPanel(), BorderLayout.CENTER);

        // Bottom Panel
        mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);

        add(mainPanel);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
    }

    private JPanel createTopPanel() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel usernameLabel = new JLabel("Username: " + username);
        JLabel ipLabel = new JLabel("Host IP:");
        ipField = new JTextField("localhost", 10);
        JLabel portLabel = new JLabel("Port:");
        portField = new JTextField("21", 5);
        connectButton = new JButton("Connect");
        refreshButton = new JButton("Refresh");

        topPanel.add(usernameLabel);
        topPanel.add(ipLabel);
        topPanel.add(ipField);
        topPanel.add(portLabel);
        topPanel.add(portField);
        topPanel.add(connectButton);
        topPanel.add(refreshButton);

        return topPanel;
    }

    private JPanel createCenterPanel() {
        JPanel centerPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Remote File Tree Panel
        JPanel remotePanel = createRemotePanel();
        centerPanel.add(remotePanel);

        // Local File Tree Panel
        JPanel localPanel = createLocalPanel();
        centerPanel.add(localPanel);

        // Buttons Panel
        JPanel buttonsPanel = createButtonsPanel();
        centerPanel.add(buttonsPanel);

        return centerPanel;
    }

    private JPanel createRemotePanel() {
        JPanel remotePanel = new JPanel(new BorderLayout());
        remotePanel.setBorder(BorderFactory.createTitledBorder("Remote Site Directory"));

        // Initialize the root node for the remote directory tree
        DefaultMutableTreeNode remoteRoot = new DefaultMutableTreeNode("Remote Site (D:\\)");
        remoteTreeModel = new DefaultTreeModel(remoteRoot);
        fileTree = new JTree(remoteTreeModel);
        JScrollPane scrollPane = new JScrollPane(fileTree);
        remotePanel.add(scrollPane, BorderLayout.CENTER);

        return remotePanel;
    }

    private void requestDirectoryContents(String path) {
        try {
            sendMessage("LIST " + path);  // Send the command to the server
            loadDriveContentsFromServer(path);  // Load the response from the server and update the tree
        } catch (IOException e) {
            statusLabel.setText("Status: Failed to retrieve directory contents");
            e.printStackTrace();
        }
    }

 // Phương thức tải thư mục từ server và hiển thị vào cây thư mục
    private void loadDriveContentsFromServer(String path) throws IOException {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(path);
        parseDirectory(rootNode);
        fileTree.setModel(new DefaultTreeModel(rootNode));
        fileTree.addTreeWillExpandListener(new javax.swing.event.TreeWillExpandListener() {
            @Override
            public void treeWillExpand(javax.swing.event.TreeExpansionEvent event) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) event.getPath().getLastPathComponent();
                if (node.getChildCount() == 0 && node.toString().startsWith("[DIR]")) {
                    String dirName = node.toString().substring(6).trim();
                    try {
                        loadSubDirectory(node, dirName); // Gọi phương thức tải thư mục con
                    } catch (IOException e) {
                        JOptionPane.showMessageDialog(null, "Lỗi khi tải nội dung thư mục: " + e.getMessage(),
                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }

            @Override
            public void treeWillCollapse(javax.swing.event.TreeExpansionEvent event) {
                // Không cần xử lý khi đóng thư mục
            }
        });

    }

    // Phương thức phân tích đệ quy
    private void parseDirectory(DefaultMutableTreeNode parent) throws IOException {
        String response;
        while ((response = in.readLine()) != null) {
            if (response.equals("END_OF_LIST") || response.equals("DIR_END")) {
                break;
            }

            DefaultMutableTreeNode node;
            if (response.startsWith("DIR:")) {
                node = new DefaultMutableTreeNode("[DIR] " + response.substring(5).trim());
                parent.add(node);
            } else if (response.startsWith("FILE:")) {
                node = new DefaultMutableTreeNode("[FILE] " + response.substring(6).trim());
                parent.add(node);
            }
        }
    }

    // Phương thức tải thư mục con khi người dùng nhấp vào
    private void loadSubDirectory(DefaultMutableTreeNode parentNode, String path) throws IOException {
        // Gửi yêu cầu đến server để lấy nội dung của thư mục con
        out.println("REQUEST_DIR:" + path);
        out.flush();

        // Đọc và phân tích thư mục con
        String response;
        while ((response = in.readLine()) != null) {
            if (response.equals("END_OF_LIST")) {
                break;
            }

            DefaultMutableTreeNode node;
            if (response.startsWith("DIR:")) {
                node = new DefaultMutableTreeNode("[DIR] " + response.substring(5).trim());
                parentNode.add(node);
            } else if (response.startsWith("FILE:")) {
                node = new DefaultMutableTreeNode("[FILE] " + response.substring(6).trim());
                parentNode.add(node);
            }
        }

        // Cập nhật mô hình cây sau khi thêm thư mục con
        ((DefaultTreeModel) fileTree.getModel()).nodeStructureChanged(parentNode);
    }


    private JPanel createLocalPanel() {
        JPanel localPanel = new JPanel(new BorderLayout());
        localPanel.setBorder(BorderFactory.createTitledBorder("Local Directory"));

        DefaultMutableTreeNode localRoot = new DefaultMutableTreeNode(System.getProperty("user.home"));
        localTreeModel = new DefaultTreeModel(localRoot);
        localFileTree = new JTree(localTreeModel);
        JScrollPane scrollPane = new JScrollPane(localFileTree);
        localPanel.add(scrollPane, BorderLayout.CENTER);

        return localPanel;
    }

    private JPanel createButtonsPanel() {
        JPanel buttonsPanel = new JPanel(new GridBagLayout());
        buttonsPanel.setBorder(BorderFactory.createTitledBorder("Actions"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        newFolderButton = new JButton("New Folder");
        newFileButton = new JButton("New File");
        uploadButton = new JButton("Upload");
        downloadButton = new JButton("Download");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");

        buttonsPanel.add(newFolderButton, gbc);
        buttonsPanel.add(newFileButton, gbc);
        buttonsPanel.add(uploadButton, gbc);
        buttonsPanel.add(downloadButton, gbc);
        buttonsPanel.add(editButton, gbc);
        buttonsPanel.add(deleteButton, gbc);

        // Add a filler to push buttons to the top
        gbc.weighty = 1.0;
        buttonsPanel.add(Box.createVerticalGlue(), gbc);

        return buttonsPanel;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Status Label
        statusLabel = new JLabel("Status: Disconnected");
        bottomPanel.add(statusLabel, BorderLayout.NORTH);

        // Upload Status Panel
        JPanel uploadPanel = new JPanel(new BorderLayout(5, 5));
        uploadStatusArea = new JTextArea(4, 50);
        uploadStatusArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(uploadStatusArea);
        uploadProgressBar = new JProgressBar(0, 100);
        uploadProgressBar.setStringPainted(true);

        uploadPanel.add(scrollPane, BorderLayout.CENTER);
        uploadPanel.add(uploadProgressBar, BorderLayout.SOUTH);
        bottomPanel.add(uploadPanel, BorderLayout.CENTER);

        return bottomPanel;
    }

    private void setupActionListeners() {
        connectButton.addActionListener(e -> connectToServer());
        refreshButton.addActionListener(e -> refreshDirectories());
        newFolderButton.addActionListener(e -> createNewFolder());
        newFileButton.addActionListener(e -> createNewFile());
        uploadButton.addActionListener(e -> uploadFile());
        downloadButton.addActionListener(e -> downloadFile());
        editButton.addActionListener(e -> editSelectedFile());
        deleteButton.addActionListener(e -> deleteSelectedFile());

        // Tree selection listeners
        fileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = fileTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    selectedRemoteFilePath = path.getLastPathComponent().toString();
                    updateButtonStates();
                }
            }
        });

        localFileTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                TreePath path = localFileTree.getPathForLocation(e.getX(), e.getY());
                if (path != null) {
                    selectedLocalFilePath = constructPath(path);
                    updateButtonStates();
                }
            }
        });
    }

    private String constructPath(TreePath treePath) {
        Object[] paths = treePath.getPath();
        StringBuilder fullPath = new StringBuilder();
        for (int i = 0; i < paths.length; i++) {
            fullPath.append(paths[i].toString());
            if (i < paths.length - 1) {
                fullPath.append(File.separator);
            }
        }
        return fullPath.toString();
    }

    private void updateButtonStates() {
        boolean isConnected = socket != null && socket.isConnected();
        boolean hasRemoteSelection = selectedRemoteFilePath != null;
        boolean hasLocalSelection = selectedLocalFilePath != null;

        newFolderButton.setEnabled(isConnected);
        newFileButton.setEnabled(isConnected);
        uploadButton.setEnabled(isConnected && hasLocalSelection);
        downloadButton.setEnabled(isConnected && hasRemoteSelection);
        editButton.setEnabled(hasLocalSelection || hasRemoteSelection);
        deleteButton.setEnabled(isConnected && (hasLocalSelection || hasRemoteSelection));
    }
//connect to server
    private void connectToServer() {
        String ipAddress = ipField.getText();
        String portNumber = portField.getText();

        try {
            int port = Integer.parseInt(portNumber);
            socket = new Socket(ipAddress, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            statusLabel.setText("Status: Connected to " + ipAddress + ":" + port);
            sendMessage("Connected: " + username);

            // Load the initial drive contents
            requestDirectoryContents("D:\\");
            refreshDirectories();
            refreshRemoteDirectory();
            initializeStreams();
        } catch (IOException e) {
            statusLabel.setText("Status: Failed to Connect");
            e.printStackTrace();
        }
    }
   

    private void refreshDirectories() {
        refreshRemoteDirectory();
        refreshLocalDirectory();
    }

    private void refreshRemoteDirectory() {
        if (isConnected()) {
            try {
                sendMessage("LIST D:\\");
                // Implementation depends on your server's response format
                updateRemoteTree();
            } catch (Exception e) {
                handleError("Failed to refresh remote directory: " + e.getMessage());
            }
        }
    }

    private void refreshLocalDirectory() {
        try {
            String rootPath = System.getProperty("user.home");
            DefaultMutableTreeNode root = new DefaultMutableTreeNode(rootPath);
            populateLocalDirectory(root, new File(rootPath));
            localTreeModel.setRoot(root);
        } catch (Exception e) {
            handleError("Failed to refresh local directory: " + e.getMessage());
        }
    }

    private void populateLocalDirectory(DefaultMutableTreeNode node, File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (File childFile : files) {
                DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(childFile.getName());
                node.add(childNode);
                if (childFile.isDirectory()) {
                    populateLocalDirectory(childNode, childFile);
                }
            }
        }
    }

    private void createNewFolder() {
        String folderName = JOptionPane.showInputDialog(this, "Enter folder name:");
        if (folderName != null && !folderName.trim().isEmpty()) {
            if (isConnected()) {
                sendMessage("MKDIR " + folderName);
                refreshRemoteDirectory();
            }
        }
    }

    private void createNewFile() {
        String fileName = JOptionPane.showInputDialog(this, "Enter file name:");
        if (fileName != null && !fileName.trim().isEmpty()) {
            if (isConnected()) {
                sendMessage("TOUCH " + fileName);
                refreshRemoteDirectory();
            }
        }
    }

    private void uploadFile() {
        if (!isConnected()) {
            handleError("Not connected to server");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            uploadStatusArea.append("Uploading " + file.getName() + "...\n");
            
            try {
                sendMessage("UPLOAD " + file.getName());
                sendFile(file);
                refreshRemoteDirectory();
            } catch (Exception e) {
                handleError("Upload failed: " + e.getMessage());
            }
        }
    }

    private void sendFile(File file) throws IOException {
        try (FileInputStream fis = new FileInputStream(file);
             DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {

            // Gửi kích thước file
            long fileSize = file.length();
            dos.writeLong(fileSize);
            dos.flush();

            // Gửi dữ liệu file
            byte[] buffer = new byte[4096];
            int bytesRead;
            long totalSent = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                dos.write(buffer, 0, bytesRead);
                dos.flush();
                totalSent += bytesRead;
            }

            // Kiểm tra nếu gửi xong file
            if (totalSent == fileSize) {
                uploadStatusArea.append("Upload complete: " + file.getName() + "\n");
            } else {
                uploadStatusArea.append("Upload incomplete. Sent " + totalSent + " of " + fileSize + " bytes.\n");
            }
        }
    }

 // Giả định rằng bạn đã khởi tạo `socket` từ trước
    private void initializeStreams() throws IOException {
        inBinary = socket.getInputStream();
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void downloadFile() {
        if (!isConnected() || selectedRemoteFilePath == null) {
            handleError("No file selected or not connected");
            return;
        }

        // Loại bỏ tiền tố "[FILE]" nếu có
        String filePath = selectedRemoteFilePath;
        if (filePath.startsWith("[FILE]")) {
            filePath = filePath.substring(6).trim();
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(filePath));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try {
                sendMessage("DOWNLOAD " + filePath); // Gửi yêu cầu tới server
                receiveFile(file); // Nhận file từ server và lưu vào file đích
            } catch (IOException e) {
                handleError("Download failed: " + e.getMessage());
            }
        }
    }


    // Phương thức nhận file từ server
    private void receiveFile(File file) throws IOException {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream());
             FileOutputStream fos = new FileOutputStream(file)) {

            // Đọc kích thước file từ server
            long fileSize = dis.readLong();
            if (fileSize <= 0) {
                handleError("File size invalid or file not found on server.");
                return;
            }

            uploadStatusArea.append("Downloading: " + file.getName() + " (" + getReadableFileSize(fileSize) + ")\n");

            byte[] buffer = new byte[4096];
            long totalRead = 0;
            int bytesRead;

            // Vòng lặp nhận dữ liệu
            while (totalRead < fileSize && (bytesRead = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
            }

            // Kiểm tra xem đã nhận đủ dữ liệu chưa
            if (totalRead == fileSize) {
                uploadStatusArea.append("Download complete: " + file.getName() + "\n");
            } else {
                handleError("Download incomplete. Expected: " + fileSize + " bytes, Received: " + totalRead + " bytes.");
            }
        } catch (IOException e) {
            handleError("Error receiving file: " + e.getMessage());
            throw e;
        }
    }



    private byte[] downloadPart(String filePath, long offset, int partSize) throws IOException {
        sendMessage("DOWNLOAD_PART " + filePath + " " + offset + " " + partSize);
        byte[] buffer = new byte[partSize];
        int bytesRead = readBytesFromServer(buffer);
        if (bytesRead == -1) return null; // Đã tải xong phần này
        if (bytesRead < partSize) {
            return Arrays.copyOf(buffer, bytesRead); // Trả về phần dữ liệu đã tải
        }
        return buffer;
    }

    // Phương thức tiện ích để đọc một dòng từ server
    private String readLineFromServer() throws IOException {
        return in.readLine();
    }

    // Phương thức tiện ích để đọc các byte từ server
    private int readBytesFromServer(byte[] buffer) throws IOException {
        return inBinary.read(buffer);
    }

  
    private void editSelectedFile() {
        try {
            // Nếu file cục bộ được chọn, mở ứng dụng mặc định để chỉnh sửa
            if (selectedLocalFilePath != null) {
                Desktop.getDesktop().edit(new File(selectedLocalFilePath));
            
            // Nếu file từ xa được chọn và client đã kết nối với server
            } else if (selectedRemoteFilePath != null && isConnected()) {
                sendMessage("EDIT " + selectedRemoteFilePath);
                
                // Thông báo việc yêu cầu chỉnh sửa file đã được gửi
                uploadStatusArea.append("Edit request sent for remote file: " + selectedRemoteFilePath + "\n");

            } else {
                handleError("No file selected for editing.");
            }

        } catch (IOException e) {
            handleError("Failed to edit file: " + e.getMessage());
        }
    }

    private void deleteSelectedFile() {
        try {
            if (selectedLocalFilePath != null) {
                File file = new File(selectedLocalFilePath);
                if (file.delete()) {
                    refreshLocalDirectory();
                }
            } else if (selectedRemoteFilePath != null && isConnected()) {
                sendMessage("DELETE " + selectedRemoteFilePath);
                refreshRemoteDirectory();
            }
        } catch (Exception e) {
            handleError("Failed to delete file: " + e.getMessage());
        }
    }

    private void updateRemoteTree() {
        try {
            DefaultMutableTreeNode root = new DefaultMutableTreeNode("Remote Site (D:\\)");
            List<String> fileList = new ArrayList<>();
            String line;
            
            // Read server response until END_OF_LIST marker
            while ((line = in.readLine()) != null && !line.equals("END_OF_LIST")) {
                fileList.add(line);
            }
            
            // Process file list and build tree
            for (String entry : fileList) {
                if (entry.startsWith("DIR:")) {
                    DefaultMutableTreeNode dirNode = new DefaultMutableTreeNode("[DIR] " + entry.substring(4));
                    root.add(dirNode);
                } else if (entry.startsWith("FILE:")) {
                    DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode("[FILE] " + entry.substring(5));
                    root.add(fileNode);
                }
            }
            
            remoteTreeModel.setRoot(root);
            fileTree.expandRow(0);
            
        } catch (IOException e) {
            handleError("Failed to update remote directory tree: " + e.getMessage());
        }
    }

    private void updateProgress(int value) {
        SwingUtilities.invokeLater(() -> {
            uploadProgressBar.setValue(value);
            uploadProgressBar.setString(value + "%");
        });
    }

    private void handleError(String message) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Error: " + message);
            uploadStatusArea.append("Error: " + message + "\n");
            JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
        });
    }

    private boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    private void sendMessage(String message) {
        if (isConnected() && out != null) {
            out.println(message);
            out.flush();
        }
    }

    // Utility method to get file size in readable format
    private String getReadableFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    // Method to handle server responses
    private String waitForResponse() throws IOException {
        if (in != null) {
            return in.readLine();
        }
        return null;
    }

    // Add file transfer methods with better error handling and progress tracking
    private void transferFile(File source, OutputStream out) throws IOException {
        long fileSize = source.length();
        long totalBytesTransferred = 0;
        int bytesRead;
        byte[] buffer = new byte[8192];

        try (FileInputStream fis = new FileInputStream(source)) {
            while ((bytesRead = fis.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalBytesTransferred += bytesRead;
                
                // Update progress
                int progress = (int) ((totalBytesTransferred * 100) / fileSize);
                updateProgress(progress);
                
                // Update status
                if (progress % 10 == 0) { // Update every 10%
                    String status = String.format("Transferred %s of %s (%d%%)",
                        getReadableFileSize(totalBytesTransferred),
                        getReadableFileSize(fileSize),
                        progress);
                    updateStatus(status);
                }
            }
            out.flush();
        }
    }

    private void updateStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Status: " + status);
            uploadStatusArea.append(status + "\n");
        });
    }

    // Add method to handle window closing
    private void setupWindowListener() {
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                closeConnection();
            }
        });
    }

    private void closeConnection() {
        try {
            if (out != null) {
                sendMessage("QUIT");
                out.close();
            }
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Add method to refresh both trees periodically
    private void setupAutoRefresh() {
        Timer refreshTimer = new Timer(30000, e -> {
            if (isConnected()) {
                refreshDirectories();
            }
        });
        refreshTimer.start();
    }

    // Enhanced file operations with better error handling
    private void createDirectory(String path) {
        try {
            Path dirPath = Paths.get(path);
            Files.createDirectories(dirPath);
            refreshDirectories();
            updateStatus("Directory created successfully: " + path);
        } catch (IOException e) {
            handleError("Failed to create directory: " + e.getMessage());
        }
    }

    private void copyFile(String sourcePath, String destinationPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path destination = Paths.get(destinationPath);
            Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            refreshDirectories();
            updateStatus("File copied successfully");
        } catch (IOException e) {
            handleError("Failed to copy file: " + e.getMessage());
        }
    }

    private void moveFile(String sourcePath, String destinationPath) {
        try {
            Path source = Paths.get(sourcePath);
            Path destination = Paths.get(destinationPath);
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
            refreshDirectories();
            updateStatus("File moved successfully");
        } catch (IOException e) {
            handleError("Failed to move file: " + e.getMessage());
        }
    }

    // Main method
    public static void main(String[] args) {
        try {
            // Set System look and feel
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            client_ui client = new client_ui("User");
            client.setupWindowListener();
            client.setupAutoRefresh();
            client.setVisible(true);
        });
    }

    // Add method to check file permissions
    private boolean checkFilePermissions(File file, String operation) {
        switch (operation.toUpperCase()) {
            case "READ":
                return file.canRead();
            case "WRITE":
                return file.canWrite();
            case "EXECUTE":
                return file.canExecute();
            default:
                return false;
        }
    }

    // Add method for drag and drop support
    private void setupDragAndDrop() {
        // Implementation for drag and drop functionality
        // This would require implementing the necessary drag and drop listeners
        // and handling file transfers between the local and remote trees
    }
} 