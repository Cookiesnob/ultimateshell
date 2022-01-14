package com.g3g4x5x6.ui.embed.nuclei.panel;

import com.alibaba.fastjson.JSONObject;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.g3g4x5x6.utils.DialogUtil;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedList;
import java.util.Map;

@Slf4j
public class TemplatesPanel extends JPanel {
    private static final String defaultNucleiTemplatesPath = System.getProperties().getProperty("user.home") + "/nuclei-templates";
    private JToolBar toolBar = new JToolBar();
    private JButton newBtn = new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/addFile.svg"));
    private JButton openBtn = new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/menu-open.svg"));
    private JButton saveAllBtn = new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/menu-saveall.svg"));
    private JButton closeBtn = new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/ignore_file.svg"));
    private JButton closeAllBtn = new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/ignore_file.svg"));
    private JButton cutBtn = new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/menu-cut.svg"));
    private JButton copyBtn = new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/copy.svg"));
    private JButton pasteBtn = new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/menu-paste.svg"));
    private JButton undoBtn = new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/undo.svg"));
    private JButton redoBtn = new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/redo.svg"));
    private JButton searchBtn = new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/replace.svg"));
    private JToggleButton lineWrapBtn = new JToggleButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/toggleSoftWrap.svg"));
    private JButton terminalBtn = new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/changeView.svg"));
    private JButton severityBtn = new JButton(new FlatSVGIcon("com/g3g4x5x6/ui/icons/sortBySeverity.svg"));
    private JTextField searchField = new JTextField();

    private JScrollPane tableScroll;
    private JTable templatesTable;
    private DefaultTableModel tableModel;
    private JPopupMenu tablePopMenu;
    private TableRowSorter<DefaultTableModel> sorter;

    private final JPopupMenu severityPopupMenu = new JPopupMenu();
    private JCheckBox infoBox = new JCheckBox("Information");
    private JCheckBox lowBox = new JCheckBox("Low");
    private JCheckBox mediumBox = new JCheckBox("Medium");
    private JCheckBox highBox = new JCheckBox("High");
    private JCheckBox criticalBox = new JCheckBox("Critical");

    private final LinkedList<String> templatesList = new LinkedList<>();

    public TemplatesPanel() {
        this.setLayout(new BorderLayout());

        toolBar.setFloatable(false);
        toolBar.add(newBtn);
        toolBar.add(openBtn);
        toolBar.add(saveAllBtn);
        toolBar.add(closeBtn);
        toolBar.add(closeAllBtn);
        toolBar.addSeparator();
        toolBar.add(cutBtn);
        toolBar.add(copyBtn);
        toolBar.add(pasteBtn);
        toolBar.addSeparator();
        toolBar.add(undoBtn);
        toolBar.add(redoBtn);
        toolBar.addSeparator();
        toolBar.add(lineWrapBtn);
        toolBar.addSeparator();
        toolBar.add(severityBtn);
        toolBar.addSeparator();
        toolBar.add(terminalBtn);
        toolBar.addSeparator();
        toolBar.add(Box.createGlue());
        toolBar.add(searchField);
        toolBar.add(searchBtn);
        initToolBarAction();


        tablePopMenu = new JPopupMenu();
        tablePopMenu.add(editAction);
        tablePopMenu.add(openDirAction);
        tablePopMenu.add(copyPathAction);
        tablePopMenu.addSeparator();
        tablePopMenu.add(generateWithSelectedAction);
        tablePopMenu.add(generateWithTagsAction);
        tablePopMenu.add(runWithSelectedAction);
        tablePopMenu.add(runWithTagsAction);


        templatesTable = new JTable();
        tableModel = new DefaultTableModel() {
            // 不可编辑
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        String[] columnNames = {
                "#",
                "templates_id",
                "templates_name",
                "templates_severity",
                "templates_tags",
                "templates_author",
                "templates_description",
                "templates_reference"};
        tableModel.setColumnIdentifiers(columnNames);
        templatesTable.setModel(tableModel);
        initDataForTable();
        tableScroll = new JScrollPane(templatesTable);
        tableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        tableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JTextField.CENTER);
        templatesTable.getColumn("#").setCellRenderer(centerRenderer);
        templatesTable.getColumn("templates_severity").setCellRenderer(centerRenderer);

        templatesTable.getColumn("#").setPreferredWidth(20);
        templatesTable.getColumn("templates_id").setPreferredWidth(60);
        templatesTable.getColumn("templates_name").setPreferredWidth(100);
        templatesTable.getColumn("templates_severity").setPreferredWidth(40);
        templatesTable.getColumn("templates_author").setPreferredWidth(30);
        templatesTable.setComponentPopupMenu(tablePopMenu);
        templatesTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            }
        });

        // 搜索功能
        sorter = new TableRowSorter<DefaultTableModel>(tableModel);
        templatesTable.setRowSorter(sorter);

        this.add(toolBar, BorderLayout.NORTH);
        this.add(tableScroll, BorderLayout.CENTER);
    }

    private void initToolBarAction() {
        infoBox.setSelected(true);
        lowBox.setSelected(true);
        mediumBox.setSelected(true);
        highBox.setSelected(true);
        criticalBox.setSelected(true);
        severityPopupMenu.add(infoBox);
        severityPopupMenu.add(lowBox);
        severityPopupMenu.add(mediumBox);
        severityPopupMenu.add(highBox);
        severityPopupMenu.add(criticalBox);
        JMenuItem okMenuItem = new JMenuItem("确认");
        okMenuItem.setIcon(new FlatSVGIcon("com/g3g4x5x6/ui/icons/inspectionsOK.svg"));
        okMenuItem.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO 过滤风险级别
            }
        });
        severityPopupMenu.add(okMenuItem);
        severityBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                severityPopupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
        terminalBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO 跳转至运行面板：RunningPanel
            }
        });

        searchField.registerKeyboardAction(e -> {
                    String searchKeyWord = searchField.getText().strip();
                    sorter.setRowFilter(RowFilter.regexFilter(searchKeyWord));
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false),
                JComponent.WHEN_FOCUSED);

        searchBtn.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchKeyWord = searchField.getText().strip();
                sorter.setRowFilter(RowFilter.regexFilter(searchKeyWord));
            }
        });
    }

    /**
     * 耗时操作
     */
    private void initDataForTable() {
        new Thread(() -> {
            try {
                // 初始化列表并输出列表大小
                log.debug("Templates Count: " + getAllTemplatesFromPath(defaultNucleiTemplatesPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
            int count = 0;
            for (String path : templatesList) {
                Map template = getMapFromYaml(path);
                JSONObject jsonObject = new JSONObject(template);
                JSONObject info = jsonObject.getJSONObject("info");
                String id = jsonObject.getString("id");
                String name = info.getString("name");
                String severity = info.getString("severity");
                String author = info.getString("author");
                String description = info.getString("description");
                String reference = info.getString("reference");
                String tags = info.getString("tags");
                count++;
                tableModel.addRow(new String[]{String.valueOf(count), id, name, severity, tags, author, description, reference});
            }
        }).start();
    }

    private Map getMapFromYaml(String path) {
        Map template;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 调基础工具类的方法
        Yaml yaml = new Yaml();
        template = yaml.loadAs(inputStream, Map.class);
        return template;
    }

    /**
     * 遍历出目录下的所有 yaml 文件
     *
     * @param rootPath 遍历目录
     * @return 匹配到的文件总数
     * @throws IOException 抛出异常
     */
    private int getAllTemplatesFromPath(String rootPath) throws IOException {
        if (Files.exists(Path.of(rootPath))) {
            Files.walkFileTree(Paths.get(rootPath), new SimpleFileVisitor<>() {
                // 访问文件时触发
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".yaml")) {
                        templatesList.add(file.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }

                // 访问目录时触发
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }
            });
            return templatesList.size();
        }
        return 0;
    }

    private AbstractAction editAction = new AbstractAction("Edit Template") {
        @Override
        public void actionPerformed(ActionEvent e) {
            log.debug("Edit This Template");
        }
    };

    private AbstractAction openDirAction = new AbstractAction("Open in Folder") {
        @Override
        public void actionPerformed(ActionEvent e) {
            log.debug("Open in Folder");
        }
    };

    private AbstractAction copyPathAction = new AbstractAction("Copy Path") {
        @Override
        public void actionPerformed(ActionEvent e) {
            log.debug("Copy Path");
        }
    };

    /**
     * 目标是可以做到多选
     */
    private AbstractAction generateWithSelectedAction = new AbstractAction("Generate command with Selected") {
        @Override
        public void actionPerformed(ActionEvent e) {
            log.debug("Generate command with Selected");
        }
    };

    private AbstractAction generateWithTagsAction = new AbstractAction("Generate command with Tags") {
        @Override
        public void actionPerformed(ActionEvent e) {
            log.debug("Generate command with Tags");
        }
    };

    private AbstractAction runWithSelectedAction = new AbstractAction("Run command with Selected") {
        @Override
        public void actionPerformed(ActionEvent e) {
            log.debug("Run command with Selected");
        }
    };

    private AbstractAction runWithTagsAction = new AbstractAction("Run command with Tags") {
        @Override
        public void actionPerformed(ActionEvent e) {
            log.debug("Run command with Tags");
        }
    };

}
