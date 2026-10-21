package de.deinname.pvpplugin.party;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.duel.DuelMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PartyManager {

    private record Invite(Party party, long time) {}

    private final PvpPlugin plugin;
    private final Map<UUID, Party> byMember = new HashMap<>();
    private final Map<UUID, Invite> invites = new HashMap<>();

    public PartyManager(PvpPlugin plugin) {
        this.plugin = plugin;
    }

    public Party get(UUID id) {
        return byMember.get(id);
    }

    public int maxSize() {
        return plugin.getConfig().getInt("party.max-size", 8);
    }

    public Party create(Player leader) {
        Party party = new Party(leader.getUniqueId());
        byMember.put(leader.getUniqueId(), party);
        return party;
    }

    public void disband(Party party) {
        notifyMembers(party, "party.disbanded");
        for (UUID id : new ArrayList<>(party.getMembers())) byMember.remove(id);
        invites.values().removeIf(i -> i.party() == party);
    }

    public void invite(Party party, Player from, Player target) {
        invites.put(target.getUniqueId(), new Invite(party, System.currentTimeMillis()));
        plugin.send(from, "party.invite-sent", "%player%", target.getName());
        Component c = plugin.msg("party.invite-received", "%player%", from.getName())
                .clickEvent(ClickEvent.runCommand("/party accept"))
                .hoverEvent(Component.text("/party accept"));
        target.sendMessage(c);
    }

    public void accept(Player p) {
        UUID id = p.getUniqueId();
        Invite invite = invites.remove(id);
        long timeout = plugin.getConfig().getLong("party.invite-timeout-seconds", 60) * 1000L;
        if (invite == null || System.currentTimeMillis() - invite.time() > timeout
                || byMember.get(invite.party().getLeader()) != invite.party()) {
            plugin.send(p, "party.no-invite");
            return;
        }
        if (byMember.containsKey(id)) {
            plugin.send(p, "party.already-in-party");
            return;
        }
        Party party = invite.party();
        if (party.getMembers().size() >= maxSize()) {
            plugin.send(p, "party.full");
            return;
        }
        party.getMembers().add(id);
        byMember.put(id, party);
        notifyMembers(party, "party.joined", "%player%", p.getName());
    }

    public void kick(Party party, UUID target, String targetName) {
        party.getMembers().remove(target);
        byMember.remove(target);
        notifyMembers(party, "party.kicked", "%player%", targetName);
        Player t = Bukkit.getPlayer(target);
        if (t != null) plugin.send(t, "party.you-kicked");
    }

    /** Spieler verlässt die Party (auch beim Disconnect). Ist er Leader, wechselt der Leader oder die Party löst sich auf. */
    public void leave(UUID id, String name) {
        Party party = byMember.remove(id);
        if (party == null) return;
        party.getMembers().remove(id);
        if (party.getMembers().isEmpty()) return;
        notifyMembers(party, "party.left", "%player%", name);
        if (party.isLeader(id)) {
            if (plugin.getConfig().getBoolean("party.disband-on-leader-leave", false)) {
                disband(party);
            } else {
                UUID next = party.getMembers().iterator().next();
                party.setLeader(next);
                String nextName = Bukkit.getOfflinePlayer(next).getName();
                notifyMembers(party, "party.new-leader", "%player%", nextName == null ? "?" : nextName);
            }
        }
    }

    public void notifyMembers(Party party, String key, String... replacements) {
        for (UUID id : party.getMembers()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(plugin.msg(key, replacements));
        }
    }

    // ------------------------------------------------------------- Modi

    private List<DuelMode> readyModes() {
        List<DuelMode> list = new ArrayList<>();
        for (DuelMode m : plugin.duels().all()) if (m.isReady()) list.add(m);
        return list;
    }

    /** Gewählter Modus der Party; wählt automatisch den ersten fertigen Modus, falls nötig. */
    public DuelMode resolveMode(Party party) {
        DuelMode current = plugin.duels().get(party.getModeKey());
        if (current != null && current.isReady()) return current;
        List<DuelMode> ready = readyModes();
        if (ready.isEmpty()) {
            party.setModeKey(null);
            return null;
        }
        party.setModeKey(ready.get(0).key());
        return ready.get(0);
    }

    public void cycleMode(Party party) {
        List<DuelMode> ready = readyModes();
        if (ready.isEmpty()) {
            party.setModeKey(null);
            return;
        }
        int idx = -1;
        for (int i = 0; i < ready.size(); i++) {
            if (ready.get(i).key().equals(party.getModeKey())) idx = i;
        }
        party.setModeKey(ready.get((idx + 1) % ready.size()).key());
    }

    // ------------------------------------------------------------ Start

    public void start(Player leader, Party party) {
        if (!party.isLeader(leader.getUniqueId())) {
            plugin.send(leader, "party.leader-only");
            return;
        }
        DuelMode mode = resolveMode(party);
        if (mode == null) {
            plugin.send(leader, "party.no-mode");
            return;
        }
        List<Player> players = new ArrayList<>();
        for (UUID id : party.getMembers()) {
            Player m = Bukkit.getPlayer(id);
            if (m == null) {
                plugin.send(leader, "party.member-offline");
                return;
            }
            if (plugin.matches().inMatch(id)) {
                plugin.send(leader, "party.member-in-match");
                return;
            }
            players.add(m);
        }
        if (players.size() < 2) {
            plugin.send(leader, "party.too-few");
            return;
        }

        List<List<Player>> teams = new ArrayList<>();
        if (party.isFfa()) {
            for (Player p : players) teams.add(List.of(p));
        } else {
            // 3 Spieler = 2 gegen 1, 4 Spieler = 2 gegen 2, usw. (zufällig gemischt)
            Collections.shuffle(players);
            int half = (players.size() + 1) / 2;
            teams.add(new ArrayList<>(players.subList(0, half)));
            teams.add(new ArrayList<>(players.subList(half, players.size())));
        }
        plugin.matches().start(mode, teams, party.isFfa());
    }

    /** Aufräumen beim Disconnect: offene Einladung entfernen. */
    public void clearInvite(UUID id) {
        invites.remove(id);
    }

    /** Alle Spieler-UUIDs, die in einer Party sind (für Tab-Completion nicht nötig, aber praktisch). */
    public HashSet<UUID> allMembers() {
        return new HashSet<>(byMember.keySet());
    }
}
