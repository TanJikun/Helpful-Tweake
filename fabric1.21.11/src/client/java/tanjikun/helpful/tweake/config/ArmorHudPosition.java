package tanjikun.helpful.tweake.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/**
 * 盔甲 HUD 的屏幕位置选项。
 *
 * 取值：
 *   HOTBAR_LEFT   —— 快捷栏左侧（HUD 底部与屏幕底部对齐，向上延伸）
 *   TOP_LEFT      —— 屏幕左上角
 *   TOP_RIGHT     —— 屏幕右上角
 *   BOTTOM_LEFT   —— 屏幕左下角
 *   BOTTOM_RIGHT  —— 屏幕右下角
 *   CUSTOM        —— 指定坐标（由 ARMOR_HUD_X / ARMOR_HUD_Y 百分比推算）
 *
 * 灵感来自：https://modrinth.com/mod/ukus-armor-hud
 */
public enum ArmorHudPosition implements IConfigOptionListEntry
{
    HOTBAR_LEFT  ("hotbar_left"),
    TOP_LEFT     ("top_left"),
    TOP_RIGHT    ("top_right"),
    BOTTOM_LEFT  ("bottom_left"),
    BOTTOM_RIGHT ("bottom_right"),
    CUSTOM       ("custom");

    private static final ArmorHudPosition[] VALUES = values();
    private static final String TRANSLATION_KEY_PREFIX = "helpful_tweake.config.armor_hud.position.";

    private final String serializedName;

    ArmorHudPosition(String serializedName)
    {
        this.serializedName = serializedName;
    }

    @Override
    public String getStringValue()
    {
        return serializedName;
    }

    @Override
    public String getDisplayName()
    {
        return StringUtils.translate(TRANSLATION_KEY_PREFIX + serializedName);
    }

    @Override
    public IConfigOptionListEntry cycle(boolean forward)
    {
        int idx = ordinal();
        int next = forward ? (idx + 1) % VALUES.length
                           : (idx - 1 + VALUES.length) % VALUES.length;
        return VALUES[next];
    }

    @Override
    public IConfigOptionListEntry fromString(String value)
    {
        for (ArmorHudPosition pos : VALUES)
        {
            if (pos.serializedName.equals(value))
            {
                return pos;
            }
        }
        return HOTBAR_LEFT;
    }
}
