package com.g3g4x5x6.ui.panels.ssh;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.g3g4x5x6.ui.panels.editor.EditorPane;
import com.g3g4x5x6.ui.panels.ssh.monitor.MonitorPane;
import com.g3g4x5x6.ui.panels.ssh.sftp.SftpBrowser;
import com.g3g4x5x6.utils.DialogUtil;
import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.JediTermWidget;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.session.SessionHeartbeatController;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.apache.sshd.sftp.client.fs.SftpFileSystemProvider;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.formdev.flatlaf.FlatClientProperties.TABBED_PANE_TRAILING_COMPONENT;

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

        init();

        this.sshPane = createTerminalWidget();
        this.sftpBrowser = new SftpBrowser(this.sftpFileSystem);
        this.monitorPane = new MonitorPane(this.session);
        this.editorPane = new EditorPane(sftpFileSystem);

        this.addTab("SSH", this.sshPane);
        this.addTab("SFTP", this.sftpBrowser);
        this.addTab("Monitor", this.monitorPane);
        this.addTab("Editor", this.editorPane);

        customComponents();
    }

    private void init(){
        this.client = SshClient.setUpDefaultClient();
        client.start();
        this.session = getSession(client);
        this.sftpFileSystem = getSftpFileSystem(session);
    }

    private ClientSession getSession(SshClient client){
        ClientSession session;
        try {
            session = client.connect(this.user, this.host, this.port).verify(3000, TimeUnit.MILLISECONDS).getSession();
            session.addPasswordIdentity(this.pass);
            session.auth().verify(3000, TimeUnit.MILLISECONDS);     // TODO No more authentication methods available
            session.setSessionHeartbeat(SessionHeartbeatController.HeartbeatType.IGNORE, Duration.ofMinutes(3));
            return session;
        } catch (IOException e) {
            e.printStackTrace();
            DialogUtil.error(e.getMessage());
            return null;
        }
    }

    private SftpFileSystem getSftpFileSystem(ClientSession session) {
        SftpFileSystemProvider provider = new SftpFileSystemProvider();
        try {
            SftpFileSystem sftpFileSystem = provider.newFileSystem(session);
            return sftpFileSystem;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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
            if (this.user.equals("")) {
                return new MyJSchShellTtyConnector(host, port);
            }
            if (this.pass.equals("")) {
                return new MyJSchShellTtyConnector(host, port, this.user);
            }
            return new MyJSchShellTtyConnector(host, port, user, pass);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private void customComponents() {
        JToolBar leading = null;
        JToolBar trailing = null;

        leading = new JToolBar();
        leading.setFloatable(false);
        leading.setBorder(null);
        leading.add(new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/project.svg")));

        trailing = new JToolBar();
        trailing.setFloatable(false);
        trailing.setBorder(null);
        trailing.add(new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/buildLoadChanges.svg")));
        trailing.add(Box.createHorizontalGlue());
        trailing.add(new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/commit.svg")));
        trailing.add(new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/diff.svg")));
        trailing.add(new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/listFiles.svg")));

//        this.putClientProperty( TABBED_PANE_LEADING_COMPONENT, leading );
        this.putClientProperty(TABBED_PANE_TRAILING_COMPONENT, trailing);
    }

}
