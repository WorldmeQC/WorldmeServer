package top.worldme.ownership.binding;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import top.worldme.ownership.config.OwnerConfig;
import top.worldme.ownership.util.OwnerKeys;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BindManager {

    private final OwnerConfig config;
    private final OwnerKeys keys;

    public BindManager(OwnerConfig config, OwnerKeys keys) {
        this.config = config;
        this.keys = keys;
    }

    public String itemId(ItemStack item) {
        return item.getType().name();
    }

    public String getOwnerUuid(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(keys.owner, PersistentDataType.STRING);
    }

    public String getOwnerName(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(keys.ownerName, PersistentDataType.STRING);
    }

    public boolean isBound(ItemStack item) {
        return getOwnerUuid(item) != null;
    }

    public boolean isOwnedBy(ItemStack item, UUID uuid) {
        String owner = getOwnerUuid(item);
        return owner != null && uuid != null && owner.equals(uuid.toString());
    }

    public void bind(ItemStack item, Player owner) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keys.owner, PersistentDataType.STRING, owner.getUniqueId().toString());
        pdc.set(keys.ownerName, PersistentDataType.STRING, owner.getName());
        pdc.set(keys.bindTime, PersistentDataType.LONG, System.currentTimeMillis());

        if (config.loreEnabled() && !config.loreLines().isEmpty()) {
            List<Component> lore = meta.lore();
            List<Component> newLore = lore == null ? new ArrayList<>() : new ArrayList<>(lore);
            for (String line : config.loreLines()) {
                newLore.add(MiniMessage.miniMessage().deserialize(line.replace("%owner%", owner.getName())));
            }
            meta.lore(newLore);
            pdc.set(keys.loreLines, PersistentDataType.INTEGER, config.loreLines().size());
        }
        item.setItemMeta(meta);
    }

    public boolean unbind(ItemStack item) {
        if (!isBound(item)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer count = pdc.get(keys.loreLines, PersistentDataType.INTEGER);
        if (count != null && count > 0) {
            List<Component> lore = meta.lore();
            if (lore != null) {
                List<Component> newLore = new ArrayList<>(lore);
                for (int i = 0; i < count && !newLore.isEmpty(); i++) {
                    newLore.remove(newLore.size() - 1);
                }
                meta.lore(newLore.isEmpty() ? null : newLore);
            }
        }
        pdc.remove(keys.owner);
        pdc.remove(keys.ownerName);
        pdc.remove(keys.bindTime);
        pdc.remove(keys.loreLines);
        item.setItemMeta(meta);
        return true;
    }
}
