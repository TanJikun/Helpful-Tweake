package tanjikun.helpful.tweake.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tanjikun.helpful.tweake.client.util.ShieldRenderContext;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 第一人称盾牌状态上下文设置。
 *
 * 注入 ItemInHandRenderer.renderArmWithItem (method_3228) 的 HEAD，
 * 检查手持物品是否盾牌，查询 LocalPlayer 的盾牌冷却状态，设置 shieldColor。
 *
 * method_3228 签名：(AbstractClientPlayer, float, float, InteractionHand, float, ItemStack, float, PoseStack, SubmitNodeCollector, int) → void
 *
 * remap=false: method 用 intermediary 名（项目约定）
 *
 * 灵感来自：https://modrinth.com/mod/shield-statuses
 */
@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer
{
    @Inject(method = "method_3228", at = @At("HEAD"), remap = false)
    private void helpfulTweake$setShieldColorFirstPerson(AbstractClientPlayer player,
                                                          float partialTick, float equipProgress,
                                                          InteractionHand hand, float swingProgress,
                                                          ItemStack stack, float handHeight,
                                                          PoseStack poseStack, SubmitNodeCollector collector,
                                                          int packedLight, CallbackInfo ci)
    {
        if (!Configs.Tools.SHIELD_STATUS.getBooleanValue()) return;
        if (!(stack.getItem() instanceof ShieldItem)) return;

        // LocalPlayer 的冷却状态由服务端同步，精确可信
        boolean onCooldown = player.getCooldowns().isOnCooldown(stack);
        ShieldRenderContext.setShieldColor(onCooldown
                ? ShieldRenderContext.COLOR_COOLDOWN
                : ShieldRenderContext.COLOR_AVAILABLE);
    }
}
