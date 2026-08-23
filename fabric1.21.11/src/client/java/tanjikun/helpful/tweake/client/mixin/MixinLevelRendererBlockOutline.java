package tanjikun.helpful.tweake.client.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.BlockOutlineRenderState;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 彩色选择框：将准心所对方块的线框选择框替换为彩虹色，3种颜色同时存在。
 * 颜色按边分组滚动：从 xyz 正方向角相邻的3条边出发，经中间6条边，
 * 最后到达 xyz 负方向角相邻的3条边，随时间循环流动。
 *
 * 用 @Inject 在 renderBlockOutline（method_62210）入口拦截并 ci.cancel()，
 * 完全接管原版选择框渲染，自己经 RenderTypes.lines() 标准管线提交顶点。
 *
 * 为什么这样注入（光影兼容，参考 BlockOutlineCustomizer 的做法）：
 *   旧实现 @Redirect renderHitOutline 内部的 ShapeRenderer.renderShape 调用，
 *   光影（Iris 等）接管渲染管线后不再走这条原版调用链，Redirect 落空导致失效；
 *   HEAD + cancel 只在方法入口抢占，不依赖原版方法体的任何调用结构，且
 *   RenderTypes.lines() 走标准 BufferSource 管线，光影包会正常处理该几何体。
 *
 * 核心思路（参考光影包 rainbow.glsl 的三角波合成，用自己的实现）：
 *   1. 三角波彩虹：tri(x) = clamp(|((x mod 6)+6) mod 6 - 3| - 1, 0, 1)
 *      三个通道相位差2，周期6，产生完整色环。
 *   2. 按边着色（非按顶点）：每条边用其中点位置和 s=(x+y+z) 做相位偏移，
 *      整条边同色。单位立方体的12条边按 s 恰好分成3组：
 *        正方向角(1,1,1)相邻3条边 s=2.5（相位领先，最先变色）
 *        中间6条边               s=1.5
 *        负方向角(0,0,0)相邻3条边 s=0.5（相位落后，最后变色）
 *      随时间整体偏移相位，颜色从正方向角出发，经其相邻3条边、中间6条边，
 *      最后流到负方向角相邻的3条边。3组相位差各1（1/6周期），3种颜色同存。
 *   3. 线宽加粗：在原版线宽基础上 ×1.3 加粗。
 *   4. 远处更细：按 1/distance 衰减线宽，保持视觉粗细比例恒定。
 *
 * ponytail: Mixin 方法名使用 intermediary，已验证存在
 * 已知上限: 无 refmap 环境，method_62210 写死；升级 Loom 生成 refmap 后可改 named 名。
 *
 * ponytail: cancel 后未复刻原版 DEBUG_SHAPES 调试形状与 highContrast
 * 黑色衬底层（无障碍高对比模式只画彩虹单层）。两者均为非默认路径，
 * 生产环境不可达/彩虹本身对比度已足够。升级路径：需要时在注入方法里
 * 按 state.highContrast() 补画 secondaryBlockOutline 衬底层。
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRendererBlockOutline
{
    /** 参考距离（格）：小于此距离线宽不衰减。 */
    private static final double REF_DISTANCE = 5.0;

    /** 远处最小线宽比例，避免线条完全消失。 */
    private static final float MIN_SCALE = 0.15F;

    /** 线宽加粗系数。 */
    private static final float WIDTH_BOOST = 1.3F;

    /** 彩虹滚动周期（毫秒），6秒一循环。 */
    private static final long PERIOD_MS = 6000L;

    @Inject(method = "method_62210", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$renderRainbowOutline(MultiBufferSource.BufferSource bufferSource,
            PoseStack poseStack, boolean translucentPass, LevelRenderState levelRenderState, CallbackInfo ci)
    {
        if (!Configs.Tools.RAINBOW_SELECTION.getBooleanValue())
        {
            return;
        }

        // 复刻原版 renderBlockOutline 的前置过滤：无目标或非本趟半透明 pass 时不接管
        BlockOutlineRenderState state = levelRenderState.blockOutlineRenderState;
        if (state == null || state.isTranslucent() != translucentPass)
        {
            return;
        }

        ci.cancel();

        Vec3 cameraPos = levelRenderState.cameraRenderState.pos;
        BlockPos pos = state.pos();
        VoxelShape shape = state.shape();
        double x = pos.getX() - cameraPos.x;
        double y = pos.getY() - cameraPos.y;
        double z = pos.getZ() - cameraPos.z;

        PoseStack.Pose poseEntry = poseStack.last();
        VertexConsumer consumer = bufferSource.getBuffer(RenderTypes.lines());

        // 距离衰减线宽：远处线条更细，保持视觉粗细比例恒定
        double distance = Math.sqrt(x * x + y * y + z * z);
        float widthScale;
        if (distance <= REF_DISTANCE)
        {
            widthScale = 1.0F;
        }
        else
        {
            widthScale = (float) (REF_DISTANCE / distance);
            if (widthScale < MIN_SCALE)
            {
                widthScale = MIN_SCALE;
            }
        }
        float finalWidth = Minecraft.getInstance().getWindow().getAppropriateLineWidth()
                * WIDTH_BOOST * widthScale;

        // 时间相位（6秒一周期）
        float timePhase = ((System.currentTimeMillis() % PERIOD_MS) / (float) PERIOD_MS) * 6.0F;

        shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
            // 法线向量
            Vector3f normal = new Vector3f(
                    (float) (maxX - minX),
                    (float) (maxY - minY),
                    (float) (maxZ - minZ)).normalize();

            // 边中点位置和 s=(x+y+z)：整条边同色，单位立方体12条边分成3组
            // 正角边 s=2.5 相位领先先变色，中间边 s=1.5，负角边 s=0.5 最后变色，
            // 颜色从正方向角相邻3条边→中间6条边→负方向角相邻3条边滚动
            float s = (float) (minX + minY + minZ + maxX + maxY + maxZ) / 2.0F;
            int color = rainbowTri(timePhase + s);

            // 渲染顶点1
            consumer.addVertex(poseEntry, (float) (minX + x), (float) (minY + y), (float) (minZ + z))
                    .setColor(color)
                    .setNormal(poseEntry, normal)
                    .setLineWidth(finalWidth);

            // 渲染顶点2
            consumer.addVertex(poseEntry, (float) (maxX + x), (float) (maxY + y), (float) (maxZ + z))
                    .setColor(color)
                    .setNormal(poseEntry, normal)
                    .setLineWidth(finalWidth);
        });

        // 原版在渲染后立即提交 lines 批次，这里保持一致，确保每帧绘制
        bufferSource.endBatch(RenderTypes.lines());
    }

    /**
     * 三角波彩虹色：三个通道用相位差2的三角波合成。
     * tri(x) = clamp(|((x mod 6) + 6) mod 6 - 3| - 1, 0, 1)
     * 产生完整色环的彩虹，6秒一周期。
     */
    @Unique
    private static int rainbowTri(float t)
    {
        float phase = t * 1.0F;
        int r = toByte(tri(phase));
        int g = toByte(tri(phase + 2.0F));
        int b = toByte(tri(phase + 4.0F));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** 三角波函数：输入 x，输出 [0,1]。周期6，峰值在 x=0。 */
    @Unique
    private static float tri(float x)
    {
        float m = ((x % 6.0F) + 6.0F) % 6.0F;
        float v = Math.abs(m - 3.0F) - 1.0F;
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    @Unique
    private static int toByte(float v)
    {
        return (int) (v * 255.0F);
    }
}
