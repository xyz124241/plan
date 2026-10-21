package de.deinname.pvpplugin.listener;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.duel.DuelMode;
import de.deinname.pvpplugin.util.Items;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Spawn-Perlen: Links- oder Rechtsklick setzt Spawn 1 bzw. 2 des Duell-Modus an der Position des Admins. */
public class PearlSpawnListener implements Listener {

    private final PvpPlugin plugin;

    public PearlSpawnListener(PvpPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.PHYSICAL || e.getHand() != EquipmentSlot.HAND) return;
        String tag = Items.tagOf(e.getItem(), plugin.spawnKey());
        if (tag == null) return;

        e.setCancelled(true); // Perle wird nie geworfen
        Player p = e.getPlayer();
        if (!p.hasPermission("pvp.admin")) return;

        String[] parts = tag.split(";");
        if (parts.length != 2) return;
        DuelMode mode = plugin.duels().get(parts[0]);
        if (mode == null) {
            plugin.send(p, "duel.not-found", "%name%", parts[0]);
            return;
        }
        int n = "2".equals(parts[1]) ? 2 : 1;
        Location loc = p.getLocation();
        plugin.duels().setSpawn(mode, n, loc);
        plugin.send(p, "duel.spawn-set", "%n%", String.valueOf(n), "%name%", mode.getName());
    }
}
