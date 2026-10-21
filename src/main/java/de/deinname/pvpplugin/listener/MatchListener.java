package de.deinname.pvpplugin.listener;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.duel.Match;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class MatchListener implements Listener {

    private final PvpPlugin plugin;

    public MatchListener(PvpPlugin plugin) {
        this.plugin = plugin;
    }

    /** Während des Countdowns kann man sich nicht bewegen (Umschauen und Fallen bleibt möglich). */
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Match m = plugin.matches().get(e.getPlayer().getUniqueId());
        if (m == null || !m.isFrozen()) return;
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;
        if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
            to.setX(from.getX());
            to.setZ(from.getZ());
            e.setTo(to);
        }
    }

    /** Kein Teamschaden, und niemand von außen kann in einen Kampf eingreifen. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        Player attacker = attacker(e);
        if (attacker == null) return;
        Match vm = plugin.matches().get(victim.getUniqueId());
        Match am = plugin.matches().get(attacker.getUniqueId());
        if (vm == null && am == null) return;
        if (vm != am) {
            e.setCancelled(true);
            return;
        }
        if (!attacker.equals(victim) && vm.sameTeam(attacker.getUniqueId(), victim.getUniqueId())) {
            e.setCancelled(true);
        }
    }

    /** Tödlicher Schaden wird abgefangen: der Spieler scheidet aus, statt zu sterben. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        UUID id = p.getUniqueId();
        Match m = plugin.matches().get(id);
        if (m == null) return;
        if (m.isFrozen() || m.isFinished() || !m.isAlive(id)) {
            e.setCancelled(true);
            return;
        }
        if (p.getHealth() - e.getFinalDamage() > 0) return;
        if (p.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || p.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) {
            return; // Totem rettet den Spieler
        }
        e.setCancelled(true);
        Player killer = attacker(e);
        String killerName = (killer != null && !killer.equals(p)) ? killer.getName() : null;
        Bukkit.getScheduler().runTask(plugin, () -> m.eliminate(p, true, killerName));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        Match m = plugin.matches().get(id);
        if (m != null) m.eliminate(p, true, null);
        plugin.parties().leave(id, p.getName());
        plugin.parties().clearInvite(id);
        plugin.duels().clearPlayer(id);
    }

    private static Player attacker(EntityDamageEvent e) {
        if (!(e instanceof EntityDamageByEntityEvent ee)) return null;
        Entity damager = ee.getDamager();
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player shooter) return shooter;
        return null;
    }
}
