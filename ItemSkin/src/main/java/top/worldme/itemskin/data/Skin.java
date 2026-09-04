package top.worldme.itemskin.data;

import top.worldme.itemskin.utils.ItemType;

import java.util.Collections;
import java.util.Set;

public class Skin {

    private final String id;
    private final String displayName;
    private final Set<ItemType> applicableTypes;

    public Skin(String id, String displayName, Set<ItemType> applicableTypes) {
        this.id = id;
        this.displayName = displayName;
        this.applicableTypes = Collections.unmodifiableSet(applicableTypes);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Set<ItemType> getApplicableTypes() {
        return applicableTypes;
    }

    public boolean canApplyTo(ItemType type) {
        return applicableTypes.contains(type);
    }
}
