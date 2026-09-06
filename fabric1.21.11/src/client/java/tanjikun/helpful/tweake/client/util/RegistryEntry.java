package tanjikun.helpful.tweake.client.util;

import net.minecraft.world.item.ItemStack;

/**
 * 注册表条目选择器中的一条记录：注册 ID + 用于显示/搜索的物品图标。
 * 物品与方块形式同时存在的注册项（如 minecraft:stone）只对应一个条目（取其物品形式）。
 */
public final class RegistryEntry
{
    private final String id;
    private final ItemStack stack;
    private final String displayName;

    public RegistryEntry(String id, ItemStack stack)
    {
        this.id = id;
        this.stack = stack;
        this.displayName = stack.getHoverName().getString();
    }

    public String id()
    {
        return this.id;
    }

    public ItemStack stack()
    {
        return this.stack;
    }

    /** 本地化的显示名称（如"石头"），用于搜索与悬浮提示。 */
    public String displayName()
    {
        return this.displayName;
    }
}
