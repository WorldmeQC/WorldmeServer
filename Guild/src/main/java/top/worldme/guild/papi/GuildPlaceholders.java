package top.worldme.guild.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import top.worldme.Guild;
import top.worldme.guild.data.GuildMember;
import top.worldme.guild.data.GuildRank;
import top.worldme.guild.manager.GuildManager;

/**
 * PlaceholderAPI 占位符：%worldmeguild_<key>%
 * 支持：has / name / tag / leader / level / balance / members / max_members / join_fee / open / rank
 */
public class GuildPlaceholders extends PlaceholderExpansion {

    private final Guild plugin;

    public GuildPlaceholders(Guild plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "worldmeguild";
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
        GuildManager manager = plugin.getGuildManager();
        top.worldme.guild.data.Guild guild = manager.guildOf(player.getUniqueId());
        String key = params.toLowerCase();
        if (key.equals("has")) {
            return String.valueOf(guild != null);
        }
        if (guild == null) {
            return "";
        }
        return switch (key) {
            case "name" -> guild.name();
            case "tag" -> guild.tag();
            case "leader" -> nameOf(guild.leader());
            case "level" -> String.valueOf(guild.level());
            case "balance" -> manager.vault().format(guild.balance());
            case "members" -> String.valueOf(guild.memberCount());
            case "max_members" -> String.valueOf(manager.memberCap(guild));
            case "join_fee" -> manager.vault().format(guild.joinFee());
            case "open" -> String.valueOf(guild.openJoin());
            case "rank" -> rankOf(guild, player);
            default -> null;
        };
    }

    private String rankOf(top.worldme.guild.data.Guild guild, OfflinePlayer player) {
        GuildMember member = guild.member(player.getUniqueId());
        if (member == null) {
            return "";
        }
        GuildRank rank = guild.rank(member.rankKey());
        return rank == null ? member.rankKey() : rank.display();
    }

    private String nameOf(java.util.UUID uuid) {
        String name = Bukkit.getOfflinePlayer(uuid).getName();
        return name == null ? "未知" : name;
    }
}
