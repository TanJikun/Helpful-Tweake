package tanjikun.helpful.tweake.client.gui.widgets;

import java.util.Collection;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.gui.widgets.WidgetListBase;

import tanjikun.helpful.tweake.client.util.JeCharactersCompat;
import tanjikun.helpful.tweake.client.util.RegistryEntries;
import tanjikun.helpful.tweake.client.util.RegistryEntry;

/**
 * 注册表条目选择器的网格列表控件。
 *
 * 在 MaLiLib 纵向列表（WidgetListBase）的基础上重写条目布局：
 * 每行按列数平铺多个 20x20 图标格子，滚动条单位为"行"。
 * 搜索过滤同时匹配注册 ID 与本地化名称（后者需安装 JustEnoughCharacters 才支持拼音）。
 */
public class WidgetListRegistryEntries extends WidgetListBase<RegistryEntry, WidgetRegistryEntryIcon>
{
    private static final int CELL_SIZE = 20;

    private final Supplier<String> filterTextSupplier;
    private final Consumer<RegistryEntry> clickListener;

    public WidgetListRegistryEntries(int x, int y, int width, int height,
                                     Supplier<String> filterTextSupplier,
                                     Consumer<RegistryEntry> clickListener)
    {
        super(x, y, width, height, null);
        this.filterTextSupplier = filterTextSupplier;
        this.clickListener = clickListener;
        this.browserEntryHeight = CELL_SIZE;
    }

    @Override
    protected Collection<RegistryEntry> getAllEntries()
    {
        return RegistryEntries.buildAllEntries();
    }

    @Override
    protected WidgetRegistryEntryIcon createListEntryWidget(int x, int y, int listIndex, boolean isOdd, RegistryEntry entry)
    {
        return new WidgetRegistryEntryIcon(x, y, CELL_SIZE, CELL_SIZE, listIndex, isOdd, entry, this.clickListener);
    }

    @Override
    protected boolean hasFilter()
    {
        return !this.filterTextSupplier.get().isEmpty();
    }

    @Override
    protected String getFilterText()
    {
        return this.filterTextSupplier.get().toLowerCase(Locale.ROOT);
    }

    @Override
    protected boolean entryMatchesFilter(RegistryEntry entry, String filterText)
    {
        // 注册 ID 匹配（始终可用）
        if (entry.id().toLowerCase(Locale.ROOT).contains(filterText))
        {
            return true;
        }
        // 本地化名称匹配（需 jecharacters 支持中文/拼音，未安装时仅精确子串）
        String name = entry.displayName();
        return name.toLowerCase(Locale.ROOT).contains(filterText)
                || JeCharactersCompat.contains(name, filterText);
    }

    /**
     * 网格布局：与父类的纵向堆叠不同，这里每行平铺 columns 个格子。
     * 滚动条值单位为行，行首条目索引 = 滚动行数 × 列数。
     */
    @Override
    protected void reCreateListEntryWidgets()
    {
        this.listWidgets.clear();
        this.maxVisibleBrowserEntries = 0;
        int columns = this.getColumns();
        int usableHeight = this.browserHeight - this.browserPaddingY - this.browserEntriesOffsetY;
        int x = this.posX + 2;
        int y = this.posY + 4 + this.browserEntriesOffsetY;
        int index = this.scrollBar.getValue() * columns;
        int usedHeight = 0;

        while (index < this.listContents.size() && usedHeight + CELL_SIZE <= usableHeight)
        {
            for (int col = 0; col < columns && index < this.listContents.size(); col++, index++)
            {
                this.listWidgets.add(this.createListEntryWidget(
                        x + col * CELL_SIZE, y, index, false, this.listContents.get(index)));
            }
            this.maxVisibleBrowserEntries++;
            usedHeight += CELL_SIZE;
            y += CELL_SIZE;
        }

        int totalRows = (this.listContents.size() + columns - 1) / columns;
        this.scrollBar.setMaxValue(totalRows - this.maxVisibleBrowserEntries);
    }

    @Override
    public void drawContents(GuiContext ctx, int mouseX, int mouseY, float partialTicks)
    {
        WidgetRegistryEntryIcon hovered = null;
        int columns = this.getColumns();
        int scrollbarHeight = this.browserHeight - this.browserEntriesOffsetY - 8;
        // 内容总高度按"行"计算，使滚动条滑块长度与网格实际高度匹配
        int totalHeight = Math.max((this.listContents.size() + columns - 1) / columns * CELL_SIZE, scrollbarHeight);
        int scrollBarX = this.posX + this.browserWidth - 9;
        int scrollBarY = this.posY + this.browserPaddingY + this.browserEntriesOffsetY;
        this.scrollBar.render(ctx, mouseX, mouseY, partialTicks, scrollBarX, scrollBarY, 8, scrollbarHeight, totalHeight);
        if (this.scrollBar.getValue() != this.lastScrollbarPosition)
        {
            this.lastScrollbarPosition = this.scrollBar.getValue();
            this.reCreateListEntryWidgets();
        }
        for (WidgetRegistryEntryIcon widget : this.listWidgets)
        {
            widget.render(ctx, mouseX, mouseY, false);
            if (widget.isMouseOver(mouseX, mouseY))
            {
                hovered = widget;
            }
        }
        this.hoveredWidget = hovered;
    }

    private int getColumns()
    {
        return Math.max(1, this.browserEntryWidth / CELL_SIZE);
    }
}
