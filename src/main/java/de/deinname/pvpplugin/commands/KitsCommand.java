package de.deinname.pvpplugin.commands;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.kit.KitManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** /kits save|delete|select <name> und /kits list */
public class KitsCommand implements TabExecutor {

    private final PvpPlugin plugin;

    public KitsCommand(PvpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            plugin.send(sender, "general.players-only");
            return true;
        }
        if (args.length == 0) {
            plugin.send(p, "kits.usage");
            return true;
        }
        UUID id = p.getUniqueId();
        KitManager kits = plugin.kits();
        String sub = args[0].toLowerCase(Locale.ROOT);

        if (sub.equals("list")) {
            if (kits.names(id).isEmpty()) plugin.send(p, "kits.list-empty");
            else plugin.send(p, "kits.list", "%kits%", String.join(", ", kits.names(id)));
            return true;
        }
        if (!(sub.equals("save") || sub.equals("delete") || sub.equals("select")) || args.length < 2) {
            plugin.send(p, "kits.usage");
            return true;
        }
        String name = args[1];
        if (!KitManager.validName(name)) {
            plugin.send(p, "kits.invalid-name");
            return true;
        }

        switch (sub) {
            case "save" -> {
                if (plugin.matches().inMatch(id)) {
                    plugin.send(p, "general.in-match");
                } else if (kits.save(p, name)) {
                    plugin.send(p, "kits.saved", "%name%", name);
                } else {
                    plugin.send(p, "kits.limit");
                }
            }
            case "delete" -> {
                if (kits.delete(id, name)) plugin.send(p, "kits.deleted", "%name%", name);
                else plugin.send(p, "kits.not-found", "%name%", name);
            }
            case "select" -> {
                if (kits.select(id, name)) plugin.send(p, "kits.selected", "%name%", name);
                else plugin.send(p, "kits.not-found", "%name%", name);
            }
            default -> plugin.send(p, "kits.usage");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("save", "delete", "select", "list")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
            }
        } else if (args.length == 2 && sender instanceof Player p
                && (args[0].equalsIgnoreCase("delete") || args[0].equalsIgnoreCase("select"))) {
            for (String n : plugin.kits().names(p.getUniqueId())) {
                if (n.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(n);
            }
        }
        return out;
    }
}
