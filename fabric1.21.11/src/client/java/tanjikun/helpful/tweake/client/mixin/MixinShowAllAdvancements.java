package tanjikun.helpful.tweake.client.mixin;

import net.minecraft.advancements.DisplayInfo;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 未完成进度显示 — 客户端渲染注入。
 *
 * 机制（反编译 AdvancementWidget.a(gir,II) / method_2330 验证）：
 *   if (display.isHidden() && (progress == null || !progress.isDone())) return;  // 跳过渲染
 *   隐藏进度在未完成时被跳过不渲染。强制 isHidden() 返回 false 后走正常渲染分支：
 *   progress 为 null 时按 0% 处理（白底 / AdvancementWidgetType.UNOBTAINED），
 *   已完成时黄底 / OBTAINED，沿用原版上色逻辑。
 *
 * 注意：仅注入此处不够。服务端用 AdvancementVisibilityEvaluator 评估可见性，只把
 *   visible 集合中的进度同步给客户端（反编译 PlayerAdvancements.flushDirty 验证）。
 *   隐藏进度不在 visible 集合，客户端进度树根本不含它们。需配合 common 侧
 *   MixinAdvancementVisibility 注入 evaluateVisibilityRule 返回 SHOW，让服务端
 *   同步隐藏进度后，本注入才有效。
 *
 * ponytail: Mixin 方法名使用 intermediary + remap = false（项目无 refmap，详见 project_memory）
 */
@Mixin(DisplayInfo.class)
public class MixinShowAllAdvancements
{
    @Inject(method = "method_824", at = @At("HEAD"), cancellable = true, remap = false)
    private void helpfulTweake$showAllAdvancements(CallbackInfoReturnable<Boolean> cir)
    {
        if (Configs.Tools.BETTER_ADVANCEMENTS.getBooleanValue()
                && Configs.Tools.SHOW_UNCOMPLETED_ADVANCEMENTS.getBooleanValue())
        {
            cir.setReturnValue(false);
        }
    }
}
