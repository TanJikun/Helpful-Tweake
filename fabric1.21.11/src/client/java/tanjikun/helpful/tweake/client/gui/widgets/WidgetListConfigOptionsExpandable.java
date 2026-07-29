package tanjikun.helpful.tweake.client.gui.widgets;

import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;

/**
 * 覆盖 createListEntryWidget，用 WidgetConfigOptionExpandable 替代原始 WidgetConfigOption，
 * 使配置行支持展开/折叠按钮。
 */
public class WidgetListConfigOptionsExpandable extends WidgetListConfigOptions
{
    public WidgetListConfigOptionsExpandable(int x, int y, int width, int height, int configWidth,
                                             float zLevel, boolean scrollbarEnabled,
                                             GuiConfigsBase parent)
    {
        super(x, y, width, height, configWidth, zLevel, scrollbarEnabled, parent);
    }

    @Override
    protected WidgetConfigOption createListEntryWidget(int x, int y, int listWidgetWidth,
                                                       boolean isOdd, ConfigOptionWrapper wrapper)
    {
        return new WidgetConfigOptionExpandable(x, y, this.browserEntryWidth, this.browserEntryHeight,
                this.maxLabelWidth, this.configWidth, wrapper, listWidgetWidth, this.parent, this);
    }
}
