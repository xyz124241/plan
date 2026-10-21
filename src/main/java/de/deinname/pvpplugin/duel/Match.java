package de.deinname.pvpplugin.duel;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.util.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ein laufender Kampf. Teams: bei Duell/Party-Teams zwei Teams, bei FFA hat jeder Spieler ein eigenes Team.
 * Spieler sterben nicht wirklich: tödlicher Schaden wird abgefangen und der Spieler scheidet aus.
 */
public class Match {

    private record PlayerState(ItemStack[] items, Location location, GameMode gameMode, double health,
                               int food, float saturation, Collection<PotionEffect> effects) {}

    private final PvpPlugin plugin;
    private final MatchManager manager;
    private final DuelMode mode;
    private final boolean ffa;

    private final List<Set<UUID>> teams = new ArrayList<>();
    private final Set<UUID> alive = new LinkedHashSet<>();
    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Map<UUID, PlayerState> saved = new HashMap<>();

    private boolean frozen = true;
    private boolean finished = false;
    private BukkitTask countdown;

    Match(PvpPlugin plugin, MatchManager manager, DuelMode mode, boolean ffa) {
        this.plugin = plugin;
        this.manager = manager;
        this.mode = mode;
        this.ffa = ffa;
    }

    // --------------------------------------------------------------- Status

    public boolean isFrozen() { return frozen; }
    public boolean isFinished() { return finished; }
    public boolean isFfa() { return ffa; }
    public boolean isAlive(UUID id) { return alive.contains(id); }

    public boolean sameTeam(UUID a, UUID b) {
        for (Set<UUID> team : teams) {
            if (team.contains(a) && team.contains(b)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------- Start

    void start(List<List<Player>> teamPlayers) {
        Location center = center();
        for (int t = 0; t < teamPlayers.size(); t++) {
            Set<UUID> team = new LinkedHashSet<>();
            for (Player p : teamPlayers.get(t)) {
                UUID id = p.getUniqueId();
                team.add(id);
                alive.add(id);
                participants.add(id);
                manager.register(id, this);
                saved.put(id, new PlayerState(Items.snapshot(p), p.getLocation(), p.getGameMode(), p.getHealth(),
                        p.getFoodLevel(), p.getSaturation(), new ArrayList<>(p.getActivePotionEffects())));
                prepare(p);
                Location dest = ffa ? center : (t == 0 ? mode.getSpawn1() : mode.getSpawn2());
                p.teleport(dest);
            }
            teams.add(team);
        }
        broadcast("match.start", "%mode%", mode.getName());
        startCountdown();
    }

    /** Mitte zwischen Spawn 1 und Spawn 2 (für FFA). */
    private Location center() {
        Location a = mode.getSpawn1();
        Location b = mode.getSpawn2();
        if (a.getWorld() == null || !a.getWorld().equals(b.getWorld())) return a;
        return new Location(a.getWorld(), (a.getX() + b.getX()) / 2.0, Math.max(a.getY(), b.getY()),
                (a.getZ() + b.getZ()) / 2.0, a.getYaw(), a.getPitch());
    }

    private void prepare(Player p) {
        p.closeInventory();
        p.leaveVehicle();
        p.setGameMode(GameMode.SURVIVAL);
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        setHealthSafe(p, 20.0);
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0f);
        Items.apply(p, mode.getKit());
    }

    private void startCountdown() {
        final int seconds = plugin.getConfig().getInt("match.countdown-seconds", 3);
        if (seconds <= 0) {
            frozen = false;
            return;
        }
        countdown = new BukkitRunnable() {
            int t = seconds;

            @Override
            public void run() {
                if (finished) {
                    cancel();
                    return;
                }
                if (t <= 0) {
                    frozen = false;
                    for (UUID id : alive) {
                        Player p = Bukkit.getPlayer(id);
                        if (p != null) p.showTitle(Title.title(plugin.raw("match.go"), Component.empty()));
                    }
                    cancel();
                    return;
                }
                for (UUID id : alive) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) {
                        p.showTitle(Title.title(Component.text(t, NamedTextColor.YELLOW), Component.empty()));
                    }
                }
                t--;
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // ------------------------------------------------------------- Ablauf

    /**
     * Spieler scheidet aus (Tod oder Disconnect). Inventar und Position werden zurückgegeben.
     *
     * @param teleport false, wenn der Spieler gerade tot ist (dann wird der Respawn-Ort gesetzt)
     * @param killer   Name des Gegners oder null
     */
    public void eliminate(Player p, boolean teleport, String killer) {
        UUID id = p.getUniqueId();
        if (!alive.remove(id)) return;
        if (!finished) {
            if (killer != null) broadcast("match.killed", "%player%", p.getName(), "%killer%", killer);
            else broadcast("match.eliminated", "%player%", p.getName());
        }
        restore(p, teleport);
        manager.unregister(id);
        if (!finished) checkEnd();
    }

    private void checkEnd() {
        long teamsAlive = teams.stream().filter(team -> team.stream().anyMatch(alive::contains)).count();
        if (teamsAlive <= 1) finish();
    }

    private void finish() {
        finished = true;
        frozen = false;
        if (countdown != null) countdown.cancel();

        if (alive.isEmpty()) {
            broadcast("match.draw");
        } else {
            List<String> names = new ArrayList<>();
            for (UUID id : alive) {
                String n = Bukkit.getOfflinePlayer(id).getName();
                names.add(n == null ? "?" : n);
            }
            broadcast("match.winner", "%winners%", String.join(", ", names));
        }

        long delay = plugin.getConfig().getLong("match.end-delay-seconds", 3) * 20L;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (UUID id : new ArrayList<>(alive)) {
                Player p = Bukkit.getPlayer(id);
                alive.remove(id);
                manager.unregister(id);
                if (p != null) restore(p, true);
            }
        }, delay);
    }

    /** Sofortiges Ende ohne Sieger (z. B. Server-Stop). */
    void abort() {
        finished = true;
        if (countdown != null) countdown.cancel();
        for (UUID id : new ArrayList<>(alive)) {
            Player p = Bukkit.getPlayer(id);
            alive.remove(id);
            manager.unregister(id);
            if (p != null) restore(p, true);
        }
    }

    private void restore(Player p, boolean teleport) {
        PlayerState s = saved.remove(p.getUniqueId());
        if (s == null) return;
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        p.setFireTicks(0);
        p.setFallDistance(0f);
        Items.apply(p, s.items());
        p.setGameMode(s.gameMode());
        setHealthSafe(p, s.health());
        p.setFoodLevel(s.food());
        p.setSaturation(s.saturation());
        p.addPotionEffects(s.effects());

        Location lobby = plugin.duels().getLobby();
        Location target = lobby != null ? lobby : s.location();
        if (teleport) p.teleport(target);
        else manager.setRespawn(p.getUniqueId(), target);
    }

    private static void setHealthSafe(Player p, double health) {
        try {
            p.setHealth(Math.max(1.0, Math.min(health, 20.0)));
        } catch (IllegalArgumentException ignored) {
            // abweichende maximale Lebenspunkte: Wert einfach lassen
        }
    }

    private void broadcast(String key, String... replacements) {
        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(plugin.msg(key, replacements));
        }
    }
}
