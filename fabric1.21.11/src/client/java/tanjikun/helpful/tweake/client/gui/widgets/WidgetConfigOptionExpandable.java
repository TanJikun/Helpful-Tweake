package tanjikun.helpful.tweake.client.gui.widgets;

import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.config.IConfigBoolean;
import fi.dy.masa.malilib.config.IConfigResettable;
import fi.dy.masa.malilib.config.IConfigSlider;
import fi.dy.masa.malilib.config.IConfigValue;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IKeybindConfigGui;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetConfigOption;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptionsBase;
import fi.dy.masa.malilib.gui.wrappers.TextFieldType;
import fi.dy.masa.malilib.hotkeys.IHotkey;
import fi.dy.masa.malilib.hotkeys.IKeybind;

import tanjikun.helpful.tweake.client.gui.ExpandState;
import tanjikun.helpful.tweake.config.Configs;

/**
 * 在指定配置行（ConfigBooleanHotkeyed 或 ConfigHotkey）的左侧添加展开/折叠按钮。
 * 命中表：
 *   ConfigBooleanHotkeyed —— BETTER_BOAT / BETTER_DURABILITY / BETTER_HARVEST / ARMOR_HUD
 *                             / VISUAL_EXPERIENCE / WORLD_SWALLOW_MAINTENANCE / WSM_CLEAR_KELP
 *   ConfigHotkey          —— CONTROLLED_CRAWL
 * 其他配置项走原版渲染逻辑。
 *
 * 所有配置项的控件（开关、热键、输入框等）统一右移 16px，与有展开按钮的配置项对齐。
 * 子配置项额外再缩进 16px（共 32px），子子配置项再缩进 16px（共 48px），与标签文字对齐。
 * 当 Litematica 未安装时，清海带及其子子配置项的标签显示为红色、按钮/文本框禁用。
 * 单机模式专用功能（更方便的收获）的标签也显示为红色。
 */
public class WidgetConfigOptionExpandable extends WidgetConfigOption
{
    private static final int CONTROL_INDENT = 16;

    // 依赖 Litematica 模组的配置项名称
    private static final Set<String> LITEMATICA_DEPENDENT_NAMES =
            Set.of("wsmClearKelp", "wsmClearKelpDistance");

    // 仅单机模式可用的配置项名称
    private static final Set<String> SINGLEPLAYER_ONLY_NAMES =
            Set.of("betterHarvest");

    // 子配置项名称（控件额外缩进 16px）
    private static final Set<String> SUB_CONFIG_NAMES = Set.of(
            "boatLiftHeight",
            "durabilityUnbreakingCalc",
            "harvestRequireHoe", "harvestHoeDurability", "harvestBlacklist",
            "armorHudPosition", "armorHudX", "armorHudY",
            "crawlTriggerMode",
            "experienceTextColor",
            "wsmRenderHeight", "wsmTargetBlocks", "wsmRenderWaterlogged",
            "wsmDistanceThreshold", "wsmTransparentBedrock", "wsmClearKelp"
    );

    // 子子配置项名称（控件额外缩进 32px）
    private static final Set<String> SUB_SUB_CONFIG_NAMES = Set.of(
            "wsmClearKelpDistance"
    );

    /**
     * 标记当前配置项是否为 slider/textfield 类型（即拥有切换按钮）。
     * 用于 addConfigButtonEntry 区分切换按钮与普通按钮：
     * - 切换按钮的 x 参数（colorDisplayPosX）已在 addConfigSliderEntry/addConfigTextFieldEntry 中右移，
     *   不应在 addConfigButtonEntry 中再次右移。
     * - 普通按钮（文本列表等）的 x 参数未右移，需在 addConfigButtonEntry 中右移。
     */
    private boolean isSliderOrTextField;

    public WidgetConfigOptionExpandable(int x, int y, int browserEntryWidth, int browserEntryHeight,
                                        int maxLabelWidth, int configWidth, ConfigOptionWrapper wrapper,
                                        int listWidgetWidth, IKeybindConfigGui host,
                                        WidgetListConfigOptionsBase<?, ?> parent)
    {
        super(x, y, browserEntryWidth, browserEntryHeight, maxLabelWidth, configWidth,
              wrapper, listWidgetWidth, host, parent);

        // super 构造完成后，禁用 Litematica 依赖配置项的所有交互控件
        if (isLitematicaMissing() && isCurrentConfigLitematicaDependent())
        {
            for (WidgetBase w : this.subWidgets)
            {
                if (w instanceof ButtonBase b)
                {
                    b.setEnabled(false);
                }
            }
            if (this.textField != null)
            {
                this.textField.textField().active = false;
            }
        }
    }

    /**
     * 返回当前配置项的层级深度：0 = 主配置项，1 = 子配置项，2 = 子子配置项。
     */
    private int getConfigLevel()
    {
        if (this.wrapper == null)
        {
            return 0;
        }
        IConfigBase config = this.wrapper.getConfig();
        if (config == null)
        {
            return 0;
        }
        String name = config.getName();
        if (SUB_SUB_CONFIG_NAMES.contains(name))
        {
            return 2;
        }
        if (SUB_CONFIG_NAMES.contains(name))
        {
            return 1;
        }
        return 0;
    }

    /**
     * 控件总缩进量 = CONTROL_INDENT（全局对齐）+ getConfigLevel() * CONTROL_INDENT（层级缩进）。
     */
    private int getIndent()
    {
        return (1 + getConfigLevel()) * CONTROL_INDENT;
    }

    @Override
    protected void addBooleanAndHotkeyWidgets(int x, int y, int w,
                                              IConfigResettable configResettable,
                                              IConfigBoolean configBoolean, IKeybind keybind)
    {
        if (configBoolean == Configs.Tools.BETTER_BOAT)
        {
            addExpandButton(x, y, w, configResettable, configBoolean, keybind,
                    ExpandState.betterBoatExpanded, ExpandState::toggleBetterBoat);
        }
        else if (configBoolean == Configs.Tools.BETTER_DURABILITY)
        {
            addExpandButton(x, y, w, configResettable, configBoolean, keybind,
                    ExpandState.betterDurabilityExpanded, ExpandState::toggleBetterDurability);
        }
        else if (configBoolean == Configs.Tools.BETTER_HARVEST)
        {
            addExpandButton(x, y, w, configResettable, configBoolean, keybind,
                    ExpandState.betterHarvestExpanded, ExpandState::toggleBetterHarvest);
        }
        else if (configBoolean == Configs.Tools.ARMOR_HUD)
        {
            addExpandButton(x, y, w, configResettable, configBoolean, keybind,
                    ExpandState.armorHudExpanded, ExpandState::toggleArmorHud);
        }
        else if (configBoolean == Configs.Tools.VISUAL_EXPERIENCE)
        {
            addExpandButton(x, y, w, configResettable, configBoolean, keybind,
                    ExpandState.visualExperienceExpanded, ExpandState::toggleVisualExperience);
        }
        else if (configBoolean == Configs.Tools.WORLD_SWALLOW_MAINTENANCE)
        {
            addExpandButton(x, y, w, configResettable, configBoolean, keybind,
                    ExpandState.worldSwallowMaintenanceExpanded, ExpandState::toggleWorldSwallowMaintenance);
        }
        else if (configBoolean == Configs.Tools.WSM_CLEAR_KELP)
        {
            // wsmClearKelp 是子配置项（level 1），需额外缩进 getIndent() - CONTROL_INDENT = 16px
            addExpandButton(x, y, w, configResettable, configBoolean, keybind,
                    ExpandState.clearKelpExpanded, ExpandState::toggleClearKelp,
                    getIndent() - CONTROL_INDENT);
        }
        else
        {
            int indent = getIndent();
            super.addBooleanAndHotkeyWidgets(x + indent, y, w - indent,
                    configResettable, configBoolean, keybind);
        }
    }

    @Override
    protected void addHotkeyConfigElements(int x, int y, int w, String label, IHotkey hotkey)
    {
        // CONTROLLED_CRAWL 是 ConfigHotkey（仅热键无 boolean 开关），单独加展开按钮
        if (hotkey == Configs.Tools.CONTROLLED_CRAWL)
        {
            ButtonGeneric expandBtn = new ButtonGeneric(x, y, 14, 20,
                    ExpandState.controlledCrawlExpanded ? "-" : "+");
            this.addButton(expandBtn, new ExpandButtonListener(ExpandState::toggleControlledCrawl));
            super.addHotkeyConfigElements(x + 16, y, w - 16, label, hotkey);
        }
        else
        {
            int indent = getIndent();
            super.addHotkeyConfigElements(x + indent, y, w - indent, label, hotkey);
        }
    }

    @Override
    protected void addConfigTextFieldEntry(int x, int y, int w, int h, int configWidth,
                                           IConfigValue config, TextFieldType type)
    {
        int indent = getIndent();
        this.isSliderOrTextField = true;
        // colorDisplayPosX 是父类 addConfigOption 中在调用本方法前写入的切换按钮 x 坐标，
        // 右移 indent 以匹配文本框偏移
        this.colorDisplayPosX += indent;
        super.addConfigTextFieldEntry(x + indent, y, w + indent, h, configWidth, config, type);
    }

    @Override
    protected void addConfigSliderEntry(int x, int y, int w, int h, int configWidth, IConfigSlider config)
    {
        int indent = getIndent();
        this.isSliderOrTextField = true;
        // colorDisplayPosX 是父类 addConfigOption 中在调用本方法前写入的切换按钮 x 坐标，
        // 右移 indent 以匹配滑块偏移
        this.colorDisplayPosX += indent;
        super.addConfigSliderEntry(x + indent, y, w + indent, h, configWidth, config);
    }

    @Override
    protected void addConfigButtonEntry(int x, int y, IConfigResettable configResettable, ButtonBase button)
    {
        int indent = getIndent();
        if (this.isSliderOrTextField)
        {
            // 切换按钮：colorDisplayPosX 已在 addConfigSliderEntry/addConfigTextFieldEntry 中右移，
            // 因此 x 参数（=colorDisplayPosX）已包含缩进，主按钮位置也正确，无需再次右移
            super.addConfigButtonEntry(x, y, configResettable, button);
            this.isSliderOrTextField = false;
        }
        else
        {
            // 普通按钮（文本列表、布尔值、选项列表、锁定列表、颜色列表等）：
            // 主按钮右移并缩小宽度，重置按钮右移
            button.setPosition(button.getX() + indent, button.getY());
            button.setWidth(button.getWidth() - indent);
            super.addConfigButtonEntry(x + indent, y, configResettable, button);
        }
    }

    @Override
    protected void addKeybindResetButton(int x, int y, IKeybind keybind,
                                         fi.dy.masa.malilib.gui.button.ConfigButtonKeybind keybindButton)
    {
        // x 参数已在 addHotkeyConfigElements 的右移参数中由父类内部计算得出，
        // 父类用已右移的 x 和缩减后的 w 计算重置按钮位置，因此 x 已包含缩进，无需再次右移。
        super.addKeybindResetButton(x, y, keybind, keybindButton);
    }

    /**
     * 覆盖 addLabel：Litematica 未安装时，依赖它的配置项标签显示为红色。
     * 仅单机模式可用的配置项（如更方便的收获）标签也显示为红色。
     * addLabel 在 super 构造期间被调用，此时 this.wrapper 已由父类赋值，可安全访问。
     */
    @Override
    protected void addLabel(int x, int y, int width, int height, int textColor, String... lines)
    {
        if (isCurrentConfigLabelRed())
        {
            textColor = 0xFFFF5555; // 红色
        }
        super.addLabel(x, y, width, height, textColor, lines);
    }

    /**
     * 覆盖 addConfigComment：依赖 Litematica 或单机模式专用的配置项，
     * 其悬停提示文字也显示为红色。
     */
    @Override
    protected void addConfigComment(int x, int y, int width, int height, String comment)
    {
        if (isCurrentConfigLabelRed() && comment != null && !comment.isEmpty())
        {
            // 在注释文本前追加红色标记（MaLiLib 的 WidgetHoverInfo 支持 § 颜色代码）
            comment = "§c" + comment;
        }
        super.addConfigComment(x, y, width, height, comment);
    }

    private void addExpandButton(int x, int y, int w,
                                 IConfigResettable configResettable,
                                 IConfigBoolean configBoolean, IKeybind keybind,
                                 boolean expanded, Runnable toggleAction)
    {
        addExpandButton(x, y, w, configResettable, configBoolean, keybind,
                expanded, toggleAction, 0);
    }

    private void addExpandButton(int x, int y, int w,
                                 IConfigResettable configResettable,
                                 IConfigBoolean configBoolean, IKeybind keybind,
                                 boolean expanded, Runnable toggleAction,
                                 int extraIndent)
    {
        ButtonGeneric expandBtn = new ButtonGeneric(x + extraIndent, y, 14, 20, expanded ? "-" : "+");
        this.addButton(expandBtn, new ExpandButtonListener(toggleAction));
        super.addBooleanAndHotkeyWidgets(x + 16 + extraIndent, y, w - 16 - extraIndent,
                configResettable, configBoolean, keybind);
    }

    private static boolean isLitematicaMissing()
    {
        return !FabricLoader.getInstance().isModLoaded("litematica");
    }

    private boolean isCurrentConfigLitematicaDependent()
    {
        if (this.wrapper == null)
        {
            return false;
        }
        IConfigBase config = this.wrapper.getConfig();
        return config != null && LITEMATICA_DEPENDENT_NAMES.contains(config.getName());
    }

    /**
     * 判断当前配置项标签是否需要显示为红色。
     * 条件：Litematica 未安装 + 依赖 Litematica，或为单机模式专用配置项。
     */
    private boolean isCurrentConfigLabelRed()
    {
        if (isLitematicaMissing() && isCurrentConfigLitematicaDependent())
        {
            return true;
        }
        if (this.wrapper == null)
        {
            return false;
        }
        IConfigBase config = this.wrapper.getConfig();
        return config != null && SINGLEPLAYER_ONLY_NAMES.contains(config.getName());
    }

    private record ExpandButtonListener(Runnable toggleAction) implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            toggleAction.run();
        }
    }
}