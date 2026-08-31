package tanjikun.helpful.tweake.client.gui;

/**
 * 管理配置界面中可展开功能项的展开/折叠状态。
 * 状态为会话级（不持久化），GUI 关闭后重置。
 */
public class ExpandState
{
    public static boolean betterAutoJumpExpanded = false;
    public static boolean betterBoatExpanded = false;
    public static boolean betterDurabilityExpanded = false;
    public static boolean betterHarvestExpanded = false;
    public static boolean armorHudExpanded = false;
    public static boolean controlledCrawlExpanded = false;
    public static boolean visualExperienceExpanded = false;
    public static boolean worldSwallowMaintenanceExpanded = false;
    public static boolean renderCopyExpanded = false;
    public static boolean clearKelpExpanded = false;
    public static boolean entityRenderOptimizationExpanded = false;
    public static boolean skipDistantEntitiesExpanded = false;
    public static boolean betterAdvancementsExpanded = false;
    public static boolean betterHopperMinecartExpanded = false;
    public static boolean suckRangeDisplayExpanded = false;
    public static boolean hopperLockedDisplayExpanded = false;
    public static boolean hopperContainerHighlightExpanded = false;
    public static boolean zoomExpanded = false;
    private static Runnable refreshCallback;

    public static void setRefreshCallback(Runnable callback)
    {
        refreshCallback = callback;
    }

    public static void toggleBetterAutoJump()
    {
        betterAutoJumpExpanded = !betterAutoJumpExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleBetterBoat()
    {
        betterBoatExpanded = !betterBoatExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleBetterDurability()
    {
        betterDurabilityExpanded = !betterDurabilityExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleBetterHarvest()
    {
        betterHarvestExpanded = !betterHarvestExpanded;
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

    public static void toggleControlledCrawl()
    {
        controlledCrawlExpanded = !controlledCrawlExpanded;
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

    public static void toggleBetterAdvancements()
    {
        betterAdvancementsExpanded = !betterAdvancementsExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleBetterHopperMinecart()
    {
        betterHopperMinecartExpanded = !betterHopperMinecartExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleSuckRangeDisplay()
    {
        suckRangeDisplayExpanded = !suckRangeDisplayExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleHopperLockedDisplay()
    {
        hopperLockedDisplayExpanded = !hopperLockedDisplayExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleHopperContainerHighlight()
    {
        hopperContainerHighlightExpanded = !hopperContainerHighlightExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }

    public static void toggleZoom()
    {
        zoomExpanded = !zoomExpanded;
        if (refreshCallback != null)
        {
            refreshCallback.run();
        }
    }
}
