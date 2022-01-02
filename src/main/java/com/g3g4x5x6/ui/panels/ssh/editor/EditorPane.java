package com.g3g4x5x6.ui.panels.ssh.editor;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.components.FlatButton;
import com.g3g4x5x6.App;
import com.g3g4x5x6.utils.ConfigUtil;
import com.g3g4x5x6.utils.DbUtil;
import com.g3g4x5x6.utils.DialogUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.sftp.client.fs.SftpFileSystem;
import org.apache.sshd.sftp.client.fs.SftpPath;
import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import java.util.Date;


/**
 * 1. 没有路径的文件、导入的文件默认保存到远程机器的 `/tmp/.ultimateshell/`
 * 2. 本地缓存文件格式：file_path_filename_timesame.ext
 * 3.
 */
@Slf4j
public class EditorPane extends JPanel {
    private BorderLayout borderLayout;
    private JToolBar toolBar;
    private JPanel editorPane;
    private JToolBar statusBar;

    // default directory
    private String defaultDir = "/tmp/.ultimateshell/";
    private String tmpFilePath = "";
    private String remotePath = "";

    // TODO toolBar
    private JTextField titleField;

    // TODO editorPane
    private RSyntaxTextArea textArea;
    private RTextScrollPane sp;

    // TODO statusBar
    private JTextField searchField;
    private JCheckBox regexCB;
    private JCheckBox matchCaseCB;

    // 远程文件系统
    private SshClient client;
    private ClientSession session;
    private SftpFileSystem fs;

    public EditorPane(SftpFileSystem sftpFileSystem) {
        this();
        this.fs = fs;
    }

    public EditorPane() {
        borderLayout = new BorderLayout();
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        editorPane = new JPanel();
        statusBar = new JToolBar();
        statusBar.setFloatable(false);

        initToolBar();
        initEditorPane();

        this.setLayout(borderLayout);
        this.add(toolBar, BorderLayout.NORTH);
        this.add(editorPane, BorderLayout.CENTER);
    }

    private void initToolBar() {
        FlatButton listBtn = new FlatButton();
        listBtn.setButtonType(FlatButton.ButtonType.toolBarButton);
        listBtn.setIcon(new FlatSVGIcon("com/g3g4x5x6/ui/icons/listFiles.svg"));
        listBtn.setToolTipText("最近打开");
        listBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.debug("最近打开列表");
                EditorDialog editorDialog = new EditorDialog();
                editorDialog.setVisible(true);
            }
        });

        FlatButton addBtn = new FlatButton();
        addBtn.setButtonType(FlatButton.ButtonType.toolBarButton);
        addBtn.setIcon(new FlatSVGIcon("com/g3g4x5x6/ui/icons/addFile.svg"));
        addBtn.setToolTipText("新建文件");
        addBtn.addActionListener(new AbstractAction() {
            @SneakyThrows
            @Override
            public void actionPerformed(ActionEvent e) {
                log.debug("新建文件");
                // TODO 1. 判断是否为空，不为空则提示是否保存现有备忘内容，否则清空
                if (!textArea.getText().strip().equals("")) {
                    int ret = JOptionPane.showConfirmDialog(App.mainFrame, "是否保存现有文件内容？", "提示", JOptionPane.YES_NO_CANCEL_OPTION);
                    log.debug(">>>>>>>>>>>>>>>>" + ret);
                    if (ret == 0) {
                        log.debug("保存现有文件内容");
                        // TODO 询问保存路径和文件名
                        if (tmpFilePath.equals("")) {
                            File editor = new File(ConfigUtil.getWorkPath() + "/editor");
                            if (!editor.exists()) {
                                editor.mkdir();
                            }
                            String tmpDir = editor.getAbsolutePath() + "/";
                            tmpFilePath = tmpDir + new Date().getTime();
                            titleField.setText(defaultDir + new Date().getTime());
                            remotePath = titleField.getText();
                        }
                        insertOrUpdate();
                        // 先保存, 再清空
                        titleField.setText("");
                        textArea.setText("");
                        tmpFilePath = "";
                        remotePath = "";
                    } else if (ret == 1) {
                        // 不保存, 清空
                        titleField.setText("");
                        textArea.setText("");
                        tmpFilePath = "";
                        remotePath = "";
                    }
                    // 取消
                    // 啥也不做
                } else {
                    titleField.setText("");
                    textArea.setText("");
                    tmpFilePath = "";
                    remotePath = "";
                }
            }
        });

        FlatButton saveBtn = new FlatButton();
        saveBtn.setButtonType(FlatButton.ButtonType.toolBarButton);
        saveBtn.setIcon(new FlatSVGIcon("com/g3g4x5x6/ui/icons/Save.svg"));
        saveBtn.setToolTipText("保存文件");
        saveBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insertOrUpdate();
            }
        });

        FlatButton importBtn = new FlatButton();
        importBtn.setButtonType(FlatButton.ButtonType.toolBarButton);
        importBtn.setIcon(new FlatSVGIcon("com/g3g4x5x6/ui/icons/import.svg"));
        importBtn.setToolTipText("导入文件");
        importBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.debug("导入文件");
                if (tmpFilePath.equals("")) {
                    if (textArea.getText().strip().equals("")) {
                        // 直接打开
                        log.debug("openNote DIR");
                        importFile();
                    } else {
                        // 询问是否保存现有备忘笔记
                        log.debug("Tips");
                        if (DialogUtil.yesOrNo(App.mainFrame, "是否保存已有文件？") == 0) {
                            insertOrUpdate();
                            importFile();
                        }
                    }
                } else {
                    if (DialogUtil.yesOrNo(App.mainFrame, "是否保存已有文件？") == 0) {
                        insertOrUpdate();
                    }
                    importFile();
                }
            }
        });

        FlatButton exportBtn = new FlatButton();
        exportBtn.setButtonType(FlatButton.ButtonType.toolBarButton);
        exportBtn.setIcon(new FlatSVGIcon("com/g3g4x5x6/ui/icons/export.svg"));
        exportBtn.setToolTipText("导出文件");
        exportBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.debug("导出文件");
                // 创建一个默认的文件选取器
                JFileChooser fileChooser = new JFileChooser();
                // 设置默认显示的文件夹为当前文件夹
                fileChooser.setCurrentDirectory(new File(ConfigUtil.getWorkPath() + "/editor"));
                // 设置文件选择的模式（只选文件、只选文件夹、文件和文件均可选）
                fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                // 打开文件选择框（线程将被阻塞, 直到选择框被关闭）
                int result = fileChooser.showOpenDialog(App.mainFrame);
                if (result == JFileChooser.APPROVE_OPTION) {
                    // 如果点击了"确定", 则获取选择的文件路径
                    File file = fileChooser.getSelectedFile();
                    String fileName = String.valueOf(new Date().getTime());
                    if (!titleField.getText().strip().equals("")) {
                        int index = titleField.getText().lastIndexOf("/");
                        if (index != -1) {
                            fileName = titleField.getText().strip().substring(index);
                        } else {
                            fileName = titleField.getText().strip();
                        }
                    }
                    try (BufferedWriter writer = Files.newBufferedWriter(Path.of(file.getAbsolutePath() + "/" + fileName), StandardCharsets.UTF_8)) {
                        writer.write(textArea.getText());
                        writer.flush();
                    } catch (IOException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        });

        FlatButton checkBtn = new FlatButton();
        checkBtn.setButtonType(FlatButton.ButtonType.toolBarButton);
        checkBtn.setIcon(new FlatSVGIcon("com/g3g4x5x6/ui/icons/shield.svg"));
        checkBtn.setToolTipText("安全检查(shell)");

        titleField = new JTextField();
        titleField.setColumns(15);
        titleField.putClientProperty("JTextField.placeholderText", "远程文件的绝对路径，例如： /home/g3g4x5x6/hello.sh");

        JButton themeBtn = new JButton("default");
        themeBtn.setSelected(true);
        String[] theme_list = new String[]{"default", "dark", "default-alt", "druid", "eclipse", "idea", "monokai", "vs"};
        JPopupMenu lanuageMenu = new JPopupMenu();
        for (String item : theme_list){
            JMenuItem temp = new JMenuItem(item);
            temp.addActionListener(new ActionListener() {
                @SneakyThrows
                @Override
                public void actionPerformed(ActionEvent e) {
                    Theme theme = Theme.load(this.getClass().getClassLoader().getResourceAsStream("org/fife/ui/rsyntaxtextarea/themes/" + temp.getText() + ".xml"));
                    theme.apply(textArea);
                    themeBtn.setToolTipText(temp.getText());
                    themeBtn.setText(temp.getText());
                }
            });
            lanuageMenu.add(temp);
        }
        themeBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                lanuageMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        // 语言设置
        JButton langBtn = new JButton("text/unix");
        langBtn.setSelected(true);
        JPopupMenu langMenu = new JPopupMenu();
        Class syntaxConstantsClass = SyntaxConstants.class;
        Field[] fields = syntaxConstantsClass.getDeclaredFields();
        for (Field field : fields) {
            try {
                String langStr = (String) field.get(syntaxConstantsClass);
                JMenuItem temp = new JMenuItem(langStr);
                temp.addActionListener(new ActionListener() {
                    @SneakyThrows
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        textArea.setSyntaxEditingStyle(langStr);
                        langBtn.setToolTipText(langStr);
                        langBtn.setText(langStr);
                    }
                });
                langMenu.add(temp);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        langBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                langMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        toolBar.add(listBtn);
        toolBar.add(addBtn);
        toolBar.add(saveBtn);
        toolBar.addSeparator();
        toolBar.add(importBtn);
        toolBar.add(exportBtn);
        toolBar.addSeparator();
        toolBar.add(checkBtn);
        toolBar.addSeparator();
        toolBar.add(titleField);
        toolBar.addSeparator();
        toolBar.add(themeBtn);
        toolBar.add(langBtn);
    }

    private void initEditorPane() {
        editorPane.setLayout(new BorderLayout());
        textArea = createTextArea();
        textArea.setSyntaxEditingStyle("text/unix");
        textArea.setCodeFoldingEnabled(true);
        sp = new RTextScrollPane(textArea);
        editorPane.add(sp);
    }

    private RSyntaxTextArea createTextArea() {

        RSyntaxTextArea textArea = new RSyntaxTextArea();
        textArea.setCaretPosition(0);
        textArea.requestFocusInWindow();
        textArea.setMarkOccurrences(true);
        textArea.setCodeFoldingEnabled(true);
        textArea.setClearWhitespaceLinesEnabled(false);

        InputMap im = textArea.getInputMap();
        ActionMap am = textArea.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "decreaseFontSize");
        am.put("decreaseFontSize", new RSyntaxTextAreaEditorKit.DecreaseFontSizeAction());
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "increaseFontSize");
        am.put("increaseFontSize", new RSyntaxTextAreaEditorKit.IncreaseFontSizeAction());

        int ctrlShift = InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, ctrlShift), "copyAsStyledText");
        am.put("copyAsStyledText", new RSyntaxTextAreaEditorKit.CopyAsStyledTextAction());

        try {

            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, ctrlShift), "copyAsStyledTextMonokai");
            am.put("copyAsStyledTextMonokai", createCopyAsStyledTextAction("monokai"));

            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, ctrlShift), "copyAsStyledTextEclipse");
            am.put("copyAsStyledTextEclipse", createCopyAsStyledTextAction("dark"));
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // Since this demo allows the LookAndFeel and RSyntaxTextArea Theme to
        // be toggled independently of one another, we set this property to
        // true so matched bracket popups look good.  In an app where the
        // developer ensures the RSTA Theme always matches the LookAndFeel as
        // far as light/dark is concerned, this property can be omitted.
        System.setProperty(MatchedBracketPopup.PROPERTY_CONSIDER_TEXTAREA_BACKGROUND, "true");

        return textArea;
    }

    private Action createCopyAsStyledTextAction(String themeName) throws IOException {
        String resource = "/org/fife/ui/rsyntaxtextarea/themes/" + themeName + ".xml";
        Theme theme = Theme.load(this.getClass().getResourceAsStream(resource));
        return new RSyntaxTextAreaEditorKit.CopyAsStyledTextAction(themeName, theme);
    }

    private void insertOrUpdate() {
        File editor = new File(ConfigUtil.getWorkPath() + "/editor");
        if (!editor.exists()) {
            editor.mkdir();
        }
        String tmpDir = editor.getAbsolutePath() + "/";

        //获取文件的后缀名
        String ext = getExt(titleField.getText().strip());
        String filePath = getFileName(titleField.getText().strip());

        // 每次检查更新缓存文件
        if (!remotePath.equals("") && !remotePath.equals(titleField.getText().strip())) {
            if (titleField.getText().strip().startsWith("/")) {
                for (String each : filePath.strip().split("/")) {
                    if (each.equals(""))
                        continue;
                    tmpDir += each + "_";
                }
            }
            setDefineTmpFilePath(defaultDir + titleField.getText().strip());
            remotePath = titleField.getText().strip();
        }

        // 初次缓存
        if (tmpFilePath.equals("")) {
            setMark();
            // 保存文件
            if (saveToTmpFile(tmpFilePath)) {
                SftpPath path = fs.getPath(titleField.getText());
                try {
                    log.debug(textArea.getText());
                    Files.write(path, textArea.getText().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.debug("文件缓存成功");
            } else {
                log.debug("文件缓存失败");
            }

        } else {    // 更新操作
            if (saveToTmpFile(tmpFilePath)) {
                SftpPath path = fs.getPath(titleField.getText());
                try {
                    Files.write(path, textArea.getText().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                log.debug("文件更新成功");
            } else {
                log.debug("文件更新失败");
            }
        }
    }

    private void openNote(String id) {
        String sqlForId = "SELECT * FROM note where id=" + id;
        try {
            Connection connection = DbUtil.getConnection();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sqlForId);
            while (resultSet.next()) {
                titleField.setText(resultSet.getString("title"));
                textArea.setText(new String(Base64.getDecoder().decode(resultSet.getString("content")), "utf-8"));
            }
            DbUtil.close(statement);
        } catch (SQLException | UnsupportedEncodingException throwables) {
            throwables.printStackTrace();
        }

    }

    private void importFile() {
        // 创建一个默认的文件选取器
        JFileChooser fileChooser = new JFileChooser();
        // 设置默认显示的文件夹为当前文件夹
        fileChooser.setCurrentDirectory(new File(ConfigUtil.getWorkPath() + "/editor"));
        // 设置文件选择的模式（只选文件、只选文件夹、文件和文件均可选）
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        // 打开文件选择框（线程将被阻塞, 直到选择框被关闭）
        int result = fileChooser.showOpenDialog(App.mainFrame);
        if (result == JFileChooser.APPROVE_OPTION) {
            // 如果点击了"确定", 则获取选择的文件路径
            File file = fileChooser.getSelectedFile();
            String fileName = file.getName();
            StringBuffer sbf = new StringBuffer();
            try (BufferedReader reader = Files.newBufferedReader(Path.of(file.getAbsolutePath()), StandardCharsets.UTF_8)) {
                String tempStr;
                while ((tempStr = reader.readLine()) != null) {
                    sbf.append(tempStr + "\n");
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            int i = fileName.lastIndexOf('.');
            if (i > 0) {
                fileName = fileName.substring(0, i);
            }
            titleField.setText(file.getName());
            textArea.setText(sbf.toString());
        }
    }

    private String getExt(String fileName) {
        //获取最后一个.的位置
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        //获取文件的后缀名
        String suffix = fileName.substring(lastIndexOf);
        return suffix;
    }

    private String getFileName(String fileName) {
        //获取最后一个.的位置
        int lastIndexOf = fileName.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return fileName;
        }
        //获取文件的后缀名
        String suffix = fileName.substring(0, lastIndexOf);
        return suffix;
    }

    private void setDefineTmpFilePath(String tmpFilePath) {
        this.tmpFilePath = tmpFilePath;
    }

    private void setMark(){
        File editor = new File(ConfigUtil.getWorkPath() + "/editor");
        if (!editor.exists()) {
            editor.mkdir();
        }
        String tmpDir = editor.getAbsolutePath() + "/";

        //获取文件的后缀名
        String ext = getExt(titleField.getText().strip());
        String filePath = getFileName(titleField.getText().strip());

        // 使用自定义保存路径
        if (titleField.getText().strip().startsWith("/")) {
            for (String each : filePath.strip().split("/")) {
                if (each.equals(""))
                    continue;
                tmpDir += each + "_";
            }
            tmpDir += new Date().getTime() + ext;
            setDefineTmpFilePath(tmpDir);
            remotePath = titleField.getText().strip();
            log.debug("tmp_file_path: " + tmpFilePath);

            // 使用默认保存路径
        } else {
            // 创建默认目录路径
            SftpPath path = fs.getPath(defaultDir);
            if (!Files.exists(path)) {
                try {
                    Files.createDirectories(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            for (String each : defaultDir.split("/")) {
                if (each.equals(""))
                    continue;
                tmpDir += each + "_";
            }
            tmpDir += new Date().getTime() + ext;
            setDefineTmpFilePath(tmpDir);
            remotePath = defaultDir + titleField.getText().strip();
            titleField.setText(defaultDir + titleField.getText().strip());
        }
    }

    private void cleanMark(){
        titleField.setText("");
        textArea.setText("");
        tmpFilePath = "";
        remotePath = "";
    }

    private boolean saveToTmpFile(String filePath) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(new File(filePath));
            fileOutputStream.write(textArea.getText().getBytes(StandardCharsets.UTF_8));
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private class EditorDialog extends JDialog {
        private JTable editorTable;
        private DefaultTableModel tableModel;
        private String[] columnNames = {"访问时间", "文件路径"};

        private JButton delButton;
        private JButton closeButton;

        public EditorDialog() {
            super(App.mainFrame);
            this.setLayout(new BorderLayout());
            this.setPreferredSize(new Dimension(600, 350));
            this.setSize(new Dimension(600, 350));
            this.setLocationRelativeTo(App.mainFrame);
            this.setModal(true);
            this.setTitle("最近打开文件");

            initEnableOption();

            initNoteListTable();

            initControlButton();

            editorTable.addMouseListener(new MouseAdapter() {
                @SneakyThrows
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        log.debug("双击打开文件");
                        // 双击动作: 打开选中笔记
                        int row = editorTable.getSelectedRow();
                        String noteId = "";
                        if (row != -1) {
                            noteId = (String) tableModel.getValueAt(row, 0);
                            log.debug("row: " + noteId);
                        }
                        if (tmpFilePath.equals("")) {
                            if (textArea.getText().strip().equals("")) {
                                // 直接打开
                                log.debug("openNote DIR");
                                openNote(noteId);
                                dispose();
                            } else {
                                // 询问是否保存现有备忘笔记
                                log.debug("Tips");
                                if (DialogUtil.yesOrNo(App.mainFrame, "是否保存已有备忘笔记？") == 0) {
                                    insertOrUpdate();
                                    openNote(noteId);
                                    dispose();
                                }

                            }
                        } else {
                            if (DialogUtil.yesOrNo(App.mainFrame, "是否保存已有备忘笔记？") == 0) {
                                insertOrUpdate();
                                openNote(noteId);
                                dispose();
                            } else {
                                openNote(noteId);
                                dispose();
                            }
                        }
                    }
                }
            });
        }

        private void initEnableOption() {
            // TODO Enable Option
            JPanel enablePanel = new JPanel();
            FlowLayout flowLayout = new FlowLayout();
            flowLayout.setAlignment(FlowLayout.LEFT);
            enablePanel.setLayout(flowLayout);

            JLabel tips = new JLabel("双击打开选中文件");
            tips.setEnabled(false);
            enablePanel.add(tips);

            this.add(enablePanel, BorderLayout.NORTH);
        }

        private void initNoteListTable() {
            editorTable = new JTable();
            tableModel = new DefaultTableModel() {
                // 不可编辑
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
            tableModel.setColumnIdentifiers(columnNames);

            initTable();

            editorTable.getColumnModel().getColumn(0).setMaxWidth(150);
            editorTable.getColumnModel().getColumn(0).setMinWidth(150);

            JScrollPane tableScroll = new JScrollPane(editorTable);
            tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

            DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
            centerRenderer.setHorizontalAlignment(JTextField.CENTER);
            editorTable.getColumn("访问时间").setCellRenderer(centerRenderer);
            this.add(tableScroll, BorderLayout.CENTER);
        }

        private void initTable() {
            // 获取主题数据
            tableModel.setRowCount(0);
            int row = 0;
            try {
                Connection connection = DbUtil.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * From note");

                while (resultSet.next()) {
                    tableModel.addRow(new String[]{
                            resultSet.getString("id"),
                            resultSet.getString("id").equals(tmpFilePath) ?
                                    "<html><strong><font color='red'>" + getCurrentNote() + "</font></strong></html>" :
                                    resultSet.getString("title")
                    });
                    row = tableModel.getRowCount() - 1;
                }
                DbUtil.close(statement, resultSet);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            editorTable.setModel(tableModel);
        }

        private void initControlButton() {
            JPanel controlPane = new JPanel();
            FlowLayout flowLayout = new FlowLayout();
            flowLayout.setAlignment(FlowLayout.LEFT);
            controlPane.setLayout(flowLayout);
            delButton = new JButton("清空缓存");
            closeButton = new JButton("关闭窗口");
            controlPane.add(delButton);
            controlPane.add(closeButton);
            this.add(controlPane, BorderLayout.SOUTH);

            delButton.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (DialogUtil.yesOrNo(App.mainFrame, "是否删除选中备忘记录？") == 0) {
                        int[] rows = editorTable.getSelectedRows();
                        int ret = 0;
                        for (int row : rows) {
                            if (row != -1) {
                                String noteId = (String) tableModel.getValueAt(row, 0);
                                log.debug("Selected note: " + noteId);
                                String sql = "DELETE FROM note WHERE id=" + noteId;
                                try {
                                    Connection connection = DbUtil.getConnection();
                                    Statement statement = connection.createStatement();
                                    ret = statement.executeUpdate(sql);
                                    DbUtil.close(statement);
                                } catch (SQLException throwables) {
                                    throwables.printStackTrace();
                                }
                                // 删除的笔记如果正在编辑，需重置 current_id = ""
                                if (noteId.equals(tmpFilePath)) {

                                }
                            }
                        }
                        if (ret == 1) {
                            DialogUtil.info("删除备忘记录成功");
                        }
                        initTable();
                    }
                }
            });

            closeButton.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
        }

        private String getCurrentNote() {
            String themeName = "";

            return themeName;
        }
    }
}
