package de.deinname.pvpplugin.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/** Markiert Inventare dieses Plugins, damit die Listener sie erkennen. */
public final class PvpHolder implements InventoryHolder {

    public enum Type { KITROOM, KITROOM_ADMIN, DUEL_SELECT, DUEL_ADMIN_SELECT, DUEL_EDIT, PARTY }

    private final Type type;
    private final String data;
    private Inventory inventory;

    private PvpHolder(Type type, String data) {
        this.type = type;
        this.data = data;
    }

    public static Inventory create(Type type, String data, int size, Component title) {
        PvpHolder holder = new PvpHolder(type, data);
        Inventory inv = Bukkit.createInventory(holder, size, title);
        holder.inventory = inv;
        return inv;
    }

    public Type type() { return type; }
    public String data() { return data; }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
