package tanjikun.helpful.tweake.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/**
 * 渲染副本的偏移方向选项。
 *
 * 取值：
 *   UP     —— Y+（上方）
 *   DOWN   —— Y-（下方）
 *   NORTH  —— Z-（北）
 *   SOUTH  —— Z+（南）
 *   WEST   —— X-（西）
 *   EAST   —— X+（东）
 */
public enum RenderDirection implements IConfigOptionListEntry
{
    UP      ("up",    0, 1, 0),
    DOWN    ("down",  0, -1, 0),
    NORTH   ("north", 0, 0, -1),
    SOUTH   ("south", 0, 0, 1),
    WEST    ("west",  -1, 0, 0),
    EAST    ("east",  1, 0, 0);

    private static final RenderDirection[] VALUES = values();
    private static final String TRANSLATION_KEY_PREFIX = "helpful_tweake.config.tools.render_direction.";

    private final String serializedName;
    private final int offsetX;
    private final int offsetY;
    private final int offsetZ;

    RenderDirection(String serializedName, int offsetX, int offsetY, int offsetZ)
    {
        this.serializedName = serializedName;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    /** 返回该方向的单位偏移量 X 分量（用于计算副本渲染位置） */
    public int getOffsetX()
    {
        return offsetX;
    }

    /** 返回该方向的单位偏移量 Y 分量（用于计算副本渲染位置） */
    public int getOffsetY()
    {
        return offsetY;
    }

    /** 返回该方向的单位偏移量 Z 分量（用于计算副本渲染位置） */
    public int getOffsetZ()
    {
        return offsetZ;
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
        for (RenderDirection dir : VALUES)
        {
            if (dir.serializedName.equals(value))
            {
                return dir;
            }
        }
        return UP;
    }
}
