package tanjikun.helpful.tweake.client.util;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.mojang.blaze3d.font.GlyphProvider;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.client.gui.font.FontManager;
import net.minecraft.client.gui.font.FontOption;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.providers.TrueTypeGlyphProviderDefinition;
import net.minecraft.resources.Identifier;

import tanjikun.helpful.tweake.client.mixin.MixinFontManagerAccessor;
import tanjikun.helpful.tweake.client.mixin.MixinMinecraftAccessor;
import tanjikun.helpful.tweake.client.mixin.MixinTrueTypeGlyphProviderAccessor;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * 加粗显示优化：检测材质包是否提供 cu_ 前缀的粗体字体文件，
 * 若存在则加载为独立 FontSet，供 MixinFont 在 bold 渲染时切换使用。
 *
 * 状态机：
 *   UNINITIALIZED → 懒加载
 *   NO_CUSTOM_FONT → default.json 无 TrueType provider，不干预（沿用原版）
 *   CUSTOM_FONT_NO_CU → 有 TrueType provider 但无 cu_ 文件，禁用原版 bold 偏移
 *   CU_AVAILABLE → cu_ 文件存在，用 cu_ 字体 + 禁用偏移
 *
 * cu_ 命名规则：cu_ + 原字体文件名（同目录）
 */
public class BoldFontManager
{
    public static final int STATE_UNINITIALIZED = 0;
    public static final int STATE_NO_CUSTOM_FONT = 1;
    public static final int STATE_CUSTOM_FONT_NO_CU = 2;
    public static final int STATE_CU_AVAILABLE = 3;

    private static volatile int state = STATE_UNINITIALIZED;
    private static GlyphSource boldGlyphSource = null;

    private BoldFontManager() {}

    /**
     * 懒加载初始化。在渲染线程调用（字体已加载完成）。
     */
    private static synchronized void initialize()
    {
        if (state != STATE_UNINITIALIZED) return;

        Minecraft mc = Minecraft.getInstance();
        ResourceManager rm = mc.getResourceManager();

        // 1. 读取 default.json（取最高优先级资源包的版本）
        JsonObject ttfProvider = findTrueTypeProvider(rm);
        if (ttfProvider == null)
        {
            state = STATE_NO_CUSTOM_FONT;
            return;
        }

        // 2. 解析 TrueType provider 参数
        String file = ttfProvider.get("file").getAsString();
        float size = ttfProvider.has("size") ? ttfProvider.get("size").getAsFloat() : 11.0f;
        float oversample = ttfProvider.has("oversample") ? ttfProvider.get("oversample").getAsFloat() : 1.0f;
        TrueTypeGlyphProviderDefinition.Shift shift = TrueTypeGlyphProviderDefinition.Shift.NONE;
        if (ttfProvider.has("shift"))
        {
            JsonArray shiftArr = ttfProvider.getAsJsonArray("shift");
            shift = new TrueTypeGlyphProviderDefinition.Shift(
                    shiftArr.get(0).getAsFloat(), shiftArr.get(1).getAsFloat());
        }
        String skip = ttfProvider.has("skip") ? ttfProvider.get("skip").getAsString() : "";

        // 3. 构造 cu_ 路径（cu_ + 原文件名，同目录）
        Identifier originalLocation = Identifier.parse(file);
        String originalPath = originalLocation.getPath();
        int lastSlash = originalPath.lastIndexOf('/');
        String dir = lastSlash >= 0 ? originalPath.substring(0, lastSlash + 1) : "";
        String filename = lastSlash >= 0 ? originalPath.substring(lastSlash + 1) : originalPath;
        Identifier cuLocation = Identifier.fromNamespaceAndPath(
                originalLocation.getNamespace(), dir + "cu_" + filename);

        // 4. 检查 cu_ 文件是否存在（ttf 文件资源路径为 assets/<ns>/font/<file>）
        Identifier cuResourceId = Identifier.fromNamespaceAndPath(
                cuLocation.getNamespace(), "font/" + cuLocation.getPath());
        if (rm.getResourceStack(cuResourceId).isEmpty())
        {
            // cu_ 文件不存在：禁用原版加粗（不偏移、不加粗）
            state = STATE_CUSTOM_FONT_NO_CU;
            return;
        }

        // 5. 加载 cu_ 字体并创建 bold FontSet
        try
        {
            TrueTypeGlyphProviderDefinition def = new TrueTypeGlyphProviderDefinition(
                    cuLocation, size, oversample, shift, skip);
            GlyphProvider glyphProvider = ((MixinTrueTypeGlyphProviderAccessor) (Object) def).invokeLoad(rm);

            FontManager fontManager = ((MixinMinecraftAccessor) mc).getFontManager();
            GlyphProvider.Conditional conditional = new GlyphProvider.Conditional(
                    glyphProvider, FontOption.Filter.ALWAYS_PASS);

            FontSet boldFontSet = ((MixinFontManagerAccessor) (Object) fontManager).invokeCreateFontSet(
                    Identifier.fromNamespaceAndPath("helpful_tweake", "bold_default"),
                    List.of(conditional),
                    Set.of());

            boldGlyphSource = boldFontSet.source(false);
            state = STATE_CU_AVAILABLE;
        }
        catch (Exception e)
        {
            // 加载失败，回退到禁用原版加粗
            state = STATE_CUSTOM_FONT_NO_CU;
        }
    }

    /**
     * 从 default.json 中查找第一个 type=ttf 的 provider。
     * 返回 null 表示无自定义 TrueType 字体。
     */
    private static JsonObject findTrueTypeProvider(ResourceManager rm)
    {
        Identifier defaultFontId = Identifier.fromNamespaceAndPath("minecraft", "font/default.json");
        List<Resource> resources = rm.getResourceStack(defaultFontId);
        if (resources.isEmpty()) return null;

        // 取最高优先级
        Resource resource = resources.get(resources.size() - 1);
        try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8))
        {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray providers = root.getAsJsonArray("providers");
            if (providers == null) return null;
            for (JsonElement elem : providers)
            {
                JsonObject provider = elem.getAsJsonObject();
                if ("ttf".equals(provider.get("type").getAsString()))
                {
                    return provider;
                }
            }
        }
        catch (Exception e)
        {
            return null;
        }
        return null;
    }

    /**
     * 获取 bold glyph（来自 cu_ 字体）。不可用时返回 null。
     * 包裹 try-catch 防止 initialize() 抛异常影响渲染流程。
     */
    public static BakedGlyph getBoldGlyph(int codepoint)
    {
        try
        {
            if (state == STATE_UNINITIALIZED) initialize();
            if (state != STATE_CU_AVAILABLE || boldGlyphSource == null) return null;
            return boldGlyphSource.getGlyph(codepoint);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * 是否应该禁用原版 bold 1像素偏移。
     * 有自定义 TrueType 字体时（无论有无 cu_）都禁用偏移。
     * 包裹 try-catch 防止 initialize() 抛异常影响渲染流程。
     */
    public static boolean shouldDisableBoldOffset()
    {
        try
        {
            if (state == STATE_UNINITIALIZED) initialize();
            return state == STATE_CU_AVAILABLE || state == STATE_CUSTOM_FONT_NO_CU;
        }
        catch (Exception e)
        {
            // 初始化失败，不禁用偏移（安全回退到原版行为）
            return false;
        }
    }

    /**
     * 重置状态（F3+T 资源重载后需要重新初始化）。
     */
    public static void reset()
    {
        state = STATE_UNINITIALIZED;
        boldGlyphSource = null;
    }
}
