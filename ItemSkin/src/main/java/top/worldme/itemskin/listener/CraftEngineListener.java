package top.worldme.itemskin.listener;

import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import top.worldme.itemskin.ItemSkinConfig;
import top.worldme.itemskin.service.SkinService;

public class CraftEngineListener implements Listener {

    private final ItemSkinConfig config;
    private final SkinService skinService;

    public CraftEngineListener(ItemSkinConfig config, SkinService skinService) {
        this.config = config;
        this.skinService = skinService;
    }

    @EventHandler
    public void onCraftEngineReload(CraftEngineReloadEvent event) {
        skinService.clearSamples();
        config.reload();
    }
}
