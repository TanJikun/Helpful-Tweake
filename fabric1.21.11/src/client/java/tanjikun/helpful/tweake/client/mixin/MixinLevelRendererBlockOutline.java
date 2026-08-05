package tanjikun.helpful.tweake.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import net.minecraft.client.renderer.LevelRenderer;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 彩色选择框：将准心所对方块的线框选择框替换为彩虹色，颜色随时间滚动。
 *
 * 完全重写版本，参考光影包的两个核心思路（不照搬代码，用自己的实现）：
 *   1. 三角波彩虹（参考 rainbow.glsl 的三角波合成思路）：
 *      用三个相位差2的三角波 tri(t*6+φ) 直接合成 RGB。
 *      三角波 tri(x) = clamp(|((x mod 6)+6) mod 6 - 3| - 1, 0, 1)
 *      三个通道相位分别为 0/2/4，覆盖完整色环，6秒一个周期。
 *   2. 远处更细（参考 worldOutline.glsl 的"轮廓只叠加不覆盖"思路）：
 *      原版线框固定 1px 宽，远处方块在屏幕上变小，线条相对变粗。
 *      按 1/distance 衰减 alpha，让线条视觉强度与方块屏幕大小（也 ∝ 1/distance）
 *      同步衰减，保持视觉粗细比例恒定。近处清晰，远处变淡。
 *
 * 距离来源：renderHitOutline (method_22712) 内部调用 renderShape (method_62296) 时，
 *   x,y,z 参数 = blockPos - cameraPos（方块相对摄像机偏移，字节码验证）。
 *   distance = sqrt(x²+y²+z²)，直接从 Args 的 index 3,4,5 获取，无需额外注入。
 *
 * 注入点：LevelRenderer.renderHitOutline (method_22712) 内对
 *         ShapeRenderer.renderShape (method_62296) 的调用。
 *         用 @ModifyArgs 在同一个注入点同时修改 index=6 (int color) 和
 *         index=7 (float alpha)，避免多个 @ModifyArg 在同一 @At 上的字节码问题。
 *
 * ponytail: Mixin 方法名与 target 字符串使用 intermediary
 * 已知上限: 本项目用 officialMojangMappings 但 Loom 1.17 未生成 refmap，导致 Mixin 注解
 *           的 method/target 字符串无法从 named 重映射到 intermediary。运行时游戏类用 intermediary。
 * 升级路径: 升级 Loom 或改用 layered mappings 生成 refmap 后，可改回 named 名。
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRendererBlockOutline
{
    /** 参考距离（格）：小于此距离 alpha 不衰减，保持清晰。 */
    private static final double REF_DISTANCE = 5.0;

    /** 远处最小 alpha 比例，避免线条完全消失。 */
    private static final float MIN_ALPHA = 0.15F;

    /** 彩虹滚动周期（毫秒），6秒一循环。 */
    private static final long PERIOD_MS = 6000L;

    @ModifyArgs(
        method = "method_22712",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/class_9974;method_62296(Lnet/minecraft/class_4587;Lnet/minecraft/class_4588;Lnet/minecraft/class_265;DDDIF)V",
            remap = false
        ),
        remap = false
    )
    private void helpfulTweake$rainbowOutlineArgs(Args args)
    {
        if (!Configs.Tools.RAINBOW_SELECTION.getBooleanValue())
        {
            return;
        }
        float t = (System.currentTimeMillis() % PERIOD_MS) / (float) PERIOD_MS;
        args.set(6, rainbowTri(t));

        // renderShape 的 x,y,z (index 3,4,5) = blockPos - cameraPos
        // distance = sqrt(x²+y²+z²)，按 1/distance 衰减 alpha
        double dx = ((Number) args.get(3)).doubleValue();
        double dy = ((Number) args.get(4)).doubleValue();
        double dz = ((Number) args.get(5)).doubleValue();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        float originalAlpha = (float) args.get(7);
        float alphaScale;
        if (distance <= REF_DISTANCE)
        {
            alphaScale = 1.0F;
        }
        else
        {
            alphaScale = (float) (REF_DISTANCE / distance);
            if (alphaScale < MIN_ALPHA)
            {
                alphaScale = MIN_ALPHA;
            }
        }
        args.set(7, originalAlpha * alphaScale);
    }

    /**
     * 三角波彩虹色：三个通道用相位差2的三角波合成。
     * tri(x) = clamp(|((x mod 6) + 6) mod 6 - 3| - 1, 0, 1)
     * 产生完整色环的彩虹，6秒一周期。
     */
    private static int rainbowTri(float t)
    {
        float phase = t * 6.0F;
        int r = toByte(tri(phase));
        int g = toByte(tri(phase + 2.0F));
        int b = toByte(tri(phase + 4.0F));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** 三角波函数：输入 x，输出 [0,1]。周期6，峰值在 x=0。 */
    private static float tri(float x)
    {
        float m = ((x % 6.0F) + 6.0F) % 6.0F;
        float v = Math.abs(m - 3.0F) - 1.0F;
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    private static int toByte(float v)
    {
        return (int) (v * 255.0F);
    }
}
