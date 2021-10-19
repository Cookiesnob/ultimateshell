package com.g3g4x5x6.ui.panels.ssh;


import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.g3g4x5x6.ui.formatter.IpAddressFormatter;
import com.g3g4x5x6.ui.formatter.PortFormatter;
import com.g3g4x5x6.utils.DialogUtil;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.config.hosts.HostConfigEntry;
import org.apache.sshd.client.session.ClientSession;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;


@Slf4j
public class SshPane extends JPanel {

    private BorderLayout borderLayout = new BorderLayout();
    private FlowLayout leftFlow = new FlowLayout();

    protected JTabbedPane basicSettingTabbedPane = new JTabbedPane();
    protected String basicSettingPaneTitle;
    protected JPanel basicSettingPane = new JPanel();

    protected JTabbedPane advancedSettingTabbedPane = new JTabbedPane();
    protected String advancedSettingPaneTitle;
    protected JPanel advancedSettingPane = new JPanel();

    private JTabbedPane mainTabbedPane;

    private JFormattedTextField hostField;
    private JFormattedTextField portField;

    private String host;
    private int port;
    private String username;
    private String password;

    public SshPane(JTabbedPane mainTabbedPane) {
        this.setLayout(borderLayout);
        this.mainTabbedPane = mainTabbedPane;

        basicSettingPaneTitle = "Basic SSH Settings";
        advancedSettingPaneTitle = "Advanced SSH Settings";

        leftFlow.setAlignment(FlowLayout.LEFT);
        initSettingPane();

        createBasicComponent();
        createAdvancedComponent();
    }

    /**
     * 初始化面板
     */
    protected void initSettingPane(){
        basicSettingPane.setLayout(leftFlow);
        basicSettingTabbedPane.addTab(basicSettingPaneTitle, basicSettingPane);
        advancedSettingTabbedPane.addTab(advancedSettingPaneTitle, advancedSettingPane);

        JPanel btnPane = new JPanel();
        btnPane.setLayout(leftFlow);
        JButton saveBtn = new JButton("保存会话");
        saveBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.debug("保存会话");
            }
        });
        btnPane.add(saveBtn);

        this.add(basicSettingTabbedPane, BorderLayout.NORTH);
        this.add(advancedSettingTabbedPane, BorderLayout.CENTER);
        this.add(btnPane, BorderLayout.SOUTH);
    }

    /**
     * 基本设置面板
     */
    private void createBasicComponent() {
        // TODO host address
        JPanel hostPane = new JPanel();
        JLabel hostLabel = new JLabel("Remote Host*");
        hostField = new JFormattedTextField(new IpAddressFormatter());
        hostField.setColumns(10);
        hostField.setText("172.17.200.104");    // For testing
        hostPane.add(hostLabel);
        hostPane.add(hostField);

        // TODO port
        JPanel portPane = new JPanel();
        JLabel portLabel = new JLabel("Port*");
        portField = new JFormattedTextField(new PortFormatter());
        portField.setColumns(4);
        portField.setText("22");
        portPane.add(portLabel);
        portPane.add(portField);

        // TODO user name
        JPanel userPane = new JPanel();
        JLabel userLabel = new JLabel("Username");
        JFormattedTextField userField = new JFormattedTextField();
        userField.setText("security");
        userField.setColumns(8);
        userPane.add(userLabel);
        userPane.add(userField);

        // TODO password
        JPanel passPane = new JPanel();
        JLabel passLabel = new JLabel("Password");
        JPasswordField passField = new JPasswordField();
        passField.setText("123456");
        passField.setColumns(8);
        passPane.add(passLabel);
        passPane.add(passField);

        // TODO Save and open session
        JPanel savePane = new JPanel();
        JButton openButton = new JButton("快速连接");
        openButton.setToolTipText("默认自动保存会话");
        JButton testButton = new JButton("测试通信");
        savePane.add(openButton);
        savePane.add(testButton);

        basicSettingPane.add(hostPane);
        basicSettingPane.add(portPane);
        basicSettingPane.add(userPane);
        basicSettingPane.add(passPane);
        basicSettingPane.add(savePane);

        openButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                log.debug("快速连接");

                // TODO 测试连接
                if (testConnection() == 1) {
                    host = hostField.getText();
                    port = Integer.parseInt(portField.getText());
                    username = userField.getText();
                    password = String.valueOf(passField.getPassword());
                    log.debug(password);

                    String defaultTitle = hostField.getText().equals("") ? "未命名" : hostField.getText();
                    int preIndex = mainTabbedPane.getSelectedIndex();
                    // 鸠占鹊巢
                    mainTabbedPane.insertTab(defaultTitle, new FlatSVGIcon("com/g3g4x5x6/ui/icons/OpenTerminal_13x13.svg"),
                            new SshTabbedPane(mainTabbedPane, createTerminalWidget(),
                                    hostField.getText(),
                                    portField.getText(),
                                    userField.getText(),
                                    String.valueOf(passField.getPassword())), "奥里给", preIndex);
                    mainTabbedPane.removeTabAt(preIndex+1);
                    mainTabbedPane.setSelectedIndex(preIndex);
                } else {
                    DialogUtil.warn("连接失败");
                }
            }
        });

        testButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                log.debug("测试连接");
                switch (testConnection()) {
                    case 0:
                        DialogUtil.warn("连接失败");
                        break;
                    case 1:
                        DialogUtil.info("连接成功");
                        break;
                    case 2:
                        DialogUtil.info("请输入主机地址！！！");
                }
            }
        });
    }

    /**
     * 高级设置面板
     */
    private void createAdvancedComponent() {
        JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(200);
        advancedSettingPane.setLayout(new BorderLayout());
        advancedSettingPane.add(splitPane, BorderLayout.CENTER);
    }

    private @NotNull JediTermWidget createTerminalWidget() {
        SshSettingsProvider sshSettingsProvider = new SshSettingsProvider();
        JediTermWidget widget = new JediTermWidget(sshSettingsProvider);
        widget.setTtyConnector(createTtyConnector());
        widget.start();
        return widget;
    }

    // TODO 创建 sFTP channel
    private @NotNull TtyConnector createTtyConnector() {
        try {
            if (username.equals("")) {
                return new MyJSchShellTtyConnector(host, port);
            }
            if (password.equals("")) {
                return new MyJSchShellTtyConnector(host, port, username);
            }
            return new MyJSchShellTtyConnector(host, port, username, password);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private int testConnection() {
        host = hostField.getText();
        port = Integer.parseInt(portField.getText());
        log.debug(host);
        log.debug(String.valueOf(port));

        HostConfigEntry hostConfigEntry = new HostConfigEntry();
        hostConfigEntry.setHostName(host);
        hostConfigEntry.setHost(host);
        hostConfigEntry.setPort(port);

        SshClient client = SshClient.setUpDefaultClient();
        client.start();

        if (host.equals("")) {
            try {
                client.close();
            } catch (IOException e) {
                log.debug(e.getMessage());
            }

            return 2;
        }
        try {
            ClientSession session = client.connect(hostConfigEntry).verify(3000).getClientSession();
            session.close();
            client.close();
            return 1;
        } catch (IOException ioException) {
            try {
                client.close();
            } catch (IOException e) {
                log.debug(e.getMessage());
            }
            log.debug(ioException.getMessage());
        }
        return 0;
    }

    // TODO 获取 sFTP channel

}