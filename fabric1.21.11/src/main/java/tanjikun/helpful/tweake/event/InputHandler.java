package tanjikun.helpful.tweake.event;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybindManager;
import fi.dy.masa.malilib.hotkeys.IKeybindProvider;
import tanjikun.helpful.tweake.Reference;
import tanjikun.helpful.tweake.config.Configs;
import tanjikun.helpful.tweake.config.Hotkeys;

public class InputHandler implements IKeybindProvider
{
    private static final InputHandler INSTANCE = new InputHandler();

    private InputHandler()
    {
    }

    public static InputHandler getInstance()
    {
        return INSTANCE;
    }

    @Override
    public void addKeysToMap(IKeybindManager manager)
    {
        // 注册独立热键
        for (IHotkey hotkey : Hotkeys.HOTKEY_LIST)
        {
            manager.addKeybindToMap(hotkey.getKeybind());
        }

        // 注册 ConfigBooleanHotkeyed 内嵌的热键
        registerHotkeyedConfigs(manager, Configs.Tools.OPTIONS);
        registerHotkeyedConfigs(manager, Configs.Optimization.OPTIONS);
    }

    private void registerHotkeyedConfigs(IKeybindManager manager, Iterable<? extends IConfigBase> configs)
    {
        for (IConfigBase config : configs)
        {
            if (config instanceof IHotkey hotkey)
            {
                manager.addKeybindToMap(hotkey.getKeybind());
            }
        }
    }

    @Override
    public void addHotkeys(IKeybindManager manager)
    {
        manager.addHotkeysForCategory(Reference.MOD_NAME, Reference.MOD_ID_LOWER + ".hotkeys.category.generic_hotkeys", Hotkeys.HOTKEY_LIST);
    }
}
