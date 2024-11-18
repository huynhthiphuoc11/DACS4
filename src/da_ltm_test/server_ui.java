package da_ltm_test;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JOptionPane;

public class server_ui extends javax.swing.JFrame {
    private ServerThread serverThread;

    public server_ui() {
        initComponents();
        setLocationRelativeTo(null);
    }

    // Modified appendMessage to include a timestamp for each log
    public void appendMessage(String msg) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        jTextAreaLogs.append("[" + dtf.format(now) + "] " + msg + "\n");
        jTextAreaLogs.setCaretPosition(jTextAreaLogs.getText().length());
    }

    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        jTextFieldPort = new javax.swing.JTextField();
        jButtonStart = new javax.swing.JButton();
        jButtonStop = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextAreaLogs = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("FTP Server");

        jLabel1.setText("Port:");
        jTextFieldPort.setText("21");

        jButtonStart.setText("Start Server");
        jButtonStart.addActionListener(evt -> jButtonStartActionPerformed(evt));

        jButtonStop.setText("Stop Server");
        jButtonStop.setEnabled(false);
        jButtonStop.addActionListener(evt -> jButtonStopActionPerformed(evt));

        jTextAreaLogs.setEditable(false);
        jTextAreaLogs.setColumns(20);
        jTextAreaLogs.setRows(5);
        jScrollPane1.setViewportView(jTextAreaLogs);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonStart)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonStop, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGap(15, 15, 15))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonStart)
                    .addComponent(jButtonStop))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 217, Short.MAX_VALUE)
                .addGap(15, 15, 15))
        );

        pack();
    }

    private void jButtonStartActionPerformed(java.awt.event.ActionEvent evt) {
        int port = Integer.parseInt(jTextFieldPort.getText());
        serverThread = new ServerThread(port, this);
        new Thread(serverThread).start();

        jButtonStart.setEnabled(false);
        jButtonStop.setEnabled(true);
        logUserAction("Started server on port: " + port);
    }

    private void jButtonStopActionPerformed(java.awt.event.ActionEvent evt) {
        int confirm = JOptionPane.showConfirmDialog(this, "Stop the server?");
        if (confirm == JOptionPane.YES_OPTION) {
            serverThread.stopServer();
            jButtonStart.setEnabled(true);
            jButtonStop.setEnabled(false);
            appendMessage("Server stopped.");
            logUserAction("Stopped server.");
        }
    }

    public void logUserAction(String action) {
        appendMessage("User action: " + action);
    }

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new server_ui().setVisible(true));
    }

    private javax.swing.JButton jButtonStart;
    private javax.swing.JButton jButtonStop;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextAreaLogs;
    private javax.swing.JTextField jTextFieldPort;
}
