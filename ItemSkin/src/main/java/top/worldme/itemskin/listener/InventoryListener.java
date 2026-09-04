package top.worldme.itemskin.listener;

import net.momirealms.craftengine.bukkit.api.CraftEngineItems;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import top.worldme.itemskin.ItemSkinConfig;
import top.worldme.itemskin.data.Skin;
import top.worldme.itemskin.service.SkinService;
import top.worldme.itemskin.utils.ItemSkinKeys;
import top.worldme.itemskin.utils.ItemType;

@SuppressWarnings("deprecation")
public class InventoryListener implements Listener {

    private final ItemSkinConfig config;
    private final SkinService skinService;

    public InventoryListener(ItemSkinConfig config, SkinService skinService) {
        this.config = config;
        this.skinService = skinService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        GameMode mode = player.getGameMode();
        if (mode != GameMode.SURVIVAL && mode != GameMode.ADVENTURE) return;

        InventoryType clickedType = event.getClickedInventory() != null ? event.getClickedInventory().getType() : null;
        InventoryType topType = event.getInventory().getType();
        if (clickedType != InventoryType.PLAYER && topType != InventoryType.PLAYER) return;

        if (!event.isLeftClick()) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor == null || cursor.getType() == Material.AIR) return;
        if (!CraftEngineItems.isCustomItem(cursor)) return;

        String skinId = getCustomItemId(cursor);
        if (skinId == null) return;

        Skin skin = config.getSkinById(skinId);
        if (skin == null) return;

        if (current == null || current.getType() == Material.AIR) return;
        if (CraftEngineItems.isCustomItem(current)) {
            player.sendMessage("§c该物品已经是自定义物品，无法应用外观。");
            event.setCancelled(true);
            return;
        }

        if (current.hasItemMeta() && current.getItemMeta().getPersistentDataContainer().has(ItemSkinKeys.SKIN_ID, PersistentDataType.STRING)) {
            player.sendMessage("§c该物品已绑定外观，请先解除后再应用新的外观。");
            event.setCancelled(true);
            return;
        }

        ItemType targetType = ItemType.fromItemStack(current);
        if (targetType == null) {
            player.sendMessage("§c该物品类型不支持应用外观。");
            event.setCancelled(true);
            return;
        }

        if (!skin.canApplyTo(targetType)) {
            player.sendMessage("§c该外观不适用于此物品类型。");
            event.setCancelled(true);
            return;
        }

        if (current.getAmount() > 1) {
            player.sendMessage("§c请将要应用外观的武器单独拆分出来。");
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        if (skinService.applySkin(current, skin)) {
            event.getClickedInventory().setItem(event.getSlot(), current);

            int amount = cursor.getAmount();
            if (amount <= 1) {
                event.setCursor(null);
            } else {
                cursor.setAmount(amount - 1);
                event.setCursor(cursor);
            }
            player.sendMessage("§a已成功应用外观 §f" + skin.getDisplayName() + " §a。");
        } else {
            player.sendMessage("§c应用外观失败，请检查配置或联系管理员。");
        }
    }

    private String getCustomItemId(ItemStack item) {
        var key = CraftEngineItems.getCustomItemId(item);
        return key != null ? key.toString() : null;
    }
}
