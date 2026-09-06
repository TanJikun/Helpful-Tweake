package tanjikun.helpful.tweake.client.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 注册表条目扫描工具：遍历物品注册表生成可选条目列表。
 *
 * 只扫描物品注册表即可同时覆盖"物品"和"方块"：
 *   - 方块与物品形式都存在的（如 minecraft:stone）注册 ID 相同，天然只出现一次；
 *   - 无物品形式的纯技术方块（如活塞臂、传送门）无法生成图标，不出现在选择器中，
 *     仍可在文本框手动输入。
 * 列表在每次打开选择器时重建（约 1500 条，开销可忽略），自动适配任意模组的注册内容，
 * 并按 ID 字典序排序。
 */
public final class RegistryEntries
{
    private RegistryEntries()
    {
    }

    /** 扫描物品注册表，返回按 ID 字典序排序的全部条目。 */
    public static List<RegistryEntry> buildAllEntries()
    {
        List<RegistryEntry> entries = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM)
        {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            entries.add(new RegistryEntry(id.toString(), new ItemStack(item)));
        }
        entries.sort(Comparator.comparing(RegistryEntry::id));
        return entries;
    }

    /** 按注册 ID 查找对应的物品图标（用于列表编辑行内的值图标），找不到返回空。 */
    public static ItemStack findStackForId(String idString)
    {
        if (idString == null)
        {
            return ItemStack.EMPTY;
        }
        try
        {
            Identifier identifier = Identifier.parse(idString.trim());
            if (BuiltInRegistries.ITEM.containsKey(identifier))
            {
                return new ItemStack(BuiltInRegistries.ITEM.getValue(identifier));
            }
            return ItemStack.EMPTY;
        }
        catch (Exception e)
        {
            return ItemStack.EMPTY;
        }
    }
}
