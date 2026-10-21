package de.deinname.pvpplugin.gui;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.kitroom.KitroomManager;
import de.deinname.pvpplugin.util.Items;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class KitroomGui {

    private KitroomGui() {}

    public static void open(PvpPlugin plugin, Player p, boolean admin) {
        Inventory inv = PvpHolder.create(
                admin ? PvpHolder.Type.KITROOM_ADMIN : PvpHolder.Type.KITROOM,
                null, KitroomManager.SIZE,
                plugin.raw(admin ? "gui.kitroom-admin" : "gui.kitroom"));
        ItemStack[] items = plugin.kitroom().get();
        for (int i = 0; i < items.length && i < inv.getSize(); i++) {
            if (items[i] != null) inv.setItem(i, items[i].clone());
        }
        p.openInventory(inv);
    }

    /** Normaler Kitroom: Klick auf ein Item gibt dem Spieler eine Kopie, der Kitroom bleibt unverändert. */
    public static void onViewClick(PvpPlugin plugin, InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (e.getClickedInventory() == top) {
            e.setCancelled(true);
            ItemStack item = e.getCurrentItem();
            if (item == null || item.getType().isAir()) return;
            Player p = (Player) e.getWhoClicked();
            if (!p.getInventory().addItem(item.clone()).isEmpty()) {
                plugin.send(p, "kitroom.inventory-full");
            }
        } else if (e.isShiftClick() || e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
        }
    }

    /** Admin-Kitroom: beim Schließen wird der Inhalt gespeichert. */
    public static void onAdminClose(PvpPlugin plugin, InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        ItemStack[] items = new ItemStack[KitroomManager.SIZE];
        for (int i = 0; i < items.length && i < inv.getSize(); i++) {
            items[i] = Items.cloneOrNull(inv.getItem(i));
        }
        plugin.kitroom().set(items);
        plugin.send(e.getPlayer(), "kitroom.saved");
    }
}
