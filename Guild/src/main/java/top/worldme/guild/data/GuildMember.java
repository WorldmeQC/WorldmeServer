package top.worldme.guild.data;

import java.util.UUID;

/**
 * 公会成员。
 */
public class GuildMember {

    private final UUID uuid;
    private String rankKey;
    private final long joinedAt;
    private double feePaid;

    public GuildMember(UUID uuid, String rankKey, long joinedAt, double feePaid) {
        this.uuid = uuid;
        this.rankKey = rankKey;
        this.joinedAt = joinedAt;
        this.feePaid = feePaid;
    }

    public UUID uuid() {
        return uuid;
    }

    public String rankKey() {
        return rankKey;
    }

    public void setRankKey(String rankKey) {
        this.rankKey = rankKey;
    }

    public long joinedAt() {
        return joinedAt;
    }

    public double feePaid() {
        return feePaid;
    }

    public void setFeePaid(double feePaid) {
        this.feePaid = feePaid;
    }
}
