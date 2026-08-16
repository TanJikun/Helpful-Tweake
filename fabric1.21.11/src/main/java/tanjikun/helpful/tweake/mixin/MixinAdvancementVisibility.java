package tanjikun.helpful.tweake.mixin;

import it.unimi.dsi.fastutil.Stack;

import net.minecraft.server.advancements.AdvancementVisibilityEvaluator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tanjikun.helpful.tweake.ServerFlagHolder;

/**
 * 未完成进度显示 — 服务端可见性注入。
 *
 * 根因（反编译 PlayerAdvancements.flushDirty / method_12876 验证）：
 *   服务端用 AdvancementVisibilityEvaluator 评估每个进度的可见性，只把 visible
 *   集合中的进度同步给客户端。隐藏进度（display.isHidden() && !isDone）被评估为
 *   不可见，不会加入 visible 集合，客户端进度树根本不包含它们。
 *
 * evaluateVisibility（method_48030）递归逻辑：
 *   1. rule = evaluateVisibilityRule(advancement, isDone)
 *   2. visible = isDone
 *   3. 递归子进度，visible |= 子结果
 *   4. if !visible: visible = evaluateVisiblityForUnfinishedNode(stack)
 *   5. output.accept(advancement, visible)  // 决定是否加入 visible 集合
 *
 * evaluateVisiblityForUnfinishedNode（method_48033）检查栈中最近 2 层是否有 SHOW：
 *   有 SHOW → true（可见）；有 HIDE → false；否则 → false。
 *   注入返回 true 使所有未完成进度都被视为可见，服务端会同步它们给客户端。
 *
 * 配合客户端 MixinShowAllAdvancements（注入 DisplayInfo.isHidden 返回 false）使
 *   渲染不跳过。VisibilityRule 是 private 内部类无法直接引用，故选此注入点
 *   （参数为 Stack，返回 boolean，无需引用 VisibilityRule）。
 *
 * ponytail: Mixin 方法名用 intermediary + remap = false（项目无 refmap，详见 project_memory）
 */
@Mixin(AdvancementVisibilityEvaluator.class)
public class MixinAdvancementVisibility
{
    @Inject(method = "method_48033", at = @At("HEAD"), cancellable = true, remap = false)
    private static void helpfulTweake$showUnfinished(Stack<?> stack, CallbackInfoReturnable<Boolean> cir)
    {
        if (ServerFlagHolder.showUncompletedAdvancements)
        {
            cir.setReturnValue(true);
        }
    }
}
