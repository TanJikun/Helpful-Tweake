package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 实体渲染优化：跳过不可见实体的渲染。
 *
 * 注入 EntityRenderDispatcher.shouldRender (method_3950) 的 RETURN，
 * 在原版 frustum 可见性判定之后追加两层检查：
 *   1. 距离检查：实体到相机距离 > 阈值 → 返回 false（不渲染）
 *   2. 遮挡检查：从相机到实体包围盒中心做光线投射，
 *      若命中方块（VISUAL 形状，玻璃等透明方块不阻挡）→ 返回 false（不渲染）
 *
 * 方块实体（BlockEntity）不经过 EntityRenderDispatcher.shouldRender，
 * 由 BlockEntityRenderDispatcher 单独渲染，因此本 Mixin 不影响方块实体。
 *
 * 注入 RETURN 而非 HEAD：先尊重原版 frustum 判定，原版已剔除的实体不再做额外计算。
 * 距离检查在遮挡检查之前：距离阈值外的实体直接剔除，避免昂贵的光线投射。
 *
 * ponytail: Mixin 方法名使用 intermediary + remap = false（项目无 refmap，详见 project_memory）
 * 已知上限: 每帧对每个未剔除实体做一次光线投射，实体密集时（如农场）开销显著。
 *           距离阈值默认32先过滤大部分实体，遮挡检查只对近距离实体执行。
 * 升级路径: 可改为离屏遮挡查询或分帧遮挡缓存。
 */
@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher
{
    @Inject(method = "method_3950", at = @At("RETURN"), cancellable = true, remap = false)
    private void helpfulTweake$skipInvisibleEntities(
            Entity entity, Frustum frustum, double camX, double camY, double camZ,
            CallbackInfoReturnable<Boolean> cir)
    {
        if (!Configs.Optimization.ENTITY_RENDER_OPTIMIZATION.getBooleanValue()
                || !Configs.Optimization.SKIP_INVISIBLE_ENTITIES.getBooleanValue())
        {
            return;
        }
        // 原版已判定不渲染，无需额外处理
        if (!cir.getReturnValue())
        {
            return;
        }
        // 距离检查（先做廉价检查，过滤掉远处实体）
        int maxDistance = Configs.Optimization.SKIP_INVISIBLE_ENTITIES_DISTANCE.getIntegerValue();
        double dx = entity.getX() - camX;
        double dy = entity.getY() - camY;
        double dz = entity.getZ() - camZ;
        double distSq = dx * dx + dy * dy + dz * dz;
        if (distSq > (double) maxDistance * (double) maxDistance)
        {
            cir.setReturnValue(false);
            return;
        }
        // 距离极近时跳过遮挡检查（相机可能在方块内或实体内部，光线投射会误判）
        if (distSq < 1.0)
        {
            return;
        }
        // 遮挡检查：从相机到实体包围盒中心做光线投射
        Level level = entity.level();
        if (level == null)
        {
            return;
        }
        AABB box = entity.getBoundingBox();
        Vec3 from = new Vec3(camX, camY, camZ);
        Vec3 to = new Vec3((box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5);
        ClipContext context = new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, entity);
        BlockHitResult hit = level.clip(context);
        if (hit.getType() != HitResult.Type.MISS)
        {
            cir.setReturnValue(false);
        }
    }
}
