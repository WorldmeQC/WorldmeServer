package top.worldme.itemskin;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.worldme.ItemSkin;
import top.worldme.itemskin.data.Skin;
import top.worldme.itemskin.utils.ItemType;

import java.util.*;
import java.util.logging.Level;

@SuppressWarnings("deprecation")
public class ItemSkinConfig {

    private final ItemSkin plugin;
    private final Map<String, Skin> skinsById = new HashMap<>();
    private boolean loaded = false;
    private boolean allowCustomItemTargets = false;

    public ItemSkinConfig(ItemSkin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
        load(plugin.getConfig());
    }

    public void load(FileConfiguration config) {
        skinsById.clear();
        loaded = false;
        allowCustomItemTargets = config.getBoolean("allow-custom-item-targets", false);

        ConfigurationSection skinsSection = config.getConfigurationSection("skins");
        if (skinsSection == null) {
            plugin.getLogger().warning("config.yml 中未找到 skins 配置段。");
            loaded = true;
            return;
        }

        int success = 0;
        int failed = 0;

        for (String key : skinsSection.getKeys(false)) {
            ConfigurationSection skinSection = skinsSection.getConfigurationSection(key);
            if (skinSection == null) {
                plugin.getLogger().warning("皮肤配置 " + key + " 不是有效配置段，已跳过。");
                failed++;
                continue;
            }

            String id = skinSection.getString("id", key);
            List<String> applicableNames = skinSection.getStringList("applicable");
            if (applicableNames.isEmpty()) {
                String single = skinSection.getString("applicable");
                if (single != null && !single.isEmpty()) {
                    applicableNames = Collections.singletonList(single);
                }
            }

            Set<ItemType> applicableTypes = EnumSet.noneOf(ItemType.class);
            for (String name : applicableNames) {
                try {
                    applicableTypes.add(ItemType.valueOf(name.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("皮肤 " + id + " 包含未知的适用类型: " + name);
                }
            }

            if (applicableTypes.isEmpty()) {
                plugin.getLogger().warning("皮肤 " + id + " 没有配置有效的适用类型，已跳过。");
                failed++;
                continue;
            }

            String displayName = skinSection.getString("display-name", null);

            BukkitItemDefinition definition = null;
            try {
                definition = CraftEngineItems.byId(id);
            } catch (IllegalStateException e) {
                plugin.getLogger().warning("CraftEngine 尚未加载，无法校验皮肤 " + id + "。");
                failed++;
                continue;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "校验皮肤 " + id + " 时发生异常", e);
                failed++;
                continue;
            }

            if (definition == null) {
                plugin.getLogger().warning("未找到 CE 皮肤物品: " + id + "，已跳过。");
                failed++;
                continue;
            }

            if (displayName == null || displayName.isEmpty()) {
                try {
                    ItemStack sample = definition.buildBukkitItem();
                    if (sample.hasItemMeta()) {
                        ItemMeta meta = sample.getItemMeta();
                        if (meta.hasDisplayName()) {
                            displayName = meta.getDisplayName();
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "获取皮肤 " + id + " 显示名失败", e);
                }
                if (displayName == null || displayName.isEmpty()) {
                    displayName = id;
                }
            }

            Skin skin = new Skin(id, displayName, applicableTypes);
            skinsById.put(id, skin);
            success++;
        }

        loaded = true;
        plugin.getLogger().info("ItemSkin 配置加载完成：成功 " + success + " 个，失败 " + failed + " 个。");
    }

    public Skin getSkinById(String id) {
        return skinsById.get(id);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public boolean isAllowCustomItemTargets() {
        return allowCustomItemTargets;
    }

    public Collection<Skin> getAllSkins() {
        return Collections.unmodifiableCollection(skinsById.values());
    }
}
