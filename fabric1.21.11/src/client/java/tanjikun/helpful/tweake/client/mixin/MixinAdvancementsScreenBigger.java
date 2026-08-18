package tanjikun.helpful.tweake.client.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

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
 * 改为九宫格自绘：把当前生效的 window.png（材质包覆盖自动生效，blit 走
 * ResourceManager 解析，与原版 GUI 贴图同一机制）切成四角 + 四边共 8 块，
 * 四角原样、上下边横拉、左右边竖拉，中间内容区空出透出进度树。
 * 渲染顺序：renderInside(进度树) → renderWindow[原版 blit(已清零) →
 * 九宫格边框 → 标签按钮/标题(原版)]。
 *
 * 九宫格 UV 布局（贴图 256×256 坐标系，窗口 252×140 位于 (0,0)，边框
 * 上 18/左右 9/下 9，材质包必须遵守原版 UV 布局才能兼容原版渲染，故可硬编码）：
 *   角: TL(0,0) TR(243,0) BL(0,131) BR(243,131)，各 9×18 / 9×9
 *   边: 上(9,0,234×18) 下(9,131,234×9) 左(0,18,9×113) 右(243,18,9×113)
 *
 * ponytail: @ModifyConstant method 用 intermediary + remap=false；
 * @Shadow 用 aliases 指定 intermediary 字段名（项目无 refmap，详见 project_memory）
 */
@Mixin(AdvancementsScreen.class)
public class MixinAdvancementsScreenBigger
{
    /** 原版窗口贴图 minecraft:textures/gui/advancements/window.png（field_2717） */
    @Shadow(aliases = {"field_2717"}, remap = false) @Final
    private static Identifier WINDOW_LOCATION;

    /** 九宫格边框厚度（与原版贴图布局一致） */
    private static final int FRAME_TOP = 18;
    private static final int FRAME_SIDE = 9;
    private static final int FRAME_BOTTOM = 9;
    /** 源贴图尺寸（blit 归一化基准；材质包物理分辨率可为其倍数，UV 按比例换算） */
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

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

    // === renderWindow：九宫格自绘边框（贴图取自当前生效材质包） ===
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
        int side = FRAME_SIDE;
        int top = FRAME_TOP;
        int bot = FRAME_BOTTOM;
        int midW = w - side * 2;      // 上下边拉伸后的绘制宽
        int midH = h - top - bot;     // 左右边拉伸后的绘制高
        // 贴图坐标系中的窗口边框 UV（252×140 窗口 @ (0,0)，256×256 贴图）
        int uvRight = 252 - side;     // 243
        int uvBottom = 140 - bot;     // 131
        int uvMidW = 252 - side * 2;  // 234
        int uvMidH = 140 - top - bot; // 113

        // 四角：原样绘制不缩放（保住圆角/装饰）
        gui.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION,
                 x, y, 0, 0, side, top, side, top, TEX_W, TEX_H);
        gui.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION,
                 x + w - side, y, uvRight, 0, side, top, side, top, TEX_W, TEX_H);
        gui.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION,
                 x, y + h - bot, 0, uvBottom, side, bot, side, bot, TEX_W, TEX_H);
        gui.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION,
                 x + w - side, y + h - bot, uvRight, uvBottom, side, bot, side, bot, TEX_W, TEX_H);
        // 四边：单向拉伸（上下横拉、左右竖拉）
        gui.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION,
                 x + side, y, side, 0, midW, top, uvMidW, top, TEX_W, TEX_H);
        gui.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION,
                 x + side, y + h - bot, side, uvBottom, midW, bot, uvMidW, bot, TEX_W, TEX_H);
        gui.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION,
                 x, y + top, 0, top, side, midH, side, uvMidH, TEX_W, TEX_H);
        gui.blit(RenderPipelines.GUI_TEXTURED, WINDOW_LOCATION,
                 x + w - side, y + top, uvRight, top, side, midH, side, uvMidH, TEX_W, TEX_H);
    }
}
