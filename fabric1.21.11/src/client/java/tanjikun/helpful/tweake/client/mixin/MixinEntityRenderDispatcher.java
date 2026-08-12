package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.client.util.EntityRenderStackCache;
import tanjikun.helpful.tweake.client.util.EntityVisibilityCache;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 实体渲染优化：跳过不可见/过远实体 + 堆叠实体去重。
 *
 * 注入 EntityRenderDispatcher.shouldRender (method_3950) 的 RETURN，
 * 在原版 frustum 可见性判定之后追加检查：
 *   1. 阻止过远实体渲染（距离检查，廉价，先做）
 *   2. 阻止不可见实体渲染（遮挡检查，昂贵，后做）
 *   3. 堆叠实体渲染优化（同坐标同类型仅渲染一个）
 *
 * 方块实体（BlockEntity）不经过 EntityRenderDispatcher.shouldRender，
 * 由 BlockEntityRenderDispatcher 单独渲染，因此本 Mixin 不影响方块实体。
 *
 * 注入 RETURN 而非 HEAD：先尊重原版 frustum 判定，原版已剔除的实体不再做额外计算。
 * 距离检查在遮挡检查之前：距离阈值外的实体直接剔除，避免昂贵的光线投射。
 *
 * ponytail: Mixin 方法名使用 intermediary + remap = false（项目无 refmap，详见 project_memory）
 * 已知上限: 遮挡检查每帧对每个未剔除实体做一次光线投射，实体密集时开销显著。
 *           距离阈值默认32先过滤大部分实体，遮挡检查只对近距离实体执行。
 * 升级路径: 可改为离屏遮挡查询或分帧遮挡缓存。
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

        double dx = entity.getX() - camX;
        double dy = entity.getY() - camY;
        double dz = entity.getZ() - camZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        // === 阻止过远实体渲染（廉价检查，先做以减少后续开销）===
        if (Configs.Optimization.SKIP_DISTANT_ENTITIES.getBooleanValue())
        {
            int maxDistance = Configs.Optimization.SKIP_DISTANT_ENTITIES_DISTANCE.getIntegerValue();
            if (distSq > (double) maxDistance * (double) maxDistance)
            {
                cir.setReturnValue(false);
                return;
            }
        }

        // === 阻止不可见实体渲染（遮挡检查，昂贵，使用位置触发缓存） ===
        // 缓存策略参考 EntityCulling：实体/相机位置变化超过 0.5 格才重算 clip()。
        // 实体不动（堆叠苦力怕等）时几乎零开销，主线程不再每帧调用 level.clip()。
        if (Configs.Optimization.SKIP_INVISIBLE_ENTITIES.getBooleanValue())
        {
            // 距离极近时跳过遮挡检查（相机可能在方块内或实体内部，光线投射会误判）
            if (distSq >= 1.0)
            {
                Boolean cached = EntityVisibilityCache.get(entity, camX, camY, camZ);
                if (cached != null)
                {
                    // 缓存命中：跳过 clip() 调用
                    if (!cached)
                    {
                        cir.setReturnValue(false);
                        return;
                    }
                }
                else
                {
                    // 缓存未命中：做光线投射并缓存结果
                    boolean visible = helpfulTweake$computeVisibility(entity, camX, camY, camZ);
                    EntityVisibilityCache.put(entity, camX, camY, camZ, visible);
                    if (!visible)
                    {
                        cir.setReturnValue(false);
                        return;
                    }
                }
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

    /**
     * 计算实体是否对相机可见（未被实心方块完全遮挡）。
     * 命中透明方块（如玻璃，isSolidRender=false）不视为遮挡。
     * @return true 可见（不应跳过），false 被遮挡（应跳过）
     */
    @Unique
    private boolean helpfulTweake$computeVisibility(Entity entity, double camX, double camY, double camZ)
    {
        Level level = entity.level();
        if (level == null) return true;
        AABB box = entity.getBoundingBox();
        Vec3 from = new Vec3(camX, camY, camZ);
        Vec3 to = new Vec3((box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5);
        // 用 COLLIDER 而非 VISUAL：1.21.11 中 getVisualShape 默认返回 Shapes.empty()，
        // VISUAL 形状对所有方块为空，光线投射永不命中。COLLIDER 用 getCollisionShape，
        // 实心方块有完整碰撞箱。
        ClipContext context = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity);
        BlockHitResult hit = level.clip(context);
        if (hit.getType() == HitResult.Type.MISS) return true;
        // 命中方块后检查是否完全遮挡视线：玻璃等透明方块 isSolidRender=false，不视为遮挡
        BlockState state = level.getBlockState(hit.getBlockPos());
        return !state.isSolidRender();
    }
}
