package top.worldme.guild.data;

/**
 * 公会功能解锁状态。
 */
public class GuildFeature {

    private final String key;
    private final String structureKey;
    private boolean unlocked;
    private boolean enabled;
    private String data;
    private long unlockedAt;

    public GuildFeature(String key, String structureKey, boolean unlocked, boolean enabled,
                        String data, long unlockedAt) {
        this.key = key;
        this.structureKey = structureKey;
        this.unlocked = unlocked;
        this.enabled = enabled;
        this.data = data;
        this.unlockedAt = unlockedAt;
    }

    public String key() {
        return key;
    }

    public String structureKey() {
        return structureKey;
    }

    public boolean unlocked() {
        return unlocked;
    }

    public void setUnlocked(boolean unlocked) {
        this.unlocked = unlocked;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String data() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public long unlockedAt() {
        return unlockedAt;
    }

    public void setUnlockedAt(long unlockedAt) {
        this.unlockedAt = unlockedAt;
    }
}
