package tanjikun.helpful.tweake.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;

import tanjikun.helpful.tweake.client.util.AdvancementScreenSize;

/**
 * 更大的进度界面 — AdvancementsScreen 侧尺寸替换。
 *
 * 原版窗口 252×140 为编译期内联立即数，出现在：
 *   - render（method_25394，声明于 Renderable/class_4068，本类覆盖）：
 *     窗口居中计算 (width-252)/2、(height-140)/2
 *   - mouseClicked（method_25402，声明于 Element/class_364，本类覆盖）：
 *     窗口原点传给进度节点命中检测
 *   - renderInside（method_2337）：内容区 234×113 黑底、空状态文本居中
 *     （117 = 234/2，56 = 113/2，两个居中立即数需同步映射为半宽/半高）
 *
 * 背景绘制（renderWindow/method_2334）：原版为整图 blit 252×140（贴图中间
 * 内容区透明，透出 renderInside 先画的进度树）。大窗口下该 blit 只有 252×140，
 * 其不透明边框会残留在放大后的内容区内部，故将 blit 宽高常量改为 0使其不渲染，
 * 并在原 blit 位置（锚点 Map.size()，JDK 方法无重映射风险）自绘边框替代：
 * 只画四周（顶 18px 含标题栏、左右/底各 9px），中间内容区空出透出进度树。
 * 渲染顺序：renderInside(进度树) → renderWindow[blit(已清零) → 自绘边框 →
 * 标签按钮/标题(原版)] —— 任何不透明全窗口填充都会盖住进度树，禁止。
 *
 * ponytail: @ModifyConstant method 用 intermediary + remap=false
 * （项目无 refmap，详见 project_memory）
 */
@Mixin(AdvancementsScreen.class)
public class MixinAdvancementsScreenBigger
{
    // === render：窗口居中的宽高 ===
    @ModifyConstant(method = "method_25394", constant = @Constant(intValue = 252), remap = false)
    private int helpfulTweake$renderWidth(int original)
    {
        return AdvancementScreenSize.windowWidth();
    }

    @ModifyConstant(method = "method_25394", constant = @Constant(intValue = 140), remap = false)
    private int helpfulTweake$renderHeight(int original)
    {
        return AdvancementScreenSize.windowHeight();
    }

    // === mouseClicked：命中检测的窗口原点 ===
    @ModifyConstant(method = "method_25402", constant = @Constant(intValue = 252), remap = false)
    private int helpfulTweake$clickWidth(int original)
    {
        return AdvancementScreenSize.windowWidth();
    }

    @ModifyConstant(method = "method_25402", constant = @Constant(intValue = 140), remap = false)
    private int helpfulTweake$clickHeight(int original)
    {
        return AdvancementScreenSize.windowHeight();
    }

    // === renderInside：内容区尺寸与空状态文本居中 ===
    @ModifyConstant(method = "method_2337", constant = @Constant(intValue = 234), remap = false)
    private int helpfulTweake$insideWidth(int original)
    {
        return AdvancementScreenSize.contentWidth();
    }

    @ModifyConstant(method = "method_2337", constant = @Constant(intValue = 113), remap = false)
    private int helpfulTweake$insideHeight(int original)
    {
        return AdvancementScreenSize.contentHeight();
    }

    /** 117 = 原版内容宽的一半（空状态文本水平居中） */
    @ModifyConstant(method = "method_2337", constant = @Constant(intValue = 117), remap = false)
    private int helpfulTweake$insideHalfWidth(int original)
    {
        return AdvancementScreenSize.contentWidth() / 2;
    }

    /** 56 = 原版内容高的一半（空状态文本垂直居中） */
    @ModifyConstant(method = "method_2337", constant = @Constant(intValue = 56), remap = false)
    private int helpfulTweake$insideHalfHeight(int original)
    {
        return AdvancementScreenSize.contentHeight() / 2;
    }

    // === renderWindow：原版整图 blit 清零（252/140 各仅此一处），改由自绘边框替代 ===
    @ModifyConstant(method = "method_2334", constant = @Constant(intValue = 252), remap = false)
    private int helpfulTweake$blitWidth(int original)
    {
        return AdvancementScreenSize.isEnabled() ? 0 : original;
    }

    @ModifyConstant(method = "method_2334", constant = @Constant(intValue = 140), remap = false)
    private int helpfulTweake$blitHeight(int original)
    {
        return AdvancementScreenSize.isEnabled() ? 0 : original;
    }

    // === renderWindow：自绘边框（只画四周，中间内容区空出透出进度树） ===
    @Inject(method = "method_2334",
            at = @At(value = "INVOKE",
                     target = "Ljava/util/Map;size()I",
                     shift = At.Shift.AFTER),
            remap = false)
    private void helpfulTweake$drawCustomBackground(GuiGraphics gui, int x, int y,
                                                    int mouseX, int mouseY, CallbackInfo ci)
    {
        if (!AdvancementScreenSize.isEnabled())
        {
            return;
        }
        int w = AdvancementScreenSize.windowWidth();
        int h = AdvancementScreenSize.windowHeight();
        // 原版 GUI 浅灰（标题深灰字 0xFF404040 在其上可读，与标签按钮风格统一）
        final int FRAME = 0xFFC6C6C6;
        final int EDGE = 0xFF000000;
        // 顶条（含标题栏，高 18）+ 左右条（宽 9）+ 底条（高 9）
        gui.fill(x, y, x + w, y + 18, FRAME);
        gui.fill(x, y + 18, x + 9, y + h - 9, FRAME);
        gui.fill(x + w - 9, y + 18, x + w, y + h - 9, FRAME);
        gui.fill(x, y + h - 9, x + w, y + h, FRAME);
        // 外圈 1px 黑色描边勾出窗口轮廓
        gui.fill(x, y, x + w, y + 1, EDGE);
        gui.fill(x, y + h - 1, x + w, y + h, EDGE);
        gui.fill(x, y, x + 1, y + h, EDGE);
        gui.fill(x + w - 1, y, x + w, y + h, EDGE);
    }
}
