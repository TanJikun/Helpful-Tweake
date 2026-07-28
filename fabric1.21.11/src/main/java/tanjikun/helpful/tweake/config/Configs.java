package tanjikun.helpful.tweake.config;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import tanjikun.helpful.tweake.HelpfulTweake;
import tanjikun.helpful.tweake.Reference;

/**
 * 模组配置总入口，负责配置的加载与保存。
 * Tools / Optimization 两个分类，配置项使用 ConfigBooleanHotkeyed 将开关与热键合并在同一行显示。
 */
public class Configs implements IConfigHandler
{
    private static final String CONFIG_FILE_NAME = Reference.MOD_ID + ".json";

    private static final String TOOLS_KEY = Reference.MOD_ID_LOWER + ".config.tools";
    private static final String OPTIMIZATION_KEY = Reference.MOD_ID_LOWER + ".config.optimization";

    public static class Tools
    {
        // 更好的自动跳跃：允许玩家平滑通过≤1.25格高的障碍，不损失水平速度
        // ConfigBooleanHotkeyed 将 boolean 开关与热键合并在同一行（类似 tweakeroo）
        public static final ConfigBooleanHotkeyed BETTER_AUTO_JUMP =
                new ConfigBooleanHotkeyed("betterAutoJump", false, "").apply(TOOLS_KEY);

        // 全局经验修补：装备栏/主副手/快捷栏/背包内所有经验修补物品都能在吸收经验时修复
        public static final ConfigBooleanHotkeyed GLOBAL_MENDING =
                new ConfigBooleanHotkeyed("globalMending", false, "").apply(TOOLS_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                BETTER_AUTO_JUMP,
                GLOBAL_MENDING
        );
    }

    public static class Optimization
    {
        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of();
    }

    public static void loadFromFile()
    {
        Path configFile = FileUtils.getConfigDirectoryAsPath().resolve(CONFIG_FILE_NAME);

        if (Files.exists(configFile) && Files.isReadable(configFile))
        {
            JsonElement element = JsonUtils.parseJsonFileAsPath(configFile);

            if (element != null && element.isJsonObject())
            {
                JsonObject root = element.getAsJsonObject();

                ConfigUtils.readConfigBase(root, "Tools", Tools.OPTIONS);
                ConfigUtils.readConfigBase(root, "Optimization", Optimization.OPTIONS);
                ConfigUtils.readHotkeys(root, "Hotkeys", Hotkeys.HOTKEY_LIST);
            }
        }
    }

    public static void saveToFile()
    {
        Path dir = FileUtils.getConfigDirectoryAsPath();

        if (Files.isDirectory(dir))
        {
            JsonObject root = new JsonObject();

            ConfigUtils.writeConfigBase(root, "Tools", Tools.OPTIONS);
            ConfigUtils.writeConfigBase(root, "Optimization", Optimization.OPTIONS);
            ConfigUtils.writeHotkeys(root, "Hotkeys", Hotkeys.HOTKEY_LIST);

            JsonUtils.writeJsonToFileAsPath(root, dir.resolve(CONFIG_FILE_NAME));
        }
        else
        {
            HelpfulTweake.LOGGER.error("saveToFile(): Config directory '{}' does not exist!", dir.toAbsolutePath());
        }
    }

    @Override
    public void load()
    {
        loadFromFile();
    }

    @Override
    public void save()
    {
        saveToFile();
    }

    // GUI 关闭时 MaLiLib 调用此方法（经 ConfigManager.onConfigsChanged），
    // 默认实现不保存，必须重写为 saveToFile() 才能持久化用户在 GUI 中的修改。
    @Override
    public void onConfigsChanged()
    {
        saveToFile();
    }
}
