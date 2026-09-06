package tanjikun.helpful.tweake.client.gui.widgets;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.client.input.MouseButtonEvent;

import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;

import tanjikun.helpful.tweake.client.util.RegistryEntry;

/**
 * 注册表条目选择器网格中的一个图标格子：渲染 16x16 物品图标，
 * 悬浮显示"名称 + 注册 ID"，点击后回调选择结果。
 */
public class WidgetRegistryEntryIcon extends WidgetListEntryBase<RegistryEntry>
{
    private final Consumer<RegistryEntry> clickListener;

    public WidgetRegistryEntryIcon(int x, int y, int width, int height,
                                    int listIndex, boolean isOdd, RegistryEntry entry,
                                    Consumer<RegistryEntry> clickListener)
    {
        super(x, y, width, height, entry, listIndex);
        this.clickListener = clickListener;
    }

    @Override
    protected boolean onMouseClickedImpl(MouseButtonEvent click, boolean doubleClick)
    {
        if (this.entry != null)
        {
            this.clickListener.accept(this.entry);
        }
        return true;
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        if (this.isMouseOver(mouseX, mouseY))
        {
            RenderUtils.drawRect(ctx, this.x, this.y, this.width, this.height, 0x50FFFFFF);
        }
        if (this.entry != null)
        {
            ctx.renderItem(this.entry.stack(), this.x + (this.width - 16) / 2, this.y + (this.height - 16) / 2);
        }
    }

    @Override
    public void postRenderHovered(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        if (this.entry != null && this.isMouseOver(mouseX, mouseY))
        {
            List<Component> lines = List.of(
                    Component.literal(this.entry.displayName()),
                    Component.literal(this.entry.id()).withStyle(ChatFormatting.GRAY));
            ctx.setTooltipForNextFrame(this.textRenderer, lines, Optional.empty(), mouseX, mouseY);
        }
    }
}
