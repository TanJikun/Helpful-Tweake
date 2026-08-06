package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 更好的攀爬：玩家攀爬可攀爬方块（梯子/藤蔓等）时，按住空格/Shift 控制上下方向，
 * 视角倾斜程度调整速度大小。
 *
 * 行为：
 *   - 按住空格（跳跃键）：向上爬，速度随 |xRot| 从 UP_SPEED_MIN 线性增至 UP_SPEED_MAX
 *   - 按住 Shift：向下爬，速度随 |xRot| 从 DOWN_SPEED_MIN 线性增至 DOWN_SPEED_MAX（绝对值）
 *   - 同时按两个键或都不按：不修改速度，遵循原版 Math.max(y, -0.15) 钳制行为
 *   - 视角倾斜程度 = |xRot| / 90，范围 0~1（与视角朝上朝下无关，仅看倾斜幅度）
 *
 * 设计意图：按键决定方向（玩家直觉操作），视角倾斜决定速度（视角越陡越快）。
 * 视角水平时按键仍有最小响应，避免按键完全失效的体验。
 *
 * 注入点：LivingEntity.handleOnClimbable(Vec3) 的 RETURN。
 * 原版该方法用 Math.max(deltaMovement.y, -0.15) 将向下速度钳制在 -0.15，
 * 因此直接注入 travel 末尾无效（下一 tick 会被钳回）。
 * 在 handleOnClimbable 的 RETURN 覆盖返回的 Vec3 的 y 分量，绕过原版钳制。
 *
 * 数据参考：https://github.com/artemisSystem/better-climbing
 *
 * ponytail: Mixin 方法名使用 intermediary（method_18801）而非 named（handleOnClimbable）
 * 已知上限: 本项目用 officialMojangMappings 但 Loom 1.17 未生成 refmap，导致 Mixin 注解
 *           的 method 字符串无法从 named 重映射到 intermediary。运行时游戏类用 intermediary。
 *           method_18801 签名 (Vec3) -> Vec3，无重载歧义。原版方法为 private，Mixin 仍可注入。
 *           仅对本地玩家 LocalPlayer 生效（客户端模组）。input.keyPresses 字段访问为普通
 *           字段读取，Loom remapJar 会自动重映射，无需 remap=false。
 * 升级路径: 升级 Loom 或改用 layered mappings 生成 refmap 后，可改回 named 名 "handleOnClimbable"。
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntityClimb
{
    /** 视角水平（pitch=0）时的向上速度：按键按下的最小响应 */
    private static final double UP_SPEED_MIN = 0.15;
    /** 视角垂直（|pitch|=90）时的向上速度：最大 */
    private static final double UP_SPEED_MAX = 0.5;
    /** 视角水平时的向下速度绝对值：按键按下的最小响应 */
    private static final double DOWN_SPEED_MIN = 0.15;
    /** 视角垂直时的向下速度绝对值：最大 */
    private static final double DOWN_SPEED_MAX = 0.6;

    // method_18801 = LivingEntity.handleOnClimbable(Vec3) -> Vec3
    @Inject(method = "method_18801", at = @At("RETURN"), remap = false, cancellable = true)
    private void helpfulTweake$betterClimb(Vec3 deltaMovement, CallbackInfoReturnable<Vec3> cir)
    {
        if (!Configs.Tools.BETTER_CLIMBING.getBooleanValue())
        {
            return;
        }

        // 仅对本地玩家生效（客户端模组）
        if (!((Object) this instanceof LocalPlayer player))
        {
            return;
        }

        // 未在攀爬：不修改返回值
        if (!player.onClimbable())
        {
            return;
        }

        // 读取按键状态：空格=向上，Shift=向下
        // input.keyPresses 是 1.21.11 引入的统一输入记录（class_10185 Input record）
        boolean jumpPressed = player.input.keyPresses.jump();
        boolean shiftPressed = player.input.keyPresses.shift();

        // 同时按或都不按：不修改，遵循原版
        if (jumpPressed == shiftPressed)
        {
            return;
        }

        // 视角倾斜程度（0=水平, 1=垂直），方向不重要，仅看倾斜幅度
        float pitchAbs = Math.abs(player.getXRot()) / 90.0F;
        if (pitchAbs > 1.0F)
        {
            pitchAbs = 1.0F;
        }

        double targetY;
        if (jumpPressed)
        {
            // 向上爬：视角越陡越快
            targetY = UP_SPEED_MIN + (UP_SPEED_MAX - UP_SPEED_MIN) * pitchAbs;
        }
        else
        {
            // 向下爬：视角越陡越快（取负值表示向下）
            double downSpeed = DOWN_SPEED_MIN + (DOWN_SPEED_MAX - DOWN_SPEED_MIN) * pitchAbs;
            targetY = -downSpeed;
        }

        // 覆盖返回的 Vec3 的 y 分量，绕过原版 Math.max(y, -0.15) 钳制
        Vec3 original = cir.getReturnValue();
        cir.setReturnValue(new Vec3(original.x, targetY, original.z));
    }
}
