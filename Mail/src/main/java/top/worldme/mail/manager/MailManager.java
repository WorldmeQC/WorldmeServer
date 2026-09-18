package top.worldme.mail.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import top.worldme.mail.api.MailApi;
import top.worldme.mail.config.MailConfig;
import top.worldme.mail.data.MailDatabase;
import top.worldme.mail.data.MailMessage;
import top.worldme.mail.util.MailItems;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MailManager implements MailApi {

    private final JavaPlugin plugin;
    private final MailConfig config;
    private final MailDatabase database;

    public MailManager(JavaPlugin plugin, MailConfig config, MailDatabase database) {
        this.plugin = plugin;
        this.config = config;
        this.database = database;
    }

    public void deleteExpired() {
        int rows = exec("DELETE FROM mails WHERE expire_at <= " + System.currentTimeMillis());
        if (rows > 0) {
            plugin.getLogger().info("已清理 " + rows + " 封过期邮件。");
        }
    }

    @Override
    public void send(UUID recipient, String subject, String content) {
        send(recipient, subject, content, null, null);
    }

    @Override
    public void send(UUID recipient, String subject, String content, List<ItemStack> items) {
        send(recipient, subject, content, items, null);
    }

    @Override
    public void sendCommands(UUID recipient, String subject, List<String> commands) {
        send(recipient, subject, null, null, commands);
    }

    @Override
    public void send(UUID recipient, String subject, String content, List<ItemStack> items, List<String> commands) {
        long now = System.currentTimeMillis();
        long expire = now + config.expireDays() * 24L * 60 * 60 * 1000;
        String subjectText = (subject == null || subject.isBlank()) ? config.defaultSubject() : subject;
        String itemsYaml = MailItems.serialize(items);
        String commandsText = (commands == null || commands.isEmpty()) ? null : String.join("\n", commands);

        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "INSERT INTO mails(recipient, subject, content, sent_at, expire_at, items, commands) VALUES(?,?,?,?,?,?,?)")) {
            ps.setString(1, recipient.toString());
            ps.setString(2, subjectText);
            ps.setString(3, content);
            ps.setLong(4, now);
            ps.setLong(5, expire);
            ps.setString(6, itemsYaml);
            ps.setString(7, commandsText);
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("发送邮件失败: " + e.getMessage());
        }
    }

    @Override
    public List<MailMessage> getMails(UUID recipient) {
        List<MailMessage> mails = new ArrayList<>();
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "SELECT * FROM mails WHERE recipient=? AND expire_at > ? ORDER BY sent_at DESC")) {
            ps.setString(1, recipient.toString());
            ps.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mails.add(readMail(rs));
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("加载邮件失败: " + e.getMessage());
        }
        return mails;
    }

    public MailMessage getById(int id) {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "SELECT * FROM mails WHERE id=? AND expire_at > ?")) {
            ps.setInt(1, id);
            ps.setLong(2, System.currentTimeMillis());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readMail(rs);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("查询邮件失败: " + e.getMessage());
        }
        return null;
    }

    private MailMessage readMail(ResultSet rs) throws java.sql.SQLException {
        MailMessage mail = new MailMessage(
                rs.getInt("id"),
                UUID.fromString(rs.getString("recipient")),
                rs.getString("subject"),
                rs.getString("content"),
                rs.getLong("sent_at"),
                rs.getLong("expire_at"),
                rs.getInt("read") == 1,
                rs.getInt("claimed") == 1,
                MailItems.deserialize(rs.getString("items")),
                deserializeCommands(rs.getString("commands"))
        );
        return mail;
    }

    private List<String> deserializeCommands(String text) {
        List<String> commands = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return commands;
        }
        for (String line : text.split("\n")) {
            String trimmed = line.strip();
            if (!trimmed.isEmpty()) {
                commands.add(trimmed);
            }
        }
        return commands;
    }

    @Override
    public void markRead(int id) {
        exec("UPDATE mails SET read=1 WHERE id=" + id);
    }

    @Override
    public boolean claimAttachments(int id, Player player) {
        MailMessage mail = getById(id);
        if (mail == null || mail.claimed()) {
            return false;
        }
        if (!mail.recipient().equals(player.getUniqueId())) {
            return false;
        }

        boolean hasItems = !mail.items().isEmpty();
        boolean hasCommands = !mail.commands().isEmpty();
        if (!hasItems && !hasCommands) {
            return false;
        }

        if (hasItems && !canAddAll(player, mail.items())) {
            return false;
        }

        if (hasItems) {
            player.getInventory().addItem(mail.items().toArray(new ItemStack[0]));
        }

        if (hasCommands) {
            for (String cmd : mail.commands()) {
                String executed = cmd;
                if (executed.startsWith("/")) {
                    executed = executed.substring(1);
                }
                executed = executed.replace("{player}", player.getName())
                        .replace("{uuid}", player.getUniqueId().toString());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), executed);
            }
        }

        exec("UPDATE mails SET claimed=1, items=NULL, commands=NULL WHERE id=" + id);
        return true;
    }

    /**
     * 预检查背包能否装下全部物品，避免部分领取的情况。
     */
    private boolean canAddAll(Player player, List<ItemStack> items) {
        ItemStack[] contents = player.getInventory().getStorageContents();
        int[] used = new int[contents.length];
        for (int i = 0; i < contents.length; i++) {
            used[i] = (contents[i] == null || contents[i].getType().isAir()) ? 0 : contents[i].getAmount();
        }
        for (ItemStack item : items) {
            int remaining = item.getAmount();
            int maxStack = item.getMaxStackSize();
            for (int i = 0; i < contents.length; i++) {
                if (remaining <= 0) {
                    break;
                }
                ItemStack current = contents[i];
                if (current == null || current.getType().isAir() || !current.isSimilar(item)) {
                    continue;
                }
                int space = current.getMaxStackSize() - used[i];
                if (space <= 0) {
                    continue;
                }
                int add = Math.min(space, remaining);
                used[i] += add;
                remaining -= add;
            }
            if (remaining > 0) {
                for (int i = 0; i < contents.length; i++) {
                    if (remaining <= 0) {
                        break;
                    }
                    if (contents[i] != null && !contents[i].getType().isAir()) {
                        continue;
                    }
                    int add = Math.min(maxStack, remaining);
                    used[i] += add;
                    remaining -= add;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int countUnread(UUID recipient) {
        int count = 0;
        for (MailMessage mail : getMails(recipient)) {
            if (!mail.read()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public void clear(UUID recipient) {
        try (PreparedStatement ps = database.getConnection().prepareStatement(
                "DELETE FROM mails WHERE recipient=?")) {
            ps.setString(1, recipient.toString());
            ps.executeUpdate();
        } catch (Exception e) {
            plugin.getLogger().warning("清空邮件失败: " + e.getMessage());
        }
    }

    public int countMails(UUID recipient) {
        return getMails(recipient).size();
    }

    private int exec(String sql) {
        try (Statement st = database.getConnection().createStatement()) {
            return st.executeUpdate(sql);
        } catch (Exception e) {
            plugin.getLogger().warning("邮件 SQL 失败: " + e.getMessage());
            return 0;
        }
    }
}