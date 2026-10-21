package de.deinname.pvpplugin.commands;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.gui.PartyGui;
import de.deinname.pvpplugin.party.Party;
import de.deinname.pvpplugin.party.PartyManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** /party (Menü), create, delete, invite, accept, kick, leave */
public class PartyCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("create", "delete", "invite", "accept", "kick", "leave");

    private final PvpPlugin plugin;

    public PartyCommand(PvpPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            plugin.send(sender, "general.players-only");
            return true;
        }
        PartyManager pm = plugin.parties();
        UUID id = p.getUniqueId();
        Party party = pm.get(id);

        if (args.length == 0) {
            if (party == null) {
                plugin.send(p, "party.no-party-hint");
            } else if (plugin.matches().inMatch(id)) {
                plugin.send(p, "general.in-match");
            } else {
                PartyGui.open(plugin, p, party);
            }
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> {
                if (party != null) {
                    plugin.send(p, "party.already-in-party");
                } else {
                    pm.create(p);
                    plugin.send(p, "party.created");
                }
            }
            case "delete" -> {
                if (party == null) plugin.send(p, "party.not-in-party");
                else if (!party.isLeader(id)) plugin.send(p, "party.leader-only");
                else pm.disband(party);
            }
            case "invite" -> invite(p, party, args);
            case "accept" -> pm.accept(p);
            case "kick" -> kick(p, party, args);
            case "leave" -> {
                if (party == null) {
                    plugin.send(p, "party.not-in-party");
                } else {
                    pm.leave(id, p.getName());
                    plugin.send(p, "party.you-left");
                }
            }
            default -> plugin.send(p, "party.usage");
        }
        return true;
    }

    private void invite(Player p, Party party, String[] args) {
        if (args.length < 2) {
            plugin.send(p, "party.usage");
            return;
        }
        if (party == null) {
            plugin.send(p, "party.not-in-party");
            return;
        }
        if (!party.isLeader(p.getUniqueId())) {
            plugin.send(p, "party.leader-only");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            plugin.send(p, "general.player-offline", "%player%", args[1]);
        } else if (target.equals(p) || plugin.parties().get(target.getUniqueId()) != null) {
            plugin.send(p, "party.target-in-party", "%player%", target.getName());
        } else if (party.getMembers().size() >= plugin.parties().maxSize()) {
            plugin.send(p, "party.full");
        } else {
            plugin.parties().invite(party, p, target);
        }
    }

    private void kick(Player p, Party party, String[] args) {
        if (args.length < 2) {
            plugin.send(p, "party.usage");
            return;
        }
        if (party == null) {
            plugin.send(p, "party.not-in-party");
            return;
        }
        if (!party.isLeader(p.getUniqueId())) {
            plugin.send(p, "party.leader-only");
            return;
        }
        UUID targetId = null;
        String targetName = args[1];
        for (UUID member : party.getMembers()) {
            String n = Bukkit.getOfflinePlayer(member).getName();
            if (n != null && n.equalsIgnoreCase(args[1])) {
                targetId = member;
                targetName = n;
            }
        }
        if (targetId == null) {
            plugin.send(p, "party.not-member", "%player%", args[1]);
        } else if (targetId.equals(p.getUniqueId())) {
            plugin.send(p, "party.cant-kick-self");
        } else {
            plugin.parties().kick(party, targetId, targetName);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        String last = args[args.length - 1].toLowerCase(Locale.ROOT);
        if (args.length == 1) {
            for (String s : SUBS) if (s.startsWith(last)) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase(Locale.ROOT).startsWith(last)) out.add(p.getName());
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("kick") && sender instanceof Player p) {
            Party party = plugin.parties().get(p.getUniqueId());
            if (party != null) {
                for (UUID member : party.getMembers()) {
                    String n = Bukkit.getOfflinePlayer(member).getName();
                    if (n != null && n.toLowerCase(Locale.ROOT).startsWith(last)) out.add(n);
                }
            }
        }
        return out;
    }
}
