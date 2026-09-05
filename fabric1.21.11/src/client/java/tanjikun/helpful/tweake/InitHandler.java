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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import tanjikun.helpful.tweake.client.util.ArmorHudRenderer;
import tanjikun.helpful.tweake.client.util.EntityRenderStackCache;
import tanjikun.helpful.tweake.client.util.HopperContainerHighlightRenderer;
import tanjikun.helpful.tweake.client.util.KelpBreaker;
import tanjikun.helpful.tweake.client.util.VisualExperienceHudRenderer;
import tanjikun.helpful.tweake.client.util.WorldSwallowMaintenanceRenderer;
import tanjikun.helpful.tweake.config.Configs;
import tanjikun.helpful.tweake.config.CommonConfigs;
import tanjikun.helpful.tweake.event.InputHandler;
import tanjikun.helpful.tweake.event.KeyCallbacks;
import tanjikun.helpful.tweake.gui.GuiConfigs;
import tanjikun.helpful.tweake.ServerFlagHolder;

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

        // 注册吸取容器高亮世界渲染回调
        WorldRenderEvents.AFTER_ENTITIES.register(new HopperContainerHighlightRenderer());

        // 堆叠实体渲染优化：每帧渲染结束后清空缓存，为下一帧准备
        WorldRenderEvents.AFTER_ENTITIES.register(context -> EntityRenderStackCache.clear());

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            // 同步 MaLiLib 配置到 CommonConfigs（供 common 侧 Mixin 读取）
            CommonConfigs.betterTotem = Configs.Tools.BETTER_TOTEM.getBooleanValue();
            CommonConfigs.globalMending = Configs.Tools.GLOBAL_MENDING.getBooleanValue();
            CommonConfigs.infiniteWater = Configs.Tools.INFINITE_WATER.getBooleanValue();
            CommonConfigs.fakePeaceful = Configs.Optimization.FAKE_PEACEFUL.getBooleanValue();
            CommonConfigs.fakePeacefulWardenHearing = Configs.Optimization.FAKE_PEACEFUL_WARDEN_HEARING.getBooleanValue();

            // 同步禁用水与岩浆互动配置到 ServerFlagHolder（供 common 侧 Mixin 读取）
            ServerFlagHolder.disableLiquidInteraction =
                    Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                    && Configs.Tools.WSM_DISABLE_LIQUID_INTERACTION.getBooleanValue();

            // 同步未完成进度显示配置到 ServerFlagHolder（供 common 侧 Mixin 读取）
            // 需服务端将隐藏进度加入 visible 集合并同步给客户端，仅注入客户端 isHidden 不够
            ServerFlagHolder.showUncompletedAdvancements =
                    Configs.Tools.BETTER_ADVANCEMENTS.getBooleanValue()
                    && Configs.Tools.SHOW_UNCOMPLETED_ADVANCEMENTS.getBooleanValue();

            // 透明基岩：配置切换时触发区块重渲染 + 光照重算
            boolean currentTransparentBedrock = Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                    && Configs.Tools.WSM_TRANSPARENT_BEDROCK.getBooleanValue();
            if (currentTransparentBedrock != lastTransparentBedrock)
            {
                lastTransparentBedrock = currentTransparentBedrock;
                if (mc.level != null)
                {
                    // 光照重算：getLightBlock() Mixin 改了返回值，但光照引擎在 chunk 加载时
                    // 预计算并缓存了光照数据，不会自动更新。需对基岩位置调用 checkBlock 触发重算。
                    relightBedrock(mc);
                    mc.levelRenderer.allChanged();
                }
            }
        });

        // 世吞运维助手：定期扫描方块更新缓存
        ClientTickEvents.END_CLIENT_TICK.register(wsmRenderer::tick);

        // 清海带：每 tick 扫描并破坏 Litematica 选区内海带
        ClientTickEvents.END_CLIENT_TICK.register(new KelpBreaker());
    }

    /**
     * 遍历玩家附近的基岩方块，调用 LightEngine.checkBlock 触发光照重算。
     *
     * 光照引擎在 chunk 加载时通过 getLightBlock() 预计算光照数据并缓存到 light section，
     * 修改 getLightBlock() 的 Mixin 返回值不会自动更新缓存。checkBlock 会让光照引擎
     * 重新查询该位置的 getLightBlock()（此时 Mixin 返回 0），并传播光照变化到周围方块。
     *
     * ponytail: 只遍历基岩层（Y=minY 到 minY+5）+ 玩家 Y±32，避免扫描全高度。
     * 已知上限: XZ 半径 48，远处基岩在 chunk 重新加载时自动更新。
     */
    private static void relightBedrock(Minecraft mc)
    {
        if (mc.player == null || mc.level == null)
        {
            return;
        }
        var lightEngine = mc.level.getChunkSource().getLightEngine();
        BlockPos playerPos = mc.player.blockPosition();
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int radius = 48;

        // 基岩层（底层）
        int minY = mc.level.getMinY();
        for (int y = minY; y <= minY + 5 && y < mc.level.getMaxY(); y++)
        {
            for (int dx = -radius; dx <= radius; dx++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    mutable.set(playerPos.getX() + dx, y, playerPos.getZ() + dz);
                    if (mc.level.getBlockState(mutable).is(Blocks.BEDROCK))
                    {
                        lightEngine.checkBlock(mutable);
                    }
                }
            }
        }

        // 玩家 Y±32（覆盖天花板等非底层基岩）
        int py = playerPos.getY();
        for (int y = py - 32; y <= py + 32; y++)
        {
            if (y < minY || y > mc.level.getMaxY())
            {
                continue;
            }
            // 跳过已扫描的基岩层
            if (y >= minY && y <= minY + 5)
            {
                continue;
            }
            for (int dx = -radius; dx <= radius; dx++)
            {
                for (int dz = -radius; dz <= radius; dz++)
                {
                    mutable.set(playerPos.getX() + dx, y, playerPos.getZ() + dz);
                    if (mc.level.getBlockState(mutable).is(Blocks.BEDROCK))
                    {
                        lightEngine.checkBlock(mutable);
                    }
                }
            }
        }
    }
}
