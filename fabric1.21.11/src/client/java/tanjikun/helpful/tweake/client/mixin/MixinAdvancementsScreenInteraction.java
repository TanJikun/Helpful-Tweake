package tanjikun.helpful.tweake.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.input.MouseButtonEvent;

import tanjikun.helpful.tweake.client.util.AdvancementDetailState;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 进度详细信息显示 — 交互处理。
 *
 * - mouseClicked（method_25402，新签名 MouseButtonEvent）：点击累加型进度时锁定
 *   详情面板（进度页面内无需悬停保持显示；悬停其他累加进度时临时显示悬停的）。
 *   悬停目标由 MixinAdvancementTabDetails 每帧写入共享状态，此处直接读取。
 * - mouseScrolled（method_25401，旧签名 DDDDZ）：鼠标在面板列表区域内时滚动列表
 *   （标题/简介固定不动），消费事件避免影响页面滚动。
 * - removed（method_25432）：退出进度页面时清空锁定与滚动状态。
 *
 * ponytail: @Inject method 用 intermediary + remap=false（项目无 refmap）；
 *   MouseButtonEvent.x()/y() 为普通方法调用，Loom remapJar 自动重映射
 */
@Mixin(AdvancementsScreen.class)
public class MixinAdvancementsScreenInteraction
{
    @Inject(method = "method_25402", at = @At("HEAD"), remap = false)
    private void helpfulTweake$onClick(MouseButtonEvent event, boolean doubleClick,
                                        CallbackInfoReturnable<Boolean> cir)
    {
        if (!Configs.Tools.BETTER_ADVANCEMENTS.getBooleanValue()
                || !Configs.Tools.SHOW_ADVANCEMENT_DETAILS.getBooleanValue())
        {
            return;
        }
        // 左键点击进度：累加进度锁定列表；点击其他进度取消锁定（列表跟随切换）
        if (event.button() == 0 && AdvancementDetailState.hoverId != null)
        {
            AdvancementDetailState.lockedId = AdvancementDetailState.hoverAccumulative
                    ? AdvancementDetailState.hoverId
                    : null;
        }
    }

    @Inject(method = "method_25401", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$onScroll(double mouseX, double mouseY,
                                         double scrollX, double scrollY,
                                         CallbackInfoReturnable<Boolean> cir)
    {
        if (!Configs.Tools.BETTER_ADVANCEMENTS.getBooleanValue()
                || !Configs.Tools.SHOW_ADVANCEMENT_DETAILS.getBooleanValue())
        {
            return;
        }
        if (AdvancementDetailState.maxScroll <= 0 || AdvancementDetailState.listH <= 0)
        {
            return;
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;
        if (mx >= AdvancementDetailState.listX
                && mx <= AdvancementDetailState.listX + AdvancementDetailState.listW
                && my >= AdvancementDetailState.listY
                && my <= AdvancementDetailState.listY + AdvancementDetailState.listH)
        {
            // 滚轮向上（scrollY>0）看上方内容（offset 减小），与原版列表滚动方向一致
            int delta = scrollY > 0 ? -12 : 12;
            AdvancementDetailState.scrollOffset = Math.max(0, Math.min(
                    AdvancementDetailState.maxScroll,
                    AdvancementDetailState.scrollOffset + delta));
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "method_25432", at = @At("HEAD"), remap = false)
    private void helpfulTweake$onRemoved(CallbackInfo ci)
    {
        AdvancementDetailState.reset();
    }
}
