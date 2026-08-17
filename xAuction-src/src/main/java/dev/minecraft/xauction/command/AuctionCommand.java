package dev.minecraft.xauction.command;

import dev.minecraft.xauction.XAuctionPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class AuctionCommand implements CommandExecutor, TabCompleter {
    private final XAuctionPlugin plugin;

    public AuctionCommand(XAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                plugin.msg().send(sender, "players-only");
                return true;
            }
            plugin.auctions().openOrHint(player);
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "bid" -> bid(sender, args);
            case "yes", "confirm", "да" -> requirePlayer(sender, plugin.auctions()::confirm);
            case "no", "deny", "нет" -> requirePlayer(sender, plugin.auctions()::deny);
            case "history" -> requirePlayer(sender, plugin.gui()::openHistory);
            case "reload" -> reload(sender);
            case "next" -> next(sender);
            default -> {
                plugin.msg().send(sender, "unknown-subcommand");
                yield true;
            }
        };
    }

    private boolean bid(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.msg().send(sender, "players-only");
            return true;
        }
        if (args.length < 2) {
            plugin.msg().send(player, "usage-bid");
            return true;
        }
        plugin.auctions().submitText(player, args[1]);
        return true;
    }

    private boolean requirePlayer(CommandSender sender, java.util.function.Consumer<Player> action) {
        if (!(sender instanceof Player player)) {
            plugin.msg().send(sender, "players-only");
            return true;
        }
        action.accept(player);
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission(plugin.configs().config().getString("admin-permission", "xauction.admin"))) {
            plugin.msg().send(sender, "no-permission");
            return true;
        }
        plugin.reloadAll();
        plugin.msg().send(sender, "reloaded");
        return true;
    }

    private boolean next(CommandSender sender) {
        if (!sender.hasPermission(plugin.configs().config().getString("admin-permission", "xauction.admin"))) {
            plugin.msg().send(sender, "no-permission");
            return true;
        }
        plugin.auctions().forceNext(true);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            Stream<String> stream = Stream.of("bid", "history", "yes", "no");
            if (sender.hasPermission(plugin.configs().config().getString("admin-permission", "xauction.admin"))) {
                stream = Stream.concat(stream, Stream.of("reload", "next"));
            }
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return stream.filter(s -> s.startsWith(prefix)).toList();
        }
        return List.of();
    }
}
