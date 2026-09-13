package top.worldme.ownership.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class OwnerKeys {

    public final NamespacedKey owner;
    public final NamespacedKey ownerName;
    public final NamespacedKey bindTime;
    public final NamespacedKey loreLines;

    public OwnerKeys(JavaPlugin plugin) {
        this.owner = new NamespacedKey(plugin, "item_owner");
        this.ownerName = new NamespacedKey(plugin, "item_owner_name");
        this.bindTime = new NamespacedKey(plugin, "item_bind_time");
        this.loreLines = new NamespacedKey(plugin, "item_bind_lore_lines");
    }
}
