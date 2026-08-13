package tanjikun.helpful.tweake;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.malilib.registry.Registry;
import fi.dy.masa.malilib.util.data.ModInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import tanjikun.helpful.tweake.client.util.ArmorHudRenderer;
import tanjikun.helpful.tweake.client.util.EntityRenderStackCache;
import tanjikun.helpful.tweake.client.util.EntityVisibilityCache;
import tanjikun.helpful.tweake.client.util.KelpBreaker;
import tanjikun.helpful.tweake.client.util.VisualExperienceHudRenderer;
import tanjikun.helpful.tweake.client.util.WorldSwallowMaintenanceRenderer;
import tanjikun.helpful.tweake.config.Configs;
import tanjikun.helpful.tweake.config.CommonConfigs;
import tanjikun.helpful.tweake.config.CrawlTriggerMode;
import tanjikun.helpful.tweake.event.InputHandler;
import tanjikun.helpful.tweake.event.KeyCallbacks;
import tanjikun.helpful.tweake.gui.GuiConfigs;
import tanjikun.helpful.tweake.ServerFlagHolder;
import tanjikun.helpful.tweake.util.CrawlState;

public class InitHandler implements IInitializationHandler
{
    private static boolean lastTransparentBedrock = false;

    /**
     * 客户端入口点调用：注册 MaLiLib 初始化处理器。
     * 独立存在以便 HelpfulTweakeClient 在检查 MaLiLib 存在性后再调用，
     * 避免类加载时触发 MaLiLib 依赖类的加载。
     */
    public static void registerMalilib()
    {
        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }

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

        // 注册可视化经验值 HUD 渲染回调
        HudRenderCallback.EVENT.register(new VisualExperienceHudRenderer());

        // 注册世吞运维助手世界渲染回调
        WorldSwallowMaintenanceRenderer wsmRenderer = new WorldSwallowMaintenanceRenderer();
        WorldRenderEvents.AFTER_ENTITIES.register(wsmRenderer);

        // 堆叠实体渲染优化：每帧渲染结束后清空缓存，为下一帧准备
        WorldRenderEvents.AFTER_ENTITIES.register(context -> EntityRenderStackCache.clear());

        // 实体可见性缓存：每帧递增帧计数器并定期清理过期 entry
        WorldRenderEvents.AFTER_ENTITIES.register(context -> EntityVisibilityCache.tick());

        // 控制爬行：客户端 tick 更新 CrawlState，让 common Mixin 能读到最新状态
        // HOLD 模式实时查询按键，TOGGLE 模式读取切换状态；计算结果写入 crawlRequested
        // 供 common 侧 MixinPlayerCrawl（同时作用于内部服务器）读取
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            // 同步 MaLiLib 配置到 CommonConfigs（供 common 侧 Mixin 读取）
            CommonConfigs.betterTotem = Configs.Tools.BETTER_TOTEM.getBooleanValue();
            CommonConfigs.globalMending = Configs.Tools.GLOBAL_MENDING.getBooleanValue();
            CommonConfigs.betterBoat = Configs.Tools.BETTER_BOAT.getBooleanValue();
            CommonConfigs.boatLiftHeight = Configs.Tools.BOAT_LIFT_HEIGHT.getDoubleValue();

            // 同步禁用水与岩浆互动配置到 ServerFlagHolder（供 common 侧 Mixin 读取）
            ServerFlagHolder.disableLiquidInteraction =
                    Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                    && Configs.Tools.WSM_DISABLE_LIQUID_INTERACTION.getBooleanValue();

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

            // 透明基岩：配置切换时触发区块重渲染（父配置项关闭时也需重渲染恢复）
            boolean currentTransparentBedrock = Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                    && Configs.Tools.WSM_TRANSPARENT_BEDROCK.getBooleanValue();
            if (currentTransparentBedrock != lastTransparentBedrock)
            {
                lastTransparentBedrock = currentTransparentBedrock;
                if (mc.level != null)
                {
                    mc.levelRenderer.allChanged();
                }
            }
        });

        // 世吞运维助手：定期扫描方块更新缓存
        ClientTickEvents.END_CLIENT_TICK.register(wsmRenderer::tick);

        // 清海带：每 tick 扫描并破坏 Litematica 选区内海带
        ClientTickEvents.END_CLIENT_TICK.register(new KelpBreaker());
    }
}
