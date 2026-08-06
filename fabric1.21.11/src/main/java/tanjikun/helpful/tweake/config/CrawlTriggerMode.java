package tanjikun.helpful.tweake.config;

import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import fi.dy.masa.malilib.util.StringUtils;

/**
 * 控制爬行的触发方式选项。
 *
 * 取值：
 *   HOLD    —— 按下：按住绑定的快捷键时切换为爬行姿势，松开后遵循原版逻辑
 *   TOGGLE  —— 切换：按一下按键趴下，再按一下遵循原版逻辑
 *
 * 灵感来自：https://modrinth.com/mod/crawl
 */
public enum CrawlTriggerMode implements IConfigOptionListEntry
{
    HOLD    ("hold"),
    TOGGLE  ("toggle");

    private static final CrawlTriggerMode[] VALUES = values();
    private static final String TRANSLATION_KEY_PREFIX = "helpful_tweake.config.crawl.trigger_mode.";

    private final String serializedName;

    CrawlTriggerMode(String serializedName)
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
        for (CrawlTriggerMode mode : VALUES)
        {
            if (mode.serializedName.equals(value))
            {
                return mode;
            }
        }
        return HOLD;
    }
}
