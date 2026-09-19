package top.worldme.market.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.StringReader;

public class MarketItems {

    private MarketItems() {
    }

    /**
     * 将物品序列化为 YAML 字符串，存入数据库 item_data 列。
     */
    public static String serialize(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return null;
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("item", item);
        return config.saveToString();
    }

    /**
     * 从 YAML 字符串反序列化物品。
     */
    public static ItemStack deserialize(String yaml) {
        if (yaml == null || yaml.isBlank()) {
            return null;
        }
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
            Object raw = config.get("item");
            if (raw instanceof ItemStack item) {
                return item.getType().isAir() ? null : item;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
