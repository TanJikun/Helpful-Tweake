package tanjikun.helpful.tweake.event;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import tanjikun.helpful.tweake.config.Configs;
import tanjikun.helpful.tweake.config.Hotkeys;
import tanjikun.helpful.tweake.gui.GuiConfigs;

public class KeyCallbacks
{
    public static void init(Minecraft mc)
    {
        Hotkeys.OPEN_GUI_SETTINGS.getKeybind().setCallback(new OpenGuiCallback(mc));
        Configs.Tools.BETTER_AUTO_JUMP.getKeybind().setCallback(new ToggleBetterAutoJumpCallback());
        Configs.Tools.GLOBAL_MENDING.getKeybind().setCallback(new ToggleGlobalMendingCallback());
    }

    private record OpenGuiCallback(Minecraft mc) implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            GuiBase.openGui(new GuiConfigs());
            return true;
        }
    }

    private record ToggleBetterAutoJumpCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Tools.BETTER_AUTO_JUMP.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Tools.BETTER_AUTO_JUMP.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.betterAutoJump." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    private record ToggleGlobalMendingCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Tools.GLOBAL_MENDING.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Tools.GLOBAL_MENDING.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.globalMending." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }
}
