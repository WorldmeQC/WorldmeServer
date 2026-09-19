package top.worldme.mail.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import top.worldme.mail.config.MailConfig;
import top.worldme.mail.config.MailMenuConfig;
import top.worldme.mail.config.MailMenuConfig.DialogConfig;
import top.worldme.mail.config.MailMenuConfig.ItemConfig;
import top.worldme.mail.manager.MailManager;
import top.worldme.mail.util.ItemParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ComposeGui implements InventoryHolder {

    private final JavaPlugin plugin;
    private final MailMenuConfig menu;
    private final MailConfig config;
    private final MailManager mailManager;
    private final Player sender;
    private final UUID recipient;
    private final String recipientName;
    private final Inventory inventory;
    private final boolean canUseCommands;

    private String subject;
    private String content;
    private String commands;
    private boolean sent = false;
    private final List<ItemStack> stashed = new ArrayList<>();

    public ComposeGui(JavaPlugin plugin, MailMenuConfig menu, MailConfig config, MailManager mailManager,
                      Player sender, UUID recipient, String recipientName) {
        this.plugin = plugin;
        this.menu = menu;
        this.config = config;
        this.mailManager = mailManager;
        this.sender = sender;
        this.recipient = recipient;
        this.recipientName = recipientName;
        this.canUseCommands = sender.hasPermission("worldme.mail.admin");
        this.inventory = Bukkit.createInventory(
                this,
                menu.composeSize(),
                MiniMessage.miniMessage().deserialize(menu.composeTitle())
        );
        render();
    }

    private void render() {
        inventory.clear();

        ItemStack decoration = buildItem(menu.composeDecoration(), null);
        if (decoration != null) {
            for (int slot : menu.composeDecorationSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, decoration.clone());
                }
            }
        }

        Map<String, String> placeholders = Map.of(
                "player", recipientName,
                "subject", displaySubject(),
                "content_length", String.valueOf(contentLength()),
                "command_count", String.valueOf(commandCount())
        );

        placeButton(menu.composeInfoSlot(), menu.composeInfo(), placeholders);
        placeButton(menu.subjectButtonSlot(), menu.subjectButton(), placeholders);
        placeButton(menu.contentButtonSlot(), menu.contentButton(), placeholders);

        if (canUseCommands) {
            placeButton(menu.commandsButtonSlot(), menu.commandsButton(), placeholders);
        } else if (decoration != null) {
            inventory.setItem(menu.commandsButtonSlot(), decoration.clone());
        }

        placeButton(menu.attachmentsHintSlot(), menu.attachmentsHint(), placeholders);
        placeButton(menu.sendSlot(), menu.sendButton(), placeholders);
        placeButton(menu.cancelSlot(), menu.cancelButton(), placeholders);
    }

    private void placeButton(int slot, ItemConfig itemConfig, Map<String, String> placeholders) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        ItemStack item = buildItem(itemConfig, placeholders);
        if (item != null) {
            inventory.setItem(slot, item);
        }
    }

    public boolean isInputSlot(int slot) {
        return menu.attachmentSlots().contains(slot);
    }

    public boolean canUseCommands() {
        return canUseCommands;
    }

    // ---------- 对话框输入 ----------

    public void openSubjectDialog() {
        showInputDialog(menu.subjectDialog(), "subject", subject, value -> this.subject = value);
    }

    public void openContentDialog() {
        showInputDialog(menu.contentDialog(), "content", content, value -> this.content = value);
    }

    public void openCommandsDialog() {
        if (!canUseCommands) {
            return;
        }
        showInputDialog(menu.commandsDialog(), "commands", commands, value -> this.commands = value);
    }

    private void showInputDialog(DialogConfig dialogConfig, String key, String initial, Consumer<String> setter) {
        stashAttachments();
        TextDialogInput.MultilineOptions multiline = dialogConfig.multiline
                ? TextDialogInput.MultilineOptions.create(
                        dialogConfig.maxLines > 0 ? dialogConfig.maxLines : null,
                        dialogConfig.height > 0 ? dialogConfig.height : null)
                : null;
        DialogInput input = DialogInput.text(
                key,
                dialogConfig.width,
                mini(dialogConfig.label),
                true,
                initial == null ? "" : initial,
                dialogConfig.maxLength,
                multiline
        );
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(mini(dialogConfig.title))
                        .inputs(List.of(input))
                        .canCloseWithEscape(false)
                        .build())
                .type(DialogType.confirmation(
                        ActionButton.create(mini(dialogConfig.confirm), null, 100,
                                DialogAction.customClick((response, audience) -> {
                                    String value = response.getText(key);
                                    setter.accept(value == null ? null : value.strip());
                                    finishDialog();
                                }, ClickCallback.Options.builder().uses(1).build())),
                        ActionButton.create(mini(dialogConfig.cancel), null, 100,
                                DialogAction.customClick((response, audience) -> finishDialog(),
                                        ClickCallback.Options.builder().uses(1).build()))
                )));
        sender.showDialog(dialog);
    }

    private void finishDialog() {
        restoreStashed();
        render();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (sender.isOnline()) {
                sender.openInventory(inventory);
            }
        });
    }

    private void stashAttachments() {
        for (int slot : menu.attachmentSlots()) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                stashed.add(item);
                inventory.setItem(slot, null);
            }
        }
    }

    private void restoreStashed() {
        if (stashed.isEmpty()) {
            return;
        }
        int index = 0;
        for (int slot : menu.attachmentSlots()) {
            if (index >= stashed.size()) {
                break;
            }
            ItemStack current = inventory.getItem(slot);
            if (current == null || current.getType().isAir()) {
                inventory.setItem(slot, stashed.get(index++));
            }
        }
        while (index < stashed.size()) {
            giveToPlayer(stashed.get(index++));
        }
        stashed.clear();
    }

    // ---------- 发送 / 取消 ----------

    public void handleSend() {
        String content = this.content;
        if (content == null || content.isBlank()) {
            sendMessage("need-content", null);
            return;
        }

        String subject = (this.subject == null || this.subject.isBlank())
                ? config.defaultSubject()
                : this.subject;

        List<ItemStack> attachments = new ArrayList<>();
        for (int slot : menu.attachmentSlots()) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                attachments.add(item.clone());
            }
        }
        for (ItemStack item : stashed) {
            attachments.add(item.clone());
        }
        stashed.clear();

        List<String> commands = new ArrayList<>();
        if (canUseCommands && this.commands != null) {
            for (String line : this.commands.split("\n")) {
                String trimmed = line.strip();
                if (!trimmed.isEmpty()) {
                    commands.add(trimmed);
                }
            }
        }

        mailManager.send(recipient, subject, content, attachments, commands);

        sendMessage("mail-sent", Map.of("player", recipientName));
        sent = true;
        clearInputSlots();
        sender.closeInventory();
    }

    public void handleCancel() {
        returnItems();
        sender.closeInventory();
    }

    public void onClose() {
        if (sent) {
            return;
        }
        returnItems();
    }

    private void clearInputSlots() {
        for (int slot : menu.attachmentSlots()) {
            inventory.setItem(slot, null);
        }
    }

    private void returnItems() {
        if (sent) {
            return;
        }
        for (int slot : menu.attachmentSlots()) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            inventory.setItem(slot, null);
            giveToPlayer(item);
        }
        for (ItemStack item : stashed) {
            giveToPlayer(item);
        }
        stashed.clear();
    }

    private void giveToPlayer(ItemStack item) {
        Map<Integer, ItemStack> leftover = sender.getInventory().addItem(item);
        for (ItemStack drop : leftover.values()) {
            sender.getWorld().dropItemNaturally(sender.getLocation(), drop);
        }
    }

    // ---------- 占位符 / 物品 ----------

    private String displaySubject() {
        if (subject == null || subject.isBlank()) {
            return "未设置";
        }
        return escape(subject);
    }

    private int contentLength() {
        return content == null ? 0 : content.length();
    }

    private int commandCount() {
        if (commands == null || commands.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String line : commands.split("\n")) {
            if (!line.strip().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private String escape(String text) {
        return MiniMessage.miniMessage().escapeTags(text);
    }

    private void sendMessage(String key, Map<String, String> placeholders) {
        String text = config.getMessage(key, placeholders);
        if (text == null || text.isEmpty()) {
            return;
        }
        sender.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage("prefix") + text));
    }

    private Component mini(String text) {
        return MiniMessage.miniMessage().deserialize(text == null ? "" : text);
    }

    private ItemStack buildItem(ItemConfig itemConfig, Map<String, String> placeholders) {
        if (itemConfig == null || itemConfig.material == null || itemConfig.material.isBlank()) {
            return null;
        }
        ItemStack item = ItemParser.buildItem(itemConfig.material, 1);
        if (item == null) {
            return null;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (itemConfig.name != null && !itemConfig.name.isEmpty()) {
            meta.displayName(MiniMessage.miniMessage().deserialize(applyPlaceholders(itemConfig.name, placeholders)));
        }
        if (!itemConfig.lore.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : itemConfig.lore) {
                lore.add(MiniMessage.miniMessage().deserialize(applyPlaceholders(line, placeholders)));
            }
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (placeholders == null) {
            return text;
        }
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return text;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    public static class GuiListener implements Listener {

        private final JavaPlugin plugin;
        private final MailMenuConfig menu;
        private final MailConfig config;
        private final MailManager mailManager;

        public GuiListener(JavaPlugin plugin, MailMenuConfig menu, MailConfig config, MailManager mailManager) {
            this.plugin = plugin;
            this.menu = menu;
            this.config = config;
            this.mailManager = mailManager;
        }

        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getInventory().getHolder() instanceof ComposeGui gui)) {
                return;
            }
            Inventory clicked = event.getClickedInventory();
            if (clicked == null) {
                return;
            }
            if (clicked.equals(event.getInventory())) {
                int slot = event.getSlot();
                if (gui.isInputSlot(slot)) {
                    event.setCancelled(false);
                    return;
                }
                event.setCancelled(true);
                if (slot == gui.menu.subjectButtonSlot()) {
                    gui.openSubjectDialog();
                } else if (slot == gui.menu.contentButtonSlot()) {
                    gui.openContentDialog();
                } else if (slot == gui.menu.commandsButtonSlot() && gui.canUseCommands()) {
                    gui.openCommandsDialog();
                } else if (slot == gui.menu.sendSlot()) {
                    gui.handleSend();
                } else if (slot == gui.menu.cancelSlot()) {
                    gui.handleCancel();
                }
            } else {
                event.setCancelled(false);
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (!(event.getInventory().getHolder() instanceof ComposeGui gui)) {
                return;
            }
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < event.getInventory().getSize() && !gui.isInputSlot(rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }

        @EventHandler
        public void onClose(InventoryCloseEvent event) {
            if (event.getInventory().getHolder() instanceof ComposeGui gui) {
                gui.onClose();
            }
        }
    }
}
