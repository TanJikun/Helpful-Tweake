package tanjikun.helpful.tweake.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.PlayerAdvancements;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tanjikun.helpful.tweake.ServerFlagHolder;

/**
 * 未完成进度显示 — 触发全量重新评估。
 *
 * 根因（反编译 PlayerAdvancements.flushDirty / method_12876 验证）：
 *   flushDirty 每tick调用，但只在 isFirstPacket=true 或 rootsToUpdate 非空或
 *   progressChanged 非空时才执行可见性评估和同步。玩家加入世界时 isFirstPacket=true
 *   全量评估一次，之后设为 false。切换功能后 rootsToUpdate 为空，flushDirty 直接返回，
 *   不会重新评估隐藏进度的可见性，导致只有之前触发过 rootsToUpdate 的 root 被重新评估。
 *
 * 修复：在功能从 false→true 时设置 isFirstPacket=true，让下次 flushDirty 全量评估。
 *   flushDirty 结束时会自动设回 false，只触发一次。
 *
 * ponytail: @Shadow 用 aliases 指定 intermediary 字段名（项目无 refmap，详见 project_memory）
 */
@Mixin(PlayerAdvancements.class)
public class MixinPlayerAdvancements
{
    @Shadow(aliases = {"field_13396"}, remap = false)
    private boolean isFirstPacket;

    @Unique
    private boolean helpfulTweake$lastShow = false;

    @Inject(method = "method_12876", at = @At("HEAD"), remap = false)
    private void helpfulTweake$triggerResync(ServerPlayer player, boolean force, CallbackInfo ci)
    {
        boolean current = ServerFlagHolder.showUncompletedAdvancements;
        if (current && !helpfulTweake$lastShow)
        {
            isFirstPacket = true;
        }
        helpfulTweake$lastShow = current;
    }
}
