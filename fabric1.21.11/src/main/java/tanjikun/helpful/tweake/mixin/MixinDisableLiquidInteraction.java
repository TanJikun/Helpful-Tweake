package tanjikun.helpful.tweake.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.ServerFlagHolder;

/**
 * 禁用水与岩浆互动：开启后水与岩浆接触不再生成黑曜石、圆石或石头（玄武岩同理）。
 *
 * 原理：LiquidBlock.shouldSpreadLiquid（method_10316）在岩浆检测到相邻水/蓝冰时，
 * 调用 Level.setBlockAndUpdate（method_8501）把岩浆自身替换为黑曜石/圆石/玄武岩，再返回 false。
 * 在 setBlockAndUpdate 调用前取消并返回 false：跳过固体方块生成，岩浆保留且不流动，
 * 水的正常流动不受影响（水非 LAVA，原方法直接返回 true）。
 *
 * ponytail: 只覆盖 LiquidBlock 侧互动；若水流到岩浆位置时 FlowingFluid 有独立互动逻辑，
 * 需追加拦截。已知 ceiling：水可能仍替换岩浆，届时扩展。
 *
 * 注意：此 Mixin 在 common 源码集中，服务端必须安装本模组才能生效。
 * 配置状态通过 ServerFlagHolder 从客户端同步（单机模式下客户端=服务端）。
 *
 * ponytail: Mixin 注解字符串使用 intermediary + remap = false（项目无 refmap，详见 project_memory）
 */
@Mixin(LiquidBlock.class)
public class MixinDisableLiquidInteraction
{
    @Inject(
            method = "method_10316",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/class_1937;method_8501(Lnet/minecraft/class_2338;Lnet/minecraft/class_2680;)Z"),
            cancellable = true,
            remap = false
    )
    private void helpfulTweake$cancelSolidFormation(
            Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir)
    {
        if (ServerFlagHolder.disableLiquidInteraction)
        {
            cir.setReturnValue(false);
        }
    }
}
