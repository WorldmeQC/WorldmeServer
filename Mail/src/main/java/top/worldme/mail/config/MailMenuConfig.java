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
    private ItemConfig prevButtonDisabled = item("GRAY_DYE", "<gray>上一页",
            "<dark_gray>已经是第一页");
    private ItemConfig nextButtonDisabled = item("GRAY_DYE", "<gray>下一页",
            "<dark_gray>已经是最后一页");
    private ItemConfig closeButton = new ItemConfig();
    private int closeSlot = 53;

    // ---------- 写信 GUI ----------
    private String composeTitle = "<gold><bold>写信";
    private int composeRows = 6;
    private ItemConfig composeDecoration = new ItemConfig();
    private final List<Integer> composeDecorationSlots = new ArrayList<>();
    private ItemConfig composeInfo = new ItemConfig();
    private int composeInfoSlot = 4;
    private ItemConfig subjectButton = item("NAME_TAG", "<aqua>主题",
            "<gray>点击输入邮件主题", "", "<gray>当前：<yellow>%subject%");
    private int subjectButtonSlot = 11;
    private ItemConfig contentButton = item("WRITABLE_BOOK", "<aqua>正文",
            "<gray>点击输入邮件正文", "<gray>支持多行文本", "", "<gray>当前字数：<yellow>%content_length%");
    private int contentButtonSlot = 13;
    private ItemConfig commandsButton = item("COMMAND_BLOCK", "<red>附带指令（管理员）",
            "<gray>点击输入要附带执行的指令", "<gray>每行一条，领取附件时以控制台执行",
            "<gray>支持 {player} / {uuid} 占位符", "", "<gray>当前指令数：<yellow>%command_count%");
    private int commandsButtonSlot = 15;
    private ItemConfig attachmentsHint = item("CHEST", "<aqua>附件物品",
            "<gray>把要寄送的物品放入下方格子", "<gray>背包空间不足时无法领取");
    private int attachmentsHintSlot = 18;
    private final List<Integer> attachmentSlots = new ArrayList<>();
    private ItemConfig sendButton = new ItemConfig();
    private int sendSlot = 45;
    private ItemConfig cancelButton = new ItemConfig();
    private int cancelSlot = 53;

    // ---------- 写信 Dialog ----------
    private final DialogConfig subjectDialog = dialog(
            "<gold>设置邮件主题", "<gray>主题", 300, 64, false, 0, 0);
    private final DialogConfig contentDialog = dialog(
            "<gold>编辑邮件正文", "<gray>正文（可多行，Shift+Enter 换行）", 400, 2000, true, 160, 50);
    private final DialogConfig commandsDialog = dialog(
            "<gold>编辑附带指令（管理员）", "<gray>每行一条指令，支持 {player} / {uuid}", 400, 2000, true, 160, 50);

    private static ItemConfig item(String material, String name, String... lore) {
        ItemConfig item = new ItemConfig();
        item.material = material;
        item.name = name;
        item.lore.addAll(List.of(lore));
        return item;
    }

    private static DialogConfig dialog(String title, String label, int width, int maxLength,
                                       boolean multiline, int height, int maxLines) {
        DialogConfig dialog = new DialogConfig();
        dialog.title = title;
        dialog.label = label;
        dialog.confirm = "<green>确定";
        dialog.cancel = "<red>取消";
        dialog.width = width;
        dialog.maxLength = maxLength;
        dialog.multiline = multiline;
        dialog.height = height;
        dialog.maxLines = maxLines;
        return dialog;
    }

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

            ConfigurationSection prevDisabled = items.getConfigurationSection("prev-button-disabled");
            if (prevDisabled != null) {
                this.prevButtonDisabled = readItemConfig(prevDisabled);
            }

            ConfigurationSection nextDisabled = items.getConfigurationSection("next-button-disabled");
            if (nextDisabled != null) {
                this.nextButtonDisabled = readItemConfig(nextDisabled);
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

                ConfigurationSection subject = cItems.getConfigurationSection("subject-button");
                if (subject != null) {
                    this.subjectButtonSlot = subject.getInt("slot", this.subjectButtonSlot);
                    this.subjectButton = readItemConfig(subject);
                }

                ConfigurationSection content = cItems.getConfigurationSection("content-button");
                if (content != null) {
                    this.contentButtonSlot = content.getInt("slot", this.contentButtonSlot);
                    this.contentButton = readItemConfig(content);
                }

                ConfigurationSection commands = cItems.getConfigurationSection("commands-button");
                if (commands != null) {
                    this.commandsButtonSlot = commands.getInt("slot", this.commandsButtonSlot);
                    this.commandsButton = readItemConfig(commands);
                }

                ConfigurationSection attachmentsHintSection = cItems.getConfigurationSection("attachments-hint");
                if (attachmentsHintSection != null) {
                    this.attachmentsHintSlot = attachmentsHintSection.getInt("slot", this.attachmentsHintSlot);
                    this.attachmentsHint = readItemConfig(attachmentsHintSection);
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

            ConfigurationSection dialogs = compose.getConfigurationSection("dialogs");
            if (dialogs != null) {
                readDialog(dialogs.getConfigurationSection("subject"), this.subjectDialog);
                readDialog(dialogs.getConfigurationSection("content"), this.contentDialog);
                readDialog(dialogs.getConfigurationSection("commands"), this.commandsDialog);
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
        dialog.multiline = section.getBoolean("multiline", dialog.multiline);
        dialog.height = section.getInt("height", dialog.height);
        dialog.maxLines = Math.max(0, section.getInt("max-lines", dialog.maxLines));
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

    public ItemConfig prevButtonDisabled() {
        return prevButtonDisabled;
    }

    public ItemConfig nextButtonDisabled() {
        return nextButtonDisabled;
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

    public ItemConfig subjectButton() {
        return subjectButton;
    }

    public int subjectButtonSlot() {
        return subjectButtonSlot;
    }

    public ItemConfig contentButton() {
        return contentButton;
    }

    public int contentButtonSlot() {
        return contentButtonSlot;
    }

    public ItemConfig commandsButton() {
        return commandsButton;
    }

    public int commandsButtonSlot() {
        return commandsButtonSlot;
    }

    public ItemConfig attachmentsHint() {
        return attachmentsHint;
    }

    public int attachmentsHintSlot() {
        return attachmentsHintSlot;
    }

    public List<Integer> attachmentSlots() {
        return Collections.unmodifiableList(attachmentSlots);
    }

    public DialogConfig subjectDialog() {
        return subjectDialog;
    }

    public DialogConfig contentDialog() {
        return contentDialog;
    }

    public DialogConfig commandsDialog() {
        return commandsDialog;
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

    public static class DialogConfig {
        public String title;
        public String label;
        public String confirm;
        public String cancel;
        public int width;
        public int maxLength;
        public boolean multiline;
        public int height;
        public int maxLines;
    }
}