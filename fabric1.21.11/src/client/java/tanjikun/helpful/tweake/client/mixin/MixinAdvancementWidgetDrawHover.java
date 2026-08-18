package tanjikun.helpful.tweake.client.mixin;

import java.text.Collator;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.CriterionProgress;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import tanjikun.helpful.tweake.client.util.AdvancementDetailState;
import tanjikun.helpful.tweake.client.util.AdvancementScreenSize;
import tanjikun.helpful.tweake.client.util.IAdvancementDetailRenderer;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 进度详细信息显示 — 悬停/锁定累加型进度时在右侧绘制子项完成状态面板。
 *
 * 机制：AdvancementWidget.drawHover（method_2331）仅悬停时被调用；锁定显示由
 *   MixinAdvancementTabDetails 注入 drawTooltips TAIL，通过本类实现的
 *   IAdvancementDetailRenderer 接口跨类调用 renderDetails。
 *
 * 坐标系：drawHover 与 drawTooltips 均在 translate(windowX+9, windowY+18)
 *   变换下执行（AdvancementsScreen.renderTooltips 设置），故绘制前抵消回屏幕坐标。
 *
 * 列表滚动：超出屏幕高度时列表区域用 scissor 裁剪，滚轮滚动查看（滚动状态在
 *   AdvancementDetailState，滚轮事件由 MixinAdvancementsScreenInteraction 处理）。
 *
 * criterion 名称翻译：依次尝试多种翻译键前缀；盔甲纹饰类 criterion 名形如
 *   armor_trimmed_minecraft:rib_armor_trim_smithing_template_smithing_trim，
 *   需提取 "rib" 部分再拼 trim_pattern.minecraft.rib。
 *
 * ponytail: @Shadow 用 aliases 指定 intermediary 字段名；@Inject method 用
 *   intermediary + remap=false（项目无 refmap，详见 project_memory）
 */
@Mixin(AdvancementWidget.class)
public class MixinAdvancementWidgetDrawHover implements IAdvancementDetailRenderer
{
    /** 中文拼音排序器（对英文按字母序，对中文按拼音序） */
    private static final Collator ZH_COLLATOR = Collator.getInstance(Locale.CHINA);

    @Shadow(aliases = {"field_46143"}, remap = false)
    @Final
    private AdvancementNode advancementNode;

    @Shadow(aliases = {"field_2712"}, remap = false)
    @Final
    private DisplayInfo display;

    @Shadow(aliases = {"field_2714"}, remap = false)
    private AdvancementProgress progress;

    @Inject(method = "method_2331", at = @At("TAIL"), remap = false)
    private void helpfulTweake$drawHoverPanel(GuiGraphics gui, int scrollX, int scrollY,
                                               float fade, int windowX, int windowY,
                                               CallbackInfo ci)
    {
        helpfulTweake$renderDetails(gui, windowX, windowY);
    }

    @Override
    @Unique
    public void helpfulTweake$renderDetails(GuiGraphics gui, int windowX, int windowY)
    {
        if (!Configs.Tools.BETTER_ADVANCEMENTS.getBooleanValue()
                || !Configs.Tools.SHOW_ADVANCEMENT_DETAILS.getBooleanValue())
        {
            return;
        }

        if (progress == null || !helpfulTweake$isAccumulative())
        {
            return;
        }

        Advancement advancement = advancementNode.advancement();
        AdvancementRequirements req = advancement.requirements();
        List<List<String>> requirements = req.requirements();

        // 收集 criterion 状态并分类
        List<String> remaining = new ArrayList<>();
        List<String> completed = new ArrayList<>();
        for (List<String> group : requirements)
        {
            String name = group.get(0);
            CriterionProgress cp = progress.getCriterion(name);
            boolean done = cp != null && cp.isDone();
            if (done)
            {
                completed.add(name);
            }
            else
            {
                remaining.add(name);
            }
        }

        // 排序：同色按拼音序；合并：红色(未完成)在上，绿色(已完成)在下
        remaining.sort(ZH_COLLATOR);
        completed.sort(ZH_COLLATOR);
        List<Map.Entry<String, Boolean>> entries = new ArrayList<>();
        for (String n : remaining)
        {
            entries.add(new AbstractMap.SimpleEntry<>(n, false));
        }
        for (String n : completed)
        {
            entries.add(new AbstractMap.SimpleEntry<>(n, true));
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        int panelW = 140;
        // 优先放窗口右外侧；更大的进度界面开启时窗口接近满屏，回退为窗口内右缘
        // （windowWidth() 关闭时返回原版 252，即保持原布局）
        int panelX = windowX + AdvancementScreenSize.windowWidth() + 8;
        if (panelX + panelW > screenW)
        {
            panelX = Math.max(windowX + 13, screenW - panelW - 4);
        }

        int lineH = 12;
        int completedCount = completed.size();
        int totalCount = requirements.size();

        // 简介换行
        Component desc = display.getDescription();
        List<FormattedCharSequence> descLines = font.split(desc, panelW - 4);

        // 面板最大高度 = 屏幕高度 - 上下边距，列表可视高度受此限制
        int maxPanelH = screenH - 16;
        int headerH = 18 + descLines.size() * lineH + 4;
        int contentH = entries.size() * lineH;
        int viewH = Math.min(contentH, maxPanelH - headerH - 8);

        // 面板高度：标题 + 简介 + 列表可视区 + 内边距
        int panelH = headerH + viewH + 8;
        int panelY = (screenH - panelH) / 2;
        if (panelY < 4)
        {
            panelY = 4;
        }

        // 目标切换时重置滚动
        String targetId = advancementNode.holder().id().toString();
        if (!targetId.equals(AdvancementDetailState.lastTargetId))
        {
            AdvancementDetailState.scrollOffset = 0;
            AdvancementDetailState.lastTargetId = targetId;
        }
        AdvancementDetailState.maxScroll = Math.max(0, contentH - viewH);
        if (AdvancementDetailState.scrollOffset > AdvancementDetailState.maxScroll)
        {
            AdvancementDetailState.scrollOffset = AdvancementDetailState.maxScroll;
        }

        // 抵消 renderTooltips 设置的 translate，回屏幕坐标
        gui.pose().pushMatrix();
        gui.pose().translate(-windowX - 9, -windowY - 18);

        // 背景面板（半透明黑）
        gui.fill(panelX - 4, panelY - 4, panelX + panelW + 4, panelY + panelH + 4, 0xE0000000);

        // === 标题行：图标 + 进度名 + "0/9" + 进度条 ===
        gui.renderItem(display.getIcon(), panelX, panelY);

        Component title = display.getTitle();
        String progressText = completedCount + "/" + totalCount;
        int titleX = panelX + 20;
        int titleY = panelY + 5;
        gui.drawString(font, title, titleX, titleY, 0xFFFFFFFF, true);

        int titleWidth = font.width(title);
        int progressTextX = titleX + titleWidth + 6;
        gui.drawString(font, progressText, progressTextX, titleY, 0xFFFFFFFF, true);

        int progressTextWidth = font.width(progressText);
        int barX = progressTextX + progressTextWidth + 6;
        int barY = panelY + 7;
        int barW = panelX + panelW - barX;
        if (barW > 0)
        {
            gui.fill(barX, barY, barX + barW, barY + 4, 0xFF000000);
            int filled = totalCount > 0 ? (int) (barW * (float) completedCount / totalCount) : 0;
            gui.fill(barX, barY, barX + filled, barY + 4, 0xFF55FF55);
        }

        // === 简介行 ===
        int descY = panelY + 18;
        for (FormattedCharSequence line : descLines)
        {
            gui.drawString(font, line, panelX, descY, 0xFFAAAAAA, true);
            descY += lineH;
        }

        // === 子项列表（scissor 裁剪 + 滚动） ===
        int listTop = panelY + headerH;
        AdvancementDetailState.listX = panelX;
        AdvancementDetailState.listY = listTop;
        AdvancementDetailState.listW = panelW;
        AdvancementDetailState.listH = viewH;

        if (viewH > 0)
        {
            gui.enableScissor(panelX - 2, listTop - 1, panelX + panelW + 2, listTop + viewH);
            int itemY = listTop - AdvancementDetailState.scrollOffset;
            for (Map.Entry<String, Boolean> entry : entries)
            {
                Component name = helpfulTweake$resolveName(entry.getKey());
                int color = entry.getValue() ? 0xFF55FF55 : 0xFFFF5555;
                gui.drawString(font, name, panelX, itemY, color, true);
                itemY += lineH;
            }
            gui.disableScissor();

            // 滚动条指示（可滚动时在列表右侧画细条）
            if (AdvancementDetailState.maxScroll > 0)
            {
                int trackX = panelX + panelW;
                gui.fill(trackX, listTop, trackX + 2, listTop + viewH, 0xFF555555);
                int thumbH = Math.max(6, viewH * viewH / contentH);
                int thumbY = listTop + (viewH - thumbH)
                        * AdvancementDetailState.scrollOffset / AdvancementDetailState.maxScroll;
                gui.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, 0xFFAAAAAA);
            }
        }

        gui.pose().popMatrix();
    }

    @Override
    @Unique
    public boolean helpfulTweake$isAccumulative()
    {
        if (advancementNode == null || advancementNode.advancement() == null)
        {
            return false;
        }
        List<List<String>> requirements = advancementNode.advancement()
                .requirements().requirements();
        if (requirements.size() < 2)
        {
            return false;
        }
        for (List<String> group : requirements)
        {
            if (group.size() != 1)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * 解析 criterion name 为可显示的 Component。
     * 盔甲纹饰类 criterion 名形如 armor_trimmed_minecraft:rib_armor_trim_smithing_template_smithing_trim，
     * 先提取纹饰短名（rib）再走 trim_pattern 翻译键；其余依次尝试常见前缀。
     * 用 getString() 与 key 比较检测翻译成功，兜底显示 shortName。
     */
    @Unique
    private static Component helpfulTweake$resolveName(String criterionKey)
    {
        String shortName = criterionKey.contains(":")
                ? criterionKey.substring(criterionKey.indexOf(':') + 1)
                : criterionKey;

        // 盔甲纹饰进度：提取 <pattern>_armor_trim 中的 pattern 短名
        String trimIdx = "_armor_trim_smithing_template";
        int idx = criterionKey.indexOf(trimIdx);
        if (idx > 0)
        {
            String head = criterionKey.substring(0, idx + "_armor_trim".length());
            int colon = head.indexOf(':');
            if (colon >= 0)
            {
                shortName = head.substring(colon + 1, head.length() - "_armor_trim".length());
            }
        }

        // 尝试多种翻译键前缀，覆盖原版常见 criterion 类型
        String[] tryKeys = {
                criterionKey,
                "biome.minecraft." + shortName,
                "entity.minecraft." + shortName,
                "block.minecraft." + shortName,
                "item.minecraft." + shortName,
                "trim_pattern.minecraft." + shortName,
                "trim_material.minecraft." + shortName,
                "wolf_variant.minecraft." + shortName,
                "cat_variant.minecraft." + shortName,
                "painting.minecraft." + shortName,
                "instrument.minecraft." + shortName,
                "enchantment.minecraft." + shortName,
                "effect.minecraft." + shortName,
                "attribute.minecraft." + shortName,
                "fluid.minecraft." + shortName,
                "villager_profession.minecraft." + shortName,
                "point_of_interest.minecraft." + shortName
        };

        for (String key : tryKeys)
        {
            Component c = Component.translatable(key);
            if (!c.getString().equals(key))
            {
                return c;
            }
        }

        // 兜底：显示 shortName（不做格式化，避免误导）
        return Component.literal(shortName);
    }
}
