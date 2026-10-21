package de.deinname.pvpplugin.listener;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.duel.Match;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

/** Gibt nach dem Tod das mit /kits select gewählte Kit. Sicherheitsnetz für Tode im Kampf. */
public class DeathRespawnListener implements Listener {

    private final PvpPlugin plugin;

    public DeathRespawnListener(PvpPlugin plugin) {
        this.plugin = plugin;
    }

    /** Normalerweise stirbt im Kampf niemand (siehe MatchListener). Falls doch (z. B. /kill), wird sauber aufgeräumt. */
    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        Match m = plugin.matches().get(id);
        if (m == null || !m.isAlive(id)) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
        e.setKeepInventory(true);
        e.setKeepLevel(true);
        m.eliminate(p, false, null);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Location target = plugin.matches().pollRespawn(p.getUniqueId());
        if (target != null) {
            e.setRespawnLocation(target); // Tod im Kampf: Inventar wurde schon zurückgegeben
            return;
        }
        // 1 Tick warten, damit das Kit nach dem Respawn gesetzt wird
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p.isOnline() && !plugin.matches().inMatch(p.getUniqueId())) {
                plugin.kits().giveSelected(p);
            }
        });
    }
}
