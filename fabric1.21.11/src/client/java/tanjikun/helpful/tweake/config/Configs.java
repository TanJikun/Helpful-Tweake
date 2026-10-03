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
import fi.dy.masa.malilib.config.options.ConfigBoolean;
import fi.dy.masa.malilib.config.options.ConfigBooleanHotkeyed;
import fi.dy.masa.malilib.config.options.ConfigColor;
import fi.dy.masa.malilib.config.options.ConfigDouble;
import fi.dy.masa.malilib.config.options.ConfigInteger;
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
        // 全局经验修补：装备栏/主副手/快捷栏/背包内所有经验修补物品都能在吸收经验时修复
        public static final ConfigBooleanHotkeyed GLOBAL_MENDING =
                new ConfigBooleanHotkeyed("globalMending", false, "").apply(TOOLS_KEY);

        // 更好的不死图腾：装备栏/主副手/快捷栏/背包内所有不死图腾都能在死亡时触发，不需要拿在手上
        public static final ConfigBooleanHotkeyed BETTER_TOTEM =
                new ConfigBooleanHotkeyed("betterTotem", false, "").apply(TOOLS_KEY);

        // 背包内无限水：快捷栏+副手+背包内共有两桶及以上的水（含鱼桶）时，生存模式放水不消耗桶中的水
        public static final ConfigBooleanHotkeyed INFINITE_WATER =
                new ConfigBooleanHotkeyed("infiniteWater", false, "").apply(TOOLS_KEY);

        // 更好的耐久显示：在物品上显示剩余耐久数字，颜色与耐久条一致
        public static final ConfigBooleanHotkeyed BETTER_DURABILITY =
                new ConfigBooleanHotkeyed("betterDurability", false, "").apply(TOOLS_KEY);

        // 更好的耐久显示子配置：根据耐久附魔等级推算实际可用次数
        public static final ConfigBooleanHotkeyed DURABILITY_UNBREAKING_CALC =
                new ConfigBooleanHotkeyed("durabilityUnbreakingCalc", false, "").apply(TOOLS_KEY);

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

        // 可视化经验值：在经验条上方显示当前等级经验进度和总经验值
        public static final ConfigBooleanHotkeyed VISUAL_EXPERIENCE =
                new ConfigBooleanHotkeyed("visualExperience", false, "").apply(TOOLS_KEY);

        // 可视化经验值子配置：文字颜色（默认 #80FF20，与原版等级数字颜色一致）
        public static final ConfigColor EXPERIENCE_TEXT_COLOR =
                new ConfigColor("experienceTextColor", "#80FF20").apply(TOOLS_KEY);

        // 世吞运维助手：容器型主配置，本身不直接提供效果，统一管理子功能
        // 注意：1.21.11 中 ominous_vault/ominous_trial_spawner 不是独立方块 ID，
        //       而是 vault/trial_spawner 的 OMINOUS 属性，因此默认列表只含 vault/trial_spawner
        public static final ConfigBooleanHotkeyed WORLD_SWALLOW_MAINTENANCE =
                new ConfigBooleanHotkeyed("worldSwallowMaintenance", false, "").apply(TOOLS_KEY);

        // 世吞运维助手子配置：渲染副本（在目标方块上方渲染旋转副本，便于远距离定位）
        public static final ConfigBooleanHotkeyed WSM_RENDER_COPY =
                new ConfigBooleanHotkeyed("wsmRenderCopy", false, "").apply(TOOLS_KEY);

        // 渲染副本子子配置：渲染方向（副本相对原方块的偏移方向，默认 Y+ 上方）
        public static final ConfigOptionList WSM_RENDER_DIRECTION =
                new ConfigOptionList("wsmRenderDirection", RenderDirection.UP).apply(TOOLS_KEY);

        // 渲染副本子子配置：渲染高度（副本沿渲染方向偏移的格数，0-128，默认8）
        public static final ConfigInteger WSM_RENDER_HEIGHT =
                new ConfigInteger("wsmRenderHeight", 8, 0, 128).apply(TOOLS_KEY);

        // 渲染副本子子配置：目标方块 ID 列表
        public static final ConfigStringList WSM_TARGET_BLOCKS =
                new ConfigStringList("wsmTargetBlocks", ImmutableList.of(
                        "minecraft:vault",
                        "minecraft:trial_spawner",
                        "minecraft:obsidian",
                        "minecraft:crying_obsidian",
                        "minecraft:deepslate_diamond_ore",
                        "minecraft:diamond_ore",
                        "minecraft:deepslate_coal_ore",
                        "minecraft:deepslate_emerald_ore",
                        "minecraft:ancient_debris",
                        "minecraft:reinforced_deepslate",
                        "minecraft:heavy_core",
                        "minecraft:ender_chest"
                ), "").apply(TOOLS_KEY);

        // 渲染副本子子配置：是否同时渲染含水方块
        public static final ConfigBooleanHotkeyed WSM_RENDER_WATERLOGGED =
                new ConfigBooleanHotkeyed("wsmRenderWaterlogged", false, "").apply(TOOLS_KEY);

        // 渲染副本子子配置：距离阈值（0-32，实际曼哈顿距离 = 该值×16，默认4）
        public static final ConfigInteger WSM_DISTANCE_THRESHOLD =
                new ConfigInteger("wsmDistanceThreshold", 4, 0, 32).apply(TOOLS_KEY);

        // 世吞运维助手子配置：透明基岩（开启后所有基岩变透明，被挡住的方块可见）
        public static final ConfigBooleanHotkeyed WSM_TRANSPARENT_BEDROCK =
                new ConfigBooleanHotkeyed("wsmTransparentBedrock", false, "").apply(TOOLS_KEY);

        // 世吞运维助手子配置：禁用水与岩浆互动（需服务端支持）
        public static final ConfigBooleanHotkeyed WSM_DISABLE_LIQUID_INTERACTION =
                new ConfigBooleanHotkeyed("wsmDisableLiquidInteraction", false, "").apply(TOOLS_KEY);

        // 世吞运维助手子配置：清海带（需安装 Litematica，未安装时配置项红色禁用）
        public static final ConfigBooleanHotkeyed WSM_CLEAR_KELP =
                new ConfigBooleanHotkeyed("wsmClearKelp", false, "").apply(TOOLS_KEY);

        // 清海带子子配置：3D 曼哈顿距离阈值（1-128，默认4）
        public static final ConfigInteger WSM_CLEAR_KELP_DISTANCE =
                new ConfigInteger("wsmClearKelpDistance", 4, 1, 128).apply(TOOLS_KEY);

        // 清海带子子配置：仅清理选区内（默认开启），关闭后只检测与玩家的距离
        public static final ConfigBooleanHotkeyed WSM_CLEAR_KELP_SELECTION_ONLY =
                new ConfigBooleanHotkeyed("wsmClearKelpSelectionOnly", true, "").apply(TOOLS_KEY);

        // 更好的进度：容器型主配置，本身只统一管理子功能，不直接提供效果
        public static final ConfigBooleanHotkeyed BETTER_ADVANCEMENTS =
                new ConfigBooleanHotkeyed("betterAdvancements", false, "").apply(TOOLS_KEY);

        // 更好的进度子配置：未完成进度显示（开启后在进度页面显示所有进度，包括隐藏的未完成进度）
        public static final ConfigBooleanHotkeyed SHOW_UNCOMPLETED_ADVANCEMENTS =
                new ConfigBooleanHotkeyed("showUncompletedAdvancements", false, "").apply(TOOLS_KEY);

        // 更好的进度子配置：进度详细信息显示（悬停累加型进度时右侧显示各子项完成状态）
        public static final ConfigBooleanHotkeyed SHOW_ADVANCEMENT_DETAILS =
                new ConfigBooleanHotkeyed("showAdvancementDetails", false, "").apply(TOOLS_KEY);

        // 更好的进度子配置：更大的进度界面（进度窗口尽量撑满屏幕但四周留边距）
        public static final ConfigBooleanHotkeyed BIGGER_ADVANCEMENTS_SCREEN =
                new ConfigBooleanHotkeyed("biggerAdvancementsScreen", false, "").apply(TOOLS_KEY);

        // 创造 Shift 移入快捷栏：开启后创造物品栏物品标签页中 shift+左键直接把 1 个物品放入快捷栏
        public static final ConfigBooleanHotkeyed CREATIVE_SHIFT_TO_HOTBAR =
                new ConfigBooleanHotkeyed("creativeShiftToHotbar", false, "").apply(TOOLS_KEY);

        // 容器信号输出显示：打开容器界面时在容器名字右边显示比较器信号强度，如"箱子 (0)"
        public static final ConfigBooleanHotkeyed CONTAINER_SIGNAL_DISPLAY =
                new ConfigBooleanHotkeyed("containerSignalDisplay", false, "").apply(TOOLS_KEY);

        // GUI界面透明度修改：容器型主配置，本身不直接提供效果，统一管理子功能
        public static final ConfigBooleanHotkeyed GUI_TRANSPARENCY =
                new ConfigBooleanHotkeyed("guiTransparency", false, "").apply(TOOLS_KEY);

        // GUI界面透明度修改子配置：GUI 不透明度（0-100%，100% 为原版完全不透明）
        public static final ConfigInteger GUI_OPACITY =
                new ConfigInteger("guiOpacity", 100, 0, 100).apply(TOOLS_KEY);

        // GUI界面透明度修改子配置：背景半透明黑色不透明度（0-100%，75% 为原版默认效果）
        public static final ConfigInteger GUI_BACKGROUND_OPACITY =
                new ConfigInteger("guiBackgroundOpacity", 75, 0, 100).apply(TOOLS_KEY);

        public static ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                GLOBAL_MENDING,
                BETTER_TOTEM,
                INFINITE_WATER,
                BETTER_DURABILITY,
                DURABILITY_UNBREAKING_CALC,
                SHIELD_STATUS,
                ARMOR_HUD,
                ARMOR_HUD_POSITION,
                ARMOR_HUD_X,
                ARMOR_HUD_Y,
                VISUAL_EXPERIENCE,
                EXPERIENCE_TEXT_COLOR,
                WORLD_SWALLOW_MAINTENANCE,
                WSM_RENDER_COPY,
                WSM_RENDER_DIRECTION,
                WSM_RENDER_HEIGHT,
                WSM_TARGET_BLOCKS,
                WSM_RENDER_WATERLOGGED,
                WSM_DISTANCE_THRESHOLD,
                WSM_TRANSPARENT_BEDROCK,
                WSM_DISABLE_LIQUID_INTERACTION,
                WSM_CLEAR_KELP,
                WSM_CLEAR_KELP_DISTANCE,
                WSM_CLEAR_KELP_SELECTION_ONLY,
                BETTER_ADVANCEMENTS,
                SHOW_UNCOMPLETED_ADVANCEMENTS,
                SHOW_ADVANCEMENT_DETAILS,
                BIGGER_ADVANCEMENTS_SCREEN,
                CREATIVE_SHIFT_TO_HOTBAR,
                CONTAINER_SIGNAL_DISPLAY,
                GUI_TRANSPARENCY,
                GUI_OPACITY,
                GUI_BACKGROUND_OPACITY
        );

        /**
         * 返回显示用配置列表。子配置项仅在对应主配置展开时显示。
         * 持久化始终使用 OPTIONS（包含全部配置项）。
         */
        public static List<IConfigBase> getDisplayOptions(boolean betterDurabilityExpanded,
                                                          boolean armorHudExpanded,
                                                          boolean visualExperienceExpanded,
                                                          boolean worldSwallowMaintenanceExpanded,
                                                          boolean renderCopyExpanded,
                                                          boolean clearKelpExpanded,
                                                          boolean betterAdvancementsExpanded,
                                                          boolean guiTransparencyExpanded)
        {
            if (betterDurabilityExpanded
                    && armorHudExpanded && visualExperienceExpanded
                    && worldSwallowMaintenanceExpanded && renderCopyExpanded && clearKelpExpanded && betterAdvancementsExpanded
                    && guiTransparencyExpanded)
            {
                return OPTIONS;
            }
            List<IConfigBase> filtered = new java.util.ArrayList<>();
            for (IConfigBase config : OPTIONS)
            {
                if (!betterDurabilityExpanded && config == DURABILITY_UNBREAKING_CALC)
                {
                    continue;
                }
                if (!armorHudExpanded && (config == ARMOR_HUD_POSITION
                        || config == ARMOR_HUD_X
                        || config == ARMOR_HUD_Y))
                {
                    continue;
                }
                if (!visualExperienceExpanded && config == EXPERIENCE_TEXT_COLOR)
                {
                    continue;
                }
                if (!worldSwallowMaintenanceExpanded && (config == WSM_RENDER_COPY
                        || config == WSM_RENDER_DIRECTION
                        || config == WSM_RENDER_HEIGHT
                        || config == WSM_TARGET_BLOCKS
                        || config == WSM_RENDER_WATERLOGGED
                        || config == WSM_DISTANCE_THRESHOLD
                        || config == WSM_TRANSPARENT_BEDROCK
                        || config == WSM_DISABLE_LIQUID_INTERACTION
                        || config == WSM_CLEAR_KELP
                        || config == WSM_CLEAR_KELP_DISTANCE
                        || config == WSM_CLEAR_KELP_SELECTION_ONLY))
                {
                    continue;
                }
                // 渲染副本子子配置：仅在渲染副本展开时显示
                if (!renderCopyExpanded && (config == WSM_RENDER_DIRECTION
                        || config == WSM_RENDER_HEIGHT
                        || config == WSM_TARGET_BLOCKS
                        || config == WSM_RENDER_WATERLOGGED
                        || config == WSM_DISTANCE_THRESHOLD))
                {
                    continue;
                }
                // 清海带子子配置：仅在清海带展开时显示
                if (!clearKelpExpanded && (config == WSM_CLEAR_KELP_DISTANCE
                        || config == WSM_CLEAR_KELP_SELECTION_ONLY))
                {
                    continue;
                }
                // 未完成进度显示仅在更好的进度展开时显示
                if (!betterAdvancementsExpanded && config == SHOW_UNCOMPLETED_ADVANCEMENTS)
                {
                    continue;
                }
                // 进度详细信息显示仅在更好的进度展开时显示
                if (!betterAdvancementsExpanded && config == SHOW_ADVANCEMENT_DETAILS)
                {
                    continue;
                }
                // 更大的进度界面仅在更好的进度展开时显示
                if (!betterAdvancementsExpanded && config == BIGGER_ADVANCEMENTS_SCREEN)
                {
                    continue;
                }
                // GUI界面透明度修改子配置：仅在主配置展开时显示
                if (!guiTransparencyExpanded && (config == GUI_OPACITY
                        || config == GUI_BACKGROUND_OPACITY))
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

        // 实体渲染优化：开启后可通过子配置项跳过过远/堆叠实体的渲染
        public static final ConfigBooleanHotkeyed ENTITY_RENDER_OPTIMIZATION =
                new ConfigBooleanHotkeyed("entityRenderOptimization", false, "").apply(OPTIMIZATION_KEY);

        // 实体渲染优化子配置：阻止过远实体渲染（与玩家距离超过阈值的实体不渲染）
        public static final ConfigBooleanHotkeyed SKIP_DISTANT_ENTITIES =
                new ConfigBooleanHotkeyed("skipDistantEntities", false, "").apply(OPTIMIZATION_KEY);

        // 阻止过远实体渲染子子配置：距离阈值（1-128，默认32）
        public static final ConfigInteger SKIP_DISTANT_ENTITIES_DISTANCE =
                new ConfigInteger("skipDistantEntitiesDistance", 32, 1, 128).apply(OPTIMIZATION_KEY);

        // 实体渲染优化子配置：堆叠实体渲染优化（同坐标同类型实体仅渲染一个）
        public static final ConfigBooleanHotkeyed STACK_ENTITY_RENDER_OPTIMIZATION =
                new ConfigBooleanHotkeyed("stackEntityRenderOptimization", false, "").apply(OPTIMIZATION_KEY);

        // 禁用B36渲染：开启后不渲染移动中的活塞技术方块（B36），活塞伸缩动画期间方块不可见
        public static final ConfigBooleanHotkeyed DISABLE_B36_RENDER =
                new ConfigBooleanHotkeyed("disableB36Render", false, "").apply(OPTIMIZATION_KEY);

        // 伪和平优化：同一方块内敌对生物超过70个时移除其全部AI，仅保留对敌对生物总数的判定
        public static final ConfigBooleanHotkeyed FAKE_PEACEFUL =
                new ConfigBooleanHotkeyed("fakePeaceful", false, "").apply(OPTIMIZATION_KEY);

        // 伪和平优化子配置：坚守者听声音不钻地（被移除AI的坚守者保留听到声音刷新钻地冷却的机制）
        public static final ConfigBooleanHotkeyed FAKE_PEACEFUL_WARDEN_HEARING =
                new ConfigBooleanHotkeyed("fakePeacefulWardenHearing", true, "").apply(OPTIMIZATION_KEY);

        // 切门刷怪塔优化：生物生成瞬间碰撞箱碰到下界/末地传送门方块则永久禁用其AI（主世界僵尸猪人豁免）
        public static final ConfigBooleanHotkeyed PORTAL_FARM_OPTIMIZATION =
                new ConfigBooleanHotkeyed("portalFarmOptimization", false, "").apply(OPTIMIZATION_KEY);

        // 列表优化：配置值列表界面行内显示图标、提供搜索选择器（使用 ConfigBoolean，不提供热键绑定）
        public static final ConfigBoolean LIST_OPTIMIZATION =
                new ConfigBoolean("listOptimization", true).apply(OPTIMIZATION_KEY);

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                BOLD_FONT,
                ENTITY_RENDER_OPTIMIZATION,
                SKIP_DISTANT_ENTITIES,
                SKIP_DISTANT_ENTITIES_DISTANCE,
                STACK_ENTITY_RENDER_OPTIMIZATION,
                DISABLE_B36_RENDER,
                FAKE_PEACEFUL,
                FAKE_PEACEFUL_WARDEN_HEARING,
                PORTAL_FARM_OPTIMIZATION,
                LIST_OPTIMIZATION
        );

        /**
         * 返回显示用配置列表。子配置项仅在对应主配置展开时显示。
         * 持久化始终使用 OPTIONS（包含全部配置项）。
         */
        public static List<IConfigBase> getDisplayOptions(boolean entityRenderOptimizationExpanded,
                                                           boolean skipDistantEntitiesExpanded,
                                                           boolean fakePeacefulExpanded)
        {
            if (entityRenderOptimizationExpanded && skipDistantEntitiesExpanded && fakePeacefulExpanded)
            {
                return OPTIONS;
            }
            List<IConfigBase> filtered = new java.util.ArrayList<>();
            for (IConfigBase config : OPTIONS)
            {
                // level1 子配置项仅在 entityRenderOptimization 展开时显示
                if (!entityRenderOptimizationExpanded
                        && (config == SKIP_DISTANT_ENTITIES
                            || config == STACK_ENTITY_RENDER_OPTIMIZATION))
                {
                    continue;
                }
                // skipDistantEntitiesDistance 仅在 skipDistantEntities 展开时显示
                if (!skipDistantEntitiesExpanded && config == SKIP_DISTANT_ENTITIES_DISTANCE)
                {
                    continue;
                }
                // 坚守者听声音不钻地仅在 fakePeaceful 展开时显示
                if (!fakePeacefulExpanded && config == FAKE_PEACEFUL_WARDEN_HEARING)
                {
                    continue;
                }
                filtered.add(config);
            }
            return filtered;
        }
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
