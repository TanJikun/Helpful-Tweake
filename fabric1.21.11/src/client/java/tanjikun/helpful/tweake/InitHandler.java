package tanjikun.helpful.tweake;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import tanjikun.helpful.tweake.client.util.ArmorHudRenderer;
import tanjikun.helpful.tweake.config.Configs;
import tanjikun.helpful.tweake.config.CrawlTriggerMode;
import tanjikun.helpful.tweake.event.InputHandler;
import tanjikun.helpful.tweake.event.KeyCallbacks;
import tanjikun.helpful.tweake.gui.GuiConfigs;
import tanjikun.helpful.tweake.util.CrawlState;

public class InitHandler implements IInitializationHandler
{
    @Override
    public void registerModHandlers()
    {
        ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());

        Registry.CONFIG_SCREEN.registerConfigScreenFactory(
                new ModInfo(Reference.MOD_ID, Reference.MOD_NAME, GuiConfigs::new)
        );

        InputEventHandler.getKeybindManager().registerKeybindProvider(InputHandler.getInstance());

        KeyCallbacks.init(Minecraft.getInstance());

        // 注册盔甲 HUD 渲染回调（Fabric API 事件，无需 Mixin）
        HudRenderCallback.EVENT.register(new ArmorHudRenderer());

        // 控制爬行：客户端 tick 更新 CrawlState，让 common Mixin 能读到最新状态
        // HOLD 模式实时查询按键，TOGGLE 模式读取切换状态；计算结果写入 crawlRequested
        // 供 common 侧 MixinPlayerCrawl（同时作用于内部服务器）读取
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            LocalPlayer player = mc.player;
            if (player == null)
            {
                CrawlState.reset();
                return;
            }
            CrawlState.setLocalPlayerUuid(player.getUUID());
            CrawlTriggerMode mode = (CrawlTriggerMode) Configs.Tools.CRAWL_TRIGGER_MODE.getOptionListValue();
            boolean requested = (mode == CrawlTriggerMode.HOLD)
                    ? Configs.Tools.CONTROLLED_CRAWL.getKeybind().isKeybindHeld()
                    : CrawlState.isToggleCrawling();
            CrawlState.setCrawlRequested(requested);
        });
    }
}
