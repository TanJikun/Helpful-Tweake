package tanjikun.helpful.tweake.client.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import tanjikun.helpful.tweake.client.mixin.MixinCameraAccessor;
import tanjikun.helpful.tweake.config.Configs;

import java.util.List;

/**
 * 吸取容器高亮：为漏斗矿车正在吸取的容器方块描边。
 *
 * 挂载于 WorldRenderEvents.AFTER_ENTITIES（Fabric API 事件，无需 Mixin），
 * 不依赖 F3+B，开关开启后持续显示。
 *
 * 判定与原版吸取逻辑一致（HopperBlockEntity.suckInItems 反编译确认）：
 *   pos = BlockPos.containing(hopper.getX/Y/Z + [0, 1.0, 0])
 *   容器存在（HopperBlockEntity.getContainerAt(level, pos) != null）→ 原版每 tick 尝试从该容器吸取
 * "正在吸取"为客户端可见的等价判定（实际物品转移是服务端逻辑，客户端无从得知）。
 *
 * ponytail: 每帧 getEntitiesOfClass 扫描玩家周围 32 格，实体数通常极少，可接受；
 * 升级路径：缓存矿车位置 + 变化检测，位置不变时跳过容器查询。
 */
public class HopperContainerHighlightRenderer implements WorldRenderEvents.AfterEntities
{
    private static final double SCAN_RADIUS = 32.0;

    @Override
    public void afterEntities(WorldRenderContext context)
    {
        if (!Configs.Tools.BETTER_HOPPER_MINECART.getBooleanValue()
                || !Configs.Tools.HOPPER_CONTAINER_HIGHLIGHT.getBooleanValue())
        {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }
        var bufferSource = context.consumers();
        if (bufferSource == null)
        {
            return;
        }

        List<MinecartHopper> carts = mc.level.getEntitiesOfClass(
                MinecartHopper.class, mc.player.getBoundingBox().inflate(SCAN_RADIUS));
        if (carts.isEmpty())
        {
            return;
        }

        // ponytail: ConfigColor.getIntegerValue() 对 6 位 hex 返回 0x00RRGGBB（alpha=0），
        // renderShape 的 color int 直接透传给 VertexConsumer.setColor(int)（ARGB），必须补 alpha，
        // 否则完全透明不可见；末位 float 参数是 lineWidth 而非 alpha（字节码验证）
        int color = Configs.Tools.HOPPER_CONTAINER_HIGHLIGHT_COLOR.getIntegerValue() | 0xFF000000;
        // renderShape 末位 float 参数是线宽（字节码验证：透传 VertexConsumer.setLineWidth）
        float lineWidth = Configs.Tools.HOPPER_CONTAINER_HIGHLIGHT_WIDTH.getIntegerValue();
        PoseStack poseStack = context.matrices();
        Vec3 camPos = ((MixinCameraAccessor) mc.gameRenderer.getMainCamera()).getPosition();
        VertexConsumer buffer = bufferSource.getBuffer(RenderTypes.lines());

        for (MinecartHopper hopper : carts)
        {
            // 原版 getSourceContainer 的查询位置：矿车上方一格
            BlockPos pos = BlockPos.containing(
                    hopper.getLevelX(), hopper.getLevelY() + 1.0, hopper.getLevelZ());
            if (HopperBlockEntity.getContainerAt(mc.level, pos) == null)
            {
                continue;
            }
            VoxelShape shape = mc.level.getBlockState(pos).getShape(mc.level, pos);
            if (shape.isEmpty())
            {
                continue;
            }
            ShapeRenderer.renderShape(poseStack, buffer, shape,
                    pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z,
                    color, lineWidth);
        }
    }
}
