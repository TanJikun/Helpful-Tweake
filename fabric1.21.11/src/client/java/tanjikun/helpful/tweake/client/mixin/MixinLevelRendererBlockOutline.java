package tanjikun.helpful.tweake.client.mixin;

import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.world.phys.shapes.VoxelShape;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 彩色选择框：将准心所对方块的线框选择框替换为彩虹色，3种颜色同时存在并沿
 * 对角线方向从 xyz 正方向角流向负方向角，颜色随时间滚动。
 *
 * 用 @Redirect 完全替换 ShapeRenderer.renderShape 调用，自己实现渲染逻辑，
 * 为每个顶点根据其位置计算不同颜色，实现"同时存在3种颜色"的效果。
 *
 * 核心思路（参考光影包 rainbow.glsl 的三角波合成，用自己的实现）：
 *   1. 三角波彩虹：tri(x) = clamp(|((x mod 6)+6) mod 6 - 3| - 1, 0, 1)
 *      三个通道相位差2，周期6。每个顶点根据位置参数 p=(x+y+z)/3 偏移相位，
 *      位置跨度对应2个相位单位（1/3周期），3个通道刚好覆盖3种颜色。
 *      正方向角(1,1,1) p=1 相位领先2，负方向角(0,0,0) p=0 相位落后，
 *      随时间整体偏移，颜色从正方向流向负方向。
 *   2. 线宽加粗：在原版线宽基础上 ×1.3 加粗。
 *   3. 远处更细：按 1/distance 衰减线宽，保持视觉粗细比例恒定。
 *
 * 关键发现（字节码验证）：renderShape 第8个参数是线宽（setLineWidth），
 *   不是 alpha。颜色 alpha 由 setColor(int) 的 ARGB int 值决定。
 *   原版调用链：addVertex → setColor(int) → setNormal(Pose, Vector3f) → setLineWidth(float)
 *
 * ponytail: Mixin 方法名与 target 字符串使用 intermediary
 * 已知上限: Loom 1.17 + officialMojangMappings 无 refmap，注解字符串无法重映射。
 * 升级路径: 升级 Loom 或改用 layered mappings 生成 refmap 后，可改回 named 名。
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

    @Redirect(
        method = "method_22712",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_9974;method_62296(Lnet/minecraft/class_4587;Lnet/minecraft/class_4588;Lnet/minecraft/class_265;DDDIF)V",
            remap = false
        ),
        remap = false
    )
    private static void helpfulTweake$redirectRenderShape(
            PoseStack pose, VertexConsumer consumer, VoxelShape shape,
            double x, double y, double z, int color, float lineWidth)
    {
        if (!Configs.Tools.RAINBOW_SELECTION.getBooleanValue())
        {
            ShapeRenderer.renderShape(pose, consumer, shape, x, y, z, color, lineWidth);
            return;
        }

        PoseStack.Pose poseEntry = pose.last();

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
        float finalWidth = lineWidth * WIDTH_BOOST * widthScale;

        // 时间相位（6秒一周期）
        float timePhase = ((System.currentTimeMillis() % PERIOD_MS) / (float) PERIOD_MS) * 6.0F;

        shape.forAllEdges((minX, minY, minZ, maxX, maxY, maxZ) -> {
            // 法线向量
            Vector3f normal = new Vector3f(
                    (float) (maxX - minX),
                    (float) (maxY - minY),
                    (float) (maxZ - minZ)).normalize();

            // 顶点1颜色：位置参数 p=(x+y+z)/3，相位 = timePhase + p*2
            // 正方向角 p 大→相位领先，颜色先变化→颜色从正方向流向负方向
            float p1 = (float) ((minX + minY + minZ) / 3.0);
            int c1 = rainbowTri(timePhase + p1 * 2.0F);

            // 顶点2颜色
            float p2 = (float) ((maxX + maxY + maxZ) / 3.0);
            int c2 = rainbowTri(timePhase + p2 * 2.0F);

            // 渲染顶点1
            consumer.addVertex(poseEntry, (float) (minX + x), (float) (minY + y), (float) (minZ + z))
                    .setColor(c1)
                    .setNormal(poseEntry, normal)
                    .setLineWidth(finalWidth);

            // 渲染顶点2
            consumer.addVertex(poseEntry, (float) (maxX + x), (float) (maxY + y), (float) (maxZ + z))
                    .setColor(c2)
                    .setNormal(poseEntry, normal)
                    .setLineWidth(finalWidth);
        });
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
