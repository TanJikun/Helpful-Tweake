package tanjikun.helpful.tweake.client.util;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import tanjikun.helpful.tweake.config.Configs;

/**
 * 可视化经验值：在经验条 1/4 处显示当前等级经验进度，3/4 处显示总经验值。
 *
 * 行为：
 *   - 1/4 处：当前等级已获得经验 / 当前等级升级所需总经验（如 "3/7"，表示已获得3点，共需7点）
 *   - 3/4 处：玩家累计总经验值（如 "1234"）
 *   - 文字颜色由子配置项 EXPERIENCE_TEXT_COLOR 决定
 *
 * 位置（与原版经验条对齐）：
 *   - 经验条：x = (width - 182) / 2，y = height - 29，宽 182，高 5
 *   - 1/4 处 x = barLeft + 45，3/4 处 x = barLeft + 137
 *   - y = barTop - 1（与经验条重叠，避免与生命值/饥饿值重叠）
 *
 * ponytail: ConfigColor.getIntegerValue() 对 6 位 hex 返回 0x00RRGGBB（alpha=0），
 *           drawString 会将其视为透明。需用 | 0xFF000000 强制 alpha=255。
 * 升级路径: 无。MaLiLib 行为如此，强制不透明是 HUD 文字的合理选择。
 *
 * ponytail: player.totalExperience 字段不受 /xp set 命令正确重置（原版行为），
 *           需根据 experienceLevel 和 experienceProgress 用原版公式重新计算。
 * 已知上限: 经验等级为 int，循环累加最多几百次，性能可接受。
 * 升级路径: 无。原版字段不可靠，公式计算是唯一可靠来源。
 *
 * 灵感来自：https://modrinth.com/mod/exp-counter
 */
public class VisualExperienceHudRenderer implements HudRenderCallback
{
    /** 经验条宽度（原版常量） */
    private static final int XP_BAR_WIDTH = 182;
    /** 经验条距屏幕底部偏移（原版：height - 29） */
    private static final int XP_BAR_BOTTOM_OFFSET = 29;

    @Override
    public void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker)
    {
        if (!Configs.Tools.VISUAL_EXPERIENCE.getBooleanValue())
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
        {
            return;
        }

        LocalPlayer player = mc.player;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        // 经验条坐标（与原版一致）
        int barLeft = (screenW - XP_BAR_WIDTH) / 2;
        int barTop = screenH - XP_BAR_BOTTOM_OFFSET;

        // 1/4 处和 3/4 处 x 坐标
        int centerX1 = barLeft + XP_BAR_WIDTH / 4;
        int centerX3 = barLeft + 3 * XP_BAR_WIDTH / 4;

        // 经验值计算
        int neededForNextLevel = player.getXpNeededForNextLevel();
        int currentLevelExp = (int) (player.experienceProgress * neededForNextLevel);
        int totalXp = getTotalExperience(player.experienceLevel, player.experienceProgress);

        // 显示文本（加粗样式，适配本模组的加粗优化 Mixin）
        Style boldStyle = Style.EMPTY.withBold(true);
        Component line1 = Component.literal(currentLevelExp + "/" + neededForNextLevel).withStyle(boldStyle);
        Component line2 = Component.literal(String.valueOf(totalXp)).withStyle(boldStyle);

        // 文字颜色（来自子配置，强制 alpha=255 不透明）
        int color = Configs.Tools.EXPERIENCE_TEXT_COLOR.getIntegerValue() | 0xFF000000;

        // 居中绘制：1/4 处显示当前等级经验进度，3/4 处显示总经验值
        // y = barTop - 1 让文字与经验条重叠，避免与生命值/饥饿值条重叠
        int y = barTop - 1;
        int line1X = centerX1 - mc.font.width(line1) / 2;
        int line2X = centerX3 - mc.font.width(line2) / 2;

        guiGraphics.drawString(mc.font, line1, line1X, y, color, true);
        guiGraphics.drawString(mc.font, line2, line2X, y, color, true);
    }

    /**
     * 根据等级和当前等级进度计算总经验值。
     * 使用原版经验公式（与 Player.getXpNeededForNextLevel 一致）：
     *   level 0-15:  每级需要 2*level + 7
     *   level 16-30: 每级需要 5*level - 38
     *   level 31+:   每级需要 9*level - 158
     *
     * 不使用 player.totalExperience 字段，因为 /xp set 命令不会正确重置它。
     */
    private static int getTotalExperience(int level, float progress)
    {
        int total = 0;
        for (int i = 0; i < level; i++)
        {
            total += getXpNeededForLevel(i);
        }
        total += (int) (progress * getXpNeededForLevel(level));
        return total;
    }

    /**
     * 原版公式：指定等级升到下一级所需经验。
     * 与 Player.getXpNeededForNextLevel() 逻辑一致，但接受任意等级参数。
     */
    private static int getXpNeededForLevel(int level)
    {
        if (level >= 30)
        {
            return 9 * level - 158;
        }
        else if (level >= 15)
        {
            return 5 * level - 38;
        }
        else
        {
            return 2 * level + 7;
        }
    }
}
