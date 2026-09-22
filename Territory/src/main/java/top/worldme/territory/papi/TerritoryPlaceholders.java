package top.worldme.territory.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import top.worldme.Territory;
import top.worldme.territory.data.OwnerType;
import top.worldme.territory.data.Region;
import top.worldme.territory.data.SubRegion;
import top.worldme.territory.manager.RegionManager;

/**
 * PlaceholderAPI 占位符：%worldmeterritory_<key>%
 * 支持：count / max / has / current / current_owner / current_sub
 */
public class TerritoryPlaceholders extends PlaceholderExpansion {

    private final Territory plugin;

    public TerritoryPlaceholders(Territory plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "worldmeterritory";
    }

    @Override
    public @NotNull String getAuthor() {
        return "WorldmeQC";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        RegionManager manager = plugin.getRegionManager();
        Player online = player.getPlayer();
        return switch (params.toLowerCase()) {
            case "count" -> String.valueOf(manager.countByOwner(OwnerType.PLAYER, player.getUniqueId()));
            case "has" -> String.valueOf(manager.countByOwner(OwnerType.PLAYER, player.getUniqueId()) > 0);
            case "max" -> online == null ? "" : String.valueOf(manager.maxClaims(online));
            case "current" -> online == null ? "野外" : currentName(manager, online);
            case "current_owner" -> online == null ? "" : currentOwner(manager, online);
            case "current_sub" -> online == null ? "" : currentSub(manager, online);
            default -> null;
        };
    }

    private String currentName(RegionManager manager, Player player) {
        Region region = manager.getRegionAt(player.getLocation());
        return region == null ? "野外" : region.name();
    }

    private String currentOwner(RegionManager manager, Player player) {
        Region region = manager.getRegionAt(player.getLocation());
        if (region == null) {
            return "";
        }
        String name = Bukkit.getOfflinePlayer(region.ownerId()).getName();
        return name == null ? "未知" : name;
    }

    private String currentSub(RegionManager manager, Player player) {
        SubRegion sub = manager.getSubRegionAt(player.getLocation());
        return sub == null ? "" : sub.name();
    }
}
