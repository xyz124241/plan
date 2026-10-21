package de.deinname.pvpplugin.gui;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.duel.DuelMode;
import de.deinname.pvpplugin.util.Items;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class DuelGui {

    /** Slots 0-40 im Edit-Menü sind Kit-Items, 41-53 sind Bedienelemente. */
    private static final int ITEM_SLOTS = Items.INV_SIZE;
    private static final int SLOT_PEARL_1 = 48;
    private static final int SLOT_INFO = 49;
    private static final int SLOT_PEARL_2 = 50;

    private DuelGui() {}

    /**
     * Slot im Edit-Menü -> Slot im Spieler-Inventar.
     * 0-26 = Hauptinventar (Spieler-Slots 9-35), 27-35 = Hotbar (0-8),
     * 36-39 = Schuhe, Hose, Brustpanzer, Helm, 40 = Offhand.
     */
    public static int toPlayerSlot(int guiSlot) {
        if (guiSlot < 27) return guiSlot + 9;
        if (guiSlot < 36) return guiSlot - 27;
        return guiSlot;
    }

    private static ItemStack modeButton(PvpPlugin plugin, DuelMode m, boolean admin) {
        String s1 = m.getSpawn1() != null ? "&agesetzt" : "&cfehlt";
        String s2 = m.getSpawn2() != null ? "&agesetzt" : "&cfehlt";
        ItemStack item;
        if (admin) {
            item = Items.button(m.getIcon(), "&b&l" + m.getName(),
                    "&7Spawn 1: " + s1, "&7Spawn 2: " + s2, "", "&eKlicke zum Bearbeiten");
        } else {
            item = Items.button(m.getIcon(), "&b&l" + m.getName(),
                    m.isReady() ? "&aKlicke, um herauszufordern" : "&cNicht bereit (Spawns fehlen)");
        }
        return Items.tag(item, plugin.modeKey(), m.key());
    }

    // ------------------------------------------------- Modus-Auswahl (27 Slots)

    public static void openSelect(PvpPlugin plugin, Player p, Player target) {
        Inventory inv = PvpHolder.create(PvpHolder.Type.DUEL_SELECT, target.getUniqueId().toString(), 27,
                plugin.raw("gui.duel-select", "%player%", target.getName()));
        int slot = 0;
        for (DuelMode m : plugin.duels().all()) {
            if (slot >= 27) break;
            inv.setItem(slot++, modeButton(plugin, m, false));
        }
        p.openInventory(inv);
    }

    public static void onSelectClick(PvpPlugin plugin, InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        String key = Items.tagOf(e.getCurrentItem(), plugin.modeKey());
        if (key == null) return;
        Player p = (Player) e.getWhoClicked();
        PvpHolder holder = (PvpHolder) e.getView().getTopInventory().getHolder();
        Bukkit.getScheduler().runTask(plugin, () -> {
            p.closeInventory();
            Player target = Bukkit.getPlayer(UUID.fromString(holder.data()));
            DuelMode mode = plugin.duels().get(key);
            if (target == null) {
                plugin.send(p, "duel.player-offline");
            } else if (mode == null) {
                plugin.send(p, "duel.mode-unavailable");
            } else {
                plugin.duels().sendChallenge(p, target, mode);
            }
        });
    }

    // ---------------------------------------------- Admin: Modus auswählen

    public static void openAdminSelect(PvpPlugin plugin, Player p) {
        Inventory inv = PvpHolder.create(PvpHolder.Type.DUEL_ADMIN_SELECT, null, 27, plugin.raw("gui.duel-admin"));
        int slot = 0;
        for (DuelMode m : plugin.duels().all()) {
            if (slot >= 27) break;
            inv.setItem(slot++, modeButton(plugin, m, true));
        }
        p.openInventory(inv);
    }

    public static void onAdminSelectClick(PvpPlugin plugin, InventoryClickEvent e) {
        e.setCancelled(true);
        if (e.getClickedInventory() != e.getView().getTopInventory()) return;
        String key = Items.tagOf(e.getCurrentItem(), plugin.modeKey());
        if (key == null) return;
        Player p = (Player) e.getWhoClicked();
        Bukkit.getScheduler().runTask(plugin, () -> {
            DuelMode mode = plugin.duels().get(key);
            if (mode != null) openEdit(plugin, p, mode);
        });
    }

    // ----------------------------------------- Admin: Items + Spawn-Perlen

    public static void openEdit(PvpPlugin plugin, Player p, DuelMode mode) {
        Inventory inv = PvpHolder.create(PvpHolder.Type.DUEL_EDIT, mode.key(), 54,
                plugin.raw("gui.duel-edit", "%name%", mode.getName()));
        for (int g = 0; g < ITEM_SLOTS; g++) {
            ItemStack item = mode.getKit()[toPlayerSlot(g)];
            if (item != null) inv.setItem(g, item.clone());
        }
        ItemStack filler = Items.button(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int g = ITEM_SLOTS; g < 54; g++) inv.setItem(g, filler);
        inv.setItem(SLOT_PEARL_1, Items.button(Material.ENDER_PEARL, "&dSpawn 1 setzen",
                "&7Klicke, um die Perle zu erhalten.", "&7Stell dich an die Position und", "&7links- oder rechtsklicke damit."));
        inv.setItem(SLOT_PEARL_2, Items.button(Material.ENDER_PEARL, "&dSpawn 2 setzen",
                "&7Klicke, um die Perle zu erhalten.", "&7Stell dich an die Position und", "&7links- oder rechtsklicke damit."));
        inv.setItem(SLOT_INFO, Items.button(Material.BOOK, "&eLayout",
                "&7Oben: Hauptinventar, dann Hotbar.",
                "&7Reihe 5: Schuhe, Hose, Brustpanzer,", "&7Helm, Offhand.",
                "&7Beim Schließen wird gespeichert."));
        p.openInventory(inv);
    }

    private static void givePearl(PvpPlugin plugin, Player p, DuelMode mode, int n) {
        ItemStack pearl = Items.button(Material.ENDER_PEARL, "&dSpawn " + n + " &7(" + mode.getName() + ")",
                "&7Links- oder Rechtsklick setzt", "&7den Spawn an deiner Position.");
        Items.tag(pearl, plugin.spawnKey(), mode.key() + ";" + n);
        p.getInventory().addItem(pearl);
        plugin.send(p, "duel.pearl-given", "%n%", String.valueOf(n));
    }

    public static void onEditClick(PvpPlugin plugin, InventoryClickEvent e) {
        if (e.getClick() == ClickType.DOUBLE_CLICK) {
            e.setCancelled(true);
            return;
        }
        Inventory top = e.getView().getTopInventory();
        int raw = e.getRawSlot();
        if (raw < ITEM_SLOTS || raw >= top.getSize()) return; // freie Item-Slots und eigenes Inventar
        e.setCancelled(true);
        PvpHolder holder = (PvpHolder) top.getHolder();
        DuelMode mode = plugin.duels().get(holder.data());
        if (mode == null) return;
        Player p = (Player) e.getWhoClicked();
        if (raw == SLOT_PEARL_1) givePearl(plugin, p, mode, 1);
        else if (raw == SLOT_PEARL_2) givePearl(plugin, p, mode, 2);
    }

    public static void onEditClose(PvpPlugin plugin, InventoryCloseEvent e) {
        PvpHolder holder = (PvpHolder) e.getInventory().getHolder();
        DuelMode mode = plugin.duels().get(holder.data());
        if (mode == null) return;
        Inventory inv = e.getInventory();
        for (int g = 0; g < ITEM_SLOTS; g++) {
            mode.getKit()[toPlayerSlot(g)] = Items.cloneOrNull(inv.getItem(g));
        }
        plugin.duels().save();
        plugin.send(e.getPlayer(), "duel.kit-saved", "%name%", mode.getName());
    }

    /** true, wenn der Drag ein geschütztes Feld (Bedienelemente) berührt. */
    public static boolean dragTouchesControls(java.util.Set<Integer> rawSlots, int topSize) {
        for (int s : rawSlots) {
            if (s >= ITEM_SLOTS && s < topSize) return true;
        }
        return false;
    }
}
