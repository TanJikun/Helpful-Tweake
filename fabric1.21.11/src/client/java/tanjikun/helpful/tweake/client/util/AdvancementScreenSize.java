package tanjikun.helpful.tweake.client.util;

import net.minecraft.client.Minecraft;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 更大的进度界面 — 窗口尺寸计算（供各 Mixin 统一取值）。
 *
 * 原版窗口 252×140 为编译期内联立即数（源码中的具名常量 WINDOW_WIDTH 等在
 * 字节码里不保留字段读取），故通过 @ModifyConstant 拦截后统一改走本类。
 *
 * 所有方法在功能关闭时返回原版值，因此调用方无需自行判断开关状态。
 * 每次调用实时计算，配置切换后下一帧即生效。
 *
 * ponytail: 窗口取"屏幕尺寸 - 固定边距"（上/下 36px 给标签页和完成按钮，
 * 左右 16px），不设上限比例；若未来需要按比例缩放，在此处集中改即可。
 */
public class AdvancementScreenSize
{
    /** 原版窗口尺寸（AdvancementsScreen.WINDOW_WIDTH / WINDOW_HEIGHT） */
    public static final int VANILLA_WIDTH = 252;
    public static final int VANILLA_HEIGHT = 140;

    /** 边距：顶部需容纳 ABOVE 型标签页（自窗口顶边向上延伸 28px），底部需容纳
     * 原版"完成"按钮（LinearLayout 布局在屏幕底部，高约 20px + 间距）。
     * 窗口经 (height - h) / 2 居中后：顶边 y = TOP_MARGIN，标签页顶 y = 36-28 = 8；
     * 底边 y = height - BOTTOM_MARGIN，在按钮（约 height-27）之上。左右仅留装饰边距。 */
    private static final int TOP_MARGIN = 36;
    private static final int BOTTOM_MARGIN = 36;
    private static final int SIDE_MARGIN = 16;

    /** 内容区内边距：左 9 / 上 18 / 右 9 / 下 9（原版 WINDOW_INSIDE_* 常量） */
    private static final int INSIDE_X = 9;
    private static final int INSIDE_Y = 18;
    private static final int INSIDE_BOTTOM = 9;

    private AdvancementScreenSize() {}

    /** 父配置（更好的进度）与子配置（更大的进度界面）同时开启才生效 */
    public static boolean isEnabled()
    {
        return Configs.Tools.BETTER_ADVANCEMENTS.getBooleanValue()
                && Configs.Tools.BIGGER_ADVANCEMENTS_SCREEN.getBooleanValue();
    }

    /** 窗口宽度：开启时为屏幕宽 - 左右边距（下限原版 252），关闭时原版值 */
    public static int windowWidth()
    {
        if (!isEnabled())
        {
            return VANILLA_WIDTH;
        }
        int w = Minecraft.getInstance().getWindow().getGuiScaledWidth() - SIDE_MARGIN * 2;
        return Math.max(VANILLA_WIDTH, w);
    }

    /** 窗口高度：开启时为屏幕高 - 上下边距（下限原版 140），关闭时原版值 */
    public static int windowHeight()
    {
        if (!isEnabled())
        {
            return VANILLA_HEIGHT;
        }
        int h = Minecraft.getInstance().getWindow().getGuiScaledHeight() - TOP_MARGIN - BOTTOM_MARGIN;
        return Math.max(VANILLA_HEIGHT, h);
    }

    /** 内容区宽度 = 窗口宽 - 左右边距（原版 234） */
    public static int contentWidth()
    {
        return windowWidth() - INSIDE_X - INSIDE_X;
    }

    /** 内容区高度 = 窗口高 - 上 18 - 下 9（原版 113） */
    public static int contentHeight()
    {
        return windowHeight() - INSIDE_Y - INSIDE_BOTTOM;
    }
}
