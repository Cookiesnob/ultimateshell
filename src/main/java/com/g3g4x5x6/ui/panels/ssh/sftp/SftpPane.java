package com.g3g4x5x6.ui.panels.ssh.sftp;


import com.g3g4x5x6.ui.formatter.IpAddressFormatter;
import com.g3g4x5x6.ui.formatter.PortFormatter;
import com.g3g4x5x6.ui.panels.SftpBrowser;
import com.g3g4x5x6.utils.Utils;
import org.apache.log4j.Logger;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.common.session.SessionHeartbeatController;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.apache.sshd.sftp.client.fs.SftpFileSystemProvider;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;


public class SftpPane extends JPanel {
    static final Logger logger = Logger.getLogger(SftpPane.class);

    private BorderLayout borderLayout = new BorderLayout();
    private SftpBrowser sftpBrowser;

    private SshClient client;
    private SftpFileSystemProvider provider;
    private URI uri;
    private SftpFileSystem fs;

    private JPanel board;
    private JFormattedTextField hostField;
    private JFormattedTextField portField;
    private String host;
    private int port;
    private String username;
    private String password;

    public SftpPane() {
        this.setLayout(borderLayout);

    }


    public SftpPane(String hostField, String portField, String userField, String passField) {
        this.setLayout(borderLayout);

        this.host = hostField;
        this.port = Integer.parseInt(portField);
        this.username = userField;
        this.password = passField;

        try {
            this.fs = createFileSystem();
            sftpBrowser = new SftpBrowser(fs);
            this.add(sftpBrowser, BorderLayout.CENTER);
        } catch (Exception e) {
            logger.debug("Constructor: " + e.getMessage());
            createBasicComponent();
            e.printStackTrace();
        }
    }


    public SftpFileSystem createFileSystem() {
        client = SshClient.setUpDefaultClient();
        // TODO 配置 SshClient
        // override any default configuration...
//        client.setSomeConfiguration(...);
//        client.setOtherConfiguration(...);
        client.start();
        provider = new SftpFileSystemProvider(client);
        uri = SftpFileSystemProvider.createFileSystemURI(this.host, this.port, this.username, this.password);
        try {
            // TODO 配置 SftpFileSystem
            Map<String, Object> params = new HashMap<>();
//            params.put("param1", value1);
//            params.put("param2", value2);
            fs = provider.newFileSystem(uri, params);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        return fs;
    }

    private void createBasicComponent() {
        // TODO host address
        JPanel hostPane = new JPanel();
        JLabel hostLabel = new JLabel("Remote Host*");
        hostField = new JFormattedTextField(new IpAddressFormatter());
        hostField.setColumns(10);
        hostField.setText(host);    // For testing
        hostPane.add(hostLabel);
        hostPane.add(hostField);

        // TODO port
        JPanel portPane = new JPanel();
        JLabel portLabel = new JLabel("Port*");
        portField = new JFormattedTextField(new PortFormatter());
        portField.setColumns(4);
        portField.setText(String.valueOf(port));
        portPane.add(portLabel);
        portPane.add(portField);

        // TODO user name
        JPanel userPane = new JPanel();
        JLabel userLabel = new JLabel("Username");
        JFormattedTextField userField = new JFormattedTextField();
        userField.setText(username);
        userField.setColumns(8);
        userPane.add(userLabel);
        userPane.add(userField);

        // TODO password
        JPanel passPane = new JPanel();
        JLabel passLabel = new JLabel("Password");
        JPasswordField passField = new JPasswordField();
        passField.setText(password);
        passField.setColumns(8);
        passPane.add(passLabel);
        passPane.add(passField);

        // TODO Save and open session
        JPanel savePane = new JPanel();
        JButton openButton = new JButton("快速连接");
        openButton.setToolTipText("默认自动保存会话");
        savePane.add(openButton);

        board = new JPanel();
        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        board.add(hostPane);
        board.add(portPane);
        board.add(userPane);
        board.add(passPane);
        board.add(savePane);
        this.add(board, BorderLayout.CENTER);

        openButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                logger.debug("快速连接");

                host = hostField.getText();
                port = Integer.parseInt(portField.getText());
                username = userField.getText();
                password = String.valueOf(passField.getPassword());

                // TODO 测试连接
                try {
                    fs = createFileSystem();
                    sftpBrowser = new SftpBrowser(fs);
                    board.setVisible(false);
                    add(sftpBrowser, BorderLayout.CENTER);
                } catch (Exception exception) {
                    logger.debug("Button: " + exception.getMessage());
                    exception.printStackTrace();
                }
            }
        });
    }

}
