package de.deinname.pvpplugin.duel;

import de.deinname.pvpplugin.util.Items;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/** Ein Duell-Modus: Name, Icon, Kit-Items (Inventar-Layout) und zwei Spawns. */
public class DuelMode {

    private String name;
    private Material icon;
    private final ItemStack[] kit = new ItemStack[Items.INV_SIZE];
    private Location spawn1;
    private Location spawn2;

    public DuelMode(String name, Material icon) {
        this.name = name;
        this.icon = icon;
    }

    public String key() { return name.toLowerCase(Locale.ROOT); }
    public boolean isReady() { return spawn1 != null && spawn2 != null; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Material getIcon() { return icon; }
    public void setIcon(Material icon) { this.icon = icon; }
    public ItemStack[] getKit() { return kit; }
    public Location getSpawn1() { return spawn1; }
    public void setSpawn1(Location spawn1) { this.spawn1 = spawn1; }
    public Location getSpawn2() { return spawn2; }
    public void setSpawn2(Location spawn2) { this.spawn2 = spawn2; }
}
