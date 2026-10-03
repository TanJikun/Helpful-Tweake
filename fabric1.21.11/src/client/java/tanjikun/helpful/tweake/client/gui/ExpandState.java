package tanjikun.helpful.tweake.client.gui;

/**
 * 管理配置界面中可展开功能项的展开/折叠状态。
 * 状态为会话级（不持久化），GUI 关闭后重置。
 */
public class ExpandState
{
    public static boolean betterDurabilityExpanded = false;
    public static boolean armorHudExpanded = false;
    public static boolean visualExperienceExpanded = false;
    public static boolean worldSwallowMaintenanceExpanded = false;
    public static boolean renderCopyExpanded = false;
    public static boolean clearKelpExpanded = false;
    public static boolean entityRenderOptimizationExpanded = false;
    public static boolean skipDistantEntitiesExpanded = false;
    public static boolean fakePeacefulExpanded = false;
    public static boolean betterAdvancementsExpanded = false;
    public static boolean guiTransparencyExpanded = false;
    private static Runnable refreshCallback;

    public static void setRefreshCallback(Runnable callback)
    {
        refreshCallback = callback;
    }

    public static void toggleBetterDurability()
    {
        betterDurabilityExpanded = !betterDurabilityExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleArmorHud()
    {
        armorHudExpanded = !armorHudExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleVisualExperience()
    {
        visualExperienceExpanded = !visualExperienceExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleWorldSwallowMaintenance()
    {
        worldSwallowMaintenanceExpanded = !worldSwallowMaintenanceExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleRenderCopy()
    {
        renderCopyExpanded = !renderCopyExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleClearKelp()
    {
        clearKelpExpanded = !clearKelpExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleEntityRenderOptimization()
    {
        entityRenderOptimizationExpanded = !entityRenderOptimizationExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleSkipDistantEntities()
    {
        skipDistantEntitiesExpanded = !skipDistantEntitiesExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleFakePeaceful()
    {
        fakePeacefulExpanded = !fakePeacefulExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleBetterAdvancements()
    {
        betterAdvancementsExpanded = !betterAdvancementsExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleGuiTransparency()
    {
        guiTransparencyExpanded = !guiTransparencyExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }
}
