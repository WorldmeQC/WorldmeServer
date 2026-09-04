package top.worldme.trigger.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.AsyncStructureSpawnEvent;
import org.bukkit.generator.structure.StructureType;

public class StructureGenerateListener implements Listener {

    @EventHandler
    public void onStructureGenerate(AsyncStructureSpawnEvent event){
        // 禁用末地城生成
        if (event.getStructure().getStructureType() == StructureType.END_CITY){
            event.setCancelled(true);
        }
    }
}
