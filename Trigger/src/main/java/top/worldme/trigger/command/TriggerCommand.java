package top.worldme.trigger.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.trigger.config.TriggerConfig;

public class TriggerCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final TriggerConfig triggerConfig;

    public TriggerCommand(JavaPlugin plugin, TriggerConfig triggerConfig) {
        this.plugin = plugin;
        this.triggerConfig = triggerConfig;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§e用法: /" + label + " reload");
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            triggerConfig.load();
            sender.sendMessage("§a[Worldme-Trigger] 配置已重载。");
            plugin.getLogger().info(sender.getName() + " 重载了 Worldme-Trigger 配置。");
            return true;
        }

        sender.sendMessage("§e用法: /" + label + " reload");
        return true;
    }
}
