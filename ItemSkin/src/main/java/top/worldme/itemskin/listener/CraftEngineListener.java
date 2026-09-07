package top.worldme.itemskin.listener;

import net.momirealms.craftengine.bukkit.api.event.CraftEngineReloadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import top.worldme.itemskin.ItemSkinConfig;
import top.worldme.itemskin.service.SkinService;

public class CraftEngineListener implements Listener {

    private static final long DEBOUNCE_MILLIS = 3000;

    private final ItemSkinConfig config;
    private final SkinService skinService;
    private long lastReloadTime = 0;

    public CraftEngineListener(ItemSkinConfig config, SkinService skinService) {
        this.config = config;
        this.skinService = skinService;
    }

    @EventHandler
    public void onCraftEngineReload(CraftEngineReloadEvent event) {
        // CraftEngine 启动时可能在短时间内连续触发多次 ReloadEvent，这里做防抖
        long now = System.currentTimeMillis();
        if (now - lastReloadTime < DEBOUNCE_MILLIS) {
            return;
        }
        lastReloadTime = now;
        skinService.clearSamples();
        config.reload();
    }
}
