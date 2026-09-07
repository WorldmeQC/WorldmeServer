package top.worldme.fishing.util;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class QuestKeys {

    public final NamespacedKey owner;
    public final NamespacedKey cycle;

    public QuestKeys(JavaPlugin plugin) {
        this.owner = new NamespacedKey(plugin, "quest_owner");
        this.cycle = new NamespacedKey(plugin, "quest_cycle");
    }
}
