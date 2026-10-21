package de.deinname.pvpplugin.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import de.deinname.pvpplugin.PvpPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** /admin duel ... ist dasselbe wie /duel admin bzw. /duel create|delete|edit ... */
public class AdminCommand implements TabExecutor {

    private final PvpPlugin plugin;
    private final DuelCommand duelCommand;

    public AdminCommand(PvpPlugin plugin, DuelCommand duelCommand) {
        this.plugin = plugin;
        this.duelCommand = duelCommand;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pvp.admin")) {
            plugin.send(sender, "general.no-permission");
            return true;
        }
        if (args.length == 0 || !args[0].equalsIgnoreCase("duel")) {
            plugin.send(sender, "duel.usage-admin");
            return true;
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        if (rest.length == 0) rest = new String[]{"admin"};
        return duelCommand.onCommand(sender, command, label, rest);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("pvp.admin")) return List.of();
        if (args.length == 1) {
            return "duel".startsWith(args[0].toLowerCase(Locale.ROOT)) ? List.of("duel") : List.of();
        }
        if (args[0].equalsIgnoreCase("duel")) {
            return duelCommand.onTabComplete(sender, command, alias, Arrays.copyOfRange(args, 1, args.length));
        }
        return List.of();
    }
}
