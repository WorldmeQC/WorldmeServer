package top.worldme.cleaner.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.cleaner.config.CleanerConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClearManager {

    private static final int TICKS_PER_SECOND = 20;

    private final JavaPlugin plugin;
    private final CleanerConfig config;
    private final List<TrashEntry> trash = new ArrayList<>();

    private int taskId = -1;
    private final List<Integer> pendingTaskIds = new ArrayList<>();

    public ClearManager(JavaPlugin plugin, CleanerConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    // ---------- 垃圾桶 ----------

    public List<TrashEntry> getTrash() {
        return Collections.unmodifiableList(trash);
    }

    public int trashSize() {
        return trash.size();
    }

    public void clearTrash() {
        trash.clear();
    }

    public boolean removeFromTrash(ItemStack item) {
        for (int i = 0; i < trash.size(); i++) {
            if (trash.get(i).item().isSimilar(item)) {
                trash.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * 将清理到的掉落物加入垃圾桶；超过容量上限时按加入时间从旧到新淘汰。
     */
    private void addTrashItems(List<ItemStack> items) {
        if (!config.trashEnabled() || items.isEmpty()) {
            return;
        }
        long now = System.currentTimeMillis();
        for (ItemStack item : items) {
            addToTrashInternal(item, now);
        }
        evictExcess();
    }

    private void addToTrashInternal(ItemStack item, long addedAt) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        int maxStackSize = item.getMaxStackSize();
        int remaining = item.getAmount();

        for (TrashEntry entry : trash) {
            if (remaining <= 0) {
                break;
            }
            ItemStack existing = entry.item();
            if (!existing.isSimilar(item)) {
                continue;
            }

            int canAdd = maxStackSize - existing.getAmount();
            if (canAdd <= 0) {
                continue;
            }

            int add = Math.min(canAdd, remaining);
            existing.setAmount(existing.getAmount() + add);
            remaining -= add;
        }

        if (remaining > 0) {
            ItemStack leftover = item.clone();
            leftover.setAmount(remaining);
            trash.add(new TrashEntry(leftover, addedAt));
        }
    }

    private void evictExcess() {
        int max = config.trashMaxItems();
        if (max <= 0) {
            return;
        }
        while (trash.size() > max) {
            trash.remove(0);
        }
    }

    // ---------- 定时清理 ----------

    public void start() {
        cancel();
        if (!config.enabled()) {
            return;
        }
        int intervalTicks = config.interval() * TICKS_PER_SECOND;
        taskId = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, () -> scheduleClearWithCountdown(config.interval()), intervalTicks, intervalTicks)
                .getTaskId();
    }

    public void cancel() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        for (int id : pendingTaskIds) {
            Bukkit.getScheduler().cancelTask(id);
        }
        pendingTaskIds.clear();
    }

    private void scheduleClearWithCountdown(int totalSeconds) {
        for (int reminderSec : config.reminders()) {
            if (totalSeconds < reminderSec) {
                continue;
            }
            final int sec = reminderSec;
            long delay = (totalSeconds - sec) * TICKS_PER_SECOND;
            pendingTaskIds.add(plugin.getServer().getScheduler()
                    .runTaskLater(plugin, () -> broadcastCountdown(sec), delay)
                    .getTaskId());
        }

        pendingTaskIds.add(plugin.getServer().getScheduler()
                .runTaskLater(plugin, this::clearItems, totalSeconds * TICKS_PER_SECOND)
                .getTaskId());
    }

    private void broadcastCountdown(int seconds) {
        String msg;
        if (seconds >= 60) {
            msg = config.getMessage("countdown-min", Map.of("min", String.valueOf(seconds / 60)));
        } else if (seconds <= 3) {
            msg = config.getMessage("countdown-final", Map.of("sec", String.valueOf(seconds)));
        } else {
            msg = config.getMessage("countdown-sec", Map.of("sec", String.valueOf(seconds)));
        }
        broadcast(msg);
    }

    private void clearItems() {
        int count = 0;
        List<ItemStack> savedItems = new ArrayList<>();

        for (World world : plugin.getServer().getWorlds()) {
            if (!config.shouldClearWorld(world.getName())) {
                continue;
            }
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Item item)) {
                    continue;
                }
                if (config.trashEnabled()) {
                    savedItems.add(item.getItemStack().clone());
                }
                item.remove();
                count++;
            }
        }

        addTrashItems(savedItems);

        broadcast(config.getMessage("cleared", Map.of("count", String.valueOf(count))));
    }

    private void broadcast(String msg) {
        if (msg == null || msg.isEmpty()) {
            return;
        }
        String prefix = config.getMessage("prefix");
        Component component = MiniMessage.miniMessage().deserialize(prefix + msg);
        Bukkit.broadcast(component);
    }

    public record TrashEntry(ItemStack item, long addedAt) {
    }
}