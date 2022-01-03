package com.g3g4x5x6.ui.panels.ssh;

import com.jediterm.terminal.Questioner;
import com.jediterm.terminal.TtyConnector;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.PtyChannelConfiguration;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Slf4j
public class DefaultTtyConnector implements TtyConnector {
    private ClientSession session;
    private ChannelShell channel;
    private PtyChannelConfiguration ptyConfig;
    private Map<String, ?> env;

    private Dimension myPendingTermSize;
    private Dimension pixelSize;

    private PipedOutputStream channelOut;
    private InputStream channelIn;
    private OutputStream outputStream;
    private BufferedReader reader;
    private BufferedWriter writer;

    public DefaultTtyConnector(ClientSession clientSession) {
        this.session = clientSession;
    }

    @Override
    public boolean init(Questioner questioner) {
        try {
            PipedOutputStream out = new PipedOutputStream();
            channelIn = new PipedInputStream(out);
            channelOut = new PipedOutputStream();
            PipedInputStream in = new PipedInputStream(channelOut);
            reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(out));

            channel = initClientChannel(session, channelIn, channelOut);

            outputStream = channel.getInvertedIn();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    private ChannelShell initClientChannel(ClientSession session, InputStream input, OutputStream output) throws IOException {
        this.ptyConfig = getPtyChannelConfiguration();
        this.env = getEnv();
        ChannelShell channel = session.createShellChannel(this.ptyConfig, this.env);
        channel.setIn(input);
        channel.setOut(output);
        channel.setErr(output);
//        channel.setPtyLines(40);
//        channel.setPtyColumns(140);
        channel.open().verify(3000, TimeUnit.MILLISECONDS);
//        channel.setPtyLines(40);
//        channel.setPtyColumns(140);
        return channel;
    }

    private PtyChannelConfiguration getPtyChannelConfiguration() {
        PtyChannelConfiguration ptyConfig = new PtyChannelConfiguration();
        ptyConfig.setPtyType("xterm");
        return ptyConfig;
    }

    private Map<String, ?> getEnv() {
        Map<String, String> env = new LinkedHashMap<>();
        String lang = System.getenv().get("LANG");
        env.put("LANG", lang != null ? lang : "zh_CN.UTF-8");
        env.put("compression.s2c", "zlib,none");
        env.put("compression.c2s", "zlib,none");
        env.put("StrictHostKeyChecking", "no");
        return env;
    }

    @SneakyThrows
    @Override
    public void close() {
        channel.close();
    }

    @Override
    public String getName() {
        String name = session.getConnectAddress().toString();
        if (name == null)
            name = "SSH";
        return name;
    }

    /**
     * TODO 本地保存会话记录：String.valueOf(chars )
     * @param chars
     * @param i
     * @param i1
     * @return
     * @throws IOException
     */
    @Override
    public int read(char[] chars, int i, int i1) throws IOException {
//        log.debug(">>>>>>>>>>>>>>>" + String.valueOf(chars ));
        return reader.read(chars, i, i1);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        outputStream.write(bytes);
        outputStream.flush();
    }

    @Override
    public boolean isConnected() {
        return channel.isOpen();
    }

    /**
     * TODO 本地保存命令历史记录：string
     * @param string
     * @throws IOException
     */
    @Override
    public void write(String string) throws IOException {
        log.debug(">>>>>>>>>>>>>>>>>>>>>>>>>>" + string);
        this.write(string.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int waitFor() throws InterruptedException {
        return channel.getExitStatus();
    }

    @Override
    public boolean ready() throws IOException {
        return true;
    }

    @Override
    public void resize(Dimension termWinSize, Dimension pixelSize) {
        log.debug(termWinSize.height + ":" + termWinSize.width);
        this.myPendingTermSize = termWinSize;
        this.pixelSize = pixelSize;
        if (this.channel != null) {
            this.resizeImmediately();
        }
    }

    private void resizeImmediately() {
        if (this.myPendingTermSize != null) {
            if (this.pixelSize == null){
                this.setPtySize(this.myPendingTermSize.width, this.myPendingTermSize.height, 0, 0);
            }
            this.setPtySize(this.myPendingTermSize.width, this.myPendingTermSize.height, pixelSize.width, pixelSize.height);
            this.myPendingTermSize = null;
            this.pixelSize = null;
        }
    }

    private void setPtySize(int col, int row, int wp, int hp) {
        log.debug(col + ":" + row + "==" + wp + ":" + hp);
        try {
            channel.sendWindowChange(col, row);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
