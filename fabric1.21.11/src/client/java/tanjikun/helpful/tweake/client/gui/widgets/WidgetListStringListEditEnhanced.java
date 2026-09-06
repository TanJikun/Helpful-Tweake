package tanjikun.helpful.tweake.client.gui.widgets;

import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.GuiStringListEdit;
import fi.dy.masa.malilib.gui.widgets.WidgetListStringListEdit;
import fi.dy.masa.malilib.gui.widgets.WidgetStringListEditEntry;

/**
 * 列表优化：创建增强版编辑行（行内图标 + "?"选择按钮）的列表控件。
 */
public class WidgetListStringListEditEnhanced extends WidgetListStringListEdit
{
    public WidgetListStringListEditEnhanced(int x, int y, int width, int height, int configWidth,
                                             GuiStringListEdit parent)
    {
        super(x, y, width, height, configWidth, parent);
    }

    @Override
    protected WidgetStringListEditEntry createListEntryWidget(int x, int y, int listIndex, boolean isOdd, String entry)
    {
        IConfigStringList config = this.getConfig();
        if (listIndex >= 0 && listIndex < config.getStrings().size())
        {
            String defaultValue = config.getDefaultStrings().size() > listIndex ? config.getDefaultStrings().get(listIndex) : "";
            return new WidgetStringListEditEntryEnhanced(x, y, this.browserEntryWidth, this.browserEntryHeight,
                    listIndex, isOdd, config.getStrings().get(listIndex), defaultValue, this);
        }
        return new WidgetStringListEditEntryEnhanced(x, y, this.browserEntryWidth, this.browserEntryHeight,
                listIndex, isOdd, "", "", this);
    }
}
