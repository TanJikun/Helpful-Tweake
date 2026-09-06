package tanjikun.helpful.tweake.client.gui;

import net.minecraft.client.gui.screens.Screen;

import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.GuiStringListEdit;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IDialogHandler;
import fi.dy.masa.malilib.gui.widgets.WidgetListStringListEdit;
import tanjikun.helpful.tweake.client.gui.widgets.WidgetListStringListEditEnhanced;

/**
 * 列表优化：增强的字符串列表编辑界面（编辑行为增强版行控件）。
 */
public class GuiStringListEditEnhanced extends GuiStringListEdit
{
    public GuiStringListEditEnhanced(IConfigStringList config, IConfigGui configGui,
                                     IDialogHandler dialogHandler, Screen parent)
    {
        super(config, configGui, dialogHandler, parent);
    }

    @Override
    protected WidgetListStringListEdit createListWidget(int listX, int listY)
    {
        return new WidgetListStringListEditEnhanced(this.dialogLeft + 10, this.dialogTop + 20,
                this.getBrowserWidth(), this.getBrowserHeight(), this.dialogWidth - 100, this);
    }
}
