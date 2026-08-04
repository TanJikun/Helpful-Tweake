package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tanjikun.helpful.tweake.client.util.ShieldStateTracker;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 监听客户端接收的声音事件，推测其他玩家的盾牌冷却状态。
 *
 * 监听 SHIELD_BREAK 声音（盾牌被斧头击中禁用时播放）：
 * - handleSoundEntityEvent (method_11125)：实体型声音，直接获取实体 ID
 * - handleSoundEvent (method_11146)：位置型声音，通过坐标找最近玩家
 *
 * 注意：服务端不向其他玩家同步盾牌冷却，只能通过声音事件间接推断。
 * 客户端本地维护 5 秒（100 tick）冷却计时器。
 *
 * remap=false: method 用 intermediary 名（项目约定）
 *
 * 灵感来自：https://modrinth.com/mod/shield-statuses
 */
@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener
{
    // 盾牌被斧头击中禁用时的声音 ID
    private static final Identifier SHIELD_BREAK_ID =
            SoundEvents.SHIELD_BREAK.value().location();

    /**
     * 实体型声音：直接通过实体 ID 定位玩家。
     */
    @Inject(method = "method_11125", at = @At("HEAD"), remap = false)
    private void helpfulTweake$onShieldBreakEntitySound(ClientboundSoundEntityPacket packet,
                                                         CallbackInfo ci)
    {
        if (!Configs.Tools.SHIELD_STATUS.getBooleanValue()) return;
        if (!isShieldBreakSound(packet.getSound())) return;

        ShieldStateTracker.markShieldDisabledByEntityId(packet.getId());
    }

    /**
     * 位置型声音：通过坐标找最近玩家（半径 2.0 内）。
     */
    @Inject(method = "method_11146", at = @At("HEAD"), remap = false)
    private void helpfulTweake$onShieldBreakPositionSound(ClientboundSoundPacket packet,
                                                           CallbackInfo ci)
    {
        if (!Configs.Tools.SHIELD_STATUS.getBooleanValue()) return;
        if (!isShieldBreakSound(packet.getSound())) return;

        ShieldStateTracker.markShieldDisabledByPosition(
                packet.getX(), packet.getY(), packet.getZ());
    }

    private static boolean isShieldBreakSound(Holder<SoundEvent> soundHolder)
    {
        if (soundHolder == null) return false;
        SoundEvent sound = soundHolder.value();
        return sound != null && SHIELD_BREAK_ID.equals(sound.location());
    }
}
