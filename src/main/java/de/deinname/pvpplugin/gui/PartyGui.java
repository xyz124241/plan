package de.deinname.pvpplugin.gui;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.duel.DuelMode;
import de.deinname.pvpplugin.party.Party;
import de.deinname.pvpplugin.util.Items;
import de.deinname.pvpplugin.util.Text;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

/**
 * Party-Menü (27 Slots): oben die Mitglieder-Köpfe,
 * unten rechts das Schwert (Modus), links daneben Start, links davon der FFA-Schalter.
 */
public final class PartyGui {

    private static final int MAX_HEADS = 18;
    private static final int SLOT_FFA = 24;
    private static final int SLOT_START = 25;
    private static final int SLOT_MODE = 26;

    private PartyGui() {}

    public static void open(PvpPlugin plugin, Player p, Party party) {
        Inventory inv = PvpHolder.create(PvpHolder.Type.PARTY, null, 27, plugin.raw("gui.party"));
        fill(plugin, inv, party);
        p.openInventory(inv);
    }

    private static void fill(PvpPlugin plugin, Inventory inv, Party party) {
        inv.clear();
        int slot = 0;
        for (UUID id : party.getMembers()) {
            if (slot >= MAX_HEADS) break;
            inv.setItem(slot++, head(id, party.isLeader(id)));
        }
        ItemStack filler = Items.button(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int s = 18; s < 27; s++) inv.setItem(s, filler);

        DuelMode mode = plugin.parties().resolveMode(party);
        String modeName = mode == null ? "&cKein Modus" : mode.getName();
        inv.setItem(SLOT_MODE, Items.button(Material.DIAMOND_SWORD, "&6Modus: &f" + modeName,
                "&7Klicke, um den Modus zu wechseln."));
        inv.setItem(SLOT_START, Items.button(Material.EMERALD_BLOCK, "&a&lStart",
                "&7Startet das Match."));
        inv.setItem(SLOT_FFA, Items.button(party.isFfa() ? Material.LIME_DYE : Material.GRAY_DYE,
                "&eFFA: " + (party.isFfa() ? "&aAN" : "&cAUS"),
                "&7An: alle gegen alle in der Mitte.", "&7Aus: Teams (z. B. 2 gegen 1)."));
    }

    private static ItemStack head(UUID id, boolean leader) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        meta.setOwningPlayer(op);
        String name = op.getName() == null ? "?" : op.getName();
        meta.displayName(Text.item((leader ? "&6★ " : "&f") + name));
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    public static void onClick(PvpPlugin plugin, InventoryClickEvent e) {
        e.setCancelled(true);
        Inventory top = e.getView().getTopInventory();
        if (e.getClickedInventory() != top) return;
        Player p = (Player) e.getWhoClicked();
        Party party = plugin.parties().get(p.getUniqueId());
        if (party == null) {
            p.closeInventory();
            return;
        }
        int slot = e.getRawSlot();
        if (slot != SLOT_FFA && slot != SLOT_START && slot != SLOT_MODE) return;
        if (!party.isLeader(p.getUniqueId())) {
            plugin.send(p, "party.leader-only");
            return;
        }
        if (slot == SLOT_MODE) {
            plugin.parties().cycleMode(party);
            fill(plugin, top, party);
        } else if (slot == SLOT_FFA) {
            party.setFfa(!party.isFfa());
            fill(plugin, top, party);
        } else {
            plugin.parties().start(p, party);
        }
    }
}
