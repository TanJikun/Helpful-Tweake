package tanjikun.helpful.tweake.client.util;

import net.fabricmc.loader.api.FabricLoader;

/**
 * JustEnoughCharacters（拼音搜索）软依赖兼容层。
 * 未安装时仅支持 ID 搜索；安装后支持中文/拼音搜索（委托 Match.contains）。
 *
 * 对 jecharacters 的类引用被隔离在内部类 JeCharactersBridge 中，
 * 只有检测到模组已安装时才会加载该类，避免运行时 NoClassDefFoundError。
 */
public final class JeCharactersCompat
{
    private static final boolean LOADED = FabricLoader.getInstance().isModLoaded("jecharacters");

    private JeCharactersCompat()
    {
    }

    public static boolean isLoaded()
    {
        return LOADED;
    }

    /**
     * 判断 text 是否匹配搜索词 query（含中文/拼音匹配）。
     * 仅在 jecharacters 已安装时有效，否则返回 false。
     */
    public static boolean contains(String text, String query)
    {
        return LOADED && JeCharactersBridge.contains(text, query);
    }

    private static final class JeCharactersBridge
    {
        static boolean contains(String text, String query)
        {
            return me.towdium.jecharacters.utils.Match.contains(text, query);
        }
    }
}
