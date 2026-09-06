package tanjikun.helpful.tweake.client.gui;

import java.util.function.Consumer;

import net.minecraft.client.gui.screens.Screen;

import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.GuiTextFieldGeneric;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.StringUtils;

import tanjikun.helpful.tweake.client.gui.widgets.WidgetListRegistryEntries;
import tanjikun.helpful.tweake.client.gui.widgets.WidgetRegistryEntryIcon;
import tanjikun.helpful.tweake.client.util.RegistryEntry;

/**
 * 注册表条目选择器：顶部搜索框 + 下方全部方块/物品的图标网格。
 * 点击图标后通过回调返回所选注册 ID，并关闭本界面回到父界面。
 * 搜索支持注册 ID；安装 JustEnoughCharacters 后额外支持中文/拼音。
 */
public class GuiRegistryEntryPicker extends GuiListBase<RegistryEntry, WidgetRegistryEntryIcon, WidgetListRegistryEntries>
{
    protected final Consumer<String> selectListener;
    protected int dialogWidth = 320;
    protected int dialogHeight;
    protected int dialogLeft;
    protected int dialogTop;
    protected GuiTextFieldGeneric searchField;

    public GuiRegistryEntryPicker(Consumer<String> selectListener, Screen parent)
    {
        super(0, 0);
        this.selectListener = selectListener;
        this.setParent(parent);
        this.title = StringUtils.translate("helpful_tweake.gui.title.registry_entry_picker");
    }

    @Override
    public void initGui()
    {
        this.setWidthAndHeight();
        this.centerOnScreen();
        this.reCreateListWidget();
        super.initGui();

        // super.initGui() 内部会 clearElements，搜索框需在其后添加
        this.searchField = new GuiTextFieldGeneric(this.dialogLeft + 10, this.dialogTop + 20,
                this.dialogWidth - 22, 16, this.font);
        this.addTextField(this.searchField, new SearchRefreshListener());
    }

    protected void setWidthAndHeight()
    {
        this.dialogHeight = GuiUtils.getScaledWindowHeight() - 90;
    }

    protected void centerOnScreen()
    {
        if (this.getParent() != null)
        {
            this.dialogLeft = this.getParent().width / 2 - this.dialogWidth / 2;
            this.dialogTop = this.getParent().height / 2 - this.dialogHeight / 2;
        }
        else
        {
            this.dialogLeft = 20;
            this.dialogTop = 20;
        }
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.dialogWidth - 14;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.dialogHeight - 50;
    }

    @Override
    protected WidgetListRegistryEntries createListWidget(int listX, int listY)
    {
        return new WidgetListRegistryEntries(this.dialogLeft + 10, this.dialogTop + 42,
                this.getBrowserWidth(), this.getBrowserHeight(),
                this::getSearchText, this::onEntrySelected);
    }

    private String getSearchText()
    {
        return this.searchField != null ? this.searchField.getValue() : "";
    }

    private void onEntrySelected(RegistryEntry entry)
    {
        this.selectListener.accept(entry.id());
        this.closePicker();
    }

    private void closePicker()
    {
        this.mc.setScreen(this.getParent());
    }

    @Override
    public void render(net.minecraft.client.gui.GuiGraphics drawContext, int mouseX, int mouseY, float partialTicks)
    {
        if (this.getParent() != null)
        {
            this.getParent().render(drawContext, mouseX, mouseY, partialTicks);
        }
        super.render(drawContext, mouseX, mouseY, partialTicks);
    }

    @Override
    protected void drawScreenBackground(GuiContext ctx, int mouseX, int mouseY)
    {
        RenderUtils.drawOutlinedBox(ctx, this.dialogLeft, this.dialogTop, this.dialogWidth, this.dialogHeight, -16777216, -6710887);
    }

    @Override
    protected void drawTitle(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        this.drawStringWithShadow(ctx, this.title, this.dialogLeft + 10, this.dialogTop + 6, -1);
    }

    /**
     * 搜索框文本变化时刷新网格：先回到顶部，避免旧滚动位置超出过滤后的内容。
     */
    private class SearchRefreshListener implements ITextFieldListener<GuiTextFieldGeneric>
    {
        @Override
        public boolean onTextChange(GuiTextFieldGeneric textField)
        {
            WidgetListRegistryEntries list = GuiRegistryEntryPicker.this.getListWidget();
            if (list != null)
            {
                list.resetScrollbarPosition();
                list.refreshEntries();
            }
            return false;
        }
    }
}
