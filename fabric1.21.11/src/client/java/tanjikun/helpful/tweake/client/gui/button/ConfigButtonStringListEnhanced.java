package tanjikun.helpful.tweake.client.gui.button;

import net.minecraft.client.input.MouseButtonEvent;

import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.interfaces.IConfigGui;
import fi.dy.masa.malilib.gui.interfaces.IDialogHandler;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;

import tanjikun.helpful.tweake.client.gui.GuiStringListEditEnhanced;

/**
 * 列表优化：打开增强版字符串列表编辑界面的按钮。
 * 行为与原版 ConfigButtonStringList 一致（含显示字符串裁剪），
 * 仅将打开的界面替换为增强版（行内图标 + "?"条目选择器）。
 */
public class ConfigButtonStringListEnhanced extends ButtonGeneric
{
    private final IConfigStringList config;
    private final IConfigGui configGui;
    private final IDialogHandler dialogHandler;

    public ConfigButtonStringListEnhanced(int x, int y, int width, int height,
                                           IConfigStringList config, IConfigGui configGui,
                                           IDialogHandler dialogHandler)
    {
        super(x, y, width, height, "", new String[0]);
        this.config = config;
        this.configGui = configGui;
        this.dialogHandler = dialogHandler;
        this.updateDisplayString();
    }

    @Override
    protected boolean onMouseClickedImpl(MouseButtonEvent click, boolean doubleClick)
    {
        super.onMouseClickedImpl(click, doubleClick);
        GuiStringListEditEnhanced gui = new GuiStringListEditEnhanced(this.config, this.configGui, null, GuiUtils.getCurrentScreen());
        if (this.dialogHandler != null)
        {
            this.dialogHandler.openDialog(gui);
        }
        else
        {
            GuiBase.openGui(gui);
        }
        return true;
    }

    @Override
    public void updateDisplayString()
    {
        this.setDisplayString(StringUtils.getClampedDisplayStringRenderlen(this.config.getStrings(), this.width - 10, "[ ", " ]"));
    }
}
