package tanjikun.helpful.tweake.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.client.gui.screens.advancements.AdvancementTab;

import tanjikun.helpful.tweake.client.util.AdvancementScreenSize;

/**
 * 更大的进度界面 — AdvancementTab 侧尺寸替换。
 *
 * 内容区 234×113 为编译期内联立即数，出现在：
 *   - drawContents（method_2310）：scissor 裁剪矩形 + 16×16 背景贴图平铺循环
 *     （循环上界 15/8 = 原版贴图列/行数，需按新内容宽高重算）
 *   - drawTooltips（method_2314）：悬停淡入遮罩 fill + 悬停范围检查（各两处，
 *     @ModifyConstant 默认匹配同值全部立即数）
 *   - scroll（method_2313）：滚动范围 clamp
 *   - canScrollHorizontally / canScrollVertically（method_76273 / method_76274）：
 *     是否可滚动的判断阈值
 *
 * 所有处理器在功能关闭时返回 AdvancementScreenSize 的原版回退值，
 * 无需各自判断开关。贴图列/行数取"内容尺寸/16 + 1"（原版 234/16+1=15、
 * 113/16+1=8，与立即数吻合，含滚动偏移余量）。
 *
 * ponytail: @ModifyConstant method 用 intermediary + remap=false
 * （项目无 refmap，详见 project_memory）
 */
@Mixin(AdvancementTab.class)
public class MixinAdvancementTabBigger
{
    // === drawContents：scissor 裁剪 + 背景贴图平铺循环 ===
    @ModifyConstant(method = "method_2310", constant = @Constant(intValue = 234), remap = false)
    private int helpfulTweake$contentsWidth(int original)
    {
        return AdvancementScreenSize.contentWidth();
    }

    @ModifyConstant(method = "method_2310", constant = @Constant(intValue = 113), remap = false)
    private int helpfulTweake$contentsHeight(int original)
    {
        return AdvancementScreenSize.contentHeight();
    }

    /** 15 = 原版背景贴图列数（234/16+1） */
    @ModifyConstant(method = "method_2310", constant = @Constant(intValue = 15), remap = false)
    private int helpfulTweake$tileColumns(int original)
    {
        return AdvancementScreenSize.contentWidth() / 16 + 1;
    }

    /** 8 = 原版背景贴图行数（113/16+1） */
    @ModifyConstant(method = "method_2310", constant = @Constant(intValue = 8), remap = false)
    private int helpfulTweake$tileRows(int original)
    {
        return AdvancementScreenSize.contentHeight() / 16 + 1;
    }

    /** 117 = 原版内容宽的一半（进度树初始居中：scrollX = 117 - 树宽/2） */
    @ModifyConstant(method = "method_2310", constant = @Constant(intValue = 117), remap = false)
    private int helpfulTweake$treeCenterX(int original)
    {
        return AdvancementScreenSize.contentWidth() / 2;
    }

    /** 56 = 原版内容高的一半（进度树初始居中：scrollY = 56 - 树高/2） */
    @ModifyConstant(method = "method_2310", constant = @Constant(intValue = 56), remap = false)
    private int helpfulTweake$treeCenterY(int original)
    {
        return AdvancementScreenSize.contentHeight() / 2;
    }

    // === drawTooltips：悬停遮罩与范围检查（234/113 各两处） ===
    @ModifyConstant(method = "method_2314", constant = @Constant(intValue = 234), remap = false)
    private int helpfulTweake$tooltipWidth(int original)
    {
        return AdvancementScreenSize.contentWidth();
    }

    @ModifyConstant(method = "method_2314", constant = @Constant(intValue = 113), remap = false)
    private int helpfulTweake$tooltipHeight(int original)
    {
        return AdvancementScreenSize.contentHeight();
    }

    // === scroll：滚动 clamp 范围 ===
    @ModifyConstant(method = "method_2313", constant = @Constant(intValue = 234), remap = false)
    private int helpfulTweake$scrollWidth(int original)
    {
        return AdvancementScreenSize.contentWidth();
    }

    @ModifyConstant(method = "method_2313", constant = @Constant(intValue = 113), remap = false)
    private int helpfulTweake$scrollHeight(int original)
    {
        return AdvancementScreenSize.contentHeight();
    }

    // === 可滚动判断阈值 ===
    @ModifyConstant(method = "method_76273", constant = @Constant(intValue = 234), remap = false)
    private int helpfulTweake$canScrollWidth(int original)
    {
        return AdvancementScreenSize.contentWidth();
    }

    @ModifyConstant(method = "method_76274", constant = @Constant(intValue = 113), remap = false)
    private int helpfulTweake$canScrollHeight(int original)
    {
        return AdvancementScreenSize.contentHeight();
    }
}
