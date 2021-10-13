package com.g3g4x5x6;

import com.formdev.flatlaf.FlatLightLaf;
import com.g3g4x5x6.ui.MainFrame;
import com.g3g4x5x6.utils.ConfigUtil;
import com.g3g4x5x6.utils.DbUtil;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.io.File;


@Slf4j
public class App {
    public static MainFrame mainFrame;

    public static void main(String[] args) {
        // 检查程序运行环境
        checkEnv();
        // 初始化数据库
        DbUtil.createDatabase();
        // 启动主界面
        SwingUtilities.invokeLater(() -> {
                    createGUI();
                }
        );
    }

    public static void createGUI() {
        // 配置主题皮肤
        try {
            if (ConfigUtil.isEnableTheme()) {
//                Class themeClass = App.class.getClassLoader().loadClass(ConfigUtil.getThemeClass());
//                UIManager.setLookAndFeel((LookAndFeel) themeClass.getConstructor().newInstance());
                UIManager.setLookAndFeel(ConfigUtil.getThemeClass());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }
        } catch (Exception ex) {
            log.error("Failed to initialize LaF");
        }

        // 启动主界面
        mainFrame = new MainFrame();
        mainFrame.setTitle("UltimateShell");
        mainFrame.pack();
        mainFrame.setVisible(true);
        log.info("主线程启动完成");
    }

    public static void checkEnv() {
        // TODO 检查是否启用自定义工作目录


        // TODO 检查程序工作目录
        String workspace = ConfigUtil.getWorkPath();

        // TODO 检查备忘笔记目录
        File note = new File(workspace + "/note");
        if (!note.exists()){
            note.mkdir();
        }
        // TODO 检查会话导出目录
        File export = new File(workspace + "/export");
        if (!export.exists()){
            export.mkdir();
        }
        // TODO 检查缓存目录
        File temp = new File(workspace + "/temp");
        if (!temp.exists()){
            temp.mkdir();
        }

        // TODO 检查工具目录
        File tools = new File(workspace + "/tools");
        if (!tools.exists()){
            tools.mkdir();
        }

        // TODO 检查编辑器缓存目录
        File editor = new File(workspace + "/editor");
        if (!editor.exists()){
            editor.mkdir();
        }

        // TODO 检查会话存储目录
        File sessions = new File(workspace + "/sessions");
        if (!sessions.exists()){
            sessions.mkdir();
        }

        // TODO 检查用户配置

    }
}
