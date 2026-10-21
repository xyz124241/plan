package de.deinname.pvpplugin.commands;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.gui.KitroomGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /kitroom (alle) und /admin-kitman-kitroom (nur OP). */
public class KitroomCommand implements CommandExecutor {

    private final PvpPlugin plugin;
    private final boolean admin;

    public KitroomCommand(PvpPlugin plugin, boolean admin) {
        this.plugin = plugin;
        this.admin = admin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            plugin.send(sender, "general.players-only");
            return true;
        }
        if (admin && !p.hasPermission("pvp.admin")) {
            plugin.send(p, "general.no-permission");
            return true;
        }
        if (plugin.matches().inMatch(p.getUniqueId())) {
            plugin.send(p, "general.in-match");
            return true;
        }
        KitroomGui.open(plugin, p, admin);
        return true;
    }
}
