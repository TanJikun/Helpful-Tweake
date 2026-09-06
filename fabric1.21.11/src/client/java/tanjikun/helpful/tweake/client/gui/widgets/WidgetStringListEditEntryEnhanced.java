package tanjikun.helpful.tweake.client.gui.widgets;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.world.item.ItemStack;

import fi.dy.masa.malilib.config.IConfigStringList;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetListStringListEdit;
import fi.dy.masa.malilib.gui.widgets.WidgetStringListEditEntry;
import fi.dy.masa.malilib.render.GuiContext;
import fi.dy.masa.malilib.util.GuiUtils;

import tanjikun.helpful.tweake.client.gui.GuiRegistryEntryPicker;
import tanjikun.helpful.tweake.client.util.RegistryEntries;

/**
 * 列表优化：增强的字符串列表编辑行。
 *
 * 相对原版 WidgetStringListEditEntry 的差异：
 *   - 序号和值之间渲染一个 16x16 图标，显示当前值对应的方块/物品；
 *   - 行最右侧增加一个"?"按钮，点击打开注册表条目选择器，
 *     点击选择器中的图标后，本行的值直接替换为所选注册 ID。
 *
 * 布局实现：通过重写 addTextField，将文本框右移（给图标留位）并收窄
 * （给"?"按钮留位），重置按钮与增删/移动按钮随返回坐标自动左移。
 */
public class WidgetStringListEditEntryEnhanced extends WidgetStringListEditEntry
{
    /** 序号与值之间为图标预留的宽度（含边距）。 */
    private static final int ICON_SHIFT = 24;
    /** 行右侧为"?"按钮预留的宽度。 */
    private static final int QUESTION_SHIFT = 20;

    private String lastIconValue = "\u0000";
    private ItemStack cachedIconStack = ItemStack.EMPTY;

    public WidgetStringListEditEntryEnhanced(int x, int y, int width, int height, int listIndex,
                                             boolean isOdd, String initialValue, String defaultValue,
                                             WidgetListStringListEdit parent)
    {
        super(x, y, width, height, listIndex, isOdd, initialValue, defaultValue, parent);

        // super 构造期间 addTextField 已右移/收窄，按钮簇已左移腾出右侧空间
        if (!this.isDummy())
        {
            ButtonGeneric questionButton = new ButtonGeneric(x + width - QUESTION_SHIFT + 2, y + 1, 16, 20, "?",
                    "helpful_tweake.gui.button.hovertext.pick_entry");
            this.addButton(questionButton, new QuestionButtonListener(this));
        }
    }

    @Override
    protected int addTextField(int x, int y, int resetX, int configWidth, int configHeight, String initialValue)
    {
        // 右移给行内图标留位，收窄给"?"按钮留位；resetX 随之左移使后续按钮整体左移
        return super.addTextField(x + ICON_SHIFT, y, resetX - QUESTION_SHIFT,
                configWidth - ICON_SHIFT - QUESTION_SHIFT, configHeight, initialValue);
    }

    @Override
    public void render(GuiContext ctx, int mouseX, int mouseY, boolean selected)
    {
        super.render(ctx, mouseX, mouseY, selected);

        if (!this.isDummy())
        {
            ItemStack stack = this.getCurrentValueStack();
            if (!stack.isEmpty())
            {
                ctx.renderItem(stack, this.x + 26, this.y + 3);
            }
        }
    }

    /**
     * 获取当前文本框值对应的物品图标（按值字符串缓存，避免每帧解析注册 ID）。
     */
    private ItemStack getCurrentValueStack()
    {
        String value = this.textField != null ? this.textField.textField().getValue() : "";
        if (!value.equals(this.lastIconValue))
        {
            this.lastIconValue = value;
            this.cachedIconStack = RegistryEntries.findStackForId(value);
        }
        return this.cachedIconStack;
    }

    /**
     * 打开注册表条目选择器。选择结果直接写入配置列表：
     * 打开选择器时本界面被挂起（removed() 已把文本框待定值写入配置），
     * 关闭选择器返回后本界面重新初始化，从配置重建各行显示。
     */
    private void openEntryPicker()
    {
        IConfigStringList config = this.parent.getConfig();
        int listIndex = this.listIndex;
        Consumer<String> onSelect = id ->
        {
            List<String> strings = config.getStrings();
            if (listIndex >= 0 && listIndex < strings.size())
            {
                strings.set(listIndex, id);
            }
            else
            {
                strings.add(id);
            }
            config.markDirty();
            config.setModified();
        };
        GuiBase.openGui(new GuiRegistryEntryPicker(onSelect, GuiUtils.getCurrentScreen()));
    }

    private record QuestionButtonListener(WidgetStringListEditEntryEnhanced parent) implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            this.parent.openEntryPicker();
        }
    }
}
