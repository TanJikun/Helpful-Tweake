package tanjikun.helpful.tweake.client.util;

/**
 * 进度详细信息面板的共享状态（跨 Mixin 传递）。
 *
 * - lockedId：点击锁定的进度 ID（进度页面内保持显示，退出页面清空）
 * - hoverId / hoverAccumulative：本帧悬停的进度（由 Tab 渲染 Mixin 每帧更新，
 *   供 Screen 点击 Mixin 读取，避免跨类访问 widgets 私有字段）
 * - scrollOffset / maxScroll：列表滚动
 * - list* ：上一帧列表区域（屏幕坐标），供滚轮命中检测
 * - lastTargetId：目标切换时重置滚动
 */
public class AdvancementDetailState
{
    public static String lockedId = null;
    public static boolean hoverAccumulative = false;
    public static String hoverId = null;

    public static int scrollOffset = 0;
    public static int maxScroll = 0;
    public static String lastTargetId = null;

    public static int listX = 0;
    public static int listY = 0;
    public static int listW = 0;
    public static int listH = 0;

    public static void reset()
    {
        lockedId = null;
        hoverId = null;
        hoverAccumulative = false;
        scrollOffset = 0;
        maxScroll = 0;
        lastTargetId = null;
        listH = 0;
    }
}
