package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 当"更好的自动跳跃"开启时，强制让原版 auto-jump 检查返回"已禁用"，
 * 避免原版 auto-jump 触发真实跳跃（会重置水平速度）与本功能冲突。
 *
 * 冲突说明（规则6）：原版 autoJumpEnabled 状态被强制视为 false，
 * 但 options.autoJump 配置项本身不变，关闭本功能后原版行为恢复。
 *
 * 目标方法 isAutoJumpEnabled() 位于 LocalPlayer（非 Minecraft 类），
 * 运行时实体为本地玩家本身，无需额外身份判断。
 */
@Mixin(LocalPlayer.class)
public class MixinMinecraft
{
    // method_3149 = LocalPlayer.isAutoJumpEnabled，运行时原版 auto-jump 逻辑通过此方法判断是否启用
    // remap = false: Loom 1.17 + officialMojangMappings 不生成 refmap，直接用 intermediary 名
    @Inject(method = "method_3149", remap = false,
            at = @At("RETURN"), cancellable = true)
    private void helpfulTweake$disableVanillaAutoJump(CallbackInfoReturnable<Boolean> cir)
    {
        if (Configs.Tools.BETTER_AUTO_JUMP.getBooleanValue())
        {
            cir.setReturnValue(false);
        }
    }
}
