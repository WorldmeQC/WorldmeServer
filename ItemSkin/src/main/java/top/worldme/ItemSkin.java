package top.worldme;

import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.itemskin.ItemSkinCommand;
import top.worldme.itemskin.ItemSkinConfig;
import top.worldme.itemskin.listener.CraftEngineListener;
import top.worldme.itemskin.listener.InventoryListener;
import top.worldme.itemskin.service.SkinService;

public class ItemSkin extends JavaPlugin {

    private static ItemSkin instance;
    private ItemSkinConfig itemSkinConfig;
    private SkinService skinService;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.itemSkinConfig = new ItemSkinConfig(this);
        this.skinService = new SkinService();


        // 注册事件
        getServer().getPluginManager().registerEvents(new InventoryListener(itemSkinConfig, skinService), this);
        getServer().getPluginManager().registerEvents(new CraftEngineListener(itemSkinConfig, skinService), this);

        // 注册命令
        ItemSkinCommand command = new ItemSkinCommand(itemSkinConfig, skinService);
        var itemSkinCommand = getCommand("itemskin");
        if (itemSkinCommand != null) {
            itemSkinCommand.setExecutor(command);
            itemSkinCommand.setTabCompleter(command);
        }

        getLogger().info("Worldme-ItemSkin 已加载。");
    }

    @Override
    public void onDisable() {
        if (skinService != null) {
            skinService.clearSamples();
        }
        getLogger().info("Worldme-ItemSkin 已卸载。");
    }

    public static ItemSkin getInstance() {
        return instance;
    }

    public ItemSkinConfig getItemSkinConfig() {
        return itemSkinConfig;
    }

    public SkinService getSkinService() {
        return skinService;
    }
}
