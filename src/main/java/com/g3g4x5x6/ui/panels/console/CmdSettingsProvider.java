package com.g3g4x5x6.ui.panels.console;

import com.jediterm.terminal.ui.UIUtil;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;

import java.awt.*;

public class CmdSettingsProvider extends DefaultSettingsProvider {
    @Override
    public Font getTerminalFont() {
        String fontName;
        if (UIUtil.isWindows) {
            fontName = "新宋体";
        } else if (UIUtil.isMac) {
            fontName = "Menlo";
        } else {
            fontName = "Monospaced";
        }
        return new Font(fontName, Font.PLAIN, (int)getTerminalFontSize());
    }

    @Override
    public float getTerminalFontSize() {
        return 16;
    }
}
