package de.deinname.pvpplugin.kit;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.util.Items;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;

/** Speichert die Kits aller Spieler (kits.yml) und das jeweils ausgewählte Kit. */
public class KitManager {

    private static final Pattern NAME = Pattern.compile("[A-Za-z0-9_-]{1,16}");

    private final PvpPlugin plugin;
    private final File file;
    private final Map<UUID, Map<String, ItemStack[]>> kits = new HashMap<>();
    private final Map<UUID, String> selected = new HashMap<>();

    public KitManager(PvpPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kits.yml");
        load();
    }

    public static boolean validName(String name) {
        return NAME.matcher(name).matches();
    }

    private void load() {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = y.getConfigurationSection("players");
        if (players == null) return;
        for (String uuidStr : players.getKeys(false)) {
            UUID id;
            try {
                id = UUID.fromString(uuidStr);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection p = players.getConfigurationSection(uuidStr);
            if (p == null) continue;
            Map<String, ItemStack[]> map = new TreeMap<>();
            ConfigurationSection ks = p.getConfigurationSection("kits");
            if (ks != null) {
                for (String name : ks.getKeys(false)) {
                    map.put(name, Items.decodeAll(ks.getStringList(name), Items.INV_SIZE));
                }
            }
            kits.put(id, map);
            String sel = p.getString("selected");
            if (sel != null && map.containsKey(sel)) selected.put(id, sel);
        }
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, Map<String, ItemStack[]>> e : kits.entrySet()) {
            String base = "players." + e.getKey();
            for (Map.Entry<String, ItemStack[]> kit : e.getValue().entrySet()) {
                y.set(base + ".kits." + kit.getKey(), Items.encodeAll(kit.getValue()));
            }
            String sel = selected.get(e.getKey());
            if (sel != null) y.set(base + ".selected", sel);
        }
        try {
            y.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("kits.yml konnte nicht gespeichert werden: " + ex.getMessage());
        }
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public boolean exists(UUID id, String name) {
        Map<String, ItemStack[]> map = kits.get(id);
        return map != null && map.containsKey(key(name));
    }

    /** Speichert das aktuelle Inventar als Kit. false = Limit erreicht. */
    public boolean save(Player p, String name) {
        Map<String, ItemStack[]> map = kits.computeIfAbsent(p.getUniqueId(), k -> new TreeMap<>());
        int max = plugin.getConfig().getInt("kits.max-per-player", 10);
        if (!map.containsKey(key(name)) && map.size() >= max) return false;
        map.put(key(name), Items.snapshot(p));
        save();
        return true;
    }

    public boolean delete(UUID id, String name) {
        Map<String, ItemStack[]> map = kits.get(id);
        if (map == null || map.remove(key(name)) == null) return false;
        if (key(name).equals(selected.get(id))) selected.remove(id);
        save();
        return true;
    }

    public boolean select(UUID id, String name) {
        if (!exists(id, name)) return false;
        selected.put(id, key(name));
        save();
        return true;
    }

    public Set<String> names(UUID id) {
        Map<String, ItemStack[]> map = kits.get(id);
        return map == null ? Set.of() : map.keySet();
    }

    /** Gibt dem Spieler sein ausgewähltes Kit (falls vorhanden). */
    public void giveSelected(Player p) {
        String sel = selected.get(p.getUniqueId());
        if (sel == null) return;
        Map<String, ItemStack[]> map = kits.get(p.getUniqueId());
        if (map == null || !map.containsKey(sel)) return;
        Items.apply(p, map.get(sel));
    }
}
