package top.worldme.market.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MarketMenuConfig {

    private final JavaPlugin plugin;
    private final File menuFile;

    // 通用背景
    public ItemConfig background = item("GRAY_STAINED_GLASS_PANE", " ");
    public boolean fillBackground = true;

    // 主菜单
    public String mainTitle = "<gold><bold>全球市场";
    public int mainRows = 3;
    public int mainInfoSlot = 4;
    public ItemConfig mainInfo = item("COMPASS", "<aqua>全球市场",
            "<gray>当前在售：<yellow>%count%</yellow> 件",
            "<gray>上架手续费：<yellow>%fee%</yellow>");
    public int browseSlot = 10;
    public ItemConfig browseButton = item("ENDER_CHEST", "<dark_aqua>浏览市场",
            "<gray>查看所有玩家上架的物品", "<gray>左键点击购买");
    public int myListingsSlot = 12;
    public ItemConfig myListingsButton = item("CHEST", "<light_purple>我的上架",
            "<gray>查看我上架的物品", "<gray>点击可下架");
    public int sellMenuSlot = 14;
    public ItemConfig sellButton = item("GOLD_INGOT", "<gold>上架物品",
            "<gray>打开上架界面");
    public int mainCloseSlot = 22;
    public ItemConfig closeButton = item("BARRIER", "<red>关闭");

    // 列表（浏览 / 我的上架）
    public String browseTitle = "<gold><bold>全部商品";
    public String mineTitle = "<gold><bold>我的上架";
    public int listRows = 6;
    public final List<Integer> listContentSlots = new ArrayList<>(List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43));
    public int listInfoSlot = 4;
    public ItemConfig listInfo = item("COMPASS", "<aqua>%title%",
            "<gray>共 <yellow>%count%</yellow> 件",
            "<gray>第 <yellow>%page%/%pages%</yellow> 页");
    public final List<String> listingLore = new ArrayList<>(List.of(
            "",
            "<gray>售价：<yellow>%price%",
            "<gray>数量：<yellow>%amount%",
            "<gray>卖家：<yellow>%seller%",
            "<gray>到期：<yellow>%expire_at%",
            "<gray>左键购买 ｜ 右键强制下架"));
    public int prevSlot = 48;
    public ItemConfig prevButton = item("ARROW", "<gray>上一页", "<gray>点击查看上一页");
    public ItemConfig prevButtonDisabled = item("GRAY_DYE", "<gray>上一页", "<dark_gray>已经是第一页");
    public int nextSlot = 50;
    public ItemConfig nextButton = item("ARROW", "<gray>下一页", "<gray>点击查看下一页");
    public ItemConfig nextButtonDisabled = item("GRAY_DYE", "<gray>下一页", "<dark_gray>已经是最后一页");
    public int backSlot = 45;
    public ItemConfig backButton = item("ARROW", "<gray>返回市场菜单");
    public int listCloseSlot = 53;
    public ItemConfig listCloseButton = item("BARRIER", "<red>关闭");

    // 上架界面
    public String sellTitle = "<gold><bold>上架物品";
    public int sellRows = 5;
    public int sellItemSlot = 13;
    public int sellItemHintSlot = 11;
    public ItemConfig sellItemHint = item("HOPPER", "<aqua>放入要上架的物品",
            "<gray>放入多少即上架多少", "<gray>放回背包请在关闭界面后进行");
    public int sellPriceSlot = 30;
    public ItemConfig sellPriceButton = item("GOLD_NUGGET", "<gold>设置价格",
            "<gray>当前：<yellow>%price%", "<gray>点击输入售价");
    public int sellInfoSlot = 4;
    public ItemConfig sellInfo = item("PAPER", "<aqua>上架信息",
            "<gray>上架手续费：<yellow>%fee%</yellow>",
            "<gray>有效期：<yellow>%expire_days%</yellow> 天",
            "<gray>成交税：<yellow>%sales_tax%%");
    public int sellConfirmSlot = 39;
    public ItemConfig sellConfirmButton = item("EMERALD", "<green>确认上架",
            "<gray>扣除手续费后上架");
    public int sellCancelSlot = 41;
    public ItemConfig sellCancelButton = item("BARRIER", "<red>取消");

    // 二次确认
    public int confirmRows = 3;
    public int confirmItemSlot = 4;
    public int confirmSlot = 10;
    public int confirmCancelSlot = 16;
    public String buyTitle = "<gold><bold>确认购买";
    public ItemConfig buyButton = item("GOLD_INGOT", "<green>确认购买",
            "<gray>花费：<yellow>%price%",
            "<gray>物品将发送至你的邮箱");
    public String removeTitle = "<gold><bold>确认下架";
    public ItemConfig removeButton = item("GOLD_INGOT", "<green>确认下架",
            "<gray>下架不退还手续费",
            "<gray>物品将发送至你的邮箱");
    public String forceTitle = "<red><bold>确认强制下架";
    public ItemConfig forceButton = item("GOLD_INGOT", "<red>确认强制下架",
            "<gray>物品将退回卖家邮箱");
    public ItemConfig confirmCancelButton = item("BARRIER", "<red>取消");

    // 价格输入对话框
    public DialogConfig priceDialog = dialog("<gold>设置价格", "<gray>售价", 300, 32);

    public MarketMenuConfig(JavaPlugin plugin) {
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
                this.mainInfoSlot = readItemSlot(items, "info", this.mainInfoSlot);
                this.mainInfo = readItemIfPresent(items, "info", this.mainInfo);
                this.browseSlot = readItemSlot(items, "browse", this.browseSlot);
                this.browseButton = readItemIfPresent(items, "browse", this.browseButton);
                this.myListingsSlot = readItemSlot(items, "my-listings", this.myListingsSlot);
                this.myListingsButton = readItemIfPresent(items, "my-listings", this.myListingsButton);
                this.sellMenuSlot = readItemSlot(items, "sell", this.sellMenuSlot);
                this.sellButton = readItemIfPresent(items, "sell", this.sellButton);
                this.mainCloseSlot = readItemSlot(items, "close", this.mainCloseSlot);
                this.closeButton = readItemIfPresent(items, "close", this.closeButton);
            }
        }

        ConfigurationSection list = config.getConfigurationSection("list");
        if (list != null) {
            this.browseTitle = list.getString("browse-title", this.browseTitle);
            this.mineTitle = list.getString("mine-title", this.mineTitle);
            this.listRows = clampRows(list.getInt("rows", this.listRows));
            List<Integer> slots = list.getIntegerList("content-slots");
            if (!slots.isEmpty()) {
                this.listContentSlots.clear();
                this.listContentSlots.addAll(slots);
            }
            ConfigurationSection items = list.getConfigurationSection("items");
            if (items != null) {
                this.listInfoSlot = readItemSlot(items, "info", this.listInfoSlot);
                this.listInfo = readItemIfPresent(items, "info", this.listInfo);
                this.prevSlot = readItemSlot(items, "prev-button", this.prevSlot);
                this.prevButton = readItemIfPresent(items, "prev-button", this.prevButton);
                this.prevButtonDisabled = readItemIfPresent(items, "prev-button-disabled", this.prevButtonDisabled);
                this.nextSlot = readItemSlot(items, "next-button", this.nextSlot);
                this.nextButton = readItemIfPresent(items, "next-button", this.nextButton);
                this.nextButtonDisabled = readItemIfPresent(items, "next-button-disabled", this.nextButtonDisabled);
                this.backSlot = readItemSlot(items, "back-button", this.backSlot);
                this.backButton = readItemIfPresent(items, "back-button", this.backButton);
                this.listCloseSlot = readItemSlot(items, "close-button", this.listCloseSlot);
                this.listCloseButton = readItemIfPresent(items, "close-button", this.listCloseButton);
            }
            List<String> lore = list.getStringList("listing-lore");
            if (!lore.isEmpty()) {
                this.listingLore.clear();
                this.listingLore.addAll(lore);
            }
        }

        ConfigurationSection sell = config.getConfigurationSection("sell");
        if (sell != null) {
            this.sellTitle = sell.getString("title", this.sellTitle);
            this.sellRows = clampRows(sell.getInt("rows", this.sellRows));
            this.sellItemSlot = sell.getInt("item-slot", this.sellItemSlot);
            ConfigurationSection items = sell.getConfigurationSection("items");
            if (items != null) {
                this.sellItemHintSlot = readItemSlot(items, "item-hint", this.sellItemHintSlot);
                this.sellItemHint = readItemIfPresent(items, "item-hint", this.sellItemHint);
                this.sellPriceSlot = readItemSlot(items, "price-button", this.sellPriceSlot);
                this.sellPriceButton = readItemIfPresent(items, "price-button", this.sellPriceButton);
                this.sellInfoSlot = readItemSlot(items, "info", this.sellInfoSlot);
                this.sellInfo = readItemIfPresent(items, "info", this.sellInfo);
                this.sellConfirmSlot = readItemSlot(items, "confirm-button", this.sellConfirmSlot);
                this.sellConfirmButton = readItemIfPresent(items, "confirm-button", this.sellConfirmButton);
                this.sellCancelSlot = readItemSlot(items, "cancel-button", this.sellCancelSlot);
                this.sellCancelButton = readItemIfPresent(items, "cancel-button", this.sellCancelButton);
            }
        }

        ConfigurationSection confirm = config.getConfigurationSection("confirm");
        if (confirm != null) {
            this.confirmRows = clampRows(confirm.getInt("rows", this.confirmRows));
            this.confirmItemSlot = confirm.getInt("item-slot", this.confirmItemSlot);
            this.confirmSlot = confirm.getInt("confirm-slot", this.confirmSlot);
            this.confirmCancelSlot = confirm.getInt("cancel-slot", this.confirmCancelSlot);
            ConfigurationSection buy = confirm.getConfigurationSection("buy");
            if (buy != null) {
                this.buyTitle = buy.getString("title", this.buyTitle);
                this.buyButton = readItemIfPresent(buy, "button", this.buyButton);
            }
            ConfigurationSection remove = confirm.getConfigurationSection("remove");
            if (remove != null) {
                this.removeTitle = remove.getString("title", this.removeTitle);
                this.removeButton = readItemIfPresent(remove, "button", this.removeButton);
            }
            ConfigurationSection force = confirm.getConfigurationSection("force");
            if (force != null) {
                this.forceTitle = force.getString("title", this.forceTitle);
                this.forceButton = readItemIfPresent(force, "button", this.forceButton);
            }
            this.confirmCancelButton = readItemIfPresent(confirm, "cancel", this.confirmCancelButton);
        }

        ConfigurationSection dialogs = config.getConfigurationSection("dialogs");
        if (dialogs != null) {
            readDialog(dialogs.getConfigurationSection("price"), this.priceDialog);
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
        return section == null ? fallback : readItem(section);
    }

    private ItemConfig readItem(ConfigurationSection section) {
        ItemConfig item = new ItemConfig();
        item.material = section.getString("material", null);
        item.name = section.getString("name", null);
        item.lore.clear();
        item.lore.addAll(section.getStringList("lore"));
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
