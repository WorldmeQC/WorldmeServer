package top.worldme.mail.data;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record MailMessage(
        int id,
        UUID recipient,
        String subject,
        String content,
        long sentAt,
        long expireAt,
        boolean read,
        boolean claimed,
        List<ItemStack> items,
        List<String> commands
) {
    public MailMessage {
        items = items == null ? new ArrayList<>() : items;
        commands = commands == null ? new ArrayList<>() : commands;
    }
}