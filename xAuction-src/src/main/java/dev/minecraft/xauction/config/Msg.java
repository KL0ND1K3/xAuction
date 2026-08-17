package dev.minecraft.xauction.config;

import dev.minecraft.xauction.compat.Clients;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class Msg {
    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final ConfigManager configs;
    private final Clients clients;

    public Msg(ConfigManager configs, Clients clients) {
        this.configs = configs;
        this.clients = clients;
    }

    public String raw(String path) {
        return configs.messages().getString(path, path);
    }

    public Component component(String path, String... kv) {
        return parse(raw(path), kv);
    }

    public Component parse(String mini, String... kv) {
        return MM.deserialize(apply(mini == null ? "" : mini, kv));
    }

    public Component parse(Player player, String mini, String... kv) {
        return clients.adapt(player, parse(mini, kv));
    }

    public List<Component> lore(Player player, List<String> lines, String... kv) {
        List<Component> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        for (String line : lines) {
            out.add(parse(player, line, kv));
        }
        return out;
    }

    public void send(CommandSender sender, String path, String... kv) {
        Component prefix = parse(raw("prefix"));
        Component body = component(path, kv);
        if (sender instanceof Player player) {
            prefix = clients.adapt(player, prefix);
            body = clients.adapt(player, body);
        }
        sender.sendMessage(prefix.append(body));
    }

    public void actionbar(Player player, String path, String... kv) {
        player.sendActionBar(clients.adapt(player, component(path, kv)));
    }

    public void broadcast(String path, String... kv) {
        List<String> lines = configs.messages().getStringList(path);
        if (lines.isEmpty()) {
            Component body = component(path, kv);
            Component prefix = parse(raw("prefix"));
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(clients.adapt(player, prefix).append(clients.adapt(player, body)));
            }
            Bukkit.getConsoleSender().sendMessage(prefix.append(body));
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            for (String line : lines) {
                player.sendMessage(parse(player, line, kv));
            }
        }
        for (String line : lines) {
            Bukkit.getConsoleSender().sendMessage(parse(line, kv));
        }
    }

    public static String apply(String text, String... kv) {
        String result = text;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            result = result.replace("{" + kv[i] + "}", kv[i + 1] == null ? "" : kv[i + 1]);
        }
        return result;
    }
}
