package top.worldme.guild.data;

import java.util.UUID;

public record GuildFundRequest(int id, int guildId, UUID applicant, double amount, long createdAt) {
}
