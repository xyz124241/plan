package de.deinname.pvpplugin.commands;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.duel.DuelMode;
import de.deinname.pvpplugin.gui.DuelGui;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * /duel <spieler>            Menü mit allen Modi, Klick sendet die Herausforderung
 * /duel accept <spieler>     Herausforderung annehmen
 * /duel create|delete|edit|admin|setlobby   (nur OP)
 */
public class DuelCommand implements TabExecutor {

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,24}");
    private static final List<String> ADMIN_SUBS = List.of("create", "delete", "edit", "admin", "setlobby");

    private final PvpPlugin plugin;

    public DuelCommand(PvpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            plugin.send(sender, "duel.usage");
            if (sender.hasPermission("pvp.admin")) plugin.send(sender, "duel.usage-admin");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (ADMIN_SUBS.contains(sub)) {
            if (!sender.hasPermission("pvp.admin")) {
                plugin.send(sender, "general.no-permission");
                return true;
            }
            adminCommand(sender, sub, args);
            return true;
        }
        if (!(sender instanceof Player p)) {
            plugin.send(sender, "general.players-only");
            return true;
        }
        if (sub.equals("accept")) {
            if (args.length < 2) {
                plugin.send(p, "duel.usage");
                return true;
            }
            Player challenger = Bukkit.getPlayerExact(args[1]);
            if (challenger == null) {
                plugin.send(p, "general.player-offline", "%player%", args[1]);
                return true;
            }
            plugin.duels().accept(p, challenger);
            return true;
        }
        challenge(p, args[0]);
        return true;
    }

    private void challenge(Player p, String targetName) {
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            plugin.send(p, "general.player-offline", "%player%", targetName);
        } else if (target.equals(p)) {
            plugin.send(p, "duel.self");
        } else if (plugin.matches().inMatch(p.getUniqueId()) || plugin.matches().inMatch(target.getUniqueId())) {
            plugin.send(p, "duel.already-in-match");
        } else if (plugin.duels().all().isEmpty()) {
            plugin.send(p, "duel.no-modes");
        } else {
            DuelGui.openSelect(plugin, p, target);
        }
    }

    private void adminCommand(CommandSender sender, String sub, String[] args) {
        switch (sub) {
            case "create" -> {
                if (args.length < 3) {
                    plugin.send(sender, "duel.usage-admin");
                    return;
                }
                if (!NAME.matcher(args[1]).matches()) {
                    plugin.send(sender, "duel.invalid-name");
                    return;
                }
                Material icon = parseItem(args[2]);
                if (icon == null) {
                    plugin.send(sender, "duel.invalid-item", "%item%", args[2]);
                    return;
                }
                if (plugin.duels().create(args[1], icon)) plugin.send(sender, "duel.created", "%name%", args[1]);
                else plugin.send(sender, "duel.exists");
            }
            case "delete" -> {
                if (args.length < 2) {
                    plugin.send(sender, "duel.usage-admin");
                    return;
                }
                DuelMode mode = plugin.duels().get(args[1]);
                if (mode == null) {
                    plugin.send(sender, "duel.not-found", "%name%", args[1]);
                    return;
                }
                plugin.duels().delete(args[1]);
                plugin.send(sender, "duel.deleted", "%name%", mode.getName());
            }
            case "edit" -> edit(sender, args);
            case "admin" -> {
                if (sender instanceof Player p) DuelGui.openAdminSelect(plugin, p);
                else plugin.send(sender, "general.players-only");
            }
            case "setlobby" -> {
                if (sender instanceof Player p) {
                    plugin.duels().setLobby(p.getLocation());
                    plugin.send(p, "duel.lobby-set");
                } else {
                    plugin.send(sender, "general.players-only");
                }
            }
            default -> plugin.send(sender, "duel.usage-admin");
        }
    }

    /** /duel edit <name>  |  /duel edit <name> icon <item>  |  /duel edit <name> rename <neuer name> */
    private void edit(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.send(sender, "duel.usage-admin");
            return;
        }
        DuelMode mode = plugin.duels().get(args[1]);
        if (mode == null) {
            plugin.send(sender, "duel.not-found", "%name%", args[1]);
            return;
        }
        if (args.length == 2) {
            if (sender instanceof Player p) DuelGui.openEdit(plugin, p, mode);
            else plugin.send(sender, "general.players-only");
            return;
        }
        String what = args[2].toLowerCase(Locale.ROOT);
        if (what.equals("icon") && args.length >= 4) {
            Material icon = parseItem(args[3]);
            if (icon == null) {
                plugin.send(sender, "duel.invalid-item", "%item%", args[3]);
                return;
            }
            mode.setIcon(icon);
            plugin.duels().save();
            plugin.send(sender, "duel.icon-set", "%name%", mode.getName());
        } else if (what.equals("rename") && args.length >= 4) {
            String newName = args[3];
            if (!NAME.matcher(newName).matches()) {
                plugin.send(sender, "duel.invalid-name");
                return;
            }
            DuelMode existing = plugin.duels().get(newName);
            if (existing != null && existing != mode) {
                plugin.send(sender, "duel.exists");
                return;
            }
            plugin.duels().rename(mode, newName);
            plugin.send(sender, "duel.renamed", "%name%", newName);
        } else {
            plugin.send(sender, "duel.usage-admin");
        }
    }

    private static Material parseItem(String s) {
        Material m = Material.matchMaterial(s);
        return (m == null || m.isAir() || !m.isItem()) ? null : m;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        boolean admin = sender.hasPermission("pvp.admin");
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);

        if (args.length == 1) {
            if ("accept".startsWith(last)) out.add("accept");
            if (admin) for (String s : ADMIN_SUBS) if (s.startsWith(last)) out.add(s);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(last)) out.add(p.getName());
            }
            return out;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (sub.equals("accept")) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getName().toLowerCase(Locale.ROOT).startsWith(last)) out.add(p.getName());
                }
            } else if (admin && (sub.equals("delete") || sub.equals("edit"))) {
                for (DuelMode m : plugin.duels().all()) if (m.key().startsWith(last)) out.add(m.getName());
            }
        } else if (admin && sub.equals("create") && args.length == 3) {
            addItems(out, last);
        } else if (admin && sub.equals("edit")) {
            if (args.length == 3) {
                for (String s : List.of("icon", "rename")) if (s.startsWith(last)) out.add(s);
            } else if (args.length == 4 && args[2].equalsIgnoreCase("icon")) {
                addItems(out, last);
            }
        }
        return out;
    }

    private static void addItems(List<String> out, String prefix) {
        for (Material m : Material.values()) {
            if (out.size() >= 50) break;
            if (m.isItem() && !m.isAir() && m.name().toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(m.name().toLowerCase(Locale.ROOT));
            }
        }
    }
}
