package top.worldme.market.bridge;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * 通过反射调用 Worldme-Mail 的 API。
 * 各插件拥有独立 ClassLoader，无法在编译期直接引用 MailApi，因此使用反射桥接。
 */
public class MailBridge {

    private final JavaPlugin plugin;
    private Object api;
    private Method sendMethod;

    public MailBridge(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean setup() {
        Plugin mail = Bukkit.getPluginManager().getPlugin("Worldme-Mail");
        if (mail == null) {
            plugin.getLogger().warning("未找到 Worldme-Mail，无法投递市场物品。");
            return false;
        }
        try {
            Method getApi = mail.getClass().getMethod("getApi");
            this.api = getApi.invoke(mail);
            if (api == null) {
                return false;
            }
            this.sendMethod = api.getClass().getMethod(
                    "send", UUID.class, String.class, String.class, List.class, List.class);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("对接邮箱 API 失败: " + e.getMessage());
            return false;
        }
    }

    public boolean isReady() {
        return api != null && sendMethod != null;
    }

    public boolean send(UUID recipient, String subject, String content, List<ItemStack> items) {
        if (!isReady()) {
            return false;
        }
        try {
            sendMethod.invoke(api, recipient, subject, content, items, null);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("发送邮件失败: " + e.getMessage());
            return false;
        }
    }
}
