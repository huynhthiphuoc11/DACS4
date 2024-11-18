package da_ltm_test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class login_ui extends JFrame {
    private JTextField usernameField;
    private JTextField hostField;
    private JPasswordField passwordField;

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                login_ui frame = new login_ui();
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public login_ui() {
        setTitle("FTP Client Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(100, 100, 831, 505);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout());
        JPanel loginPanel = createLoginPanel();
        mainPanel.add(loginPanel, BorderLayout.WEST);
        JPanel imagePanel = createImagePanel();
        mainPanel.add(imagePanel, BorderLayout.CENTER);
        getContentPane().add(mainPanel);
    }

    private JPanel createLoginPanel() {
        JPanel loginPanel = new JPanel();
        loginPanel.setBackground(Color.WHITE);
        loginPanel.setLayout(null);
        loginPanel.setPreferredSize(new Dimension(288, 496));

        JLabel ftpIconLabel = new JLabel();
        ftpIconLabel.setIcon(new ImageIcon(getClass().getResource("/resource/logo_1.png")));
        ftpIconLabel.setBounds(107, 11, 86, 99);
        loginPanel.add(ftpIconLabel);

        usernameField = createPlaceholderTextField("Username");
        usernameField.setBounds(59, 156, 179, 31);
        loginPanel.add(usernameField);

        hostField = createPlaceholderTextField("Host");
        hostField.setBounds(59, 198, 179, 31);
        loginPanel.add(hostField);

        passwordField = new JPasswordField();
        passwordField.setBounds(59, 238, 179, 31);
        addPlaceholder(passwordField, "Password");
        loginPanel.add(passwordField);

        JButton signInButton = new JButton("Sign in");
        signInButton.setBounds(59, 305, 179, 31);
        signInButton.setBackground(Color.BLUE);
        signInButton.setForeground(Color.WHITE);
        signInButton.addActionListener(this::signInAction);
        loginPanel.add(signInButton);

        return loginPanel;
    }

    private JTextField createPlaceholderTextField(String placeholder) {
        JTextField textField = new JTextField(placeholder);
        textField.setForeground(Color.GRAY);
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                    textField.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (textField.getText().isEmpty()) {
                    textField.setForeground(Color.GRAY);
                    textField.setText(placeholder);
                }
            }
        });
        return textField;
    }

    private void addPlaceholder(JPasswordField passwordField, String placeholder) {
        passwordField.setForeground(Color.GRAY);
        passwordField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (new String(passwordField.getPassword()).equals(placeholder)) {
                    passwordField.setText("");
                    passwordField.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (new String(passwordField.getPassword()).isEmpty()) {
                    passwordField.setForeground(Color.GRAY);
                    passwordField.setText(placeholder);
                }
            }
        });
    }

    private JPanel createImagePanel() {
        JPanel imagePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Image backgroundImage = new ImageIcon(getClass().getResource("/resource/FTPimageBack.png")).getImage();
                int imgWidth = 350;  // Desired width
                int imgHeight = 350; // Desired height
                int x = (getWidth() - imgWidth) / 2;
                int y = (getHeight() - imgHeight) / 2;
                g.drawImage(backgroundImage, x, y, imgWidth, imgHeight, this);
            }
        };
        imagePanel.setBackground(SystemColor.control);
        imagePanel.setPreferredSize(new Dimension(307, 306));
        imagePanel.setLayout(new BorderLayout());
        imagePanel.setOpaque(true);
        return imagePanel;
    }

    private void signInAction(ActionEvent e) {
        String username = usernameField.getText();
        String host = hostField.getText();
        String password = new String(passwordField.getPassword());

        DatabaseConnection dbConnection = new DatabaseConnection();

        try (Connection conn = dbConnection.connect()) {
            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                pstmt.setString(1, username);
                pstmt.setString(2, password);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Login successful!");

                    // Send notification to server
                    try (Socket socket = new Socket("localhost", 21)) {
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println(username);  // Send username to server
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Error connecting to the server:\n" + ex.getMessage(),
                                "Connection Error", JOptionPane.ERROR_MESSAGE);
                    }

                    client_ui mainDashboard = new client_ui(username);
                    mainDashboard.setVisible(true);
                    this.dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid username or password.");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error connecting to the database:\n" + ex.getMessage(),
                    "Connection Error", JOptionPane.ERROR_MESSAGE);
        }
    }
} 