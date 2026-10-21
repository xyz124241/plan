package de.deinname.pvpplugin.kitroom;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.util.Items;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;

public class KitroomManager {

    public static final int SIZE = 54;

    private final PvpPlugin plugin;
    private final File file;
    private ItemStack[] items;

    public KitroomManager(PvpPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "kitroom.yml");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        this.items = Items.decodeAll(y.getStringList("items"), SIZE);
    }

    public ItemStack[] get() {
        return items;
    }

    public void set(ItemStack[] newItems) {
        this.items = newItems;
        save();
    }

    private void save() {
        YamlConfiguration y = new YamlConfiguration();
        y.set("items", Items.encodeAll(items));
        try {
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("kitroom.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }
}
