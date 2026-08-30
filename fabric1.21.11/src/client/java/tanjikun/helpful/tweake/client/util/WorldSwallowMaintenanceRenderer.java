package tanjikun.helpful.tweake.client.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import tanjikun.helpful.tweake.client.mixin.MixinBlockEntityRenderDispatcherAccessor;
import tanjikun.helpful.tweake.client.mixin.MixinCameraAccessor;
import tanjikun.helpful.tweake.config.Configs;
import tanjikun.helpful.tweake.config.RenderDirection;

/**
 * 世吞运维助手：在目标方块上方渲染旋转副本，便于远距离定位。
 *
 * 行为：
 *   - 扫描玩家周围（水平曼哈顿距离 ≤ 距离阈值×16，Y ± 32）的方块
 *   - 匹配目标方块列表中的方块（如开启含水检测则还包括所有含水方块）
 *   - 在匹配方块位置 + 渲染方向 × 渲染高度 处渲染一个绕 X 轴旋转 45° 的方块副本
 *   - 渲染时再次检查水平曼哈顿距离条件（玩家可能移动）
 *
 * 性能：
 *   - 每 20 tick（1秒）扫描一次，缓存匹配方块
 *   - 渲染时遍历缓存列表，检查距离条件
 *
 * ponytail: Y 扫描范围限制为玩家 Y ± 32，避免扫描整个世界高度
 * 已知上限: 默认距离阈值 4（64格）时，扫描区域约 8192×65=532K 方块，每秒一次可接受
 * 升级路径: 若需扫描全高度，可改用区块级遍历或异步扫描
 *
 * 注意：1.21.11 中 ominous_vault/ominous_trial_spawner 不是独立方块 ID，
 *       而是 vault/trial_spawner 的 OMINOUS 属性，无法在方块 ID 列表中区分。
 */
public class WorldSwallowMaintenanceRenderer implements WorldRenderEvents.AfterEntities
{
    private static final int SCAN_INTERVAL_TICKS = 20;

    private final List<CachedBlock> cachedBlocks = new ArrayList<>();
    private int tickCounter = 0;

    private Set<Block> targetBlocks = new HashSet<>();
    private List<String> lastTargetList = null;
    private boolean lastWaterloggedFlag = false;

    // 箱子/末影箱的 BakedModel 是空的（视觉由 BlockEntityRenderer 处理），
    // renderSingleBlock 渲染不出，需用 ChestModel + ModelPart 直接渲染。
    private ModelPart chestModelRoot;

    @Override
    public void afterEntities(WorldRenderContext context)
    {
        if (!Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                || !Configs.Tools.WSM_RENDER_COPY.getBooleanValue())
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        MultiBufferSource bufferSource = context.consumers();
        if (bufferSource == null)
        {
            return;
        }

        int renderDistance = Configs.Tools.WSM_RENDER_HEIGHT.getIntegerValue();
        RenderDirection renderDir = (RenderDirection) Configs.Tools.WSM_RENDER_DIRECTION.getOptionListValue();
        int distanceThreshold = Configs.Tools.WSM_DISTANCE_THRESHOLD.getIntegerValue() * 16;
        Vec3 camPos = ((MixinCameraAccessor) mc.gameRenderer.getMainCamera()).getPosition();
        BlockPos playerPos = mc.player.blockPosition();

        PoseStack poseStack = context.matrices();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();

        for (CachedBlock cb : cachedBlocks)
        {
            int manhattan = Math.abs(cb.pos.getX() - playerPos.getX())
                          + Math.abs(cb.pos.getZ() - playerPos.getZ());
            if (manhattan > distanceThreshold)
            {
                continue;
            }

            // 渲染位置 = 方块位置 + 渲染方向 × 渲染距离，相对于相机坐标
            // 绕 X 轴旋转 45°：先移到方块中心，旋转，再移回
            poseStack.pushPose();
            poseStack.translate(cb.pos.getX() - camPos.x + renderDistance * renderDir.getOffsetX(),
                                cb.pos.getY() - camPos.y + renderDistance * renderDir.getOffsetY(),
                                cb.pos.getZ() - camPos.z + renderDistance * renderDir.getOffsetZ());
            poseStack.translate(0.5, 0.5, 0.5);
            poseStack.mulPose(Axis.XP.rotationDegrees(45.0f));
            poseStack.translate(-0.5, -0.5, -0.5);

            if (isChestLike(cb.state))
            {
                // 箱子/末影箱：BakedModel 为空，用 ChestModel + ModelPart 渲染。
                // ModelPart 内部自动按 SCALE_FACTOR(1/16) 缩放，无需额外 scale。
                // 纹理走图集系统，需用 Material.buffer() 获取 UV 重映射的 VertexConsumer。
                ensureChestModel();
                Material material = getChestMaterial(cb.state);
                MaterialSet materialSet = ((MixinBlockEntityRenderDispatcherAccessor)
                    mc.getBlockEntityRenderDispatcher()).getMaterials();
                VertexConsumer buffer = material.buffer(materialSet, bufferSource,
                    RenderTypes::entityCutoutNoCull);
                chestModelRoot.render(poseStack, buffer,
                                      LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            else
            {
                dispatcher.renderSingleBlock(cb.state, poseStack, bufferSource,
                                             LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            }
            poseStack.popPose();
        }
    }

    /**
     * 判断是否为箱子类方块（BakedModel 为空，需用 ChestModel 渲染）。
     * 覆盖普通箱子、陷阱箱、铜箱子（均为 ChestBlock 子类）和末影箱。
     */
    private boolean isChestLike(BlockState state)
    {
        Block block = state.getBlock();
        return block instanceof ChestBlock || block == Blocks.ENDER_CHEST;
    }

    /**
     * 获取箱子 Material（图集纹理 + RenderType 信息）。
     * 用 Sheets 预定义的 Material，确保纹理已注册到图集。
     */
    private Material getChestMaterial(BlockState state)
    {
        Block block = state.getBlock();
        if (block == Blocks.ENDER_CHEST)
        {
            return Sheets.ENDER_CHEST_LOCATION;
        }
        if (block == Blocks.TRAPPED_CHEST)
        {
            return Sheets.CHEST_TRAP_LOCATION;
        }
        if (block == Blocks.COPPER_CHEST)
        {
            return Sheets.COPPER_CHEST_LOCATION;
        }
        if (block == Blocks.EXPOSED_COPPER_CHEST)
        {
            return Sheets.EXPOSED_COPPER_CHEST_LOCATION;
        }
        if (block == Blocks.WEATHERED_COPPER_CHEST)
        {
            return Sheets.WEATHERED_COPPER_CHEST_LOCATION;
        }
        if (block == Blocks.OXIDIZED_COPPER_CHEST)
        {
            return Sheets.OXIDIZED_COPPER_CHEST_LOCATION;
        }
        return Sheets.CHEST_LOCATION;
    }

    private void ensureChestModel()
    {
        if (chestModelRoot == null)
        {
            chestModelRoot = ChestModel.createSingleBodyLayer().bakeRoot();
        }
    }

    /**
     * 客户端 tick 回调：定期扫描方块，更新缓存。
     * 由 InitHandler 通过 ClientTickEvents.END_CLIENT_TICK 注册调用。
     */
    public void tick(Minecraft mc)
    {
        if (!Configs.Tools.WORLD_SWALLOW_MAINTENANCE.getBooleanValue()
                || !Configs.Tools.WSM_RENDER_COPY.getBooleanValue())
        {
            if (!cachedBlocks.isEmpty())
            {
                cachedBlocks.clear();
            }
            return;
        }
        if (mc.player == null || mc.level == null)
        {
            if (!cachedBlocks.isEmpty())
            {
                cachedBlocks.clear();
            }
            return;
        }

        tickCounter++;
        if (tickCounter < SCAN_INTERVAL_TICKS)
        {
            return;
        }
        tickCounter = 0;

        updateTargetBlocks();
        scanBlocks(mc);
    }

    /**
     * 从配置解析目标方块集合。仅在配置变更时重新解析。
     */
    private void updateTargetBlocks()
    {
        List<String> currentList = Configs.Tools.WSM_TARGET_BLOCKS.getStrings();
        boolean currentFlag = Configs.Tools.WSM_RENDER_WATERLOGGED.getBooleanValue();

        if (currentList.equals(lastTargetList) && currentFlag == lastWaterloggedFlag)
        {
            return;
        }
        lastTargetList = new ArrayList<>(currentList);
        lastWaterloggedFlag = currentFlag;

        targetBlocks.clear();
        for (String id : currentList)
        {
            try
            {
                Identifier identifier = Identifier.parse(id);
                Block block = BuiltInRegistries.BLOCK.getValue(identifier);
                if (block != null && block != Blocks.AIR)
                {
                    targetBlocks.add(block);
                }
            }
            catch (Exception e)
            {
                // 无效 ID，跳过
            }
        }
    }

    /**
     * 扫描玩家周围方块，将匹配的方块加入缓存。
     * XZ 遍历曼哈顿菱形，Y 限制为玩家 Y ± 32。
     */
    private void scanBlocks(Minecraft mc)
    {
        cachedBlocks.clear();
        if (targetBlocks.isEmpty() && !lastWaterloggedFlag)
        {
            return;
        }

        int threshold = Configs.Tools.WSM_DISTANCE_THRESHOLD.getIntegerValue() * 16;
        BlockPos playerPos = mc.player.blockPosition();
        int px = playerPos.getX();
        int pz = playerPos.getZ();
        int minY = playerPos.getY() - 32;
        int maxY = playerPos.getY() + 32;

        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for (int dx = -threshold; dx <= threshold; dx++)
        {
            int remaining = threshold - Math.abs(dx);
            for (int dz = -remaining; dz <= remaining; dz++)
            {
                int x = px + dx;
                int z = pz + dz;
                for (int y = minY; y <= maxY; y++)
                {
                    mutablePos.set(x, y, z);
                    BlockState state = mc.level.getBlockState(mutablePos);
                    if (state.isAir())
                    {
                        continue;
                    }
                    boolean match = targetBlocks.contains(state.getBlock());
                    if (!match && lastWaterloggedFlag)
                    {
                        match = state.getFluidState().is(Fluids.WATER);
                    }
                    if (match)
                    {
                        cachedBlocks.add(new CachedBlock(mutablePos.immutable(), state));
                    }
                }
            }
        }
    }

    private record CachedBlock(BlockPos pos, BlockState state) {}
}
