package tanjikun.helpful.tweake.client.util;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import tanjikun.helpful.tweake.config.ArmorHudPosition;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 盔甲 HUD：在屏幕上显示玩家身上 4 件盔甲的图标与耐久信息。
 *
 * 行为：
 *   - 4 个槽位横向排列：头盔、胸甲、护腿、靴子（自左而右）
 *   - 空槽位不渲染图标，但保留占位（HUD 整体宽度固定，避免布局跳动）
 *   - 当"更好的耐久显示"开启时，会通过其 Mixin 在物品上叠加耐久数字，
 *     本类无需额外处理（兼容自动生效）
 *
 * 位置计算：
 *   HOTBAR_LEFT   —— 屏幕底部、快捷栏左侧，HUD 底部与屏幕底部对齐
 *   TOP_LEFT      —— 屏幕左上角
 *   TOP_RIGHT     —— 屏幕右上角
 *   BOTTOM_LEFT   —— 屏幕左下角
 *   BOTTOM_RIGHT  —— 屏幕右下角
 *   CUSTOM        —— 由 ARMOR_HUD_X / ARMOR_HUD_Y 百分比推算
 *                    （0% = 屏幕左上角，100% = 屏幕右下角）
 *
 * 灵感来自：https://modrinth.com/mod/ukus-armor-hud
 */
public class ArmorHudRenderer implements HudRenderCallback
{
    /** 单槽位尺寸（含 1px 内边距），与原版 GUI 槽位一致 */
    private static final int SLOT_SIZE = 18;
    /** HUD 总宽（4 槽横向排列） */
    private static final int HUD_WIDTH = SLOT_SIZE * 4;
    /** HUD 总高（单行） */
    private static final int HUD_HEIGHT = SLOT_SIZE;
    /** HUD 与快捷栏的水平间距（仅 HOTBAR_LEFT 用） */
    private static final int HOTBAR_GAP = 2;
    /** 快捷栏半宽（原版常量） */
    private static final int HOTBAR_HALF_WIDTH = 91;
    /** 快捷栏高度（原版常量） */
    private static final int HOTBAR_HEIGHT = 22;

    @Override
    public void onHudRender(GuiGraphics guiGraphics, DeltaTracker deltaTracker)
    {
        if (!Configs.Tools.ARMOR_HUD.getBooleanValue())
        {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui)
        {
            return;
        }

        // 头、胸、腿、靴（顺序与原版生存背包一致）
        ItemStack[] armor = {
                mc.player.getItemBySlot(EquipmentSlot.HEAD),
                mc.player.getItemBySlot(EquipmentSlot.CHEST),
                mc.player.getItemBySlot(EquipmentSlot.LEGS),
                mc.player.getItemBySlot(EquipmentSlot.FEET)
        };

        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int[] origin = computeOrigin(screenW, screenH);

        int x = origin[0];
        int y = origin[1];

        // 横向渲染 4 个槽位（自左而右：头盔 → 靴子）
        for (int i = 0; i < armor.length; i++)
        {
            int slotX = x + i * SLOT_SIZE + 1; // 1px 内边距
            int slotY = y + 1;
            // 空槽位跳过图标渲染，但保留占位（不调整 x，保持布局稳定）
            if (!armor[i].isEmpty())
            {
                guiGraphics.renderItem(armor[i], slotX, slotY);
                guiGraphics.renderItemDecorations(mc.font, armor[i], slotX, slotY, null);
            }
        }
    }

    /**
     * 根据配置的屏幕位置枚举计算 HUD 左上角坐标。
     */
    private int[] computeOrigin(int screenW, int screenH)
    {
        ArmorHudPosition pos =
                (ArmorHudPosition) Configs.Tools.ARMOR_HUD_POSITION.getOptionListValue();

        int x;
        int y;

        switch (pos)
        {
            case HOTBAR_LEFT:
                // 屏幕底部、快捷栏左侧，HUD 垂直居中于快捷栏
                x = (screenW / 2 - HOTBAR_HALF_WIDTH) - HUD_WIDTH - HOTBAR_GAP;
                y = screenH - HOTBAR_HEIGHT + (HOTBAR_HEIGHT - HUD_HEIGHT) / 2;
                break;
            case TOP_LEFT:
                x = 0;
                y = 0;
                break;
            case TOP_RIGHT:
                x = screenW - HUD_WIDTH;
                y = 0;
                break;
            case BOTTOM_LEFT:
                x = 0;
                y = screenH - HUD_HEIGHT;
                break;
            case BOTTOM_RIGHT:
                x = screenW - HUD_WIDTH;
                y = screenH - HUD_HEIGHT;
                break;
            case CUSTOM:
            default:
                // 0% = 屏幕左上角，100% = 屏幕右下角
                double xPercent = Configs.Tools.ARMOR_HUD_X.getDoubleValue() / 100.0;
                double yPercent = Configs.Tools.ARMOR_HUD_Y.getDoubleValue() / 100.0;
                x = (int) Math.round((screenW - HUD_WIDTH) * xPercent);
                y = (int) Math.round((screenH - HUD_HEIGHT) * yPercent);
                break;
        }

        return new int[] {x, y};
    }
}
