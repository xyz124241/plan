package de.deinname.pvpplugin;

import de.deinname.pvpplugin.commands.AdminCommand;
import de.deinname.pvpplugin.commands.DuelCommand;
import de.deinname.pvpplugin.commands.KitroomCommand;
import de.deinname.pvpplugin.commands.KitsCommand;
import de.deinname.pvpplugin.commands.PartyCommand;
import de.deinname.pvpplugin.duel.DuelManager;
import de.deinname.pvpplugin.duel.MatchManager;
import de.deinname.pvpplugin.kit.KitManager;
import de.deinname.pvpplugin.kitroom.KitroomManager;
import de.deinname.pvpplugin.listener.DeathRespawnListener;
import de.deinname.pvpplugin.listener.GuiClickListener;
import de.deinname.pvpplugin.listener.MatchListener;
import de.deinname.pvpplugin.listener.PearlSpawnListener;
import de.deinname.pvpplugin.party.PartyManager;
import de.deinname.pvpplugin.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class PvpPlugin extends JavaPlugin {

    private YamlConfiguration messages;
    private NamespacedKey spawnKey;
    private NamespacedKey modeKey;

    private KitroomManager kitroom;
    private DuelManager duels;
    private PartyManager parties;
    private KitManager kits;
    private MatchManager matches;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadMessages();

        spawnKey = new NamespacedKey(this, "spawn_pearl");
        modeKey = new NamespacedKey(this, "duel_mode");

        kitroom = new KitroomManager(this);
        duels = new DuelManager(this);
        parties = new PartyManager(this);
        kits = new KitManager(this);
        matches = new MatchManager(this);

        DuelCommand duelCommand = new DuelCommand(this);
        register("kitroom", new KitroomCommand(this, false));
        register("admin-kitman-kitroom", new KitroomCommand(this, true));
        register("duel", duelCommand);
        register("admin", new AdminCommand(this, duelCommand));
        register("party", new PartyCommand(this));
        register("kits", new KitsCommand(this));

        getServer().getPluginManager().registerEvents(new GuiClickListener(this), this);
        getServer().getPluginManager().registerEvents(new PearlSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new MatchListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathRespawnListener(this), this);
    }

    @Override
    public void onDisable() {
        if (matches != null) matches.endAll();
    }

    private void register(String name, CommandExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().severe("Befehl fehlt in plugin.yml: " + name);
            return;
        }
        cmd.setExecutor(executor);
        if (executor instanceof TabCompleter completer) cmd.setTabCompleter(completer);
    }

    private void loadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        if (!file.exists()) saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(file);
        try (InputStream in = getResource("messages.yml")) {
            if (in != null) {
                messages.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8)));
            }
        } catch (IOException ignored) {
            // Defaults sind optional
        }
    }

    private String format(String key, String... replacements) {
        String s = messages.getString(key, key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            s = s.replace(replacements[i], replacements[i + 1]);
        }
        return s;
    }

    /** Nachricht ohne Prefix (z. B. für GUI-Titel). Ersetzungen als Paare: "%name%", "wert". */
    public Component raw(String key, String... replacements) {
        return Text.c(format(key, replacements));
    }

    /** Nachricht mit Prefix. */
    public Component msg(String key, String... replacements) {
        return Text.c(messages.getString("prefix", "") + format(key, replacements));
    }

    public void send(CommandSender to, String key, String... replacements) {
        to.sendMessage(msg(key, replacements));
    }

    public NamespacedKey spawnKey() { return spawnKey; }
    public NamespacedKey modeKey() { return modeKey; }
    public KitroomManager kitroom() { return kitroom; }
    public DuelManager duels() { return duels; }
    public PartyManager parties() { return parties; }
    public KitManager kits() { return kits; }
    public MatchManager matches() { return matches; }
}
