package top.worldme.territory.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.territory.data.PermissionFlag;
import top.worldme.territory.data.SettingFlag;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TerritoryMenuConfig {

    private final JavaPlugin plugin;
    private final File menuFile;

    // 通用背景
    public ItemConfig background = item("GRAY_STAINED_GLASS_PANE", " ");
    public boolean fillBackground = true;

    // 领地列表
    public String listTitle = "<gold><bold>我的领地";
    public int listRows = 6;
    public final List<Integer> listContentSlots = new ArrayList<>(List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43));
    public ItemConfig listItem = item("OAK_DOOR", "<green>%name%",
            "<gray>世界：<white>%world%",
            "<gray>范围：<white>%bounds%",
            "<gray>体积：<white>%volume%</white> 单位",
            "<gray>成员：<white>%members%</white> 人",
            "<gray>传送点：%warp%",
            "",
            "<yellow>点击管理");
    public int listInfoSlot = 4;
    public ItemConfig listInfo = item("COMPASS", "<aqua>我的领地",
            "<gray>数量：<yellow>%count%/%max%",
            "<gray>每单位体积：<yellow>16×16×16");
    public int createSlot = 48;
    public ItemConfig createButton = item("GRASS_BLOCK", "<green>创建领地",
            "<gray>认领当前所在的 16³ 单元",
            "<gray>费用：<yellow>%cost%");
    public int listPrevSlot = 45;
    public ItemConfig listPrevButton = item("ARROW", "<gray>上一页", "<gray>点击查看上一页");
    public int listNextSlot = 53;
    public ItemConfig listNextButton = item("ARROW", "<gray>下一页", "<gray>点击查看下一页");
    public int listCloseSlot = 49;
    public ItemConfig listCloseButton = item("BARRIER", "<red>关闭");

    // 领地管理
    public String detailTitle = "<gold>管理 · %name%";
    public int detailRows = 4;
    public int detailInfoSlot = 4;
    public ItemConfig detailInfo = item("MAP", "<aqua>%name%",
            "<gray>世界：<white>%world%",
            "<gray>范围：<white>%bounds%",
            "<gray>体积：<white>%volume%</white> 单位",
            "<gray>成员：<white>%members%</white> 人",
            "<gray>传送点：%warp%");
    public int detailExpandSlot = 19;
    public ItemConfig detailExpand = item("IRON_PICKAXE", "<green>扩展领地",
            "<gray>按方向扩展领地大小",
            "<gray>当前体积：<white>%volume%</white> 单位",
            "<yellow>点击选择方向");
    public int detailTeleportSlot = 20;
    public ItemConfig detailTeleport = item("ENDER_PEARL", "<green>传送到领地", "<gray>传送到领地默认传送点");
    public int detailSethomeSlot = 21;
    public ItemConfig detailSethome = item("LODESTONE", "<green>设置传送点", "<gray>将当前位置设为领地传送点");
    public int detailEnterSlot = 22;
    public ItemConfig detailEnter = item("LIME_BANNER", "<green>设置进入消息",
            "<gray>当前：%enter%", "<yellow>点击输入");
    public int detailLeaveSlot = 23;
    public ItemConfig detailLeave = item("YELLOW_BANNER", "<yellow>设置离开消息",
            "<gray>当前：%leave%", "<yellow>点击输入");
    public int detailNameSlot = 24;
    public ItemConfig detailName = item("NAME_TAG", "<gold>领地改名",
            "<gray>当前：<white>%name%", "<yellow>点击输入");
    public int detailMembersSlot = 25;
    public ItemConfig detailMembers = item("PLAYER_HEAD", "<aqua>领地成员",
            "<gray>成员：<white>%members%</white> 人",
            "<yellow>点击管理");
    public int detailSettingsSlot = 26;
    public ItemConfig detailSettings = item("REDSTONE_COMPARATOR", "<yellow>领地设置",
            "<gray>刷怪、爆炸、火焰等规则",
            "<yellow>点击设置");
    public int detailUnclaimSlot = 27;
    public ItemConfig detailUnclaim = item("TNT", "<dark_red>删除领地", "<red>Shift+左键 确认删除");
    public int detailBackSlot = 31;
    public ItemConfig detailBack = item("ARROW", "<gray>返回领地列表");

    // 扩展方向
    public String expandTitle = "<green>扩展 · %name%";
    public int expandRows = 3;
    public int expandInfoSlot = 4;
    public ItemConfig expandInfo = item("COMPASS", "<aqua>选择扩展方向",
            "<gray>当前体积：<white>%volume%</white> 单位",
            "<gray>每单位：<yellow>%unit_cost%");
    public int expandNorthSlot = 10;
    public ItemConfig expandNorth = item("LIGHT_BLUE_DYE", "<aqua>向北扩展");
    public int expandSouthSlot = 12;
    public ItemConfig expandSouth = item("ORANGE_DYE", "<gold>向南扩展");
    public int expandWestSlot = 14;
    public ItemConfig expandWest = item("WHITE_DYE", "<white>向西扩展");
    public int expandEastSlot = 16;
    public ItemConfig expandEast = item("GRAY_DYE", "<gray>向东扩展");
    public int expandUpSlot = 20;
    public ItemConfig expandUp = item("FEATHER", "<green>向上扩展");
    public int expandDownSlot = 24;
    public ItemConfig expandDown = item("SCAFFOLDING", "<dark_green>向下扩展");
    public int expandBackSlot = 22;
    public ItemConfig expandBack = item("ARROW", "<gray>返回领地管理");
    public int expandStep = 1;

    // 领地设置
    public String settingsTitle = "<gold>领地设置 · %name%";
    public int settingsRows = 3;
    public int settingsInfoSlot = 4;
    public ItemConfig settingsInfo = item("COMPASS", "<aqua>领地设置",
            "<gray>左键开启 / 右键关闭");
    public final Map<SettingFlag, Integer> settingsSlots = new LinkedHashMap<>();
    public final Map<SettingFlag, ItemConfig> settingsItems = new LinkedHashMap<>();
    public int settingsBackSlot = 22;
    public ItemConfig settingsBack = item("ARROW", "<gray>返回领地管理");

    // 成员列表
    public String membersTitle = "<gold>领地成员 · %name%";
    public int membersRows = 6;
    public final List<Integer> membersContentSlots = new ArrayList<>(List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43));
    public ItemConfig membersItem = item("PLAYER_HEAD", "<aqua>%player%",
            "<gray>点击设置权限");
    public int membersInfoSlot = 4;
    public ItemConfig membersInfo = item("PLAYER_HEAD", "<aqua>%name%",
            "<gray>成员：<white>%count%</white> 人");
    public int membersAddSlot = 48;
    public ItemConfig membersAdd = item("WRITABLE_BOOK", "<green>添加成员",
            "<gray>点击输入玩家名称");
    public int membersBackSlot = 49;
    public ItemConfig membersBack = item("ARROW", "<gray>返回领地管理");

    // 成员权限
    public String memberPermissionsTitle = "<gold>成员权限 · %player%";
    public int memberPermissionsRows = 3;
    public int memberPermissionsInfoSlot = 4;
    public ItemConfig memberPermissionsInfo = item("PAPER", "<aqua>%player%",
            "<gray>左键授予 / 右键取消");
    public final Map<PermissionFlag, Integer> permissionSlots = new LinkedHashMap<>();
    public final Map<PermissionFlag, ItemConfig> permissionItems = new LinkedHashMap<>();
    public int memberPermissionsRemoveSlot = 20;
    public ItemConfig memberPermissionsRemove = item("TNT", "<dark_red>移除成员", "<red>Shift+左键 确认移除");
    public int memberPermissionsBackSlot = 22;
    public ItemConfig memberPermissionsBack = item("ARROW", "<gray>返回成员列表");

    // 对话框
    public DialogConfig nameDialog = dialog("<gold>领地改名", "<gray>新名称", 300, 24);
    public DialogConfig enterDialog = dialog("<gold>进入消息", "<gray>留空使用默认", 300, 64);
    public DialogConfig leaveDialog = dialog("<gold>离开消息", "<gray>留空使用默认", 300, 64);
    public DialogConfig memberDialog = dialog("<gold>添加成员", "<gray>玩家名称", 300, 16);

    public TerritoryMenuConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.menuFile = new File(plugin.getDataFolder(), "menu.yml");
        load();
    }

    public void load() {
        if (!menuFile.exists()) {
            plugin.saveResource("menu.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(menuFile);

        ConfigurationSection bg = config.getConfigurationSection("background");
        if (bg != null) {
            this.background = readItem(bg);
            this.fillBackground = bg.getBoolean("fill", true);
        }

        ConfigurationSection list = config.getConfigurationSection("list");
        if (list != null) {
            this.listTitle = list.getString("title", this.listTitle);
            this.listRows = clampRows(list.getInt("rows", this.listRows));
            List<Integer> slots = list.getIntegerList("content-slots");
            if (!slots.isEmpty()) {
                this.listContentSlots.clear();
                this.listContentSlots.addAll(slots);
            }
            this.listItem = readItemIfPresent(list, "item", this.listItem);
            ConfigurationSection items = list.getConfigurationSection("items");
            if (items != null) {
                this.listInfoSlot = readItemSlot(items, "info", this.listInfoSlot);
                this.listInfo = readItemIfPresent(items, "info", this.listInfo);
                this.createSlot = readItemSlot(items, "create", this.createSlot);
                this.createButton = readItemIfPresent(items, "create", this.createButton);
                this.listPrevSlot = readItemSlot(items, "prev-button", this.listPrevSlot);
                this.listPrevButton = readItemIfPresent(items, "prev-button", this.listPrevButton);
                this.listNextSlot = readItemSlot(items, "next-button", this.listNextSlot);
                this.listNextButton = readItemIfPresent(items, "next-button", this.listNextButton);
                this.listCloseSlot = readItemSlot(items, "close-button", this.listCloseSlot);
                this.listCloseButton = readItemIfPresent(items, "close-button", this.listCloseButton);
            }
        }

        ConfigurationSection detail = config.getConfigurationSection("detail");
        if (detail != null) {
            this.detailTitle = detail.getString("title", this.detailTitle);
            this.detailRows = clampRows(detail.getInt("rows", this.detailRows));
            ConfigurationSection items = detail.getConfigurationSection("items");
            if (items != null) {
                this.detailInfoSlot = readItemSlot(items, "info", this.detailInfoSlot);
                this.detailInfo = readItemIfPresent(items, "info", this.detailInfo);
                this.detailExpandSlot = readItemSlot(items, "expand", this.detailExpandSlot);
                this.detailExpand = readItemIfPresent(items, "expand", this.detailExpand);
                this.detailTeleportSlot = readItemSlot(items, "teleport", this.detailTeleportSlot);
                this.detailTeleport = readItemIfPresent(items, "teleport", this.detailTeleport);
                this.detailSethomeSlot = readItemSlot(items, "sethome", this.detailSethomeSlot);
                this.detailSethome = readItemIfPresent(items, "sethome", this.detailSethome);
                this.detailEnterSlot = readItemSlot(items, "enter-message", this.detailEnterSlot);
                this.detailEnter = readItemIfPresent(items, "enter-message", this.detailEnter);
                this.detailLeaveSlot = readItemSlot(items, "leave-message", this.detailLeaveSlot);
                this.detailLeave = readItemIfPresent(items, "leave-message", this.detailLeave);
                this.detailNameSlot = readItemSlot(items, "name", this.detailNameSlot);
                this.detailName = readItemIfPresent(items, "name", this.detailName);
                this.detailMembersSlot = readItemSlot(items, "members", this.detailMembersSlot);
                this.detailMembers = readItemIfPresent(items, "members", this.detailMembers);
                this.detailSettingsSlot = readItemSlot(items, "settings", this.detailSettingsSlot);
                this.detailSettings = readItemIfPresent(items, "settings", this.detailSettings);
                this.detailUnclaimSlot = readItemSlot(items, "unclaim", this.detailUnclaimSlot);
                this.detailUnclaim = readItemIfPresent(items, "unclaim", this.detailUnclaim);
                this.detailBackSlot = readItemSlot(items, "back", this.detailBackSlot);
                this.detailBack = readItemIfPresent(items, "back", this.detailBack);
            }
        }

        ConfigurationSection expand = config.getConfigurationSection("expand");
        if (expand != null) {
            this.expandTitle = expand.getString("title", this.expandTitle);
            this.expandRows = clampRows(expand.getInt("rows", this.expandRows));
            ConfigurationSection items = expand.getConfigurationSection("items");
            if (items != null) {
                this.expandInfoSlot = readItemSlot(items, "info", this.expandInfoSlot);
                this.expandInfo = readItemIfPresent(items, "info", this.expandInfo);
                this.expandNorthSlot = readItemSlot(items, "north", this.expandNorthSlot);
                this.expandNorth = readItemIfPresent(items, "north", this.expandNorth);
                this.expandSouthSlot = readItemSlot(items, "south", this.expandSouthSlot);
                this.expandSouth = readItemIfPresent(items, "south", this.expandSouth);
                this.expandWestSlot = readItemSlot(items, "west", this.expandWestSlot);
                this.expandWest = readItemIfPresent(items, "west", this.expandWest);
                this.expandEastSlot = readItemSlot(items, "east", this.expandEastSlot);
                this.expandEast = readItemIfPresent(items, "east", this.expandEast);
                this.expandUpSlot = readItemSlot(items, "up", this.expandUpSlot);
                this.expandUp = readItemIfPresent(items, "up", this.expandUp);
                this.expandDownSlot = readItemSlot(items, "down", this.expandDownSlot);
                this.expandDown = readItemIfPresent(items, "down", this.expandDown);
                this.expandBackSlot = readItemSlot(items, "back", this.expandBackSlot);
                this.expandBack = readItemIfPresent(items, "back", this.expandBack);
            }
            this.expandStep = expand.getInt("expand-step", this.expandStep);
        }
        this.expandStep = Math.max(1, config.getInt("expand-step", this.expandStep));

        readSettings(config.getConfigurationSection("settings"));
        readMembers(config.getConfigurationSection("members"));
        readPermissionItems(config.getConfigurationSection("member-permissions"));

        ConfigurationSection dialogs = config.getConfigurationSection("dialogs");
        if (dialogs != null) {
            readDialog(dialogs.getConfigurationSection("name"), this.nameDialog);
            readDialog(dialogs.getConfigurationSection("enter"), this.enterDialog);
            readDialog(dialogs.getConfigurationSection("leave"), this.leaveDialog);
            readDialog(dialogs.getConfigurationSection("member"), this.memberDialog);
        }
    }

    private void readSettings(ConfigurationSection section) {
        settingsSlots.clear();
        settingsItems.clear();
        ConfigurationSection items = section == null ? null : section.getConfigurationSection("items");
        int fallbackSlot = 10;
        for (SettingFlag flag : SettingFlag.values()) {
            ItemConfig fallback = item("LIME_DYE", "<yellow>" + flag.name(),
                    "<gray>当前：%state%");
            ConfigurationSection itemSection = items == null ? null : items.getConfigurationSection(flag.name());
            int slot = fallbackSlot;
            ItemConfig item = fallback;
            if (itemSection != null) {
                slot = itemSection.getInt("slot", fallbackSlot);
                item = readItemWithLore(itemSection, fallback);
            }
            settingsSlots.put(flag, slot);
            settingsItems.put(flag, item);
            fallbackSlot++;
            if (fallbackSlot == 17) {
                fallbackSlot = 19;
            }
        }
        if (section != null) {
            this.settingsTitle = section.getString("title", this.settingsTitle);
            this.settingsRows = clampRows(section.getInt("rows", this.settingsRows));
            ConfigurationSection info = section.getConfigurationSection("items.info");
            if (info != null) {
                this.settingsInfoSlot = info.getInt("slot", this.settingsInfoSlot);
                this.settingsInfo = readItemWithLore(info, this.settingsInfo);
            }
            ConfigurationSection back = section.getConfigurationSection("items.back");
            if (back != null) {
                this.settingsBackSlot = back.getInt("slot", this.settingsBackSlot);
                this.settingsBack = readItemWithLore(back, this.settingsBack);
            }
        }
    }

    private void readMembers(ConfigurationSection section) {
        if (section == null) {
            return;
        }
        this.membersTitle = section.getString("title", this.membersTitle);
        this.membersRows = clampRows(section.getInt("rows", this.membersRows));
        List<Integer> slots = section.getIntegerList("content-slots");
        if (!slots.isEmpty()) {
            this.membersContentSlots.clear();
            this.membersContentSlots.addAll(slots);
        }
        this.membersItem = readItemIfPresent(section, "item", this.membersItem);
        ConfigurationSection items = section.getConfigurationSection("items");
        if (items != null) {
            this.membersInfoSlot = readItemSlot(items, "info", this.membersInfoSlot);
            this.membersInfo = readItemIfPresent(items, "info", this.membersInfo);
            this.membersAddSlot = readItemSlot(items, "add", this.membersAddSlot);
            this.membersAdd = readItemIfPresent(items, "add", this.membersAdd);
            this.membersBackSlot = readItemSlot(items, "back", this.membersBackSlot);
            this.membersBack = readItemIfPresent(items, "back", this.membersBack);
        }
    }

    private void readPermissionItems(ConfigurationSection section) {
        permissionSlots.clear();
        permissionItems.clear();
        ConfigurationSection items = section == null ? null : section.getConfigurationSection("items");
        int fallbackSlot = 10;
        for (PermissionFlag flag : PermissionFlag.values()) {
            ItemConfig fallback = item("PAPER", "<yellow>" + flag.name(), "<gray>当前：%state%");
            ConfigurationSection itemSection = items == null ? null : items.getConfigurationSection(flag.name());
            int slot = fallbackSlot;
            ItemConfig item = fallback;
            if (itemSection != null) {
                slot = itemSection.getInt("slot", fallbackSlot);
                item = readItemWithLore(itemSection, fallback);
            }
            permissionSlots.put(flag, slot);
            permissionItems.put(flag, item);
            fallbackSlot++;
        }
        if (section != null) {
            this.memberPermissionsTitle = section.getString("title", this.memberPermissionsTitle);
            this.memberPermissionsRows = clampRows(section.getInt("rows", this.memberPermissionsRows));
            ConfigurationSection info = section.getConfigurationSection("items.info");
            if (info != null) {
                this.memberPermissionsInfoSlot = info.getInt("slot", this.memberPermissionsInfoSlot);
                this.memberPermissionsInfo = readItemWithLore(info, this.memberPermissionsInfo);
            }
            ConfigurationSection remove = section.getConfigurationSection("items.remove");
            if (remove != null) {
                this.memberPermissionsRemoveSlot = remove.getInt("slot", this.memberPermissionsRemoveSlot);
                this.memberPermissionsRemove = readItemWithLore(remove, this.memberPermissionsRemove);
            }
            ConfigurationSection back = section.getConfigurationSection("items.back");
            if (back != null) {
                this.memberPermissionsBackSlot = back.getInt("slot", this.memberPermissionsBackSlot);
                this.memberPermissionsBack = readItemWithLore(back, this.memberPermissionsBack);
            }
        }
    }

    private void readDialog(ConfigurationSection section, DialogConfig dialog) {
        if (section == null) {
            return;
        }
        dialog.title = section.getString("title", dialog.title);
        dialog.label = section.getString("label", dialog.label);
        dialog.confirm = section.getString("confirm", dialog.confirm);
        dialog.cancel = section.getString("cancel", dialog.cancel);
        dialog.width = section.getInt("width", dialog.width);
        dialog.maxLength = Math.max(1, section.getInt("max-length", dialog.maxLength));
    }

    private int readItemSlot(ConfigurationSection parent, String name, int fallback) {
        ConfigurationSection section = parent.getConfigurationSection(name);
        return section == null ? fallback : section.getInt("slot", fallback);
    }

    private ItemConfig readItemIfPresent(ConfigurationSection parent, String name, ItemConfig fallback) {
        ConfigurationSection section = parent.getConfigurationSection(name);
        return section == null ? fallback : readItemWithLore(section, fallback);
    }

    private ItemConfig readItem(ConfigurationSection section) {
        return readItemWithLore(section, new ItemConfig());
    }

    private ItemConfig readItemWithLore(ConfigurationSection section, ItemConfig fallback) {
        ItemConfig item = new ItemConfig();
        item.material = section.getString("material", fallback.material);
        item.materialOn = section.getString("material-on", fallback.materialOn);
        item.materialOff = section.getString("material-off", fallback.materialOff);
        item.name = section.getString("name", fallback.name);
        item.lore.clear();
        List<String> lore = section.getStringList("lore");
        if (lore.isEmpty()) {
            item.lore.addAll(fallback.lore);
        } else {
            item.lore.addAll(lore);
        }
        return item;
    }

    private int clampRows(int value) {
        return Math.max(1, Math.min(6, value));
    }

    private static ItemConfig item(String material, String name, String... lore) {
        ItemConfig item = new ItemConfig();
        item.material = material;
        item.name = name;
        item.lore.addAll(List.of(lore));
        return item;
    }

    private static DialogConfig dialog(String title, String label, int width, int maxLength) {
        DialogConfig dialog = new DialogConfig();
        dialog.title = title;
        dialog.label = label;
        dialog.confirm = "<green>确定";
        dialog.cancel = "<red>取消";
        dialog.width = width;
        dialog.maxLength = maxLength;
        return dialog;
    }

    public static class ItemConfig {
        public String material;
        public String materialOn;
        public String materialOff;
        public String name;
        public final List<String> lore = new ArrayList<>();
    }

    public static class DialogConfig {
        public String title;
        public String label;
        public String confirm;
        public String cancel;
        public int width;
        public int maxLength;
    }
}
