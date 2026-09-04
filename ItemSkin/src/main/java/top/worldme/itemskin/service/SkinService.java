package top.worldme.itemskin.service;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import net.momirealms.craftengine.bukkit.item.BukkitItemDefinition;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import top.worldme.itemskin.data.Skin;
import top.worldme.itemskin.utils.ItemAppearanceUtils;
import top.worldme.itemskin.utils.ItemSkinKeys;

import java.util.HashMap;
import java.util.Map;

public class SkinService {

    private static final String EMPTY_MODEL = "";
    private static final int NO_CMD = -1;

    private final Map<String, ItemStack> skinSamples = new HashMap<>();

    /**
     * 应用外观到目标物品。
     *
     * @param target 被应用外观的武器/工具
     * @param skin   皮肤定义
     * @return 是否成功
     */
    public boolean applySkin(ItemStack target, Skin skin) {
        if (target == null || target.getType().isAir()) {
            return false;
        }

        BukkitItemDefinition definition = CraftEngineItems.byId(skin.getId());
        if (definition == null) {
            return false;
        }

        ItemStack sample = getSample(definition, skin.getId());
        if (sample == null) {
            return false;
        }

        ItemMeta targetMeta = target.getItemMeta();
        if (targetMeta == null) {
            targetMeta = new ItemStack(target.getType()).getItemMeta();
            if (targetMeta == null) {
                return false;
            }
        }

        PersistentDataContainer pdc = targetMeta.getPersistentDataContainer();

        if (ItemAppearanceUtils.getString(pdc, ItemSkinKeys.SKIN_ID) != null) {
            return false;
        }

        // 备份原模型
        NamespacedKey originalModel = ItemAppearanceUtils.getItemModel(targetMeta);
        int originalCmd = ItemAppearanceUtils.getCustomModelData(targetMeta);

        // 从皮肤样本读取模型；若样本未设置 item_model，则回退使用皮肤 ID
        ItemMeta sampleMeta = sample.hasItemMeta() ? sample.getItemMeta() : null;
        NamespacedKey skinModel = sampleMeta != null ? ItemAppearanceUtils.getItemModel(sampleMeta) : null;
        Integer skinCmd = sampleMeta != null && ItemAppearanceUtils.hasCustomModelData(sampleMeta)
                ? ItemAppearanceUtils.getCustomModelData(sampleMeta)
                : null;

        if (skinModel == null) {
            try {
                skinModel = NamespacedKey.fromString(skin.getId());
            } catch (IllegalArgumentException ignored) {
            }
        }

        // 写入 PDC
        ItemAppearanceUtils.setString(pdc, ItemSkinKeys.SKIN_ID, skin.getId());
        ItemAppearanceUtils.setString(pdc, ItemSkinKeys.ORIGINAL_MODEL, originalModel != null ? originalModel.toString() : EMPTY_MODEL);
        ItemAppearanceUtils.setInt(pdc, ItemSkinKeys.ORIGINAL_CMD, originalCmd);

        // 应用模型
        if (skinModel != null) {
            ItemAppearanceUtils.setItemModel(targetMeta, skinModel);
        } else {
            ItemAppearanceUtils.setItemModel(targetMeta, null);
        }
        if (skinCmd != null && skinCmd != NO_CMD) {
            ItemAppearanceUtils.setCustomModelData(targetMeta, skinCmd);
        } else {
            ItemAppearanceUtils.setCustomModelData(targetMeta, null);
        }

        // 追加 Lore
        String loreLine = "§7已应用外观：" + skin.getDisplayName();
        ItemAppearanceUtils.appendLore(targetMeta, loreLine);
        ItemAppearanceUtils.setString(pdc, ItemSkinKeys.LORE_SUFFIX, loreLine);

        ItemAppearanceUtils.updateItemMeta(target, targetMeta);
        return true;
    }

    /**
     * 解除目标物品上的外观，返还皮肤物品。
     *
     * @param player 玩家
     * @param target 主手物品
     * @return 是否成功
     */
    public boolean unloadSkin(Player player, ItemStack target) {
        if (target == null || target.getType().isAir()) {
            return false;
        }

        ItemMeta meta = target.getItemMeta();
        if (meta == null) {
            return false;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String skinId = ItemAppearanceUtils.getString(pdc, ItemSkinKeys.SKIN_ID);
        if (skinId == null || skinId.isEmpty()) {
            return false;
        }

        BukkitItemDefinition definition = CraftEngineItems.byId(skinId);
        if (definition == null) {
            return false;
        }

        ItemStack returnSkin = definition.buildBukkitItem();
        if (returnSkin == null || returnSkin.getType().isAir()) {
            return false;
        }

        // 恢复模型
        String originalModelStr = ItemAppearanceUtils.getString(pdc, ItemSkinKeys.ORIGINAL_MODEL);
        int originalCmd = ItemAppearanceUtils.getInt(pdc, ItemSkinKeys.ORIGINAL_CMD, NO_CMD);

        if (originalModelStr == null || originalModelStr.isEmpty() || EMPTY_MODEL.equals(originalModelStr)) {
            ItemAppearanceUtils.setItemModel(meta, null);
        } else {
            try {
                ItemAppearanceUtils.setItemModel(meta, NamespacedKey.fromString(originalModelStr));
            } catch (IllegalArgumentException e) {
                ItemAppearanceUtils.setItemModel(meta, null);
            }
        }

        if (originalCmd == NO_CMD) {
            ItemAppearanceUtils.setCustomModelData(meta, null);
        } else {
            ItemAppearanceUtils.setCustomModelData(meta, originalCmd);
        }

        // 恢复 Lore
        String loreSuffix = ItemAppearanceUtils.getString(pdc, ItemSkinKeys.LORE_SUFFIX);
        if (loreSuffix != null) {
            ItemAppearanceUtils.removeLoreSuffix(meta, loreSuffix);
        }

        // 清除 PDC
        ItemAppearanceUtils.remove(pdc, ItemSkinKeys.SKIN_ID);
        ItemAppearanceUtils.remove(pdc, ItemSkinKeys.ORIGINAL_MODEL);
        ItemAppearanceUtils.remove(pdc, ItemSkinKeys.ORIGINAL_CMD);
        ItemAppearanceUtils.remove(pdc, ItemSkinKeys.LORE_SUFFIX);

        ItemAppearanceUtils.updateItemMeta(target, meta);

        giveOrDrop(player, returnSkin);
        return true;
    }

    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            Location loc = player.getLocation();
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(loc, drop);
            }
        }
    }

    private ItemStack getSample(BukkitItemDefinition definition, String id) {
        ItemStack sample = skinSamples.get(id);
        if (sample == null) {
            sample = definition.buildBukkitItem();
            if (sample != null && !sample.getType().isAir()) {
                skinSamples.put(id, sample.clone());
            }
        }
        return sample != null ? sample.clone() : null;
    }

    public void clearSamples() {
        skinSamples.clear();
    }
}
