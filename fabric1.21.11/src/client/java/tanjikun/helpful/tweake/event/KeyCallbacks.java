package tanjikun.helpful.tweake.event;

import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import tanjikun.helpful.tweake.util.CrawlState;
import tanjikun.helpful.tweake.config.Configs;
import tanjikun.helpful.tweake.config.CrawlTriggerMode;
import tanjikun.helpful.tweake.config.Hotkeys;
import tanjikun.helpful.tweake.gui.GuiConfigs;

public class KeyCallbacks
{
    public static void init(Minecraft mc)
    {
        Hotkeys.OPEN_GUI_SETTINGS.getKeybind().setCallback(new OpenGuiCallback(mc));
        Configs.Tools.BETTER_AUTO_JUMP.getKeybind().setCallback(new ToggleBetterAutoJumpCallback());
        Configs.Tools.GLOBAL_MENDING.getKeybind().setCallback(new ToggleGlobalMendingCallback());
        Configs.Tools.BETTER_TOTEM.getKeybind().setCallback(new ToggleBetterTotemCallback());
        Configs.Tools.BETTER_CLIMBING.getKeybind().setCallback(new ToggleBetterClimbingCallback());
        Configs.Tools.BETTER_BOAT.getKeybind().setCallback(new ToggleBetterBoatCallback());
        Configs.Tools.BETTER_DURABILITY.getKeybind().setCallback(new ToggleBetterDurabilityCallback());
        Configs.Tools.BETTER_HARVEST.getKeybind().setCallback(new ToggleBetterHarvestCallback());
        Configs.Tools.SHIELD_STATUS.getKeybind().setCallback(new ToggleShieldStatusCallback());
        Configs.Tools.ARMOR_HUD.getKeybind().setCallback(new ToggleArmorHudCallback());
        Configs.Tools.CONTROLLED_CRAWL.getKeybind().setCallback(new CrawlKeyCallback());
        Configs.Optimization.BOLD_FONT.getKeybind().setCallback(new ToggleBoldFontCallback());
        Configs.Optimization.ENTITY_RENDER_OPTIMIZATION.getKeybind().setCallback(new ToggleEntityRenderOptimizationCallback());
        Configs.Optimization.SKIP_INVISIBLE_ENTITIES.getKeybind().setCallback(new ToggleSkipInvisibleEntitiesCallback());
        Configs.Optimization.SKIP_DISTANT_ENTITIES.getKeybind().setCallback(new ToggleSkipDistantEntitiesCallback());
        Configs.Optimization.STACK_ENTITY_RENDER_OPTIMIZATION.getKeybind().setCallback(new ToggleStackEntityRenderOptimizationCallback());
    }

    private record OpenGuiCallback(Minecraft mc) implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            GuiBase.openGui(new GuiConfigs());
            return true;
        }
    }

    private record ToggleBetterAutoJumpCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Tools.BETTER_AUTO_JUMP.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Tools.BETTER_AUTO_JUMP.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.betterAutoJump." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    private record ToggleGlobalMendingCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Tools.GLOBAL_MENDING.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Tools.GLOBAL_MENDING.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.globalMending." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    private record ToggleBetterTotemCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Tools.BETTER_TOTEM.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Tools.BETTER_TOTEM.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.betterTotem." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    private record ToggleBetterClimbingCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Tools.BETTER_CLIMBING.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Tools.BETTER_CLIMBING.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.betterClimbing." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    private record ToggleBetterBoatCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Tools.BETTER_BOAT.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Tools.BETTER_BOAT.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.betterBoat." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    private record ToggleBetterDurabilityCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Tools.BETTER_DURABILITY.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Tools.BETTER_DURABILITY.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.betterDurability." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    private record ToggleBetterHarvestCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Tools.BETTER_HARVEST.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Tools.BETTER_HARVEST.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.betterHarvest." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    private record ToggleBoldFontCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Optimization.BOLD_FONT.toggleBooleanValue();
            Configs.saveToFile();
            // 切换时重置状态，让下次渲染重新检测
            tanjikun.helpful.tweake.client.util.BoldFontManager.reset();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Optimization.BOLD_FONT.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.boldFont." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    private record ToggleEntityRenderOptimizationCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Optimization.ENTITY_RENDER_OPTIMIZATION.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Optimization.ENTITY_RENDER_OPTIMIZATION.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.entityRenderOptimization." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    private record ToggleSkipInvisibleEntitiesCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Optimization.SKIP_INVISIBLE_ENTITIES.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Optimization.SKIP_INVISIBLE_ENTITIES.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.skipInvisibleEntities." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    private record ToggleSkipDistantEntitiesCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Optimization.SKIP_DISTANT_ENTITIES.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Optimization.SKIP_DISTANT_ENTITIES.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.skipDistantEntities." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    private record ToggleStackEntityRenderOptimizationCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Optimization.STACK_ENTITY_RENDER_OPTIMIZATION.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Optimization.STACK_ENTITY_RENDER_OPTIMIZATION.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.stackEntityRenderOptimization." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    private record ToggleShieldStatusCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Tools.SHIELD_STATUS.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Tools.SHIELD_STATUS.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.shieldStatus." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    private record ToggleArmorHudCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            Configs.Tools.ARMOR_HUD.toggleBooleanValue();
            Configs.saveToFile();
            if (Minecraft.getInstance().player != null)
            {
                boolean enabled = Configs.Tools.ARMOR_HUD.getBooleanValue();
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("helpful_tweake.message.armorHud." + (enabled ? "enabled" : "disabled")),
                        true);
            }
            return true;
        }
    }

    /**
     * 控制爬行按键回调。
     * HOLD 模式下不做事（爬行状态由 Mixin 实时查询 isKeybindHeld() 判定），
     * TOGGLE 模式下切换 toggleCrawling 状态。
     */
    private record CrawlKeyCallback() implements IHotkeyCallback
    {
        @Override
        public boolean onKeyAction(KeyAction action, IKeybind key)
        {
            if (Configs.Tools.CRAWL_TRIGGER_MODE.getOptionListValue() == CrawlTriggerMode.TOGGLE)
            {
                CrawlState.toggle();
            }
            // HOLD 模式下也不传播按键事件（避免被其他系统误处理）
            return true;
        }
    }
}
