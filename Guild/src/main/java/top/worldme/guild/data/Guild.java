package top.worldme.guild.data;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一个公会。领地为其功能载体，regionId 指向 Worldme-Territory 中的领地。
 */
public class Guild {

    private final int id;
    private String name;
    private String tag;
    private final UUID leader;
    private double balance;
    private double joinFee;
    private boolean openJoin;
    private int level;
    private long exp;
    private int regionId;
    private final long createdAt;
    private final Map<UUID, GuildMember> members = new ConcurrentHashMap<>();
    private final Map<String, GuildRank> ranks = new ConcurrentHashMap<>();
    private final Map<String, GuildFeature> features = new ConcurrentHashMap<>();

    public Guild(int id, String name, String tag, UUID leader, double balance, double joinFee,
                 boolean openJoin, int level, long exp, int regionId, long createdAt) {
        this.id = id;
        this.name = name;
        this.tag = tag;
        this.leader = leader;
        this.balance = balance;
        this.joinFee = joinFee;
        this.openJoin = openJoin;
        this.level = level;
        this.exp = exp;
        this.regionId = regionId;
        this.createdAt = createdAt;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String tag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public UUID leader() {
        return leader;
    }

    public double balance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public double joinFee() {
        return joinFee;
    }

    public void setJoinFee(double joinFee) {
        this.joinFee = joinFee;
    }

    public boolean openJoin() {
        return openJoin;
    }

    public void setOpenJoin(boolean openJoin) {
        this.openJoin = openJoin;
    }

    public int level() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long exp() {
        return exp;
    }

    public void setExp(long exp) {
        this.exp = exp;
    }

    public int regionId() {
        return regionId;
    }

    public void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    public long createdAt() {
        return createdAt;
    }

    public Map<UUID, GuildMember> members() {
        return Collections.unmodifiableMap(members);
    }

    public Collection<GuildMember> memberList() {
        return Collections.unmodifiableCollection(members.values());
    }

    public int memberCount() {
        return members.size();
    }

    public GuildMember member(UUID uuid) {
        return members.get(uuid);
    }

    public boolean isMember(UUID uuid) {
        return members.containsKey(uuid);
    }

    public void addMember(GuildMember member) {
        members.put(member.uuid(), member);
    }

    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    public Map<String, GuildRank> ranks() {
        return Collections.unmodifiableMap(ranks);
    }

    public GuildRank rank(String key) {
        return ranks.get(key);
    }

    public void addRank(GuildRank rank) {
        ranks.put(rank.key(), rank);
    }

    public void removeRank(String key) {
        ranks.remove(key);
    }

    public String rankKeyOf(UUID uuid) {
        GuildMember member = members.get(uuid);
        return member == null ? null : member.rankKey();
    }

    public boolean hasPermission(UUID uuid, GuildPermission permission) {
        if (leader.equals(uuid)) {
            return true;
        }
        GuildMember member = members.get(uuid);
        if (member == null) {
            return false;
        }
        GuildRank rank = ranks.get(member.rankKey());
        return rank != null && rank.has(permission);
    }

    public Map<String, GuildFeature> features() {
        return Collections.unmodifiableMap(features);
    }

    public GuildFeature feature(String key) {
        return features.get(key);
    }

    public void addFeature(GuildFeature feature) {
        features.put(feature.key(), feature);
    }
}
