package top.worldme.mail.api;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.worldme.mail.data.MailMessage;

import java.util.List;
import java.util.UUID;

/**
 * 邮件外部接口，供其他模块调用。
 * 通过 {@code (Mail) Bukkit.getPluginManager().getPlugin("Worldme-Mail")} 获取实例后调用 getApi()。
 */
public interface MailApi {

    void send(UUID recipient, String subject, String content);

    void send(UUID recipient, String subject, String content, List<ItemStack> items);

    void send(UUID recipient, String subject, String content, List<ItemStack> items, List<String> commands);

    void sendCommands(UUID recipient, String subject, List<String> commands);

    List<MailMessage> getMails(UUID recipient);

    int countUnread(UUID recipient);

    void markRead(int id);

    boolean claimAttachments(int id, Player player);

    void clear(UUID recipient);
}