package tanjikun.helpful.tweake.client.util;

import net.minecraft.client.gui.GuiGraphics;

/**
 * AdvancementWidget 的 duck-typing 接口。
 * 由 MixinAdvancementWidgetDrawHover 实现，供 Tab/Screen 的 Mixin 跨类调用
 * （Mixin 类运行时不存在，不能直接强转到 Mixin 类，必须走接口）。
 *
 * 必须放在 mixin 包之外：mixin 包（helpful-tweake.client.mixins.json 的 package）
 * 中的非 Mixin 类被直接引用会抛 IllegalClassLoadError（Mixin 框架禁止），
 * 曾导致 AdvancementTab 构造即崩、进度页只剩"这里好像什么都没有……"。
 */
public interface IAdvancementDetailRenderer
{
    /** 在屏幕坐标系绘制详情面板（windowX/windowY 为进度窗口原点，用于抵消当前 translate） */
    void helpfulTweake$renderDetails(GuiGraphics gui, int windowX, int windowY);

    /** 是否为累加型进度（每个 requirement 组恰好 1 个 criterion 且总数 >= 2） */
    boolean helpfulTweake$isAccumulative();
}
