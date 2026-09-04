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

        // 更好的攀爬：攀爬时根据视角方向调整纵向速度
        public static final ConfigBooleanHotkeyed BETTER_CLIMBING =
                new ConfigBooleanHotkeyed("betterClimbing", false, "").apply(TOOLS_KEY);

        // 更好的船：船只可越过高度不大于抬升高度的障碍
        public static final ConfigBooleanHotkeyed BETTER_BOAT =
                new ConfigBooleanHotkeyed("betterBoat", false, "").apply(TOOLS_KEY);

        // 更好的船子配置：抬升高度（0-200，默认0）
        public static final ConfigDouble BOAT_LIFT_HEIGHT =
                new ConfigDouble("boatLiftHeight", 0.0, 0.0, 200.0).apply(TOOLS_KEY);

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

        // 更好的漏斗矿车：容器型主配置，本身只统一管理子功能，不直接提供效果
        public static final ConfigBooleanHotkeyed BETTER_HOPPER_MINECART =
                new ConfigBooleanHotkeyed("betterHopperMinecart", false, "").apply(TOOLS_KEY);

        // 更好的漏斗矿车子配置：吸取范围显示（F3+B 显示碰撞箱时，漏斗矿车额外显示掉落物吸取范围框）
        public static final ConfigBooleanHotkeyed BETTER_HOPPER_MINECART_HITBOX =
                new ConfigBooleanHotkeyed("betterHopperMinecartHitbox", false, "").apply(TOOLS_KEY);

        // 吸取范围显示的子子配置：框线颜色
        public static final ConfigColor HOPPER_SUCK_RANGE_COLOR =
                new ConfigColor("hopperSuckRangeColor", "#00FF00").apply(TOOLS_KEY);

        // 吸取范围显示的子子配置：漏斗矿车锁定显示（被激活铁轨锁定时换色显示）
        public static final ConfigBooleanHotkeyed HOPPER_MINECART_LOCKED_DISPLAY =
                new ConfigBooleanHotkeyed("hopperMinecartLockedDisplay", false, "").apply(TOOLS_KEY);

        // 漏斗矿车锁定显示的子子子配置：锁定时的框线颜色
        public static final ConfigColor HOPPER_LOCKED_COLOR =
                new ConfigColor("hopperLockedColor", "#FF0000").apply(TOOLS_KEY);

        // 更好的漏斗矿车子配置：吸取容器高亮（为漏斗矿车正在吸取的容器描边）
        public static final ConfigBooleanHotkeyed HOPPER_CONTAINER_HIGHLIGHT =
                new ConfigBooleanHotkeyed("hopperContainerHighlight", false, "").apply(TOOLS_KEY);

        // 吸取容器高亮的子配置：描边颜色
        public static final ConfigColor HOPPER_CONTAINER_HIGHLIGHT_COLOR =
                new ConfigColor("hopperContainerHighlightColor", "#00FF00").apply(TOOLS_KEY);

        // 吸取容器高亮的子配置：描边宽度（1-10，默认2）
        public static final ConfigInteger HOPPER_CONTAINER_HIGHLIGHT_WIDTH =
                new ConfigInteger("hopperContainerHighlightWidth", 2, 1, 10).apply(TOOLS_KEY);

        // 放大镜：按住按键放大屏幕画面（滚轮临时调整倍数），默认热键 C
        public static final ConfigBooleanHotkeyed ZOOM =
                new ConfigBooleanHotkeyed("zoom", false, "C").apply(TOOLS_KEY);

        // 放大镜子配置：默认放大倍数（按下按键时使用的倍数，1-32，默认2）
        public static final ConfigDouble ZOOM_DEFAULT_MULTIPLIER =
                new ConfigDouble("zoomDefaultMultiplier", 2.0, 1.0, 32.0).apply(TOOLS_KEY);

        // 放大镜子配置：过渡函数（默认指数缓出，共14种缓动可选）
        public static final ConfigOptionList ZOOM_TRANSITION =
                new ConfigOptionList("zoomTransition", ZoomTransitionMode.EASE_OUT_EXPO).apply(TOOLS_KEY);

        // 放大镜子配置：过渡速度（非即时模式下动画播放速度，动画时长=1000ms/该值，默认1.5）
        public static final ConfigDouble ZOOM_TRANSITION_SPEED =
                new ConfigDouble("zoomTransitionSpeed", 1.5, 0.1, 10.0).apply(TOOLS_KEY);

        public static ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                GLOBAL_MENDING,
                BETTER_TOTEM,
                BETTER_CLIMBING,
                BETTER_BOAT,
                BOAT_LIFT_HEIGHT,
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
                BETTER_HOPPER_MINECART,
                BETTER_HOPPER_MINECART_HITBOX,
                HOPPER_SUCK_RANGE_COLOR,
                HOPPER_MINECART_LOCKED_DISPLAY,
                HOPPER_LOCKED_COLOR,
                HOPPER_CONTAINER_HIGHLIGHT,
                HOPPER_CONTAINER_HIGHLIGHT_COLOR,
                HOPPER_CONTAINER_HIGHLIGHT_WIDTH,
                ZOOM,
                ZOOM_DEFAULT_MULTIPLIER,
                ZOOM_TRANSITION,
                ZOOM_TRANSITION_SPEED
        );

        /**
         * 返回显示用配置列表。子配置项仅在对应主配置展开时显示。
         * 持久化始终使用 OPTIONS（包含全部配置项）。
         */
        public static List<IConfigBase> getDisplayOptions(boolean betterBoatExpanded,
                                                          boolean betterDurabilityExpanded,
                                                          boolean armorHudExpanded,
                                                          boolean visualExperienceExpanded,
                                                          boolean worldSwallowMaintenanceExpanded,
                                                          boolean renderCopyExpanded,
                                                          boolean clearKelpExpanded,
                                                          boolean betterAdvancementsExpanded,
                                                          boolean betterHopperMinecartExpanded,
                                                          boolean suckRangeDisplayExpanded,
                                                          boolean hopperLockedDisplayExpanded,
                                                          boolean hopperContainerHighlightExpanded,
                                                          boolean zoomExpanded)
        {
            if (betterBoatExpanded && betterDurabilityExpanded
                    && armorHudExpanded && visualExperienceExpanded
                    && worldSwallowMaintenanceExpanded && renderCopyExpanded && clearKelpExpanded && betterAdvancementsExpanded
                    && betterHopperMinecartExpanded && suckRangeDisplayExpanded && hopperLockedDisplayExpanded
                    && hopperContainerHighlightExpanded && zoomExpanded)
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
                // 碰撞箱子配置仅在更好的漏斗矿车展开时显示
                if (!betterHopperMinecartExpanded && config == BETTER_HOPPER_MINECART_HITBOX)
                {
                    continue;
                }
                // 框线颜色、漏斗矿车锁定显示仅在吸取范围显示展开时显示
                if (!suckRangeDisplayExpanded
                        && (config == HOPPER_SUCK_RANGE_COLOR || config == HOPPER_MINECART_LOCKED_DISPLAY))
                {
                    continue;
                }
                // 锁定框线颜色仅在漏斗矿车锁定显示展开时显示
                if (!hopperLockedDisplayExpanded && config == HOPPER_LOCKED_COLOR)
                {
                    continue;
                }
                // 吸取容器高亮仅在更好的漏斗矿车展开时显示，其颜色子配置仅在自身展开时显示
                if (!betterHopperMinecartExpanded && config == HOPPER_CONTAINER_HIGHLIGHT)
                {
                    continue;
                }
                if (!hopperContainerHighlightExpanded
                        && (config == HOPPER_CONTAINER_HIGHLIGHT_COLOR || config == HOPPER_CONTAINER_HIGHLIGHT_WIDTH))
                {
                    continue;
                }
                // 放大镜子配置仅在放大镜展开时显示
                if (!zoomExpanded && (config == ZOOM_DEFAULT_MULTIPLIER
                        || config == ZOOM_TRANSITION
                        || config == ZOOM_TRANSITION_SPEED))
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

        public static final ImmutableList<IConfigBase> OPTIONS = ImmutableList.of(
                BOLD_FONT,
                ENTITY_RENDER_OPTIMIZATION,
                SKIP_DISTANT_ENTITIES,
                SKIP_DISTANT_ENTITIES_DISTANCE,
                STACK_ENTITY_RENDER_OPTIMIZATION,
                DISABLE_B36_RENDER
        );

        /**
         * 返回显示用配置列表。子配置项仅在对应主配置展开时显示。
         * 持久化始终使用 OPTIONS（包含全部配置项）。
         */
        public static List<IConfigBase> getDisplayOptions(boolean entityRenderOptimizationExpanded,
                                                           boolean skipDistantEntitiesExpanded)
        {
            if (entityRenderOptimizationExpanded && skipDistantEntitiesExpanded)
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
