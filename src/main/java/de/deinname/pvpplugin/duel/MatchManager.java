package de.deinname.pvpplugin.duel;

import de.deinname.pvpplugin.PvpPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Verwaltet alle laufenden Kämpfe (Duelle und Party-Matches). */
public class MatchManager {

    private final PvpPlugin plugin;
    private final Map<UUID, Match> byPlayer = new HashMap<>();
    private final Map<UUID, Location> respawn = new HashMap<>();

    public MatchManager(PvpPlugin plugin) {
        this.plugin = plugin;
    }

    public Match get(UUID id) {
        return byPlayer.get(id);
    }

    public boolean inMatch(UUID id) {
        return byPlayer.containsKey(id);
    }

    void register(UUID id, Match match) {
        byPlayer.put(id, match);
    }

    void unregister(UUID id) {
        byPlayer.remove(id);
    }

    /** Startet einen Kampf. Bei ffa=false sind es zwei Teams (Spawn 1 / Spawn 2), bei ffa=true alle gegen alle. */
    public void start(DuelMode mode, List<List<Player>> teams, boolean ffa) {
        new Match(plugin, this, mode, ffa).start(teams);
    }

    /** Respawn-Ort für Spieler, die im Kampf gestorben sind. */
    void setRespawn(UUID id, Location loc) {
        respawn.put(id, loc);
    }

    public Location pollRespawn(UUID id) {
        return respawn.remove(id);
    }

    /** Beendet alle Kämpfe sofort und gibt allen ihr Inventar zurück (Server-Stop). */
    public void endAll() {
        for (Match m : new HashSet<>(byPlayer.values())) m.abort();
    }
}
