package top.worldme.guild.feature;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 公会功能注册表。核心与外部模块均可注册功能提供者。
 */
public class FeatureRegistry {

    private final Map<String, GuildFeatureProvider> providers = new LinkedHashMap<>();

    public void register(GuildFeatureProvider provider) {
        if (provider == null || provider.key() == null) {
            return;
        }
        providers.put(provider.key(), provider);
    }

    public GuildFeatureProvider get(String key) {
        return providers.get(key);
    }

    public boolean contains(String key) {
        return providers.containsKey(key);
    }

    public Collection<GuildFeatureProvider> all() {
        return Collections.unmodifiableCollection(providers.values());
    }

    public Map<String, GuildFeatureProvider> providers() {
        return Collections.unmodifiableMap(providers);
    }
}
