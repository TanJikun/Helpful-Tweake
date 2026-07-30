package tanjikun.helpful.tweake.client.mixin;

import java.awt.Color;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 更好的耐久显示：在物品图标上叠加剩余耐久数字。
 *
 * 行为：
 *   - 耐久满时不显示
 *   - 数字格式：1~999 直接显示，1000~999999 显示为 1.0K~999K，10^6+ 显示为 1.0M+
 *   - 数字颜色 = 原版耐久条颜色 = HSV(120°×剩余/总耐久, 1, 1)
 *   - 子配置"耐久附魔推算"开启时，显示考虑耐久附魔后的实际可用次数
 *
 * 注入点：GuiGraphics.renderItemDecorations(Font, ItemStack, int, int, String) 的 RETURN。
 * 该方法是物品装饰（耐久条、数量、冷却）的总入口，在其返回后叠加耐久数字。
 *
 * ponytail: Mixin 方法名使用 intermediary（method_51432）而非 named（renderItemDecorations）
 * 已知上限: 本项目用 officialMojangMappings 但 Loom 1.17 未生成 refmap，导致 Mixin 注解
 *           的 method 字符串无法从 named 重映射到 intermediary。运行时游戏类用 intermediary。
 * 升级路径: 升级 Loom 或改用 layered mappings 生成 refmap 后，可改回 named 名 "renderItemDecorations"。
 */
@Mixin(GuiGraphics.class)
public class MixinGuiGraphicsDurability
{
    // method_51432 = GuiGraphics.renderItemDecorations(Font, ItemStack, int, int, String)
    @Inject(method = "method_51432", at = @At("RETURN"), remap = false)
    private void helpfulTweake$renderDurabilityText(Font font, ItemStack stack, int x, int y, String text, CallbackInfo ci)
    {
        if (!Configs.Tools.BETTER_DURABILITY.getBooleanValue())
        {
            return;
        }
        if (stack.isEmpty() || !stack.isDamaged())
        {
            return;
        }

        int max = stack.getMaxDamage();
        if (max <= 0)
        {
            return;
        }
        int remaining = max - stack.getDamageValue();

        if (remaining <= 0)
        {
            return;
        }

        // 颜色基于原始耐久比例（与原版耐久条一致）
        int color = helpfulTweake$getDurabilityColor(remaining, max);

        // 显示数字：开启推算时用有效耐久，否则用原始剩余耐久
        int displayValue = remaining;
        if (Configs.Tools.DURABILITY_UNBREAKING_CALC.getBooleanValue())
        {
            displayValue = helpfulTweake$getEffectiveRemaining(stack, remaining);
        }

        String display = helpfulTweake$formatDurability(displayValue);

        // 缩小至 0.5 倍并置于物品图标顶部，避开底部耐久条
        GuiGraphics gg = (GuiGraphics) (Object) this;
        float scale = 0.5F;
        int textWidth = font.width(display);
        // 目标中心：(x+8, y+2)，缩放后坐标系需除以 scale
        int drawX = (int) ((x + 8) / scale) - textWidth / 2;
        int drawY = (int) ((y + 2) / scale);

        gg.pose().pushMatrix();
        gg.pose().scale(scale, scale);
        gg.drawString(font, display, drawX, drawY, color, true);
        gg.pose().popMatrix();
    }

    /**
     * 根据耐久附魔等级推算实际可用次数。
     *
     * Java版耐久附魔公式（来源：Minecraft Wiki 耐久附魔页面）：
     *   工具/武器：每次使用有 level/(level+1) 概率不消耗耐久，有效耐久 = remaining × (level+1)
     *   盔甲：每次受击有 level×0.6/(level+1) 概率不消耗耐久，
     *         有效耐久 = remaining × (level+1) / (1 + 0.4×level)
     *
     * 1.21.11 无 ArmorItem 类，通过 EQUIPPABLE 数据组件判断是否为盔甲。
     */
    @Unique
    private static int helpfulTweake$getEffectiveRemaining(ItemStack stack, int remaining)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null)
        {
            return remaining;
        }
        // 1.21.11 中 Enchantments.UNBREAKING 是 ResourceKey<Enchantment>，需通过注册表转为 Holder<Enchantment>
        Holder<Enchantment> unbreaking = mc.level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.UNBREAKING);
        int level = EnchantmentHelper.getItemEnchantmentLevel(unbreaking, stack);
        if (level <= 0)
        {
            return remaining;
        }
        boolean isArmor = stack.get(DataComponents.EQUIPPABLE) != null;
        double multiplier;
        if (isArmor)
        {
            multiplier = (double) (level + 1) / (1.0 + 0.4 * level);
        }
        else
        {
            multiplier = level + 1;
        }
        // 不四舍五入，直接截断为整数
        return (int) (remaining * multiplier);
    }

    /**
     * 格式化耐久数字。
     * 1~999 直接显示；1000+ 按单位 K/M/B/T/Q 显示，1位小数后截断（不四舍五入）。
     * 单位换算：K=10^3, M=10^6, B=10^9, T=10^12, Q=10^15。
     * <100 时显示 1 位小数（如 1.9K），>=100 时显示整数（如 999K）。
     */
    @Unique
    private static String helpfulTweake$formatDurability(int value)
    {
        if (value < 1000)
        {
            return Integer.toString(value);
        }
        String[] suffixes = {"", "K", "M", "B", "T", "Q"};
        int tier = 0;
        long v = value;
        while (v >= 1000 && tier < suffixes.length - 1)
        {
            v /= 1000;
            tier++;
        }
        // v 现在是 [1, 999] 范围内的主值，根据原始值重建带1位小数的形式
        double scaled = value / Math.pow(1000.0, tier);
        // 截断到1位小数：先×10 floor 再÷10
        double truncated = Math.floor(scaled * 10.0) / 10.0;
        if (truncated < 100)
        {
            return String.format("%.1f%s", truncated, suffixes[tier]);
        }
        return (int) truncated + suffixes[tier];
    }

    /**
     * 计算耐久数字颜色，与原版耐久条颜色一致。
     * 原版公式：HSVtoRGB(remaining/(3×max), 1, 1)，即 HSV(120°×remaining/max, 1, 1)。
     */
    @Unique
    private static int helpfulTweake$getDurabilityColor(int remaining, int max)
    {
        float hue = (float) remaining / (3.0F * max);
        return Color.HSBtoRGB(hue, 1.0F, 1.0F);
    }
}
