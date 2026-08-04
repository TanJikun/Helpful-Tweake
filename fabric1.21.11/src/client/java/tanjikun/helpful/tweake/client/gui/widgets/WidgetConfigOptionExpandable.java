package tanjikun.helpful.tweake.client.gui.widgets;

import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigResettable;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import fi.dy.masa.malilib.hotkeys.IKeybind;

import tanjikun.helpful.tweake.client.gui.ExpandState;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 在 ConfigBooleanHotkeyed 行的 toggle 按钮左侧添加展开/折叠按钮。
 * 对 BETTER_BOAT 和 BETTER_DURABILITY 配置项生效，其他配置项走原版逻辑。
 */
public class WidgetConfigOptionExpandable extends WidgetConfigOption
{
    public WidgetConfigOptionExpandable(int x, int y, int browserEntryWidth, int browserEntryHeight,
                                        int maxLabelWidth, int configWidth, ConfigOptionWrapper wrapper,
                                        int listWidgetWidth, IKeybindConfigGui host,
                                        WidgetListConfigOptionsBase<?, ?> parent)
    {
        super(x, y, browserEntryWidth, browserEntryHeight, maxLabelWidth, configWidth,
              wrapper, listWidgetWidth, host, parent);
    }

    @Override
    protected void addBooleanAndHotkeyWidgets(int x, int y, int w,
                                              IConfigResettable configResettable,
                                              IConfigBoolean configBoolean, IKeybind keybind)
    {
        if (configBoolean == Configs.Tools.BETTER_BOAT)
        {
            addExpandButton(x, y, w, configResettable, configBoolean, keybind,
                    ExpandState.betterBoatExpanded, ExpandState::toggleBetterBoat);
        }
        else if (configBoolean == Configs.Tools.BETTER_DURABILITY)
        {
            addExpandButton(x, y, w, configResettable, configBoolean, keybind,
                    ExpandState.betterDurabilityExpanded, ExpandState::toggleBetterDurability);
        }
        else if (configBoolean == Configs.Tools.BETTER_HARVEST)
        {
            addExpandButton(x, y, w, configResettable, configBoolean, keybind,
                    ExpandState.betterHarvestExpanded, ExpandState::toggleBetterHarvest);
        }
        else if (configBoolean == Configs.Tools.ARMOR_HUD)
        {
            addExpandButton(x, y, w, configResettable, configBoolean, keybind,
                    ExpandState.armorHudExpanded, ExpandState::toggleArmorHud);
        }
        else
        {
            super.addBooleanAndHotkeyWidgets(x, y, w, configResettable, configBoolean, keybind);
        }
    }

    private void addExpandButton(int x, int y, int w,
                                 IConfigResettable configResettable,
                                 IConfigBoolean configBoolean, IKeybind keybind,
                                 boolean expanded, Runnable toggleAction)
    {
        ButtonGeneric expandBtn = new ButtonGeneric(x, y, 14, 20, expanded ? "-" : "+");
        this.addButton(expandBtn, new ExpandButtonListener(toggleAction));
        super.addBooleanAndHotkeyWidgets(x + 16, y, w - 16, configResettable, configBoolean, keybind);
    }

    private record ExpandButtonListener(Runnable toggleAction) implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            toggleAction.run();
        }
    }
}
