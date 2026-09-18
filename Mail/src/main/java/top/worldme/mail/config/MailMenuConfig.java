package top.worldme.mail.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MailMenuConfig {

    private final JavaPlugin plugin;
    private final File menuFile;

    // ---------- 邮箱 GUI ----------
    private String title = "<gold><bold>我的邮箱";
    private int rows = 6;
    private ItemConfig decoration = new ItemConfig();
    private final List<Integer> decorationSlots = new ArrayList<>();
    private ItemConfig info = new ItemConfig();
    private int infoSlot = 4;
    private ItemConfig empty = new ItemConfig();
    private int emptySlot = 22;
    private final List<Integer> mailSlots = new ArrayList<>();
    private ItemConfig mailUnread = new ItemConfig();
    private ItemConfig mailRead = new ItemConfig();
    private final List<String> mailLore = new ArrayList<>();
    private ItemConfig prevButton = new ItemConfig();
    private int prevSlot = 48;
    private ItemConfig nextButton = new ItemConfig();
    private int nextSlot = 50;
    private ItemConfig closeButton = new ItemConfig();
    private int closeSlot = 53;

    // ---------- 写信 GUI ----------
    private String composeTitle = "<gold><bold>写信";
    private int composeRows = 6;
    private ItemConfig composeDecoration = new ItemConfig();
    private final List<Integer> composeDecorationSlots = new ArrayList<>();
    private ItemConfig composeInfo = new ItemConfig();
    private int composeInfoSlot = 4;
    private int subjectSlot = 10;
    private ItemConfig subjectHint = new ItemConfig();
    private int subjectHintSlot = 11;
    private int contentSlot = 13;
    private ItemConfig contentHint = new ItemConfig();
    private int contentHintSlot = 14;
    private int commandsSlot = 16;
    private ItemConfig commandsHint = new ItemConfig();
    private int commandsHintSlot = 15;
    private final List<Integer> attachmentSlots = new ArrayList<>();
    private ItemConfig sendButton = new ItemConfig();
    private int sendSlot = 45;
    private ItemConfig cancelButton = new ItemConfig();
    private int cancelSlot = 53;

    public MailMenuConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        this.menuFile = new File(plugin.getDataFolder(), "menu.yml");
        load();
    }

    public void load() {
        if (!menuFile.exists()) {
            plugin.saveResource("menu.yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(menuFile);

        this.title = config.getString("title", this.title);
        this.rows = clampRows(config.getInt("rows", 6));

        ConfigurationSection items = config.getConfigurationSection("items");
        if (items != null) {
            ConfigurationSection deco = items.getConfigurationSection("decoration");
            if (deco != null) {
                this.decoration = readItemConfig(deco);
                this.decorationSlots.clear();
                this.decorationSlots.addAll(deco.getIntegerList("slots"));
            }

            ConfigurationSection infoSection = items.getConfigurationSection("info");
            if (infoSection != null) {
                this.infoSlot = infoSection.getInt("slot", this.infoSlot);
                this.info = readItemConfig(infoSection);
            }

            ConfigurationSection emptySection = items.getConfigurationSection("empty");
            if (emptySection != null) {
                this.emptySlot = emptySection.getInt("slot", this.emptySlot);
                this.empty = readItemConfig(emptySection);
            }

            this.mailSlots.clear();
            this.mailSlots.addAll(items.getIntegerList("mail-slots"));

            ConfigurationSection mailSection = items.getConfigurationSection("mail");
            if (mailSection != null) {
                ConfigurationSection unread = mailSection.getConfigurationSection("unread");
                if (unread != null) {
                    this.mailUnread = readItemConfig(unread);
                }
                ConfigurationSection read = mailSection.getConfigurationSection("read");
                if (read != null) {
                    this.mailRead = readItemConfig(read);
                }
                this.mailLore.clear();
                this.mailLore.addAll(mailSection.getStringList("lore"));
            }

            ConfigurationSection prev = items.getConfigurationSection("prev-button");
            if (prev != null) {
                this.prevSlot = prev.getInt("slot", this.prevSlot);
                this.prevButton = readItemConfig(prev);
            }

            ConfigurationSection next = items.getConfigurationSection("next-button");
            if (next != null) {
                this.nextSlot = next.getInt("slot", this.nextSlot);
                this.nextButton = readItemConfig(next);
            }

            ConfigurationSection close = items.getConfigurationSection("close-button");
            if (close != null) {
                this.closeSlot = close.getInt("slot", this.closeSlot);
                this.closeButton = readItemConfig(close);
            }
        }

        ConfigurationSection compose = config.getConfigurationSection("compose");
        if (compose != null) {
            this.composeTitle = compose.getString("title", this.composeTitle);
            this.composeRows = clampRows(compose.getInt("rows", 6));
            ConfigurationSection cItems = compose.getConfigurationSection("items");
            if (cItems != null) {
                ConfigurationSection deco = cItems.getConfigurationSection("decoration");
                if (deco != null) {
                    this.composeDecoration = readItemConfig(deco);
                    this.composeDecorationSlots.clear();
                    this.composeDecorationSlots.addAll(deco.getIntegerList("slots"));
                }

                ConfigurationSection infoSection = cItems.getConfigurationSection("info");
                if (infoSection != null) {
                    this.composeInfoSlot = infoSection.getInt("slot", this.composeInfoSlot);
                    this.composeInfo = readItemConfig(infoSection);
                }

                ConfigurationSection subject = cItems.getConfigurationSection("subject");
                if (subject != null) {
                    this.subjectSlot = subject.getInt("slot", this.subjectSlot);
                }
                ConfigurationSection subjectHintSection = cItems.getConfigurationSection("subject-hint");
                if (subjectHintSection != null) {
                    this.subjectHintSlot = subjectHintSection.getInt("slot", this.subjectHintSlot);
                    this.subjectHint = readItemConfig(subjectHintSection);
                }

                ConfigurationSection content = cItems.getConfigurationSection("content");
                if (content != null) {
                    this.contentSlot = content.getInt("slot", this.contentSlot);
                }
                ConfigurationSection contentHintSection = cItems.getConfigurationSection("content-hint");
                if (contentHintSection != null) {
                    this.contentHintSlot = contentHintSection.getInt("slot", this.contentHintSlot);
                    this.contentHint = readItemConfig(contentHintSection);
                }

                ConfigurationSection commands = cItems.getConfigurationSection("commands");
                if (commands != null) {
                    this.commandsSlot = commands.getInt("slot", this.commandsSlot);
                }
                ConfigurationSection commandsHintSection = cItems.getConfigurationSection("commands-hint");
                if (commandsHintSection != null) {
                    this.commandsHintSlot = commandsHintSection.getInt("slot", this.commandsHintSlot);
                    this.commandsHint = readItemConfig(commandsHintSection);
                }

                ConfigurationSection attachments = cItems.getConfigurationSection("attachments");
                if (attachments != null) {
                    this.attachmentSlots.clear();
                    this.attachmentSlots.addAll(attachments.getIntegerList("slots"));
                }

                ConfigurationSection send = cItems.getConfigurationSection("send-button");
                if (send != null) {
                    this.sendSlot = send.getInt("slot", this.sendSlot);
                    this.sendButton = readItemConfig(send);
                }

                ConfigurationSection cancel = cItems.getConfigurationSection("cancel-button");
                if (cancel != null) {
                    this.cancelSlot = cancel.getInt("slot", this.cancelSlot);
                    this.cancelButton = readItemConfig(cancel);
                }
            }
        }
    }

    private int clampRows(int value) {
        return Math.max(1, Math.min(6, value));
    }

    private ItemConfig readItemConfig(ConfigurationSection section) {
        ItemConfig item = new ItemConfig();
        item.material = section.getString("material", null);
        item.name = section.getString("name", null);
        item.lore.clear();
        item.lore.addAll(section.getStringList("lore"));
        return item;
    }

    // ---------- 邮箱 ----------
    public String title() {
        return title;
    }

    public int size() {
        return rows * 9;
    }

    public ItemConfig decoration() {
        return decoration;
    }

    public List<Integer> decorationSlots() {
        return Collections.unmodifiableList(decorationSlots);
    }

    public ItemConfig info() {
        return info;
    }

    public int infoSlot() {
        return infoSlot;
    }

    public ItemConfig empty() {
        return empty;
    }

    public int emptySlot() {
        return emptySlot;
    }

    public List<Integer> mailSlots() {
        return Collections.unmodifiableList(mailSlots);
    }

    public ItemConfig mailUnread() {
        return mailUnread;
    }

    public ItemConfig mailRead() {
        return mailRead;
    }

    public List<String> mailLore() {
        return Collections.unmodifiableList(mailLore);
    }

    public ItemConfig prevButton() {
        return prevButton;
    }

    public int prevSlot() {
        return prevSlot;
    }

    public ItemConfig nextButton() {
        return nextButton;
    }

    public int nextSlot() {
        return nextSlot;
    }

    public ItemConfig closeButton() {
        return closeButton;
    }

    public int closeSlot() {
        return closeSlot;
    }

    // ---------- 写信 ----------
    public String composeTitle() {
        return composeTitle;
    }

    public int composeSize() {
        return composeRows * 9;
    }

    public ItemConfig composeDecoration() {
        return composeDecoration;
    }

    public List<Integer> composeDecorationSlots() {
        return Collections.unmodifiableList(composeDecorationSlots);
    }

    public ItemConfig composeInfo() {
        return composeInfo;
    }

    public int composeInfoSlot() {
        return composeInfoSlot;
    }

    public int subjectSlot() {
        return subjectSlot;
    }

    public ItemConfig subjectHint() {
        return subjectHint;
    }

    public int subjectHintSlot() {
        return subjectHintSlot;
    }

    public int contentSlot() {
        return contentSlot;
    }

    public ItemConfig contentHint() {
        return contentHint;
    }

    public int contentHintSlot() {
        return contentHintSlot;
    }

    public int commandsSlot() {
        return commandsSlot;
    }

    public ItemConfig commandsHint() {
        return commandsHint;
    }

    public int commandsHintSlot() {
        return commandsHintSlot;
    }

    public List<Integer> attachmentSlots() {
        return Collections.unmodifiableList(attachmentSlots);
    }

    public ItemConfig sendButton() {
        return sendButton;
    }

    public int sendSlot() {
        return sendSlot;
    }

    public ItemConfig cancelButton() {
        return cancelButton;
    }

    public int cancelSlot() {
        return cancelSlot;
    }

    public static class ItemConfig {
        public String material;
        public String name;
        public final List<String> lore = new ArrayList<>();
    }
}