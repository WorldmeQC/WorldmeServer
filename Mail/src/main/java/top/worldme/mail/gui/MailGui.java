package top.worldme.mail.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import top.worldme.mail.config.MailConfig;
import top.worldme.mail.config.MailMenuConfig;
import top.worldme.mail.config.MailMenuConfig.ItemConfig;
import top.worldme.mail.data.MailMessage;
import top.worldme.mail.manager.MailManager;
import top.worldme.mail.util.ItemParser;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MailGui implements InventoryHolder {

    private final JavaPlugin plugin;
    private final MailMenuConfig menu;
    private final MailConfig config;
    private final MailManager mailManager;
    private final Player player;
    private final Inventory inventory;
    private int page = 0;

    public MailGui(JavaPlugin plugin, MailMenuConfig menu, MailConfig config, MailManager mailManager, Player player) {
        this.plugin = plugin;
        this.menu = menu;
        this.config = config;
        this.mailManager = mailManager;
        this.player = player;
        this.inventory = Bukkit.createInventory(
                this,
                menu.size(),
                MiniMessage.miniMessage().deserialize(menu.title())
        );
        render();
    }

    private int mailCount() {
        return mailManager.getMails(player.getUniqueId()).size();
    }

    private int maxPage() {
        int slots = menu.mailSlots().size();
        if (slots <= 0) {
            return 0;
        }
        return Math.max(0, (mailCount() - 1) / slots);
    }

    private void render() {
        inventory.clear();
        page = Math.max(0, Math.min(page, maxPage()));

        ItemStack decoration = buildItem(menu.decoration(), null);
        if (decoration != null) {
            for (int slot : menu.decorationSlots()) {
                if (slot >= 0 && slot < inventory.getSize()) {
                    inventory.setItem(slot, decoration.clone());
                }
            }
        }

        ItemStack info = buildItem(menu.info(), Map.of(
                "count", String.valueOf(mailCount()),
                "page", String.valueOf(page + 1),
                "pages", String.valueOf(maxPage() + 1),
                "expire_days", String.valueOf(config.expireDays())
        ));
        if (info != null && menu.infoSlot() >= 0 && menu.infoSlot() < inventory.getSize()) {
            inventory.setItem(menu.infoSlot(), info);
        }

        ItemStack prev = buildItem(page > 0 ? menu.prevButton() : menu.prevButtonDisabled(), null);
        if (prev != null && menu.prevSlot() >= 0 && menu.prevSlot() < inventory.getSize()) {
            inventory.setItem(menu.prevSlot(), prev);
        }
        ItemStack next = buildItem(page < maxPage() ? menu.nextButton() : menu.nextButtonDisabled(), null);
        if (next != null && menu.nextSlot() >= 0 && menu.nextSlot() < inventory.getSize()) {
            inventory.setItem(menu.nextSlot(), next);
        }
        ItemStack close = buildItem(menu.closeButton(), null);
        if (close != null && menu.closeSlot() >= 0 && menu.closeSlot() < inventory.getSize()) {
            inventory.setItem(menu.closeSlot(), close);
        }

        List<MailMessage> mails = mailManager.getMails(player.getUniqueId());
        if (mails.isEmpty()) {
            ItemStack empty = buildItem(menu.empty(), null);
            if (empty != null && menu.emptySlot() >= 0 && menu.emptySlot() < inventory.getSize()) {
                inventory.setItem(menu.emptySlot(), empty);
            }
            return;
        }

        List<Integer> slots = menu.mailSlots();
        int start = page * slots.size();
        for (int i = 0; i < slots.size(); i++) {
            int slot = slots.get(i);
            if (slot < 0 || slot >= inventory.getSize()) {
                continue;
            }
            int index = start + i;
            if (index < mails.size()) {
                inventory.setItem(slot, mailIcon(mails.get(index)));
            }
        }
    }

    private ItemStack mailIcon(MailMessage mail) {
        ItemConfig iconConfig = mail.read() ? menu.mailRead() : menu.mailUnread();
        ItemStack item = ItemParser.buildItem(iconConfig.material, 1);
        if (item == null) {
            item = new ItemStack(mail.read() ? Material.BOOK : Material.PAPER);
        }

        Map<String, String> placeholders = Map.of(
                "subject", mail.subject() == null ? "" : mail.subject(),
                "sent_at", format(mail.sentAt()),
                "expire_at", format(mail.expireAt()),
                "attachment", attachmentText(mail)
        );

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        if (iconConfig.name != null && !iconConfig.name.isEmpty()) {
            meta.displayName(MiniMessage.miniMessage().deserialize(applyPlaceholders(iconConfig.name, placeholders)));
        }
        List<Component> lore = new ArrayList<>();
        for (String line : menu.mailLore()) {
            if (line.contains("%content%")) {
                String content = mail.content() == null ? "" : mail.content();
                if (content.isBlank()) {
                    lore.add(MiniMessage.miniMessage().deserialize(
                            applyPlaceholders(line.replace("%content%", ""), placeholders)));
                } else {
                    for (String contentLine : content.split("\n")) {
                        lore.add(MiniMessage.miniMessage().deserialize(
                                applyPlaceholders(line.replace("%content%", contentLine), placeholders)));
                    }
                }
            } else {
                lore.add(MiniMessage.miniMessage().deserialize(applyPlaceholders(line, placeholders)));
            }
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String attachmentText(MailMessage mail) {
        boolean hasItems = !mail.items().isEmpty();
        boolean hasCommands = !mail.commands().isEmpty();
        if (mail.claimed()) {
            return "<green>附件已领取</green>";
        }
        if (!hasItems && !hasCommands) {
            return "<gray>无附件</gray>";
        }
        StringBuilder sb = new StringBuilder("<green>右键领取附件</green>");
        if (hasItems) {
            sb.append("\n<gray>物品数量：<yellow>").append(mail.items().size()).append("</yellow>");
        }
        if (hasCommands) {
            sb.append("\n<gray>指令数量：<yellow>").append(mail.commands().size()).append("</yellow>");
        }
        return sb.toString();
    }

    private String format(long millis) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(config.dateFormat()).withZone(config.zoneId());
        return fmt.format(Instant.ofEpochMilli(millis));
    }

    public void handleClick(int slot, boolean leftClick, boolean rightClick) {
        if (slot == menu.closeSlot()) {
            player.closeInventory();
            return;
        }
        if (slot == menu.prevSlot()) {
            if (page > 0) {
                page--;
                render();
            } else {
                sendMessage("first-page", null);
            }
            return;
        }
        if (slot == menu.nextSlot()) {
            if (page < maxPage()) {
                page++;
                render();
            } else {
                sendMessage("last-page", null);
            }
            return;
        }
        int slotIndex = menu.mailSlots().indexOf(slot);
        if (slotIndex < 0) {
            return;
        }
        ItemStack current = inventory.getItem(slot);
        if (current == null || current.getType().isAir()) {
            return;
        }
        List<MailMessage> mails = mailManager.getMails(player.getUniqueId());
        int index = slotIndex + page * menu.mailSlots().size();
        if (index >= mails.size()) {
            return;
        }
        MailMessage mail = mails.get(index);

        if (leftClick) {
            if (!mail.read()) {
                mailManager.markRead(mail.id());
                sendMessage("marked-read", null);
                render();
            }
        } else if (rightClick) {
            if (!mail.items().isEmpty() || !mail.commands().isEmpty()) {
                if (mail.claimed()) {
                    sendMessage("already-claimed", null);
                } else if (mailManager.claimAttachments(mail.id(), player)) {
                    sendMessage("claimed", null);
                    render();
                } else {
                    sendMessage("inventory-full", null);
                }
            } else {
                sendMessage("no-attachments", null);
            }
        }
    }

    private void sendMessage(String key, Map<String, String> placeholders) {
        String text = config.getMessage(key, placeholders);
        if (text == null || text.isEmpty()) {
            return;
        }
        player.sendMessage(MiniMessage.miniMessage().deserialize(config.getMessage("prefix") + text));
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
            if (!(event.getInventory().getHolder() instanceof MailGui gui)) {
                return;
            }
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getInventory()) {
                return;
            }
            gui.handleClick(event.getSlot(), event.isLeftClick(), event.isRightClick());
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getInventory().getHolder() instanceof MailGui) {
                event.setCancelled(true);
            }
        }
    }
}