package dev.minecraft.xauction.economy;

import dev.minecraft.xauction.XAuctionPlugin;
import dev.minecraft.xauction.util.Money;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.UUID;

public final class EconomyService {
    private final XAuctionPlugin plugin;
    private Hook hook;

    public EconomyService(XAuctionPlugin plugin) {
        this.plugin = plugin;
    }

    public void hook() {
        String prefer = plugin.configs().config().getString("economy.provider", "auto");
        prefer = prefer == null ? "auto" : prefer.toLowerCase();
        hook = null;
        if ("rpg".equals(prefer)) {
            hook = rpg();
        } else if ("vault".equals(prefer)) {
            hook = vault();
        } else {
            hook = vault();
            if (hook == null) {
                hook = rpg();
            }
        }
    }

    public boolean ready() {
        return hook != null;
    }

    public String describe() {
        return hook == null ? "none" : hook.name();
    }

    public double balance(OfflinePlayer player) {
        return hook == null ? 0 : hook.balance(player);
    }

    public boolean has(OfflinePlayer player, double amount) {
        return hook != null && hook.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        return hook != null && hook.withdraw(player, amount);
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        return hook != null && hook.deposit(player, amount);
    }

    public String format(double amount) {
        if (hook != null) {
            String formatted = hook.format(amount);
            if (formatted != null && !formatted.isBlank()) {
                return formatted;
            }
        }
        return Money.format(amount);
    }

    private Hook vault() {
        Hook vault1 = vault1();
        if (vault1 != null) {
            return vault1;
        }
        return vault2();
    }

    private Hook vault1() {
        try {
            Class<?> type = Class.forName("net.milkbowl.vault.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(type);
            if (rsp == null || rsp.getProvider() == null) {
                return null;
            }
            Object eco = rsp.getProvider();
            Method name = method(eco, "getName");
            Method format = method(eco, "format", double.class);
            Method hasOff = method(eco, "has", OfflinePlayer.class, double.class);
            Method balOff = method(eco, "getBalance", OfflinePlayer.class);
            Method wOff = method(eco, "withdrawPlayer", OfflinePlayer.class, double.class);
            Method dOff = method(eco, "depositPlayer", OfflinePlayer.class, double.class);
            if (balOff == null || wOff == null || dOff == null) {
                return null;
            }
            return new Hook() {
                @Override
                public String name() {
                    return "Vault/" + invokeString(name, eco);
                }

                @Override
                public double balance(OfflinePlayer player) {
                    Object v = invoke(balOff, eco, player);
                    return v instanceof Number n ? n.doubleValue() : 0;
                }

                @Override
                public boolean has(OfflinePlayer player, double amount) {
                    if (hasOff != null) {
                        Object v = invoke(hasOff, eco, player, amount);
                        return Boolean.TRUE.equals(v);
                    }
                    return balance(player) + 1e-9 >= amount;
                }

                @Override
                public boolean withdraw(OfflinePlayer player, double amount) {
                    return success(invoke(wOff, eco, player, amount));
                }

                @Override
                public boolean deposit(OfflinePlayer player, double amount) {
                    return success(invoke(dOff, eco, player, amount));
                }

                @Override
                public String format(double amount) {
                    Object v = format == null ? null : invoke(format, eco, amount);
                    return v == null ? Money.format(amount) : String.valueOf(v);
                }
            };
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Hook vault2() {
        try {
            Class<?> type = Class.forName("net.milkbowl.vault2.economy.Economy");
            RegisteredServiceProvider<?> rsp = Bukkit.getServicesManager().getRegistration(type);
            if (rsp == null || rsp.getProvider() == null) {
                return null;
            }
            Object eco = rsp.getProvider();
            String pluginName = "xAuction";
            Method name = first(eco, "getName", "getProviderName");
            Method balUuid = method(eco, "getBalance", UUID.class);
            Method balNamed = method(eco, "getBalance", String.class, UUID.class);
            Method hasUuid = method(eco, "has", UUID.class, BigDecimal.class);
            Method hasNamed = method(eco, "has", String.class, UUID.class, BigDecimal.class);
            Method wUuid = method(eco, "withdraw", UUID.class, BigDecimal.class);
            Method wNamed = method(eco, "withdraw", String.class, UUID.class, BigDecimal.class);
            Method dUuid = method(eco, "deposit", UUID.class, BigDecimal.class);
            Method dNamed = method(eco, "deposit", String.class, UUID.class, BigDecimal.class);
            Method formatBd = method(eco, "format", BigDecimal.class);
            if (balUuid == null && balNamed == null) {
                return null;
            }
            return new Hook() {
                @Override
                public String name() {
                    return "VaultUnlocked/" + invokeString(name, eco);
                }

                @Override
                public double balance(OfflinePlayer player) {
                    Object v = balUuid != null
                            ? invoke(balUuid, eco, player.getUniqueId())
                            : invoke(balNamed, eco, pluginName, player.getUniqueId());
                    return decimal(v);
                }

                @Override
                public boolean has(OfflinePlayer player, double amount) {
                    BigDecimal bd = BigDecimal.valueOf(amount);
                    Object v;
                    if (hasUuid != null) {
                        v = invoke(hasUuid, eco, player.getUniqueId(), bd);
                    } else if (hasNamed != null) {
                        v = invoke(hasNamed, eco, pluginName, player.getUniqueId(), bd);
                    } else {
                        return balance(player) + 1e-9 >= amount;
                    }
                    return Boolean.TRUE.equals(v);
                }

                @Override
                public boolean withdraw(OfflinePlayer player, double amount) {
                    BigDecimal bd = BigDecimal.valueOf(amount);
                    Object v = wUuid != null
                            ? invoke(wUuid, eco, player.getUniqueId(), bd)
                            : invoke(wNamed, eco, pluginName, player.getUniqueId(), bd);
                    return success(v);
                }

                @Override
                public boolean deposit(OfflinePlayer player, double amount) {
                    BigDecimal bd = BigDecimal.valueOf(amount);
                    Object v = dUuid != null
                            ? invoke(dUuid, eco, player.getUniqueId(), bd)
                            : invoke(dNamed, eco, pluginName, player.getUniqueId(), bd);
                    return success(v);
                }

                @Override
                public String format(double amount) {
                    if (formatBd == null) {
                        return Money.format(amount);
                    }
                    Object v = invoke(formatBd, eco, BigDecimal.valueOf(amount));
                    return v == null ? Money.format(amount) : String.valueOf(v);
                }
            };
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Hook rpg() {
        Plugin rpg = Bukkit.getPluginManager().getPlugin("RPG");
        if (rpg == null || !rpg.isEnabled()) {
            return null;
        }
        try {
            Object store = rpg.getClass().getMethod("store").invoke(rpg);
            Method get = store.getClass().getMethod("get", UUID.class, String.class);
            Method save = null;
            for (Method method : store.getClass().getMethods()) {
                if (method.getName().equals("save") && method.getParameterCount() == 1) {
                    save = method;
                    break;
                }
            }
            Method boards = null;
            try {
                boards = rpg.getClass().getMethod("boards");
            } catch (NoSuchMethodException ignored) {
            }
            Method refresh = null;
            Object boardService = boards == null ? null : boards.invoke(rpg);
            if (boardService != null) {
                try {
                    refresh = boardService.getClass().getMethod("refresh", Player.class);
                } catch (NoSuchMethodException ignored) {
                }
            }
            Method saveFinal = save;
            Method refreshFinal = refresh;
            Object boardFinal = boardService;
            return new Hook() {
                @Override
                public String name() {
                    return "RPG";
                }

                @Override
                public double balance(OfflinePlayer player) {
                    Object data = data(player);
                    if (data == null) {
                        return 0;
                    }
                    Object v = invoke(method(data, "coins"), data);
                    return v instanceof Number n ? n.doubleValue() : 0;
                }

                @Override
                public boolean has(OfflinePlayer player, double amount) {
                    return balance(player) + 1e-9 >= amount;
                }

                @Override
                public boolean withdraw(OfflinePlayer player, double amount) {
                    return add(player, -amount);
                }

                @Override
                public boolean deposit(OfflinePlayer player, double amount) {
                    return add(player, amount);
                }

                @Override
                public String format(double amount) {
                    return Money.format(amount) + " ●";
                }

                private boolean add(OfflinePlayer player, double delta) {
                    Object data = data(player);
                    if (data == null) {
                        return false;
                    }
                    Method coinsGet = method(data, "coins");
                    Method coinsSet = method(data, "coins", double.class);
                    Object now = invoke(coinsGet, data);
                    double value = now instanceof Number n ? n.doubleValue() : 0;
                    double next = Money.round(value + delta);
                    if (next < -1e-9) {
                        return false;
                    }
                    invoke(coinsSet, data, Math.max(0, next));
                    if (saveFinal != null) {
                        invoke(saveFinal, store, data);
                    }
                    if (refreshFinal != null && boardFinal != null && player instanceof Player online) {
                        invoke(refreshFinal, boardFinal, online);
                    }
                    return true;
                }

                private Object data(OfflinePlayer player) {
                    String name = player.getName() == null ? "unknown" : player.getName();
                    return invoke(get, store, player.getUniqueId(), name);
                }
            };
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("RPG economy hook failed: " + e.getMessage());
            return null;
        }
    }

    private static boolean success(Object response) {
        if (response instanceof Boolean b) {
            return b;
        }
        if (response == null) {
            return false;
        }
        Method m = method(response, "transactionSuccess");
        if (m == null) {
            m = method(response, "success");
        }
        Object v = invoke(m, response);
        return Boolean.TRUE.equals(v);
    }

    private static double decimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd.doubleValue();
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        return 0;
    }

    private static Method method(Object target, String name, Class<?>... types) {
        if (target == null) {
            return null;
        }
        try {
            return target.getClass().getMethod(name, types);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    private static Method first(Object target, String... names) {
        for (String name : names) {
            Method m = method(target, name);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    private static Object invoke(Method method, Object target, Object... args) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static String invokeString(Method method, Object target) {
        Object v = invoke(method, target);
        return v == null ? "unknown" : String.valueOf(v);
    }

    private interface Hook {
        String name();

        double balance(OfflinePlayer player);

        boolean has(OfflinePlayer player, double amount);

        boolean withdraw(OfflinePlayer player, double amount);

        boolean deposit(OfflinePlayer player, double amount);

        String format(double amount);
    }
}
