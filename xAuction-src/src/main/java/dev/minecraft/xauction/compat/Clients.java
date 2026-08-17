package dev.minecraft.xauction.compat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

public final class Clients {
    public static final int V1_8 = 47;
    public static final int V1_9 = 107;
    public static final int V1_13 = 393;
    public static final int V1_16 = 735;

    private static final LegacyComponentSerializer VANILLA = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .build();

    private Method viaPlayer;
    private Method viaUuid;
    private boolean viaFailed;

    public int protocol(Player player) {
        if (player == null) {
            return Integer.MAX_VALUE;
        }
        Object api = viaApi();
        if (api == null) {
            return Integer.MAX_VALUE;
        }
        try {
            if (viaPlayer != null) {
                return asInt(viaPlayer.invoke(api, player));
            }
            if (viaUuid != null) {
                return asInt(viaUuid.invoke(api, player.getUniqueId()));
            }
        } catch (Throwable ignored) {
        }
        return Integer.MAX_VALUE;
    }

    public boolean hex(Player player) {
        return protocol(player) >= V1_16;
    }

    public boolean longTitle(Player player) {
        return protocol(player) >= V1_13;
    }

    public boolean modernSounds(Player player) {
        return protocol(player) >= V1_9;
    }

    public Component adapt(Player player, Component component) {
        if (component == null) {
            return Component.empty();
        }
        if (hex(player)) {
            return component;
        }
        return VANILLA.deserialize(VANILLA.serialize(component));
    }

    public Component title(Player player, Component component) {
        Component c = adapt(player, component);
        if (longTitle(player)) {
            return c;
        }
        String s = VANILLA.serialize(c);
        if (s.length() > 32) {
            s = s.substring(0, splitAt(s, 32));
        }
        return VANILLA.deserialize(s);
    }

    public Material item(Player player, Material material) {
        if (material == null || material.isAir()) {
            return Material.STONE;
        }
        if (protocol(player) >= V1_16) {
            return material;
        }
        String name = material.name();
        if (name.contains("DEEPSLATE") || name.contains("AMETHYST") || name.contains("COPPER") || name.startsWith("RAW_")) {
            return Material.STONE;
        }
        if (name.contains("NETHERITE") || name.equals("ANCIENT_DEBRIS")) {
            return Material.DIAMOND;
        }
        if (name.equals("SUNFLOWER") || name.equals("TORCHFLOWER")) {
            return Material.DANDELION;
        }
        return switch (material) {
            case TOTEM_OF_UNDYING -> Material.EMERALD;
            case ELYTRA -> Material.LEATHER_CHESTPLATE;
            case NETHER_STAR -> Material.NETHER_STAR;
            case BEACON -> Material.BEACON;
            case EXPERIENCE_BOTTLE -> Material.EXPERIENCE_BOTTLE;
            case ECHO_SHARD, RECOVERY_COMPASS -> Material.COMPASS;
            default -> material;
        };
    }

    public Sound sound(Player player, Sound sound) {
        if (sound == null || modernSounds(player)) {
            return sound;
        }
        String name = sound.name();
        if (name.startsWith("UI_") || name.contains("BOOK") || name.contains("BUTTON") || name.contains("NOTE")) {
            return Sound.ENTITY_ITEM_PICKUP;
        }
        return sound;
    }

    public static int splitAt(String text, int max) {
        if (text.length() <= max) {
            return text.length();
        }
        int i = max;
        if (i > 0 && text.charAt(i - 1) == ChatColor.COLOR_CHAR) {
            i--;
        }
        return Math.max(0, i);
    }

    private Object viaApi() {
        if (viaFailed) {
            return null;
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin("ViaVersion");
        if (plugin == null || !plugin.isEnabled()) {
            return null;
        }
        try {
            Object api = Class.forName("com.viaversion.viaversion.api.Via").getMethod("getAPI").invoke(null);
            if (viaPlayer == null && viaUuid == null) {
                try {
                    viaPlayer = api.getClass().getMethod("getPlayerVersion", Player.class);
                } catch (NoSuchMethodException ignored) {
                }
                try {
                    viaUuid = api.getClass().getMethod("getPlayerVersion", UUID.class);
                } catch (NoSuchMethodException ignored) {
                }
            }
            return api;
        } catch (Throwable t) {
            viaFailed = true;
            return null;
        }
    }

    private static int asInt(Object value) {
        if (value instanceof Integer i) {
            return i;
        }
        if (value == null) {
            return Integer.MAX_VALUE;
        }
        try {
            Object v = value.getClass().getMethod("getVersion").invoke(value);
            if (v instanceof Integer i) {
                return i;
            }
        } catch (Throwable ignored) {
        }
        return Integer.MAX_VALUE;
    }
}
