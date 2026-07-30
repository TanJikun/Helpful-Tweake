package tanjikun.helpful.tweake.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import fi.dy.masa.malilib.config.IConfigBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase;
import fi.dy.masa.malilib.gui.GuiConfigsBase.ConfigOptionWrapper;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.IConfigGuiAllTab;
import fi.dy.masa.malilib.gui.widgets.WidgetListConfigOptions;
import fi.dy.masa.malilib.util.StringUtils;

import tanjikun.helpful.tweake.Reference;
import tanjikun.helpful.tweake.client.gui.ExpandState;
import tanjikun.helpful.tweake.client.gui.widgets.WidgetListConfigOptionsExpandable;
import tanjikun.helpful.tweake.config.Configs;
import tanjikun.helpful.tweake.config.Hotkeys;

public class GuiConfigs extends GuiConfigsBase implements IConfigGuiAllTab
{
    private static ConfigGuiTab currentTab = ConfigGuiTab.ALL;

    public GuiConfigs()
    {
        super(10, 50, Reference.MOD_ID, null, Reference.MOD_ID_LOWER + ".gui.title.configs");
    }

    @Override
    public void initGui()
    {
        super.initGui();
        this.clearOptions();
        ExpandState.setRefreshCallback(this::refreshList);

        int x = 10;
        int y = 26;
        for (ConfigGuiTab tab : ConfigGuiTab.values())
        {
            if (!this.useAllTab() && tab == ConfigGuiTab.ALL) continue;
            x += this.createButton(x, y, -1, tab);
        }
    }

    private int createButton(int x, int y, int width, ConfigGuiTab tab)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
        button.setEnabled(currentTab != tab);
        this.addButton(button, new ButtonListener(tab, this));
        return button.getWidth() + 2;
    }

    @Override
    protected WidgetListConfigOptions createListWidget(int x, int y)
    {
        return new WidgetListConfigOptionsExpandable(x, y, this.getBrowserWidth(), this.getBrowserHeight(),
                this.getConfigWidth(), 0.0F, this.useKeybindSearch(), this);
    }

    public void refreshList()
    {
        this.reCreateListWidget();
        Objects.requireNonNull(this.getListWidget()).resetScrollbarPosition();
        this.initGui();
    }

    @Override
    public List<ConfigOptionWrapper> getConfigs()
    {
        List<? extends IConfigBase> configs;

        if (currentTab == ConfigGuiTab.ALL && this.useAllTab())
        {
            return this.getAllConfigs();
        }
        else if (currentTab == ConfigGuiTab.TOOLS)
        {
            configs = Configs.Tools.getDisplayOptions(ExpandState.betterBoatExpanded, ExpandState.betterDurabilityExpanded);
        }
        else if (currentTab == ConfigGuiTab.OPTIMIZATION)
        {
            configs = Configs.Optimization.OPTIONS;
        }
        else
        {
            configs = Collections.emptyList();
        }

        return ConfigOptionWrapper.createFor(configs);
    }

    @Override
    public boolean useAllTab()
    {
        return true;
    }

    @Override
    public List<ConfigOptionWrapper> getAllConfigs()
    {
        List<ConfigOptionWrapper> configs = new ArrayList<>();
        configs.addAll(ConfigOptionWrapper.createFor(Configs.Tools.getDisplayOptions(ExpandState.betterBoatExpanded, ExpandState.betterDurabilityExpanded)));
        configs.addAll(ConfigOptionWrapper.createFor(Configs.Optimization.OPTIONS));
        configs.addAll(ConfigOptionWrapper.createFor(Hotkeys.HOTKEY_LIST));
        return configs;
    }

    public enum ConfigGuiTab
    {
        ALL          (IConfigGuiAllTab.getTranslationKey()),
        TOOLS        (Reference.MOD_ID_LOWER + ".gui.button.config_gui.tools"),
        OPTIMIZATION (Reference.MOD_ID_LOWER + ".gui.button.config_gui.optimization");

        private final String translationKey;

        ConfigGuiTab(String translationKey)
        {
            this.translationKey = translationKey;
        }

        public String getDisplayName()
        {
            return StringUtils.translate(this.translationKey);
        }
    }

    private record ButtonListener(ConfigGuiTab tab, GuiConfigs parent) implements IButtonActionListener
    {
        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton)
        {
            currentTab = this.tab;
            this.parent.reCreateListWidget();
            Objects.requireNonNull(this.parent.getListWidget()).resetScrollbarPosition();
            this.parent.initGui();
        }
    }
}
