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
 * 禁用水与岩浆互动：开启后水与岩浆接触不再生成黑曜石、圆石或石头。
 *
 * 原理：LiquidBlock.shouldSpreadLiquid（method_10316）是液体流动时检查与异种液体互动的核心方法。
 * 该方法在检测到水-岩浆接触时，将方块替换为黑曜石/圆石/石头并返回 false（阻止流动）。
 * 注入 HEAD 返回 true（允许流动，跳过互动），液体将正常流动而不生成任何副产物。
 *
 * 注意：此 Mixin 在 common 源码集中，服务端必须安装本模组才能生效。
 * 配置状态通过 ServerFlagHolder 从客户端同步（单机模式下客户端=服务端）。
 *
 * ponytail: Mixin 方法名使用 intermediary + remap = false（项目无 refmap，详见 project_memory）
 */
@Mixin(LiquidBlock.class)
public class MixinDisableLiquidInteraction
{
    @Inject(method = "method_10316", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$disableLiquidInteraction$shouldSpreadLiquid(
            Level level, BlockPos pos, BlockState state, CallbackInfoReturnable<Boolean> cir)
    {
        if (ServerFlagHolder.disableLiquidInteraction)
        {
            cir.setReturnValue(true);
        }
    }
}
