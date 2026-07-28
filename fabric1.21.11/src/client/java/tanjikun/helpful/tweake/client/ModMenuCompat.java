package tanjikun.helpful.tweake.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

import net.minecraft.client.gui.screens.Screen;

import tanjikun.helpful.tweake.gui.GuiConfigs;

/**
 * Mod Menu 软联动入口。
 * 仅在 Mod Menu 存在时由其通过 fabric.mod.json 的 "modmenu" entrypoint 加载，
 * 因此本类可以安全地直接引用 ModMenuApi，不会在缺少 Mod Menu 时导致崩溃。
 */
public class ModMenuCompat implements ModMenuApi
{
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory()
    {
        return (Screen parent) -> new GuiConfigs();
    }
}
