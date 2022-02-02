package com.g3g4x5x6.ui.panels.ssh;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.g3g4x5x6.ui.MainFrame;
import com.g3g4x5x6.ui.panels.ssh.editor.EditorPane;
import com.g3g4x5x6.ui.panels.ssh.monitor.MonitorPane;
import com.g3g4x5x6.ui.panels.ssh.sftp.SftpBrowser;
import com.g3g4x5x6.utils.DialogUtil;
import com.jediterm.terminal.ui.JediTermWidget;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.session.SessionHeartbeatController;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.apache.sshd.sftp.client.fs.SftpFileSystemProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TRAILING_COMPONENT;


@Slf4j
public class SshTabbedPane extends JTabbedPane {
    private JediTermWidget sshPane;
    private SftpBrowser sftpBrowser;
    private MonitorPane monitorPane;
    private EditorPane editorPane;

    private String host;
    private int port;
    private String user;
    private String pass;

    private SshClient client;
    private ClientSession session;
    private SftpFileSystem sftpFileSystem;

    public SshTabbedPane(String hostField, String portField, String userField, String passField) {
        this.host = hostField;
        this.port = Integer.parseInt(portField);
        this.user = userField;
        this.pass = passField;
        // 等待进度条
        MainFrame.addWaitProgressBar();

        init();
        this.sshPane = createTerminalWidget();
        this.sftpBrowser = new SftpBrowser(this.sftpFileSystem);
        this.editorPane = new EditorPane(this.sftpFileSystem);
        this.monitorPane = new MonitorPane(this.session);
        this.addTab("SSH", this.sshPane);
        this.addTab("SFTP", this.sftpBrowser);
        this.addTab("Editor", this.editorPane);
        this.addTab("Monitor", this.monitorPane);
        customComponents();

        // 关闭进度条
        MainFrame.removeWaitProgressBar();
    }

    public SshTabbedPane(ClientSession session) {
        this.session = session;
        this.sftpFileSystem = getSftpFileSystem(session);

        // 等待进度条
        MainFrame.addWaitProgressBar();

        this.sshPane = createTerminalWidget();
        this.sftpBrowser = new SftpBrowser(this.sftpFileSystem);
        this.editorPane = new EditorPane(this.sftpFileSystem);
        this.monitorPane = new MonitorPane(this.session);
        this.addTab("SSH", this.sshPane);
        this.addTab("SFTP", this.sftpBrowser);
        this.addTab("Editor", this.editorPane);
        this.addTab("Monitor", this.monitorPane);
        customComponents();

        // 关闭进度条
        MainFrame.removeWaitProgressBar();
    }

    public ClientSession getSession() {
        return session;
    }

    private void init() {
        this.client = SshClient.setUpDefaultClient();
        this.client.start();
        this.session = getSession(client);
        this.sftpFileSystem = getSftpFileSystem(session);
    }

    private ClientSession getSession(SshClient client) {
        ClientSession session;
        try {
            session = client.connect(this.user, this.host, this.port).verify(5000, TimeUnit.MILLISECONDS).getSession();
            session.addPasswordIdentity(this.pass);
            session.auth().verify(15, TimeUnit.SECONDS);
            session.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE, Duration.ofMinutes(3));
            session.sendIgnoreMessage("".getBytes(StandardCharsets.UTF_8));
            return session;
        } catch (IOException e) {
            e.printStackTrace();
            DialogUtil.error(e.getMessage());
            return null;
        }
    }

    private SftpFileSystem getSftpFileSystem(ClientSession session) {
        if (sftpFileSystem == null || !sftpFileSystem.isOpen()) {
            SftpFileSystemProvider provider = new SftpFileSystemProvider();
            try {
                return provider.newFileSystem(session);
            } catch (IOException e) {
                e.printStackTrace();
                DialogUtil.error(e.getMessage());
                return null;
            }
        } else {
            return sftpFileSystem;
        }
    }

    private @NotNull JediTermWidget createTerminalWidget() {
        SshSettingsProvider sshSettingsProvider = new SshSettingsProvider();
        JediTermWidget widget = new JediTermWidget(sshSettingsProvider);
        widget.setTtyConnector(new DefaultTtyConnector(session));
        widget.start();
        return widget;
    }

    private void customComponents() {
        JToolBar trailing = new JToolBar();
        trailing.setFloatable(false);
        trailing.setBorder(null);
        trailing.add(new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/buildLoadChanges.svg")));
        trailing.add(Box.createHorizontalGlue());
        trailing.add(new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/commit.svg")));
        trailing.add(new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/diff.svg")));
        trailing.add(new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/listFiles.svg")));

        this.putClientProperty(TABBED_PANE_TRAILING_COMPONENT, trailing);
    }

    public void resetSession() {
        // TODO 重连后终端大小不对，需要拉伸窗口重新触发调整终端大小; 或者从 SFTP 窗口跳转回来也好
        // 等待进度条
        MainFrame.addWaitProgressBar();

        new Thread(()->{
            reset4Session();
            reset4TerminalWidget();
            reset4SftpBrowser();
            reset4EditorPane();
            reset4MonitorPane();
        }).start();
        // 关闭进度条
        MainFrame.removeWaitProgressBar();
    }

    private void reset4Session() {
        this.session = getSession(client);
    }

    private void reset4TerminalWidget() {
        this.remove(0);
        this.sshPane = createTerminalWidget();
        this.insertTab("SSH", null, this.sshPane, "", 0);
    }

    private void reset4SftpBrowser() {
        try {
            this.sftpBrowser.setFs(getSftpFileSystem(session));
            this.sftpBrowser.setSftpClient(sftpBrowser.getFs().getClient());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reset4EditorPane() {
        this.editorPane.setFs(getSftpFileSystem(session));
    }

    private void reset4MonitorPane() {
        this.monitorPane.setSession(session);
    }

}
