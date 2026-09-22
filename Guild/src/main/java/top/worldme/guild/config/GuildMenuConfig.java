package top.worldme.guild.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GuildMenuConfig {

    private final JavaPlugin plugin;
    private final File menuFile;

    public ItemConfig background = item("GRAY_STAINED_GLASS_PANE", " ");
    public boolean fillBackground = true;

    public String mainTitle = "<gold><bold>公会";
    public int mainRows = 4;
    public int infoSlot = 4;
    public ItemConfig infoItem = item("WHITE_BANNER", "<gold>%name%");
    public int createSlot = 11;
    public ItemConfig createButton = item("WRITABLE_BOOK", "<green>创建公会");
    public int joinSlot = 15;
    public ItemConfig joinButton = item("PLAYER_HEAD", "<yellow>加入公会");
    public int depositSlot = 10;
    public ItemConfig depositButton = item("HOPPER", "<green>存入资金");
    public int withdrawSlot = 12;
    public ItemConfig withdrawButton = item("DROPPER", "<yellow>取出资金");
    public int upgradeSlot = 13;
    public ItemConfig upgradeButton = item("EXPERIENCE_BOTTLE", "<light_purple>升级公会");
    public int membersSlot = 14;
    public ItemConfig membersButton = item("PLAYER_HEAD", "<aqua>成员管理");
    public int tpSlot = 16;
    public ItemConfig tpButton = item("ENDER_PEARL", "<green>传送到公会领地");
    public int leaveSlot = 25;
    public ItemConfig leaveButton = item("OAK_DOOR", "<red>退出公会");
    public int disbandSlot = 25;
    public ItemConfig disbandButton = item("TNT", "<dark_red>解散公会");
    public int closeSlot = 35;
    public ItemConfig closeButton = item("BARRIER", "<red>关闭");

    public String membersTitle = "<gold>公会成员";
    public int membersRows = 6;
    public final List<Integer> membersContentSlots = new ArrayList<>(List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43));
    public ItemConfig membersItem = item("PLAYER_HEAD", "<aqua>%player%");
    public int membersInfoSlot = 4;
    public ItemConfig membersInfo = item("PAPER", "<aqua>%name%");
    public int membersBackSlot = 49;
    public ItemConfig membersBack = item("ARROW", "<gray>返回公会界面");

    public String warehouseTitle = "<dark_gray>公会仓库";
    public int warehouseRows = 6;
    public final List<Integer> warehouseContentSlots = new ArrayList<>(List.of(
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44));
    public int warehouseCloseSlot = 49;
    public ItemConfig warehouseClose = item("BARRIER", "<red>关闭并保存");

    public DialogConfig createDialog = dialog("<gold>创建公会", "<gray>公会名称", 300, 16);
    public DialogConfig joinDialog = dialog("<gold>加入公会", "<gray>公会名称", 300, 16);
    public DialogConfig amountDialog = dialog("<gold>输入金额", "<gray>金额", 300, 12);

    public GuildMenuConfig(JavaPlugin plugin) {
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

        ConfigurationSection main = config.getConfigurationSection("main");
        if (main != null) {
            this.mainTitle = main.getString("title", this.mainTitle);
            this.mainRows = clampRows(main.getInt("rows", this.mainRows));
            ConfigurationSection items = main.getConfigurationSection("items");
            if (items != null) {
                this.infoSlot = readItemSlot(items, "info", this.infoSlot);
                this.infoItem = readItemIfPresent(items, "info", this.infoItem);
                this.createSlot = readItemSlot(items, "create", this.createSlot);
                this.createButton = readItemIfPresent(items, "create", this.createButton);
                this.joinSlot = readItemSlot(items, "join", this.joinSlot);
                this.joinButton = readItemIfPresent(items, "join", this.joinButton);
                this.depositSlot = readItemSlot(items, "deposit", this.depositSlot);
                this.depositButton = readItemIfPresent(items, "deposit", this.depositButton);
                this.withdrawSlot = readItemSlot(items, "withdraw", this.withdrawSlot);
                this.withdrawButton = readItemIfPresent(items, "withdraw", this.withdrawButton);
                this.upgradeSlot = readItemSlot(items, "upgrade", this.upgradeSlot);
                this.upgradeButton = readItemIfPresent(items, "upgrade", this.upgradeButton);
                this.membersSlot = readItemSlot(items, "members", this.membersSlot);
                this.membersButton = readItemIfPresent(items, "members", this.membersButton);
                this.tpSlot = readItemSlot(items, "tp", this.tpSlot);
                this.tpButton = readItemIfPresent(items, "tp", this.tpButton);
                this.leaveSlot = readItemSlot(items, "leave", this.leaveSlot);
                this.leaveButton = readItemIfPresent(items, "leave", this.leaveButton);
                this.disbandSlot = readItemSlot(items, "disband", this.disbandSlot);
                this.disbandButton = readItemIfPresent(items, "disband", this.disbandButton);
                this.closeSlot = readItemSlot(items, "close", this.closeSlot);
                this.closeButton = readItemIfPresent(items, "close", this.closeButton);
            }
        }

        ConfigurationSection members = config.getConfigurationSection("members");
        if (members != null) {
            this.membersTitle = members.getString("title", this.membersTitle);
            this.membersRows = clampRows(members.getInt("rows", this.membersRows));
            List<Integer> slots = members.getIntegerList("content-slots");
            if (!slots.isEmpty()) {
                this.membersContentSlots.clear();
                this.membersContentSlots.addAll(slots);
            }
            this.membersItem = readItemIfPresent(members, "item", this.membersItem);
            ConfigurationSection items = members.getConfigurationSection("items");
            if (items != null) {
                this.membersInfoSlot = readItemSlot(items, "info", this.membersInfoSlot);
                this.membersInfo = readItemIfPresent(items, "info", this.membersInfo);
                this.membersBackSlot = readItemSlot(items, "back", this.membersBackSlot);
                this.membersBack = readItemIfPresent(items, "back", this.membersBack);
            }
        }

        ConfigurationSection warehouse = config.getConfigurationSection("warehouse");
        if (warehouse != null) {
            this.warehouseTitle = warehouse.getString("title", this.warehouseTitle);
            this.warehouseRows = clampRows(warehouse.getInt("rows", this.warehouseRows));
            List<Integer> slots = warehouse.getIntegerList("content-slots");
            if (!slots.isEmpty()) {
                this.warehouseContentSlots.clear();
                this.warehouseContentSlots.addAll(slots);
            }
            this.warehouseCloseSlot = warehouse.getInt("close-slot", this.warehouseCloseSlot);
            this.warehouseClose = readItemIfPresent(warehouse, "close", this.warehouseClose);
        }

        ConfigurationSection dialogs = config.getConfigurationSection("dialogs");
        if (dialogs != null) {
            readDialog(dialogs.getConfigurationSection("create"), this.createDialog);
            readDialog(dialogs.getConfigurationSection("join"), this.joinDialog);
            readDialog(dialogs.getConfigurationSection("amount"), this.amountDialog);
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
