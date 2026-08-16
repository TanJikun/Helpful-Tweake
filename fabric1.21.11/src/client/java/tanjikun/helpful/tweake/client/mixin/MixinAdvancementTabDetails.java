package tanjikun.helpful.tweake.client.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;

import tanjikun.helpful.tweake.client.util.AdvancementDetailState;
import tanjikun.helpful.tweake.client.util.IAdvancementDetailRenderer;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 进度详细信息显示 — 锁定显示支持。
 *
 * 机制：注入 AdvancementTab.drawTooltips（method_2314）TAIL，每帧调用（无需悬停）。
 *   1. 遍历 widgets 检测悬停目标，把 hoverId/hoverAccumulative 写入共享状态
 *      （供 Screen 的点击 Mixin 读取，避免跨类访问私有 widgets 字段）。
 *   2. 若鼠标未悬停在累加型进度上但存在锁定目标（点击锁定），渲染锁定进度的面板。
 *      悬停在累加型进度上时跳过（drawHover TAIL 已渲染，避免双重绘制）。
 *
 * 坐标系：drawTooltips 在 translate(windowX+9, windowY+18) 变换下执行，
 *   参数4/5 即窗口原点（由 AdvancementsScreen.renderTooltips 传入）。
 *   参数2/3 是 tab 内坐标（原版限制 0~234 / 0~113 才画 tooltip，此处复刻该范围）。
 *
 * ponytail: @Shadow 用 aliases 指定 intermediary 字段名；@Inject method 用
 *   intermediary + remap=false（项目无 refmap，详见 project_memory）
 */
@Mixin(AdvancementTab.class)
public class MixinAdvancementTabDetails
{
    @Shadow(aliases = {"field_2685"}, remap = false)
    private Map<AdvancementHolder, AdvancementWidget> widgets;

    @Shadow(aliases = {"field_2690"}, remap = false)
    private double scrollX;

    @Shadow(aliases = {"field_2689"}, remap = false)
    private double scrollY;

    @Inject(method = "method_2314", at = @At("TAIL"), remap = false)
    private void helpfulTweake$renderLockedPanel(GuiGraphics gui, int mouseX, int mouseY,
                                                  int windowX, int windowY, CallbackInfo ci)
    {
        if (!Configs.Tools.BETTER_ADVANCEMENTS.getBooleanValue()
                || !Configs.Tools.SHOW_ADVANCEMENT_DETAILS.getBooleanValue())
        {
            AdvancementDetailState.hoverId = null;
            AdvancementDetailState.hoverAccumulative = false;
            return;
        }

        int sx = (int) this.scrollX;
        int sy = (int) this.scrollY;

        // 检测悬停目标（复刻原版 tooltip 显示范围：tab 内容区 0~234 / 0~113）
        AdvancementDetailState.hoverId = null;
        AdvancementDetailState.hoverAccumulative = false;
        if (mouseX > 0 && mouseX < 234 && mouseY > 0 && mouseY < 113)
        {
            for (Map.Entry<AdvancementHolder, AdvancementWidget> entry : widgets.entrySet())
            {
                AdvancementWidget widget = entry.getValue();
                if (widget.isMouseOver(sx, sy, mouseX, mouseY))
                {
                    String id = entry.getKey().id().toString();
                    boolean acc = ((IAdvancementDetailRenderer) widget).helpfulTweake$isAccumulative();
                    AdvancementDetailState.hoverId = id;
                    AdvancementDetailState.hoverAccumulative = acc;
                    // 悬停在任意进度上均不渲染锁定面板：
                    // 累加进度由 drawHover TAIL 渲染（列表切换为悬停目标）；
                    // 非累加进度无子项列表，仅显示原版 tooltip，锁定列表隐藏
                    return;
                }
            }
        }

        // 无悬停时渲染锁定目标
        if (AdvancementDetailState.lockedId == null)
        {
            return;
        }
        for (Map.Entry<AdvancementHolder, AdvancementWidget> entry : widgets.entrySet())
        {
            if (entry.getKey().id().toString().equals(AdvancementDetailState.lockedId))
            {
                ((IAdvancementDetailRenderer) entry.getValue())
                        .helpfulTweake$renderDetails(gui, windowX, windowY);
                return;
            }
        }
    }
}
