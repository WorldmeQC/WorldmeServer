package top.worldme.guild.data;

/**
 * 公会职级。
 */
public class GuildRank {

    private final String key;
    private String display;
    private int priority;
    private int permissions;

    public GuildRank(String key, String display, int priority, int permissions) {
        this.key = key;
        this.display = display;
        this.priority = priority;
        this.permissions = permissions;
    }

    public String key() {
        return key;
    }

    public String display() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public int priority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public int permissions() {
        return permissions;
    }

    public void setPermissions(int permissions) {
        this.permissions = permissions;
    }

    public boolean has(GuildPermission permission) {
        return (permissions & permission.bit()) != 0;
    }

    public void set(GuildPermission permission, boolean value) {
        if (value) {
            permissions |= permission.bit();
        } else {
            permissions &= ~permission.bit();
        }
    }
}
