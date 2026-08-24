package tanjikun.helpful.tweake.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.debug.EntityHitboxDebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.minecart.MinecartHopper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import tanjikun.helpful.tweake.config.Configs;
@Mixin(EntityHitboxDebugRenderer.class)
public class MixinEntityHitboxDebugRenderer
{
    private static final int SUCK_RANGE_COLOR = 0xFF00FFFF;

    @Inject(method = "method_75432", at = @At("TAIL"), remap = false)
    private void helpfulTweake$renderHopperMinecartSuckRange(Entity entity, float partialTick,
            boolean isServer, CallbackInfo ci)
    {
        // isServer=true 仅在开发模式的本地服务器实体绘制时出现，跟随客户端实体绘制一次即可
        // 主开关（更好的漏斗矿车）与子开关（碰撞箱）需同时开启
        if (isServer || !Configs.Tools.BETTER_HOPPER_MINECART.getBooleanValue()
                || !Configs.Tools.BETTER_HOPPER_MINECART_HITBOX.getBooleanValue())
        {
            return;
        }
        if (!(entity instanceof MinecartHopper hopper))
        {
            return;
        }

        // 与原版 showHitboxes 相同的插值偏移，使范围框随矿车平滑移动
        Vec3 delta = entity.getPosition(partialTick).subtract(entity.position());

        // 吸取范围 1：矿车上方的吸取柱
        AABB suckColumn = hopper.getSuckAabb()
                .move(hopper.getLevelX() - 0.5, hopper.getLevelY() - 0.5, hopper.getLevelZ() - 0.5)
                .move(delta);
        Gizmos.cuboid(suckColumn, GizmoStyle.stroke(SUCK_RANGE_COLOR));

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
        Gizmos.cuboid(contactArea, GizmoStyle.stroke(SUCK_RANGE_COLOR));
    }
}