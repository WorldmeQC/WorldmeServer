package top.worldme.mail.util;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MailItems {

    private MailItems() {
    }

    /**
     * 将物品列表序列化为 YAML 字符串，存入数据库 items 列。
     */
    public static String serialize(List<ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        List<ItemStack> list = new ArrayList<>();
        for (ItemStack item : items) {
            if (item != null && !item.getType().isAir()) {
                list.add(item);
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("items", list);
        return config.saveToString();
    }

    /**
     * 从 YAML 字符串反序列化物品列表。
     */
    public static List<ItemStack> deserialize(String yaml) {
        List<ItemStack> items = new ArrayList<>();
        if (yaml == null || yaml.isBlank()) {
            return items;
        }
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(new StringReader(yaml));
            List<?> list = config.getList("items");
            if (list == null) {
                return items;
            }
            for (Object obj : list) {
                if (obj instanceof ItemStack item) {
                    if (!item.getType().isAir()) {
                        items.add(item);
                    }
                } else if (obj instanceof Map<?, ?> map) {
                    try {
                        Map<String, Object> stringMap = new java.util.HashMap<>();
                        for (Map.Entry<?, ?> entry : map.entrySet()) {
                            if (entry.getKey() instanceof String key) {
                                stringMap.put(key, entry.getValue());
                            }
                        }
                        ItemStack item = ItemStack.deserialize(stringMap);
                        if (item != null && !item.getType().isAir()) {
                            items.add(item);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return items;
    }
}