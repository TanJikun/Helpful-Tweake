package tanjikun.helpful.tweake.config;

import java.util.List;

import com.google.common.collect.ImmutableList;

import fi.dy.masa.malilib.config.options.ConfigHotkey;
import tanjikun.helpful.tweake.Reference;

public class Hotkeys
{
    private static final String HOTKEYS_KEY = Reference.MOD_ID_LOWER + ".config.hotkeys";

    // 打开配置界面，默认 Alt+X
    // MaLiLib 使用 GLFW 键名（去掉 KEY_ 前缀），LEFT_ALT 即左 Alt 键
    public static final ConfigHotkey OPEN_GUI_SETTINGS = new ConfigHotkey("openGuiSettings", "LEFT_ALT,X").apply(HOTKEYS_KEY);

    public static final List<ConfigHotkey> HOTKEY_LIST = ImmutableList.of(
            OPEN_GUI_SETTINGS
    );
}
