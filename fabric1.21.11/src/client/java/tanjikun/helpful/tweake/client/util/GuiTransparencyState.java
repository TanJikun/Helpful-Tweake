package tanjikun.helpful.tweake.client.util;

/**
 * GUI 界面透明度修改的运行时状态与工具方法。
 *
 * Mixin 类之间禁止直接互相引用（会触发 IllegalClassLoadError），
 * 因此 Screen 侧设置的渲染作用域标志与颜色 alpha 缩放逻辑集中在此普通工具类。
 *
 * isGuiOpacityActive() 仅在屏幕渲染期间为真（由 MixinScreenGuiTransparency
 * 在 Screen.renderWithTooltipAndSubtitles 的 HEAD/RETURN 间设置），
 * 即只有游戏内 GUI 界面的绘制内容会被缩放 alpha。
 */
public final class GuiTransparencyState
{
    /** 当前正处于屏幕渲染期间（GUI 不透明度生效窗口） */
    private static boolean inScreenRender;
    /** 正在重放被 cancel 的 submit 调用（或正在绘制背景），暂停拦截防止递归/双重缩放 */
    private static boolean suspended;

    private GuiTransparencyState()
    {
    }

    public static boolean isGuiOpacityActive()
    {
        return inScreenRender && !suspended;
    }

    public static void setInScreenRender(boolean active)
    {
        inScreenRender = active;
    }

    /**
     * 暂停/恢复拦截（cancel 后重放 submit 调用、绘制背景时使用）。
     * 返回之前的暂停状态，恢复时原样传回。
     */
    public static boolean setSuspended(boolean value)
    {
        boolean old = suspended;
        suspended = value;
        return old;
    }

    /**
     * 将 ARGB 颜色的 alpha 通道按百分比缩放（0-100）。
     * 100 返回原值，0 返回完全透明。
     */
    public static int scaleAlpha(int argb, int percent)
    {
        if (percent >= 100)
        {
            return argb;
        }
        int alpha = (argb >>> 24) * percent / 100;
        return (alpha << 24) | (argb & 0x00FFFFFF);
    }
}
