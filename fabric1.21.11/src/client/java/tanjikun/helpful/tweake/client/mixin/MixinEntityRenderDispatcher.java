package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.client.util.EntityRenderStackCache;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 实体渲染优化：跳过过远实体 + 堆叠实体去重。
 *
 * 注入 EntityRenderDispatcher.shouldRender (method_3950) 的 RETURN，
 * 在原版 frustum 可见性判定之后追加检查：
 *   1. 阻止过远实体渲染（距离检查）
 *   2. 堆叠实体渲染优化（同坐标同类型仅渲染一个）
 *
 * 方块实体（BlockEntity）不经过 EntityRenderDispatcher.shouldRender，
 * 由 BlockEntityRenderDispatcher 单独渲染，因此本 Mixin 不影响方块实体。
 *
 * 注入 RETURN 而非 HEAD：先尊重原版 frustum 判定，原版已剔除的实体不再做额外计算。
 *
 * ponytail: Mixin 方法名使用 intermediary + remap = false（项目无 refmap，详见 project_memory）
 */
@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher
{
    @Inject(method = "method_3950", at = @At("RETURN"), cancellable = true, remap = false)
    private void helpfulTweake$entityRenderOptimization(
            Entity entity, Frustum frustum, double camX, double camY, double camZ,
            CallbackInfoReturnable<Boolean> cir)
    {
        // 原版已判定不渲染，无需额外处理
        if (!cir.getReturnValue())
        {
            return;
        }
        if (!Configs.Optimization.ENTITY_RENDER_OPTIMIZATION.getBooleanValue())
        {
            return;
        }

        // === 阻止过远实体渲染 ===
        if (Configs.Optimization.SKIP_DISTANT_ENTITIES.getBooleanValue())
        {
            double dx = entity.getX() - camX;
            double dy = entity.getY() - camY;
            double dz = entity.getZ() - camZ;
            int maxDistance = Configs.Optimization.SKIP_DISTANT_ENTITIES_DISTANCE.getIntegerValue();
            if (dx * dx + dy * dy + dz * dz > (double) maxDistance * (double) maxDistance)
            {
                cir.setReturnValue(false);
                return;
            }
        }

        // === 堆叠实体渲染优化 ===
        if (Configs.Optimization.STACK_ENTITY_RENDER_OPTIMIZATION.getBooleanValue())
        {
            if (EntityRenderStackCache.shouldSkip(entity))
            {
                cir.setReturnValue(false);
            }
        }
    }
}
