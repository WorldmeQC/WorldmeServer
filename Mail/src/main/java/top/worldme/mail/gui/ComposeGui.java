package top.worldme.mail.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import top.worldme.mail.config.MailConfig;
import top.worldme.mail.config.MailMenuConfig;
import top.worldme.mail.config.MailMenuConfig.ItemConfig;
import top.worldme.mail.manager.MailManager;
import top.worldme.mail.util.ItemParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ComposeGui implements InventoryHolder {

    private final JavaPlugin plugin;
    private final MailMenuConfig menu;
    private final MailConfig config;
    private final MailManager mailManager;
    private final Player sender;
    private final UUID recipient;
    private final String recipientName;
    private final Inventory inventory;
    private boolean sent = false;

    public ComposeGui(JavaPlugin plugin, MailMenuConfig menu, MailConfig config, MailManager mailManager,
                      Player sender, UUID recipient, String recipientName) {
        this.plugin = plugin;
        this.menu = menu;
        this.config = config;
        this.mailManager = mailManager;
        this.sender = sender;
        this.recipient = recipient;
        this.recipientName = recipientName;
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

        ItemStack info = buildItem(menu.composeInfo(), Map.of("player", recipientName));
        if (info != null && menu.composeInfoSlot() >= 0 && menu.composeInfoSlot() < inventory.getSize()) {
            inventory.setItem(menu.composeInfoSlot(), info);
        }

        placeHint(menu.subjectHintSlot(), menu.subjectHint(), null);
        placeHint(menu.contentHintSlot(), menu.contentHint(), null);
        placeHint(menu.commandsHintSlot(), menu.commandsHint(), null);

        ItemStack send = buildItem(menu.sendButton(), null);
        if (send != null && menu.sendSlot() >= 0 && menu.sendSlot() < inventory.getSize()) {
            inventory.setItem(menu.sendSlot(), send);
        }
        ItemStack cancel = buildItem(menu.cancelButton(), null);
        if (cancel != null && menu.cancelSlot() >= 0 && menu.cancelSlot() < inventory.getSize()) {
            inventory.setItem(menu.cancelSlot(), cancel);
        }
    }

    private void placeHint(int slot, ItemConfig itemConfig, Map<String, String> placeholders) {
        if (slot < 0 || slot >= inventory.getSize()) {
            return;
        }
        ItemStack item = buildItem(itemConfig, placeholders);
        if (item != null) {
            inventory.setItem(slot, item);
        }
    }

    public boolean isInputSlot(int slot) {
        if (slot == menu.subjectSlot() || slot == menu.contentSlot() || slot == menu.commandsSlot()) {
            return true;
        }
        return menu.attachmentSlots().contains(slot);
    }

    private void handleHint(int slot) {
        if (slot == menu.contentHintSlot() || slot == menu.commandsHintSlot()) {
            Map<Integer, ItemStack> leftover = sender.getInventory().addItem(new ItemStack(Material.WRITABLE_BOOK));
            for (ItemStack drop : leftover.values()) {
                sender.getWorld().dropItemNaturally(sender.getLocation(), drop);
            }
        }
    }

    public void handleSend() {
        ItemStack contentBook = inventory.getItem(menu.contentSlot());
        String content = extractBookContent(contentBook);
        if (content == null) {
            sendMessage("need-content", null);
            return;
        }

        String subject = null;
        ItemStack subjectItem = inventory.getItem(menu.subjectSlot());
        if (subjectItem != null && !subjectItem.getType().isAir()) {
            ItemMeta meta = subjectItem.getItemMeta();
            if (meta != null && meta.hasDisplayName()) {
                subject = PlainTextComponentSerializer.plainText().serialize(meta.displayName());
            }
        }
        if (subject == null || subject.isBlank()) {
            subject = config.defaultSubject();
        }

        List<ItemStack> attachments = new ArrayList<>();
        for (int slot : menu.attachmentSlots()) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.getType().isAir()) {
                attachments.add(item.clone());
            }
        }

        List<String> commands = extractCommands(inventory.getItem(menu.commandsSlot()));

        mailManager.send(recipient, subject, content, attachments, commands);

        sendMessage("mail-sent", Map.of("player", recipientName));
        sent = true;
        clearInputSlots();
        sender.closeInventory();
    }

    private void clearInputSlots() {
        inventory.setItem(menu.subjectSlot(), null);
        inventory.setItem(menu.contentSlot(), null);
        inventory.setItem(menu.commandsSlot(), null);
        for (int slot : menu.attachmentSlots()) {
            inventory.setItem(slot, null);
        }
    }

    public void handleCancel() {
        returnItems();
        sender.closeInventory();
    }

    private void returnItems() {
        if (sent) {
            return;
        }
        List<Integer> slots = new ArrayList<>();
        slots.add(menu.subjectSlot());
        slots.add(menu.contentSlot());
        slots.add(menu.commandsSlot());
        slots.addAll(menu.attachmentSlots());
        for (int slot : slots) {
            ItemStack item = inventory.getItem(slot);
            if (item == null || item.getType().isAir()) {
                continue;
            }
            inventory.setItem(slot, null);
            Map<Integer, ItemStack> leftover = sender.getInventory().addItem(item);
            for (ItemStack drop : leftover.values()) {
                sender.getWorld().dropItemNaturally(sender.getLocation(), drop);
            }
        }
    }

    private String extractBookContent(ItemStack book) {
        if (book == null) {
            return null;
        }
        Material type = book.getType();
        if (type != Material.WRITABLE_BOOK && type != Material.WRITTEN_BOOK) {
            return null;
        }
        ItemMeta meta = book.getItemMeta();
        if (!(meta instanceof BookMeta bookMeta)) {
            return null;
        }
        List<String> pages = bookMeta.getPages();
        StringBuilder sb = new StringBuilder();
        for (String page : pages) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(page);
        }
        if (sb.toString().isBlank()) {
            return null;
        }
        return sb.toString();
    }

    private List<String> extractCommands(ItemStack book) {
        List<String> commands = new ArrayList<>();
        if (book == null) {
            return commands;
        }
        Material type = book.getType();
        if (type != Material.WRITABLE_BOOK && type != Material.WRITTEN_BOOK) {
            return commands;
        }
        ItemMeta meta = book.getItemMeta();
        if (meta instanceof BookMeta bookMeta) {
            for (String page : bookMeta.getPages()) {
                String line = page.strip();
                if (!line.isEmpty()) {
                    commands.add(line);
                }
            }
        }
        return commands;
    }

    private void sendMessage(String key, Map<String, String> placeholders) {
        String text = config.getMessage(key, placeholders);
        if (text == null || text.isEmpty()) {
            return;
        }
        sender.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage("prefix") + text));
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
                } else if (slot == gui.menu.sendSlot()) {
                    event.setCancelled(true);
                    gui.handleSend();
                } else if (slot == gui.menu.cancelSlot()) {
                    event.setCancelled(true);
                    gui.handleCancel();
                } else {
                    event.setCancelled(true);
                    gui.handleHint(slot);
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
                gui.returnItems();
            }
        }
    }
}