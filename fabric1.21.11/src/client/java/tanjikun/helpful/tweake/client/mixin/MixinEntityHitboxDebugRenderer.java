package tanjikun.helpful.tweake.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import tanjikun.helpful.tweake.config.Configs;
@Mixin(EntityHitboxDebugRenderer.class)
public class MixinEntityHitboxDebugRenderer
{
    @Inject(method = "method_75432", at = @At("TAIL"), remap = false)
    private void helpfulTweake$renderHopperMinecartSuckRange(Entity entity, float partialTick,
            boolean isServer, CallbackInfo ci)
    {
        // isServer=true 仅在开发模式的本地服务器实体绘制时出现，跟随客户端实体绘制一次即可
        // 主开关（更好的漏斗矿车）与子开关（吸取范围显示）需同时开启
        if (isServer || !Configs.Tools.BETTER_HOPPER_MINECART.getBooleanValue()
                || !Configs.Tools.BETTER_HOPPER_MINECART_HITBOX.getBooleanValue())
        {
            return;
        }
        if (!(entity instanceof MinecartHopper hopper))
        {
            return;
        }

        // 框线颜色：默认绿色；开启锁定显示且矿车被充能激活铁轨锁定时换为锁定颜色
        // ponytail: 1.21.11 的 enabled 字段不同步到客户端（isEnabled() 客户端恒为默认值 true），
        // 改为客户端复刻原版判定（OldMinecartBehavior/NewMinecartBehavior.tick）：
        // 矿车所在铁轨是 ACTIVATOR_RAIL 且 POWERED=true → activateMinecart(true) → enabled=false
        int color = Configs.Tools.HOPPER_SUCK_RANGE_COLOR.getIntegerValue() | 0xFF000000;
        if (Configs.Tools.HOPPER_MINECART_LOCKED_DISPLAY.getBooleanValue() && helpfulTweake$isLocked(hopper))
        {
            color = Configs.Tools.HOPPER_LOCKED_COLOR.getIntegerValue() | 0xFF000000;
        }

        // 与原版 showHitboxes 相同的插值偏移，使范围框随矿车平滑移动
        Vec3 delta = entity.getPosition(partialTick).subtract(entity.position());

        // 吸取范围 1：矿车上方的吸取柱
        AABB suckColumn = hopper.getSuckAabb()
                .move(hopper.getLevelX() - 0.5, hopper.getLevelY() - 0.5, hopper.getLevelZ() - 0.5)
                .move(delta);
        Gizmos.cuboid(suckColumn, GizmoStyle.stroke(color));

        // 吸取范围 2：矿车周围的接触区域
        AABB original = entity.getBoundingBox();
        AABB contactArea = new AABB(
                original.minX - 0.25, // X轴负方向
                original.minY - 0.2, // Y轴负方向（下面）
                original.minZ - 0.25, // Z轴负方向
                original.maxX + 0.25, // X轴正方向
                original.maxY - 0.15, // Y轴正方向（上面）
                original.maxZ + 0.25 // Z轴正方向
        ).move(delta);
        Gizmos.cuboid(contactArea, GizmoStyle.stroke(color));
    }

    /**
     * 客户端复刻原版锁定判定：矿车所在铁轨为充能的激活铁轨即视为锁定。
     */
    private static boolean helpfulTweake$isLocked(MinecartHopper hopper)
    {
        BlockState railState = hopper.level().getBlockState(hopper.getCurrentBlockPosOrRailBelow());
        return railState.is(Blocks.ACTIVATOR_RAIL) && railState.getValue(PoweredRailBlock.POWERED);
    }
}