package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tanjikun.helpful.tweake.client.util.ShieldRenderContext;

/**
 * 捕获当前渲染实体的 UUID，供第三人称盾牌状态查询使用。
 *
 * 注入 LivingEntityRenderer.extractRenderState (method_62355) 的 HEAD，
 * 将当前实体的 UUID 存入 ThreadLocal。
 *
 * method_62355 签名：(LivingEntity, LivingEntityRenderState, float) → void
 *
 * 注意：依赖 extractRenderState 和 submit 在同一线程连续调用（对同一实体）。
 * 如果渲染管线改为先 extract 所有实体再 submit 所有实体，此 ThreadLocal 不可靠。
 *
 * remap=false: method 用 intermediary 名（项目约定）
 *
 * 灵感来自：https://modrinth.com/mod/shield-statuses
 */
@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer
{
    @Inject(method = "method_62355", at = @At("HEAD"), remap = false)
    private void helpfulTweake$captureEntityUuid(LivingEntity entity, LivingEntityRenderState state,
                                                  float partialTick, CallbackInfo ci)
    {
        ShieldRenderContext.setCurrentEntityUuid(entity.getUUID());
    }
}
