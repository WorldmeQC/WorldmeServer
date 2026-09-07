package top.worldme.fishing.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class FishingConfig {

    private final JavaPlugin plugin;
    private final List<QuestFish> questFishes = new ArrayList<>();
    private final Map<String, String> messages = new HashMap<>();
    private final List<Reward> defaultRewards = new ArrayList<>();
    private final Map<Integer, List<Reward>> rewardPools = new HashMap<>();
    private String cycleTime = "04:30";

    public FishingConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        this.cycleTime = config.getString("cycle-time", "04:30");

        messages.clear();
        ConfigurationSection messagesSection = config.getConfigurationSection("messages");
        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(false)) {
                messages.put(key, messagesSection.getString(key, ""));
            }
        }

        questFishes.clear();
        List<Map<?, ?>> fishMaps = config.getMapList("quest-fishes");
        for (Map<?, ?> map : fishMaps) {
            String cfLootId = Objects.toString(map.get("cf-loot-id"), null);
            String ceItemId = Objects.toString(map.get("ce-item-id"), null);
            if (cfLootId == null || cfLootId.isBlank() || ceItemId == null || ceItemId.isBlank()) {
                plugin.getLogger().warning("quest-fishes 中存在缺少 cf-loot-id 或 ce-item-id 的条目，已跳过。");
                continue;
            }
            int weight = parseInt(map.get("weight"), 10);

            // lore 为多条描述；兼容旧的单条 hint 字段
            List<String> lore = new ArrayList<>();
            Object loreObj = map.get("lore");
            if (loreObj instanceof List<?> loreList) {
                for (Object line : loreList) {
                    lore.add(Objects.toString(line, ""));
                }
            } else {
                String single = Objects.toString(map.get("hint"), null);
                if (single == null) {
                    single = Objects.toString(loreObj, "");
                }
                if (!single.isBlank()) {
                    lore.add(single);
                }
            }

            questFishes.add(new QuestFish(cfLootId, ceItemId, weight, lore));
        }

        if (questFishes.isEmpty()) {
            plugin.getLogger().warning("quest-fishes 列表为空，渔夫任务将不会分配任何任务鱼。");
        }

        defaultRewards.clear();
        rewardPools.clear();
        ConfigurationSection poolsSection = config.getConfigurationSection("reward-pools");
        if (poolsSection != null) {
            for (String key : poolsSection.getKeys(false)) {
                List<Map<?, ?>> rewardMaps = poolsSection.getMapList(key);
                List<Reward> rewards = new ArrayList<>();
                for (Map<?, ?> rewardMap : rewardMaps) {
                    rewards.add(parseReward(rewardMap));
                }
                if ("default".equalsIgnoreCase(key)) {
                    defaultRewards.addAll(rewards);
                } else {
                    try {
                        int count = Integer.parseInt(key);
                        rewardPools.put(count, rewards);
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("reward-pools 中的键 " + key + " 不是整数也不是 default，已跳过。");
                    }
                }
            }
        }
    }

    private Reward parseReward(Map<?, ?> rewardMap) {
        String command = Objects.toString(rewardMap.get("command"), null);
        String ceItemId = Objects.toString(rewardMap.get("ce-item-id"), null);
        int amount = parseInt(rewardMap.get("amount"), 1);
        return new Reward(command, ceItemId, amount);
    }

    private int parseInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public String getCycleTime() {
        return cycleTime;
    }

    public List<QuestFish> getQuestFishes() {
        return Collections.unmodifiableList(questFishes);
    }

    public List<Reward> getDefaultRewards() {
        return Collections.unmodifiableList(defaultRewards);
    }

    public Map<Integer, List<Reward>> getRewardPools() {
        return Collections.unmodifiableMap(rewardPools);
    }

    /**
     * 根据累计完成次数获取对应奖池奖励。未配置特定次数时返回默认奖池。
     */
    public List<Reward> getRewardsForCompletion(int totalCompleted) {
        List<Reward> pool = rewardPools.get(totalCompleted);
        if (pool != null && !pool.isEmpty()) {
            return Collections.unmodifiableList(pool);
        }
        return Collections.unmodifiableList(defaultRewards);
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "");
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String text = messages.getOrDefault(key, "");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            text = text.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return text;
    }

    public QuestFish getQuestFishByLootId(String lootId) {
        for (QuestFish fish : questFishes) {
            if (fish.cfLootId().equals(lootId)) {
                return fish;
            }
        }
        return null;
    }

    public record QuestFish(String cfLootId, String ceItemId, int weight, List<String> lore) {
    }

    public record Reward(String command, String ceItemId, int amount) {
        public boolean isCommand() {
            return command != null && !command.isBlank();
        }

        public boolean isItem() {
            return ceItemId != null && !ceItemId.isBlank();
        }
    }
}
