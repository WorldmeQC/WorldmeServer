package top.worldme.guild.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.guild.api.GuildApi;
import top.worldme.guild.config.FeatureDefinition;
import top.worldme.guild.config.GuildConfig;
import top.worldme.guild.config.StructureConfig;
import top.worldme.guild.data.Guild;
import top.worldme.guild.data.GuildDatabase;
import top.worldme.guild.data.GuildFeature;
import top.worldme.guild.data.GuildFundRequest;
import top.worldme.guild.data.GuildJoinRequest;
import top.worldme.guild.data.GuildMember;
import top.worldme.guild.data.GuildPermission;
import top.worldme.guild.data.GuildRank;
import top.worldme.guild.economy.VaultHook;
import top.worldme.guild.feature.FeatureRegistry;
import top.worldme.guild.feature.GuildFeatureProvider;
import top.worldme.guild.feature.StructureScanner;
import top.worldme.territory.api.TerritoryApi;
import top.worldme.territory.data.Direction;
import top.worldme.territory.data.OwnerType;
import top.worldme.territory.data.PermissionFlag;
import top.worldme.territory.data.Region;
import top.worldme.territory.data.RegionMember;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GuildManager implements GuildApi {

    public static final String RANK_LEADER = "leader";
    public static final String RANK_OFFICER = "officer";
    public static final String RANK_MEMBER = "member";

    private final JavaPlugin plugin;
    private final GuildConfig config;
    private final StructureConfig structureConfig;
    private final GuildDatabase database;
    private final VaultHook vault;
    private final TerritoryApi territory;
    private final FeatureRegistry registry = new FeatureRegistry();

    private final List<Guild> guilds = new CopyOnWriteArrayList<>();
    private final Map<UUID, Integer> memberIndex = new ConcurrentHashMap<>();
    private final List<GuildJoinRequest> joinRequests = new CopyOnWriteArrayList<>();
    private final List<GuildFundRequest> fundRequests = new CopyOnWriteArrayList<>();

    public GuildManager(JavaPlugin plugin, GuildConfig config, StructureConfig structureConfig,
                        GuildDatabase database, VaultHook vault, TerritoryApi territory) {
        this.plugin = plugin;
        this.config = config;
        this.structureConfig = structureConfig;
        this.database = database;
        this.vault = vault;
        this.territory = territory;
    }

    public GuildConfig config() {
        return config;
    }

    public StructureConfig structureConfig() {
        return structureConfig;
    }

    public VaultHook vault() {
        return vault;
    }

    public static UUID guildUuid(int guildId) {
        return new UUID(0L, guildId);
    }

    // ---------- 加载 ----------

    public void load() {
        guilds.clear();
        memberIndex.clear();
        joinRequests.clear();
        fundRequests.clear();

        try (Statement st = database.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM guilds")) {
            while (rs.next()) {
                Guild guild = new Guild(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("tag"),
                        UUID.fromString(rs.getString("leader")),
                        rs.getDouble("balance"),
                        rs.getDouble("join_fee"),
                        rs.getInt("open_join") == 1,
                        rs.getInt("level"),
                        rs.getLong("exp"),
                        rs.getInt("region_id"),
                        rs.getLong("created_at")
                );
                guilds.add(guild);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载公会失败: " + e.getMessage());
        }

        for (Guild guild : guilds) {
            loadRanks(guild);
            if (guild.ranks().isEmpty()) {
                createDefaultRanks(guild);
            }
            loadMembers(guild);
            loadFeatures(guild);
        }

        try (Statement st = database.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM guild_join_requests")) {
            while (rs.next()) {
                joinRequests.add(new GuildJoinRequest(rs.getInt("id"), rs.getInt("guild_id"),
                        UUID.fromString(rs.getString("uuid")), rs.getLong("created_at")));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载入会申请失败: " + e.getMessage());
        }

        try (Statement st = database.getConnection().createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM guild_fund_requests")) {
            while (rs.next()) {
                fundRequests.add(new GuildFundRequest(rs.getInt("id"), rs.getInt("guild_id"),
                        UUID.fromString(rs.getString("uuid")), rs.getDouble("amount"), rs.getLong("created_at")));
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载用款申请失败: " + e.getMessage());
        }

        plugin.getLogger().info("公会加载完成：" + guilds.size() + " 个。");
    }

    private void loadRanks(Guild guild) {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "SELECT * FROM guild_ranks WHERE guild_id=?")) {
            ps.setInt(1, guild.id());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    guild.addRank(new GuildRank(rs.getString("rank_key"), rs.getString("display"),
                            rs.getInt("priority"), rs.getInt("permissions")));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载公会职级失败: " + e.getMessage());
        }
    }

    private void loadMembers(Guild guild) {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "SELECT * FROM guild_members WHERE guild_id=?")) {
            ps.setInt(1, guild.id());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    guild.addMember(new GuildMember(uuid, rs.getString("rank_key"),
                            rs.getLong("joined_at"), rs.getDouble("fee_paid")));
                    memberIndex.put(uuid, guild.id());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载公会成员失败: " + e.getMessage());
        }
    }

    private void loadFeatures(Guild guild) {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "SELECT * FROM guild_features WHERE guild_id=?")) {
            ps.setInt(1, guild.id());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    guild.addFeature(new GuildFeature(rs.getString("feature_key"), rs.getString("structure_key"),
                            rs.getInt("unlocked") == 1, rs.getInt("enabled") == 1,
                            rs.getString("data"), rs.getLong("unlocked_at")));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载公会功能失败: " + e.getMessage());
        }
    }

    // ---------- 查询 ----------

    @Override
    public Guild guildOf(UUID player) {
        Integer id = memberIndex.get(player);
        return id == null ? null : guildById(id);
    }

    @Override
    public Guild guildById(int id) {
        for (Guild guild : guilds) {
            if (guild.id() == id) {
                return guild;
            }
        }
        return null;
    }

    @Override
    public Guild guildByName(String name) {
        for (Guild guild : guilds) {
            if (guild.name().equalsIgnoreCase(name)) {
                return guild;
            }
        }
        return null;
    }

    @Override
    public List<Guild> all() {
        return new ArrayList<>(guilds);
    }

    @Override
    public int regionIdOf(Guild guild) {
        return guild == null ? -1 : guild.regionId();
    }

    public Region regionOf(Guild guild) {
        if (guild == null || guild.regionId() < 0 || territory == null) {
            return null;
        }
        return territory.regionById(guild.regionId());
    }

    public int memberCap(Guild guild) {
        return config.memberCap(guild.level());
    }

    public List<GuildJoinRequest> joinRequests(Guild guild) {
        List<GuildJoinRequest> result = new ArrayList<>();
        for (GuildJoinRequest request : joinRequests) {
            if (request.guildId() == guild.id()) {
                result.add(request);
            }
        }
        return result;
    }

    public List<GuildFundRequest> fundRequests(Guild guild) {
        List<GuildFundRequest> result = new ArrayList<>();
        for (GuildFundRequest request : fundRequests) {
            if (request.guildId() == guild.id()) {
                result.add(request);
            }
        }
        return result;
    }

    // ---------- 创建 ----------

    public GuildResult create(Player player, String name) {
        UUID leader = player.getUniqueId();
        if (guildOf(leader) != null) {
            return GuildResult.IN_GUILD;
        }
        if (name == null || name.isBlank() || name.trim().length() > 16) {
            return GuildResult.NAME_INVALID;
        }
        String trimmed = name.trim();
        if (guildByName(trimmed) != null) {
            return GuildResult.NAME_TAKEN;
        }
        if (territory == null) {
            return GuildResult.REGION_UNAVAILABLE;
        }
        if (!vault.isReady()) {
            return GuildResult.ERROR;
        }
        if (!vault.withdraw(leader, config.createCost())) {
            return GuildResult.NO_MONEY;
        }

        int id = insertGuild(trimmed, leader);
        if (id < 0) {
            vault.deposit(leader, config.createCost());
            return GuildResult.ERROR;
        }
        Guild guild = new Guild(id, trimmed, trimmed, leader, 0, config.defaultJoinFee(), false,
                1, 0, -1, System.currentTimeMillis());
        createDefaultRanks(guild);
        addMemberInternal(guild, leader, RANK_LEADER, System.currentTimeMillis(), 0);

        Region region = createGuildRegion(guild, player);
        if (region == null) {
            deleteGuildInternal(guild);
            vault.deposit(leader, config.createCost());
            return GuildResult.CREATE_FAILED;
        }

        guild.setRegionId(region.id());
        updateRegionId(guild);
        guilds.add(guild);
        memberIndex.put(leader, id);
        syncRegionMembers(guild);
        unlockLevelFeatures(guild);
        return GuildResult.OK;
    }

    private int insertGuild(String name, UUID leader) {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT INTO guilds(name, tag, leader, balance, join_fee, open_join, level, exp, region_id, created_at)"
                        + " VALUES(?,?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, name);
            ps.setString(3, leader.toString());
            ps.setDouble(4, 0);
            ps.setDouble(5, config.defaultJoinFee());
            ps.setInt(6, 0);
            ps.setInt(7, 1);
            ps.setLong(8, 0);
            ps.setInt(9, -1);
            ps.setLong(10, System.currentTimeMillis());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        } catch (Exception e) {
            plugin.getLogger().warning("创建公会失败: " + e.getMessage());
            return -1;
        }
    }

    private Region createGuildRegion(Guild guild, Player player) {
        World world = player.getWorld();
        Location location = player.getLocation();
        int centerX = location.getBlockX() >> 4;
        int centerZ = location.getBlockZ() >> 4;
        int section = Math.floorDiv(location.getBlockY(), 16);
        int chunksX = config.regionChunksX();
        int chunksZ = config.regionChunksZ();
        int minChunkX = centerX - chunksX / 2;
        int minChunkZ = centerZ - chunksZ / 2;
        return territory.createRegion(OwnerType.GUILD, guildUuid(guild.id()), guild.name(), world,
                minChunkX, section, minChunkZ, chunksX, config.regionSectionsY(), chunksZ);
    }

    private void createDefaultRanks(Guild guild) {
        GuildRank leader = new GuildRank(RANK_LEADER, "会长", 100, GuildPermission.all());
        GuildRank officer = new GuildRank(RANK_OFFICER, "官员", 50, GuildPermission.mask(
                GuildPermission.BUILD, GuildPermission.BREAK, GuildPermission.USE_CONTAINER,
                GuildPermission.USE_DOOR, GuildPermission.USE_BUTTON, GuildPermission.INTERACT_ENTITY,
                GuildPermission.TELEPORT, GuildPermission.MANAGE_MEMBERS, GuildPermission.INVITE,
                GuildPermission.KICK, GuildPermission.DEPOSIT));
        GuildRank member = new GuildRank(RANK_MEMBER, "成员", 10, GuildPermission.mask(
                GuildPermission.BUILD, GuildPermission.BREAK, GuildPermission.USE_CONTAINER,
                GuildPermission.USE_DOOR, GuildPermission.USE_BUTTON, GuildPermission.INTERACT_ENTITY,
                GuildPermission.TELEPORT, GuildPermission.DEPOSIT));
        for (GuildRank rank : List.of(leader, officer, member)) {
            guild.addRank(rank);
            persistRank(guild.id(), rank);
        }
    }

    private void persistRank(int guildId, GuildRank rank) {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT OR REPLACE INTO guild_ranks(guild_id, rank_key, display, priority, permissions) VALUES(?,?,?,?,?)")) {
            ps.setInt(1, guildId);
            ps.setString(2, rank.key());
            ps.setString(3, rank.display());
            ps.setInt(4, rank.priority());
            ps.setInt(5, rank.permissions());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("保存公会职级失败: " + e.getMessage());
        }
    }

    // ---------- 成员 ----------

    public GuildResult addMember(Player actor, Guild guild, UUID target) {
        if (!guild.hasPermission(actor.getUniqueId(), GuildPermission.INVITE)
                && !actor.getUniqueId().equals(guild.leader())) {
            return GuildResult.NO_PERMISSION;
        }
        if (guildOf(target) != null) {
            return GuildResult.MEMBER_EXISTS;
        }
        if (memberCap(guild) > 0 && guild.memberCount() >= memberCap(guild)) {
            return GuildResult.MEMBER_LIMIT;
        }
        addMemberInternal(guild, target, RANK_MEMBER, System.currentTimeMillis(), 0);
        syncRegionMembers(guild);
        return GuildResult.OK;
    }

    private void addMemberInternal(Guild guild, UUID uuid, String rankKey, long joinedAt, double feePaid) {
        guild.addMember(new GuildMember(uuid, rankKey, joinedAt, feePaid));
        memberIndex.put(uuid, guild.id());
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT OR REPLACE INTO guild_members(guild_id, player_uuid, rank_key, joined_at, fee_paid) VALUES(?,?,?,?,?)")) {
            ps.setInt(1, guild.id());
            ps.setString(2, uuid.toString());
            ps.setString(3, rankKey);
            ps.setLong(4, joinedAt);
            ps.setDouble(5, feePaid);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("保存公会成员失败: " + e.getMessage());
        }
    }

    public GuildResult kick(Player actor, Guild guild, UUID target) {
        if (target.equals(guild.leader())) {
            return GuildResult.IS_LEADER;
        }
        if (!guild.hasPermission(actor.getUniqueId(), GuildPermission.KICK)
                && !actor.getUniqueId().equals(guild.leader())) {
            return GuildResult.NO_PERMISSION;
        }
        GuildMember member = guild.member(target);
        if (member == null) {
            return GuildResult.NOT_MEMBER;
        }
        double refund = Math.min(member.feePaid(), guild.balance());
        removeMemberInternal(guild, target);
        if (refund > 0) {
            payOut(guild, target, refund);
        }
        syncRegionMembers(guild);
        notify(target, config.getMessage("kick-target", Map.of("name", guild.name())));
        return GuildResult.OK;
    }

    public GuildResult setRank(Player actor, Guild guild, UUID target, String rankKey) {
        if (!guild.hasPermission(actor.getUniqueId(), GuildPermission.MANAGE_MEMBERS)
                && !actor.getUniqueId().equals(guild.leader())) {
            return GuildResult.NO_PERMISSION;
        }
        if (target.equals(guild.leader())) {
            return GuildResult.IS_LEADER;
        }
        GuildMember member = guild.member(target);
        if (member == null) {
            return GuildResult.NOT_MEMBER;
        }
        if (guild.rank(rankKey) == null) {
            return GuildResult.NOT_FOUND;
        }
        member.setRankKey(rankKey);
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "UPDATE guild_members SET rank_key=? WHERE guild_id=? AND player_uuid=?")) {
            ps.setString(1, rankKey);
            ps.setInt(2, guild.id());
            ps.setString(3, target.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("更新成员职级失败: " + e.getMessage());
        }
        syncRegionMembers(guild);
        return GuildResult.OK;
    }

    private void removeMemberInternal(Guild guild, UUID uuid) {
        guild.removeMember(uuid);
        memberIndex.remove(uuid);
        exec("DELETE FROM guild_members WHERE guild_id=" + guild.id() + " AND player_uuid='" + uuid + "'");
        removeRequestsOf(guild.id(), uuid);
    }

    // ---------- 入会 ----------

    public GuildResult join(Player player, Guild guild) {
        UUID uuid = player.getUniqueId();
        if (guildOf(uuid) != null) {
            return GuildResult.IN_GUILD;
        }
        if (!guild.openJoin()) {
            return GuildResult.NEED_APPROVAL;
        }
        if (rejoinCooldownLeft(uuid) > 0) {
            return GuildResult.REJOIN_COOLDOWN;
        }
        if (memberCap(guild) > 0 && guild.memberCount() >= memberCap(guild)) {
            return GuildResult.MEMBER_LIMIT;
        }
        double fee = guild.joinFee();
        if (fee > 0 && !vault.has(uuid, fee)) {
            return GuildResult.NO_MONEY;
        }
        if (fee > 0 && !vault.withdraw(uuid, fee)) {
            return GuildResult.NO_MONEY;
        }
        double share = Math.round(fee * config.feeGuildShare() * 100.0) / 100.0;
        if (share > 0) {
            addBalance(guild, share);
        }
        addMemberInternal(guild, uuid, RANK_MEMBER, System.currentTimeMillis(), fee);
        syncRegionMembers(guild);
        return GuildResult.OK;
    }

    public GuildResult requestJoin(Player player, Guild guild) {
        UUID uuid = player.getUniqueId();
        if (guildOf(uuid) != null) {
            return GuildResult.IN_GUILD;
        }
        if (guild.openJoin()) {
            return GuildResult.OPEN_JOIN;
        }
        if (rejoinCooldownLeft(uuid) > 0) {
            return GuildResult.REJOIN_COOLDOWN;
        }
        if (joinRequestOf(uuid) != null) {
            return GuildResult.ALREADY_APPLIED;
        }
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT INTO guild_join_requests(guild_id, uuid, created_at) VALUES(?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, guild.id());
            ps.setString(2, uuid.toString());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
            int id = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getInt(1);
                }
            }
            joinRequests.add(new GuildJoinRequest(id, guild.id(), uuid, System.currentTimeMillis()));
            notify(guild.leader(), config.getMessage("join-requested", Map.of("name", player.getName())));
            return GuildResult.OK;
        } catch (Exception e) {
            plugin.getLogger().warning("提交入会申请失败: " + e.getMessage());
            return GuildResult.ERROR;
        }
    }

    public GuildJoinRequest joinRequestOf(UUID uuid) {
        for (GuildJoinRequest request : joinRequests) {
            if (request.applicant().equals(uuid)) {
                return request;
            }
        }
        return null;
    }

    public GuildResult cancelJoinRequest(Player player) {
        GuildJoinRequest request = joinRequestOf(player.getUniqueId());
        if (request == null) {
            return GuildResult.NOT_FOUND;
        }
        deleteJoinRequest(request.id());
        return GuildResult.OK;
    }

    public GuildResult approveJoin(Player leader, Guild guild, UUID applicant) {
        if (!leader.getUniqueId().equals(guild.leader())) {
            return GuildResult.NO_PERMISSION;
        }
        GuildJoinRequest request = joinRequestOf(applicant);
        if (request == null || request.guildId() != guild.id()) {
            return GuildResult.NOT_FOUND;
        }
        if (guildOf(applicant) != null) {
            deleteJoinRequest(request.id());
            return GuildResult.IN_GUILD;
        }
        if (memberCap(guild) > 0 && guild.memberCount() >= memberCap(guild)) {
            return GuildResult.MEMBER_LIMIT;
        }
        double fee = guild.joinFee();
        if (fee > 0) {
            if (!vault.has(applicant, fee) || !vault.withdraw(applicant, fee)) {
                return GuildResult.NO_MONEY;
            }
            double share = Math.round(fee * config.feeGuildShare() * 100.0) / 100.0;
            if (share > 0) {
                addBalance(guild, share);
            }
        }
        addMemberInternal(guild, applicant, RANK_MEMBER, System.currentTimeMillis(), fee);
        syncRegionMembers(guild);
        deleteJoinRequest(request.id());
        notify(applicant, config.getMessage("join-approved", Map.of("name", guild.name())));
        return GuildResult.OK;
    }

    public GuildResult denyJoin(Player leader, Guild guild, UUID applicant) {
        if (!leader.getUniqueId().equals(guild.leader())) {
            return GuildResult.NO_PERMISSION;
        }
        GuildJoinRequest request = joinRequestOf(applicant);
        if (request == null || request.guildId() != guild.id()) {
            return GuildResult.NOT_FOUND;
        }
        deleteJoinRequest(request.id());
        notify(applicant, config.getMessage("join-denied", Map.of("name", guild.name())));
        return GuildResult.OK;
    }

    private void deleteJoinRequest(int id) {
        joinRequests.removeIf(request -> request.id() == id);
        exec("DELETE FROM guild_join_requests WHERE id=" + id);
    }

    public long rejoinCooldownLeft(UUID uuid) {
        long hours = config.rejoinCooldownHours();
        if (hours <= 0) {
            return 0;
        }
        long last = lastLeaveAt(uuid);
        long until = last + hours * 3600000L;
        return Math.max(0, until - System.currentTimeMillis());
    }

    private long lastLeaveAt(UUID uuid) {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "SELECT leave_at FROM guild_quit WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    public long leaveLockLeft(Guild guild, UUID uuid) {
        if (guild.leader().equals(uuid) || config.noLeaveDays() <= 0) {
            return 0;
        }
        GuildMember member = guild.member(uuid);
        if (member == null) {
            return 0;
        }
        long until = member.joinedAt() + config.noLeaveDays() * 86400000L;
        return Math.max(0, until - System.currentTimeMillis());
    }

    public GuildResult leave(Player player) {
        Guild guild = guildOf(player.getUniqueId());
        if (guild == null) {
            return GuildResult.NO_GUILD;
        }
        if (guild.leader().equals(player.getUniqueId())) {
            return GuildResult.IS_LEADER;
        }
        if (leaveLockLeft(guild, player.getUniqueId()) > 0) {
            return GuildResult.TOO_SOON;
        }
        removeMemberInternal(guild, player.getUniqueId());
        recordQuit(player.getUniqueId());
        syncRegionMembers(guild);
        return GuildResult.OK;
    }

    private void recordQuit(UUID uuid) {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT OR REPLACE INTO guild_quit(uuid, leave_at) VALUES(?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("记录退会时间失败: " + e.getMessage());
        }
    }

    public GuildResult disband(Player player) {
        Guild guild = guildOf(player.getUniqueId());
        if (guild == null) {
            return GuildResult.NO_GUILD;
        }
        if (!guild.leader().equals(player.getUniqueId())) {
            return GuildResult.NOT_LEADER;
        }
        deleteGuildInternal(guild);
        return GuildResult.OK;
    }

    private void deleteGuildInternal(Guild guild) {
        Region region = regionOf(guild);
        if (region != null && territory != null) {
            territory.deleteRegion(region);
        }
        guilds.remove(guild);
        for (UUID uuid : guild.members().keySet()) {
            memberIndex.remove(uuid);
        }
        exec("DELETE FROM guilds WHERE id=" + guild.id());
        exec("DELETE FROM guild_members WHERE guild_id=" + guild.id());
        exec("DELETE FROM guild_ranks WHERE guild_id=" + guild.id());
        exec("DELETE FROM guild_features WHERE guild_id=" + guild.id());
        exec("DELETE FROM guild_fund_requests WHERE guild_id=" + guild.id());
        exec("DELETE FROM guild_join_requests WHERE guild_id=" + guild.id());
    }

    // ---------- 设置 ----------

    public GuildResult setJoinFee(Player actor, Guild guild, double fee) {
        if (!actor.getUniqueId().equals(guild.leader())) {
            return GuildResult.NO_PERMISSION;
        }
        if (fee < config.minJoinFee()) {
            return GuildResult.FEE_TOO_LOW;
        }
        guild.setJoinFee(round(fee));
        updateGuildDouble("join_fee", guild.joinFee(), guild.id());
        return GuildResult.OK;
    }

    public GuildResult setOpenJoin(Player actor, Guild guild, boolean open) {
        if (!actor.getUniqueId().equals(guild.leader())) {
            return GuildResult.NO_PERMISSION;
        }
        guild.setOpenJoin(open);
        exec("UPDATE guilds SET open_join=" + (open ? 1 : 0) + " WHERE id=" + guild.id());
        return GuildResult.OK;
    }

    // ---------- 资金 ----------

    @Override
    public boolean deposit(UUID uuid, double amount) {
        Guild guild = guildOf(uuid);
        if (guild == null || amount <= 0) {
            return false;
        }
        if (!vault.withdraw(uuid, amount)) {
            return false;
        }
        addBalance(guild, amount);
        return true;
    }

    @Override
    public boolean withdraw(Player player, double amount) {
        Guild guild = guildOf(player.getUniqueId());
        if (guild == null || amount <= 0) {
            return false;
        }
        if (!guild.hasPermission(player.getUniqueId(), GuildPermission.WITHDRAW)
                && !player.getUniqueId().equals(guild.leader())) {
            return false;
        }
        return payOut(guild, player.getUniqueId(), amount);
    }

    public boolean payOut(Guild guild, UUID to, double amount) {
        if (guild == null || amount <= 0 || guild.balance() + 1.0E-4 < amount) {
            return false;
        }
        guild.setBalance(round(guild.balance() - amount));
        updateGuildDouble("balance", guild.balance(), guild.id());
        vault.deposit(to, amount);
        return true;
    }

    public void addBalance(Guild guild, double amount) {
        if (guild == null || amount == 0) {
            return;
        }
        guild.setBalance(round(guild.balance() + amount));
        updateGuildDouble("balance", guild.balance(), guild.id());
    }

    public GuildResult requestFunds(Player player, Guild guild, double amount) {
        if (guild == null) {
            return GuildResult.NO_GUILD;
        }
        if (player.getUniqueId().equals(guild.leader())) {
            return GuildResult.IS_LEADER;
        }
        if (!guild.isMember(player.getUniqueId()) || amount <= 0) {
            return GuildResult.ERROR;
        }
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT INTO guild_fund_requests(guild_id, uuid, amount, created_at) VALUES(?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, guild.id());
            ps.setString(2, player.getUniqueId().toString());
            ps.setDouble(3, round(amount));
            ps.setLong(4, System.currentTimeMillis());
            ps.executeUpdate();
            int id = -1;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getInt(1);
                }
            }
            fundRequests.add(new GuildFundRequest(id, guild.id(), player.getUniqueId(), round(amount), System.currentTimeMillis()));
            return GuildResult.OK;
        } catch (Exception e) {
            plugin.getLogger().warning("提交用款申请失败: " + e.getMessage());
            return GuildResult.ERROR;
        }
    }

    public GuildFundRequest fundRequest(int id) {
        for (GuildFundRequest request : fundRequests) {
            if (request.id() == id) {
                return request;
            }
        }
        return null;
    }

    public GuildResult approveFund(Player leader, Guild guild, int requestId) {
        if (!leader.getUniqueId().equals(guild.leader())) {
            return GuildResult.NO_PERMISSION;
        }
        GuildFundRequest request = fundRequest(requestId);
        if (request == null || request.guildId() != guild.id()) {
            return GuildResult.NOT_FOUND;
        }
        if (!payOut(guild, request.applicant(), request.amount())) {
            return GuildResult.NO_MONEY;
        }
        deleteFundRequest(requestId);
        return GuildResult.OK;
    }

    public GuildResult denyFund(Player leader, Guild guild, int requestId) {
        if (!leader.getUniqueId().equals(guild.leader())) {
            return GuildResult.NO_PERMISSION;
        }
        GuildFundRequest request = fundRequest(requestId);
        if (request == null || request.guildId() != guild.id()) {
            return GuildResult.NOT_FOUND;
        }
        deleteFundRequest(requestId);
        return GuildResult.OK;
    }

    private void deleteFundRequest(int id) {
        fundRequests.removeIf(request -> request.id() == id);
        exec("DELETE FROM guild_fund_requests WHERE id=" + id);
    }

    private void removeRequestsOf(int guildId, UUID uuid) {
        fundRequests.removeIf(request -> request.guildId() == guildId && request.applicant().equals(uuid));
        joinRequests.removeIf(request -> request.guildId() == guildId && request.applicant().equals(uuid));
        exec("DELETE FROM guild_fund_requests WHERE guild_id=" + guildId + " AND uuid='" + uuid + "'");
        exec("DELETE FROM guild_join_requests WHERE guild_id=" + guildId + " AND uuid='" + uuid + "'");
    }

    // ---------- 升级 ----------

    public GuildResult upgrade(Player player) {
        Guild guild = guildOf(player.getUniqueId());
        if (guild == null) {
            return GuildResult.NO_GUILD;
        }
        if (!guild.leader().equals(player.getUniqueId())) {
            return GuildResult.NOT_LEADER;
        }
        if (guild.level() >= config.maxLevel()) {
            return GuildResult.MAX_LEVEL;
        }
        double cost = config.upgradeCostFor(guild.level());
        if (!vault.isReady() || !vault.has(player.getUniqueId(), cost)) {
            return GuildResult.NO_MONEY;
        }
        if (!vault.withdraw(player.getUniqueId(), cost)) {
            return GuildResult.NO_MONEY;
        }
        guild.setLevel(guild.level() + 1);
        exec("UPDATE guilds SET level=" + guild.level() + " WHERE id=" + guild.id());

        Region region = regionOf(guild);
        if (region != null && territory != null && config.expandPerLevel() > 0) {
            for (int i = 0; i < config.expandPerLevel(); i++) {
                territory.expandRegion(region, Direction.EAST, 1);
                territory.expandRegion(region, Direction.SOUTH, 1);
            }
        }
        unlockLevelFeatures(guild);
        syncRegionMembers(guild);
        return GuildResult.OK;
    }

    private void unlockLevelFeatures(Guild guild) {
        for (FeatureDefinition definition : structureConfig.features().values()) {
            if (!definition.unlockType().needsLevel() || guild.level() < definition.requiredLevel()) {
                continue;
            }
            GuildFeature feature = guild.feature(definition.key());
            if (feature == null) {
                feature = new GuildFeature(definition.key(), definition.structureKey(), true, true, null, System.currentTimeMillis());
                guild.addFeature(feature);
                persistFeature(guild.id(), feature);
            } else if (!feature.unlocked()) {
                feature.setUnlocked(true);
                feature.setEnabled(true);
                feature.setUnlockedAt(System.currentTimeMillis());
                persistFeature(guild.id(), feature);
            }
        }
    }

    public void persistFeature(int guildId, GuildFeature feature) {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT OR REPLACE INTO guild_features(guild_id, feature_key, structure_key, unlocked, enabled, data, unlocked_at)"
                        + " VALUES(?,?,?,?,?,?,?)")) {
            ps.setInt(1, guildId);
            ps.setString(2, feature.key());
            ps.setString(3, feature.structureKey());
            ps.setInt(4, feature.unlocked() ? 1 : 0);
            ps.setInt(5, feature.enabled() ? 1 : 0);
            ps.setString(6, feature.data());
            ps.setLong(7, feature.unlockedAt());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("保存公会功能失败: " + e.getMessage());
        }
    }

    // ---------- 领地同步 ----------

    public void syncRegionMembers(Guild guild) {
        Region region = regionOf(guild);
        if (region == null || territory == null) {
            return;
        }
        for (GuildMember member : guild.memberList()) {
            int mask = territoryMask(guild, member);
            if (territory.hasRegionMember(region, member.uuid())) {
                territory.setRegionMemberPermissions(region, member.uuid(), mask);
            } else {
                territory.addRegionMember(region, member.uuid(), mask);
            }
        }
        for (RegionMember regionMember : new ArrayList<>(region.memberList())) {
            if (!guild.isMember(regionMember.uuid())) {
                territory.removeRegionMember(region, regionMember.uuid());
            }
        }
    }

    private int territoryMask(Guild guild, GuildMember member) {
        if (member.uuid().equals(guild.leader())) {
            return PermissionFlag.all();
        }
        GuildRank rank = guild.rank(member.rankKey());
        if (rank == null) {
            return 0;
        }
        int mask = 0;
        if (rank.has(GuildPermission.BUILD)) {
            mask |= PermissionFlag.BUILD.bit();
        }
        if (rank.has(GuildPermission.BREAK)) {
            mask |= PermissionFlag.BREAK.bit();
        }
        if (rank.has(GuildPermission.USE_CONTAINER)) {
            mask |= PermissionFlag.USE_CONTAINER.bit();
        }
        if (rank.has(GuildPermission.USE_DOOR)) {
            mask |= PermissionFlag.USE_DOOR.bit();
        }
        if (rank.has(GuildPermission.USE_BUTTON)) {
            mask |= PermissionFlag.USE_BUTTON.bit();
        }
        if (rank.has(GuildPermission.INTERACT_ENTITY)) {
            mask |= PermissionFlag.INTERACT_ENTITY.bit();
        }
        if (rank.has(GuildPermission.TELEPORT)) {
            mask |= PermissionFlag.TELEPORT.bit();
        }
        return mask;
    }

    // ---------- 通知 ----------

    @Override
    public void notify(UUID uuid, String miniMessage) {
        if (miniMessage == null || miniMessage.isEmpty()) {
            return;
        }
        Player online = Bukkit.getPlayer(uuid);
        if (online != null && online.isOnline()) {
            online.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(miniMessage));
            return;
        }
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT INTO guild_notices(uuid, message, created_at) VALUES(?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, miniMessage);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("离线通知入库失败: " + e.getMessage());
        }
    }

    public void flushNotices(Player player) {
        List<String> pending = new ArrayList<>();
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "SELECT message FROM guild_notices WHERE uuid=? ORDER BY id")) {
            ps.setString(1, player.getUniqueId().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pending.add(rs.getString(1));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("读取离线通知失败: " + e.getMessage());
            return;
        }
        if (pending.isEmpty()) {
            return;
        }
        for (String message : pending) {
            player.sendMessage(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(message));
        }
        exec("DELETE FROM guild_notices WHERE uuid='" + player.getUniqueId() + "'");
    }

    // ---------- 功能注册 ----------

    @Override
    public void registerFeature(GuildFeatureProvider provider) {
        registry.register(provider);
    }

    @Override
    public FeatureRegistry featureRegistry() {
        return registry;
    }

    public GuildFeatureProvider provider(String key) {
        return registry.get(key);
    }

    // ---------- 结构识别与功能解锁 ----------

    /**
     * 启动时扫描全部公会领地，补全结构解锁状态。
     */
    public void scanAll() {
        for (Guild guild : guilds) {
            scanGuildRegion(guild);
        }
    }

    /**
     * 扫描某个公会领地内的全部结构并解锁对应功能。
     */
    public void scanGuildRegion(Guild guild) {
        Region region = regionOf(guild);
        if (region == null) {
            return;
        }
        for (String structureKey : StructureScanner.scan(region, structureConfig)) {
            unlockByStructure(guild, structureKey, null);
        }
    }

    /**
     * 玩家在公会领地内放置方块后，检查是否补全了某个结构。
     */
    public void handleBlockPlace(Player player, Location location) {
        Guild guild = guildOf(player.getUniqueId());
        if (guild == null) {
            return;
        }
        Region region = regionOf(guild);
        if (region == null || location.getWorld() == null || !region.contains(location)) {
            return;
        }
        for (String structureKey : StructureScanner.checkAt(region, location, structureConfig)) {
            unlockByStructure(guild, structureKey, location);
        }
    }

    /**
     * 依据结构键解锁相应功能。返回是否发生了新的解锁。
     */
    public boolean unlockByStructure(Guild guild, String structureKey, Location location) {
        if (guild == null || structureKey == null) {
            return false;
        }
        boolean changed = false;
        for (FeatureDefinition definition : structureConfig.features().values()) {
            if (!definition.unlockType().needsStructure()) {
                continue;
            }
            if (!structureKey.equals(definition.structureKey())) {
                continue;
            }
            if (guild.level() < definition.requiredLevel()) {
                continue;
            }
            GuildFeature feature = guild.feature(definition.key());
            if (feature != null && feature.unlocked()) {
                continue;
            }
            if (feature == null) {
                feature = new GuildFeature(definition.key(), structureKey, true, true, null,
                        System.currentTimeMillis());
                guild.addFeature(feature);
            } else {
                feature.setUnlocked(true);
                feature.setEnabled(true);
                feature.setUnlockedAt(System.currentTimeMillis());
            }
            persistFeature(guild.id(), feature);
            GuildFeatureProvider provider = registry.get(definition.key());
            if (provider != null) {
                provider.onUnlock(guild, location);
                provider.onEnable(guild);
            }
            String name = provider == null ? definition.key() : provider.displayName();
            for (UUID uuid : guild.members().keySet()) {
                notify(uuid, config.getMessage("feature-unlocked", Map.of("feature", name)));
            }
            changed = true;
        }
        return changed;
    }

    /**
     * 周期性驱动已启用功能的 tick 效果（如信标 buff）。
     */
    public void tickFeatures() {
        for (Guild guild : guilds) {
            for (GuildFeature feature : guild.features().values()) {
                if (!feature.unlocked() || !feature.enabled()) {
                    continue;
                }
                GuildFeatureProvider provider = registry.get(feature.key());
                if (provider != null) {
                    provider.onTick(guild);
                }
            }
        }
    }

    // ---------- DB 工具 ----------

    private void updateRegionId(Guild guild) {
        exec("UPDATE guilds SET region_id=" + guild.regionId() + " WHERE id=" + guild.id());
    }

    private void updateGuildDouble(String column, double value, int id) {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "UPDATE guilds SET " + column + "=? WHERE id=?")) {
            ps.setDouble(1, value);
            ps.setInt(2, id);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("更新公会字段失败: " + e.getMessage());
        }
    }

    private void exec(String sql) {
        try (Statement st = database.getConnection().createStatement()) {
            st.executeUpdate(sql);
        } catch (Exception e) {
            plugin.getLogger().warning("公会 SQL 失败: " + e.getMessage());
        }
    }

    private static double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    public enum GuildResult {
        OK,
        IN_GUILD,
        NAME_TAKEN,
        NAME_INVALID,
        NO_MONEY,
        NOT_FOUND,
        NO_GUILD,
        NOT_LEADER,
        NOT_MEMBER,
        NO_PERMISSION,
        ALREADY_APPLIED,
        NEED_APPROVAL,
        OPEN_JOIN,
        REJOIN_COOLDOWN,
        TOO_SOON,
        IS_LEADER,
        MEMBER_EXISTS,
        MEMBER_LIMIT,
        FEE_TOO_LOW,
        MAX_LEVEL,
        REGION_UNAVAILABLE,
        CREATE_FAILED,
        ERROR
    }
}
