package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 更好的攀爬：玩家攀爬可攀爬方块（梯子/藤蔓等）时，根据视角倾斜方向调整纵向速度。
 *
 * 行为：
 *   - 向下爬（视角向下倾斜 20°~90°）：y 速度线性减至 -0.4，持续 60 tick 后再减至 -0.6
 *   - 向上爬（视角向上倾斜 20°~90°）：y 速度线性增至 0.25，持续 60 tick 后再增至 0.5
 *   - 视角不在 20°~90° 范围、未在攀爬、或方向改变时，重置计时器
 *
 * 注入点：LivingEntity.handleOnClimbable(Vec3) 的 RETURN。
 * 原版该方法用 Math.max(deltaMovement.y, -0.15) 将向下速度钳制在 -0.15，
 * 因此直接注入 travel 末尾无效（下一 tick 会被钳回）。
 * 在 handleOnClimbable 的 RETURN 覆盖返回的 Vec3 的 y 分量，绕过原版钳制。
 *
 * 数据来源：https://github.com/artemisSystem/better-climbing
 *
 * ponytail: Mixin 方法名使用 intermediary（method_18801）而非 named（handleOnClimbable）
 * 已知上限: 本项目用 officialMojangMappings 但 Loom 1.17 未生成 refmap，导致 Mixin 注解
 *           的 method 字符串无法从 named 重映射到 intermediary。运行时游戏类用 intermediary。
 *           method_18801 签名 (Vec3) -> Vec3，无重载歧义。原版方法为 private，Mixin 仍可注入。
 * 升级路径: 升级 Loom 或改用 layered mappings 生成 refmap 后，可改回 named 名 "handleOnClimbable"。
 */
@Mixin(LivingEntity.class)
public class MixinLivingEntityClimb
{
    /** 触发角度阈值（度数） */
    private static final float PITCH_THRESHOLD = 20.0F;
    private static final float PITCH_MAX = 90.0F;

    /** 第一阶段速度阈值 */
    private static final double DOWN_SPEED_STAGE1 = -0.4;
    private static final double DOWN_SPEED_STAGE2 = -0.6;
    private static final double UP_SPEED_STAGE1 = 0.25;
    private static final double UP_SPEED_STAGE2 = 0.5;

    /** 线性增速所需 tick 数（从 0 到第一阶段速度） */
    private static final int RAMP_TICKS = 10;
    /** 从第一阶段到第二阶段速度的线性增量 tick 数 */
    private static final int RAMP_TICKS_2 = 5;
    /** 持续攀爬进入第二阶段所需 tick 数（保持第一阶段速度的时间） */
    private static final int STAGE2_TICKS = 60;

    /** 当前攀爬方向：0=无, 1=向下, 2=向上 */
    @Unique
    private int helpfulTweake$climbDir = 0;

    /** 当前方向已持续 tick 数 */
    @Unique
    private int helpfulTweake$climbTicks = 0;

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

        // 未在攀爬：重置状态，不修改返回值
        if (!player.onClimbable())
        {
            helpfulTweake$climbDir = 0;
            helpfulTweake$climbTicks = 0;
            return;
        }

        float xRot = player.getXRot();
        int newDir = 0;
        // xRot > 0 向下看；xRot < 0 向上看
        if (xRot >= PITCH_THRESHOLD && xRot <= PITCH_MAX)
        {
            newDir = 1; // 向下
        }
        else if (xRot <= -PITCH_THRESHOLD && xRot >= -PITCH_MAX)
        {
            newDir = 2; // 向上
        }

        // 方向改变或停止：重置计时器
        if (newDir != helpfulTweake$climbDir)
        {
            helpfulTweake$climbDir = newDir;
            helpfulTweake$climbTicks = 0;
        }

        // 视角不在触发范围：不修改速度
        if (newDir == 0)
        {
            return;
        }

        // 根据持续 tick 数计算目标 y 速度（绝对计算，不依赖 currentY）
        // 阶段1：线性从 0 到第一阶段速度（RAMP_TICKS tick）
        // 阶段2：保持第一阶段速度（STAGE2_TICKS tick）
        // 阶段3：线性从第一阶段到第二阶段速度（RAMP_TICKS_2 tick）
        // 阶段4：保持第二阶段速度
        double targetY;
        if (newDir == 1)
        {
            // 向下
            if (helpfulTweake$climbTicks < RAMP_TICKS)
            {
                targetY = DOWN_SPEED_STAGE1 * helpfulTweake$climbTicks / RAMP_TICKS;
            }
            else if (helpfulTweake$climbTicks < RAMP_TICKS + STAGE2_TICKS)
            {
                targetY = DOWN_SPEED_STAGE1;
            }
            else if (helpfulTweake$climbTicks < RAMP_TICKS + STAGE2_TICKS + RAMP_TICKS_2)
            {
                int t = helpfulTweake$climbTicks - RAMP_TICKS - STAGE2_TICKS;
                targetY = DOWN_SPEED_STAGE1 + (DOWN_SPEED_STAGE2 - DOWN_SPEED_STAGE1) * t / RAMP_TICKS_2;
            }
            else
            {
                targetY = DOWN_SPEED_STAGE2;
            }
        }
        else
        {
            // 向上
            if (helpfulTweake$climbTicks < RAMP_TICKS)
            {
                targetY = UP_SPEED_STAGE1 * helpfulTweake$climbTicks / RAMP_TICKS;
            }
            else if (helpfulTweake$climbTicks < RAMP_TICKS + STAGE2_TICKS)
            {
                targetY = UP_SPEED_STAGE1;
            }
            else if (helpfulTweake$climbTicks < RAMP_TICKS + STAGE2_TICKS + RAMP_TICKS_2)
            {
                int t = helpfulTweake$climbTicks - RAMP_TICKS - STAGE2_TICKS;
                targetY = UP_SPEED_STAGE1 + (UP_SPEED_STAGE2 - UP_SPEED_STAGE1) * t / RAMP_TICKS_2;
            }
            else
            {
                targetY = UP_SPEED_STAGE2;
            }
        }

        // 覆盖返回的 Vec3 的 y 分量，绕过原版 Math.max(y, -0.15) 钳制
        Vec3 original = cir.getReturnValue();
        cir.setReturnValue(new Vec3(original.x, targetY, original.z));
        helpfulTweake$climbTicks++;
    }
}
