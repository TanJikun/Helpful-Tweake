package tanjikun.helpful.tweake.client.gui;

/**
 * 管理配置界面中可展开功能项的展开/折叠状态。
 * 状态为会话级（不持久化），GUI 关闭后重置。
 */
public class ExpandState
{
    public static boolean betterBoatExpanded = false;
    private static Runnable refreshCallback;

    public static void setRefreshCallback(Runnable callback)
    {
        refreshCallback = callback;
    }

    public static void toggleBetterBoat()
    {
        betterBoatExpanded = !betterBoatExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }
}
