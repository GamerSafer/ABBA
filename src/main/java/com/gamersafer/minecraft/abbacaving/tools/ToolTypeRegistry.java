package com.gamersafer.minecraft.abbacaving.tools;

import java.util.HashMap;
import java.util.Map;

final class ToolTypeRegistry {

    private ToolTypeRegistry() {

    }

    private static final Map<String, ToolType> REGISTRY = new HashMap<>();

    static {
        register(ToolTypes.Keys.HELMET, ToolTypes.HELMET);
        register(ToolTypes.Keys.CHESTPLATE, ToolTypes.CHESTPLATE);
        register(ToolTypes.Keys.LEGGINGS, ToolTypes.LEGGINGS);
        register(ToolTypes.Keys.BOOTS, ToolTypes.BOOTS);
        register(ToolTypes.Keys.SHIELD, ToolTypes.SHIELD);

        register(ToolTypes.Keys.BLOCK, ToolTypes.BLOCK);
        register(ToolTypes.Keys.PICKAXE, ToolTypes.PICKAXE);
        register(ToolTypes.Keys.SWORD, ToolTypes.SWORD);
        register(ToolTypes.Keys.BOW, ToolTypes.BOW);
        register(ToolTypes.Keys.BEEF, ToolTypes.BEEF);
        register(ToolTypes.Keys.SHOVEL, ToolTypes.SHOVEL);
        register(ToolTypes.Keys.TORCH, ToolTypes.TORCH);
    }

    private static void register(final String identifier, final ToolType type) {
        REGISTRY.put(identifier, type);
    }

    public static ToolType fromIdentifier(final String identifier) {
        return REGISTRY.get(identifier);
    }

}

