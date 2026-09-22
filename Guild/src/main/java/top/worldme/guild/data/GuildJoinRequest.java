package top.worldme.guild.data;

import java.util.UUID;

public record GuildJoinRequest(int id, int guildId, UUID applicant, long createdAt) {
}
