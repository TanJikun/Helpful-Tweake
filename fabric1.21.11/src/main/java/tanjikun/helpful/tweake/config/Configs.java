package tanjikun.helpful.tweake.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import fi.dy.masa.malilib.config.ConfigUtils;
import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigHandler;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigOptionList;
import fi.dy.masa.malilib.config.options.ConfigStringList;
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

        // 更好的不死图腾：装备栏/主副手/快捷栏/背包内所有不死图腾都能在死亡时触发，不需要拿在手上
        public static final ConfigBooleanHotkeyed BETTER_TOTEM =
                new ConfigBooleanHotkeyed("betterTotem", false, "").apply(TOOLS_KEY);

        // 更好的攀爬：攀爬时根据视角方向调整纵向速度
        public static final ConfigBooleanHotkeyed BETTER_CLIMBING =
                new ConfigBooleanHotkeyed("betterClimbing", false, "").apply(TOOLS_KEY);

        // 更好的船：船只可越过高度不大于抬升高度的障碍
        public static final ConfigBooleanHotkeyed BETTER_BOAT =
                new ConfigBooleanHotkeyed("betterBoat", false, "").apply(TOOLS_KEY);

        // 更好的船子配置：抬升高度（0-200，默认0）
        public static final ConfigDouble BOAT_LIFT_HEIGHT =
                new ConfigDouble("boatLiftHeight", 0.0, 0.0, 200.0).apply(TOOLS_KEY);

        // 更好的耐久显示：在物品上显示剩余耐久数字，颜色与耐久条一致
        public static final ConfigBooleanHotkeyed BETTER_DURABILITY =
                new ConfigBooleanHotkeyed("betterDurability", false, "").apply(TOOLS_KEY);

        // 更好的耐久显示子配置：根据耐久附魔等级推算实际可用次数
        public static final ConfigBooleanHotkeyed DURABILITY_UNBREAKING_CALC =
                new ConfigBooleanHotkeyed("durabilityUnbreakingCalc", false, "").apply(TOOLS_KEY);

        // 更方便的收获：右键收获成熟作物并消耗种子重新种植
        public static final ConfigBooleanHotkeyed BETTER_HARVEST =
                new ConfigBooleanHotkeyed("betterHarvest", false, "").apply(TOOLS_KEY);

        // 更方便的收获子配置：是否需要手持锄头
        public static final ConfigBooleanHotkeyed HARVEST_REQUIRE_HOE =
                new ConfigBooleanHotkeyed("harvestRequireHoe", false, "").apply(TOOLS_KEY);

        // 更方便的收获子配置：手持锄头收获时是否消耗耐久（仅在 requireHoe=true 时生效）
        public static final ConfigBooleanHotkeyed HARVEST_HOE_DURABILITY =
                new ConfigBooleanHotkeyed("harvestHoeDurability", false, "").apply(TOOLS_KEY);

        // 更方便的收获子配置：作物黑名单（游戏内 ID 列表）
        public static final ConfigStringList HARVEST_BLACKLIST =
                new ConfigStringList("harvestBlacklist", ImmutableList.of(), "").apply(TOOLS_KEY);

        // 盾牌状态显示：在盾牌上叠加颜色层（绿色=可用，红色=冷却中）
        public static final ConfigBooleanHotkeyed SHIELD_STATUS =
                new ConfigBooleanHotkeyed("shieldStatus", false, "").apply(TOOLS_KEY);

        // 盔甲 HUD：在屏幕上显示身上 4 件盔甲的图标与耐久信息
        public static final ConfigBooleanHotkeyed ARMOR_HUD =
                new ConfigBooleanHotkeyed("armorHud", false, "").apply(TOOLS_KEY);

        // 盔甲 HUD 子配置：屏幕位置
        public static final ConfigOptionList ARMOR_HUD_POSITION =
                new ConfigOptionList("armorHudPosition", ArmorHudPosition.HOTBAR_LEFT).apply(TOOLS_KEY);

        // 盔甲 HUD 子配置：自定义坐标 X 百分比（1-100，仅当 ARMOR_HUD_POSITION=CUSTOM 时生效）
        public static final ConfigDouble ARMOR_HUD_X =
                new ConfigDouble("armorHudX", 50.0, 1.0, 100.0).apply(TOOLS_KEY);

        // 盔甲 HUD 子配置：自定义坐标 Y 百分比（1-100，仅当 ARMOR_HUD_POSITION=CUSTOM 时生效）
        public static final ConfigDouble ARMOR_HUD_Y =
                new ConfigDouble("armorHudY", 50.0, 1.0, 100.0).apply(TOOLS_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                BETTER_AUTO_JUMP,
                GLOBAL_MENDING,
                BETTER_TOTEM,
                BETTER_CLIMBING,
                BETTER_BOAT,
                BOAT_LIFT_HEIGHT,
                BETTER_DURABILITY,
                DURABILITY_UNBREAKING_CALC,
                BETTER_HARVEST,
                HARVEST_REQUIRE_HOE,
                HARVEST_HOE_DURABILITY,
                HARVEST_BLACKLIST,
                SHIELD_STATUS,
                ARMOR_HUD,
                ARMOR_HUD_POSITION,
                ARMOR_HUD_X,
                ARMOR_HUD_Y
        );

        /**
         * 返回显示用配置列表。子配置项仅在对应主配置展开时显示。
         * 持久化始终使用 OPTIONS（包含全部配置项）。
         */
        public static List<IConfigBase> getDisplayOptions(boolean betterBoatExpanded,
                                                          boolean betterDurabilityExpanded,
                                                          boolean betterHarvestExpanded,
                                                          boolean armorHudExpanded)
        {
            if (betterBoatExpanded && betterDurabilityExpanded && betterHarvestExpanded && armorHudExpanded)
            {
                return OPTIONS;
            }
            List<IConfigBase> filtered = new java.util.ArrayList<>();
            for (IConfigBase config : OPTIONS)
            {
                if (!betterBoatExpanded && config == BOAT_LIFT_HEIGHT)
                {
                    continue;
                }
                if (!betterDurabilityExpanded && config == DURABILITY_UNBREAKING_CALC)
                {
                    continue;
                }
                if (!betterHarvestExpanded && (config == HARVEST_REQUIRE_HOE
                        || config == HARVEST_HOE_DURABILITY
                        || config == HARVEST_BLACKLIST))
                {
                    continue;
                }
                if (!armorHudExpanded && (config == ARMOR_HUD_POSITION
                        || config == ARMOR_HUD_X
                        || config == ARMOR_HUD_Y))
                {
                    continue;
                }
                filtered.add(config);
            }
            return filtered;
        }
    }

    public static class Optimization
    {
        // 加粗显示优化：使用 cu_ 前缀粗体字体替代原版1像素偏移加粗
        public static final ConfigBooleanHotkeyed BOLD_FONT =
                new ConfigBooleanHotkeyed("boldFont", true, "").apply(OPTIMIZATION_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                BOLD_FONT
        );
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
