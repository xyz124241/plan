package de.deinname.pvpplugin.duel;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.util.Items;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/** Verwaltet Duell-Modi (duels.yml), die Lobby und offene Herausforderungen. */
public class DuelManager {

    private record Challenge(String modeKey, long time) {}

    private final PvpPlugin plugin;
    private final File file;
    private final Map<String, DuelMode> modes = new TreeMap<>();
    private Location lobby;
    /** Ziel -> (Herausforderer -> Herausforderung) */
    private final Map<UUID, Map<UUID, Challenge>> challenges = new HashMap<>();

    public DuelManager(PvpPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "duels.yml");
        load();
    }

    private void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        lobby = y.getLocation("lobby");
        ConfigurationSection sec = y.getConfigurationSection("duels");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(key);
            if (s == null) continue;
            Material icon = Material.matchMaterial(s.getString("icon", "IRON_SWORD"));
            if (icon == null || icon.isAir()) icon = Material.IRON_SWORD;
            DuelMode mode = new DuelMode(s.getString("name", key), icon);
            var kit = Items.decodeAll(s.getStringList("kit"), Items.INV_SIZE);
            System.arraycopy(kit, 0, mode.getKit(), 0, Items.INV_SIZE);
            mode.setSpawn1(s.getLocation("spawn1"));
            mode.setSpawn2(s.getLocation("spawn2"));
            modes.put(mode.key(), mode);
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        if (lobby != null) y.set("lobby", lobby);
        for (DuelMode m : modes.values()) {
            String p = "duels." + m.key();
            y.set(p + ".name", m.getName());
            y.set(p + ".icon", m.getIcon().name());
            y.set(p + ".kit", Items.encodeAll(m.getKit()));
            if (m.getSpawn1() != null) y.set(p + ".spawn1", m.getSpawn1());
            if (m.getSpawn2() != null) y.set(p + ".spawn2", m.getSpawn2());
        }
        try {
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("duels.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- Modi

    public DuelMode get(String name) {
        return name == null ? null : modes.get(name.toLowerCase(Locale.ROOT));
    }

    public Collection<DuelMode> all() {
        return modes.values();
    }

    public boolean create(String name, Material icon) {
        if (get(name) != null) return false;
        DuelMode mode = new DuelMode(name, icon);
        modes.put(mode.key(), mode);
        save();
        return true;
    }

    public boolean delete(String name) {
        if (modes.remove(name.toLowerCase(Locale.ROOT)) == null) return false;
        save();
        return true;
    }

    public void rename(DuelMode mode, String newName) {
        modes.remove(mode.key());
        mode.setName(newName);
        modes.put(mode.key(), mode);
        save();
    }

    public void setSpawn(DuelMode mode, int n, Location loc) {
        if (n == 1) mode.setSpawn1(loc);
        else mode.setSpawn2(loc);
        save();
    }

    public Location getLobby() { return lobby; }

    public void setLobby(Location loc) {
        this.lobby = loc;
        save();
    }

    // ------------------------------------------------------ Herausforderungen

    public void sendChallenge(Player from, Player to, DuelMode mode) {
        if (!mode.isReady()) {
            plugin.send(from, "duel.mode-not-ready");
            return;
        }
        if (plugin.matches().inMatch(from.getUniqueId()) || plugin.matches().inMatch(to.getUniqueId())) {
            plugin.send(from, "duel.already-in-match");
            return;
        }
        challenges.computeIfAbsent(to.getUniqueId(), k -> new HashMap<>())
                .put(from.getUniqueId(), new Challenge(mode.key(), System.currentTimeMillis()));
        plugin.send(from, "duel.challenge-sent", "%player%", to.getName(), "%mode%", mode.getName());
        Component received = plugin.msg("duel.challenge-received", "%player%", from.getName(), "%mode%", mode.getName())
                .clickEvent(ClickEvent.runCommand("/duel accept " + from.getName()))
                .hoverEvent(Component.text("/duel accept " + from.getName()));
        to.sendMessage(received);
    }

    public void accept(Player acceptor, Player challenger) {
        Map<UUID, Challenge> map = challenges.get(acceptor.getUniqueId());
        Challenge c = map == null ? null : map.remove(challenger.getUniqueId());
        long timeout = plugin.getConfig().getLong("duel.challenge-timeout-seconds", 60) * 1000L;
        if (c == null || System.currentTimeMillis() - c.time() > timeout) {
            plugin.send(acceptor, "duel.no-challenge", "%player%", challenger.getName());
            return;
        }
        DuelMode mode = get(c.modeKey());
        if (mode == null || !mode.isReady()) {
            plugin.send(acceptor, "duel.mode-unavailable");
            return;
        }
        if (plugin.matches().inMatch(acceptor.getUniqueId()) || plugin.matches().inMatch(challenger.getUniqueId())) {
            plugin.send(acceptor, "duel.already-in-match");
            return;
        }
        plugin.matches().start(mode, List.of(List.of(challenger), List.of(acceptor)), false);
    }

    /** Entfernt alle offenen Herausforderungen eines Spielers (z. B. beim Verlassen). */
    public void clearPlayer(UUID id) {
        challenges.remove(id);
        for (Map<UUID, Challenge> m : challenges.values()) m.remove(id);
    }
}
