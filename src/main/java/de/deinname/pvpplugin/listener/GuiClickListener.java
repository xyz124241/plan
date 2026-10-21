package de.deinname.pvpplugin.listener;

import de.deinname.pvpplugin.PvpPlugin;
import de.deinname.pvpplugin.gui.DuelGui;
import de.deinname.pvpplugin.gui.KitroomGui;
import de.deinname.pvpplugin.gui.PartyGui;
import de.deinname.pvpplugin.gui.PvpHolder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class GuiClickListener implements Listener {

    private final PvpPlugin plugin;

    public GuiClickListener(PvpPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof PvpHolder holder)) return;
        switch (holder.type()) {
            case KITROOM -> KitroomGui.onViewClick(plugin, e);
            case KITROOM_ADMIN -> { /* Admin darf frei verschieben */ }
            case DUEL_SELECT -> DuelGui.onSelectClick(plugin, e);
            case DUEL_ADMIN_SELECT -> DuelGui.onAdminSelectClick(plugin, e);
            case DUEL_EDIT -> DuelGui.onEditClick(plugin, e);
            case PARTY -> PartyGui.onClick(plugin, e);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof PvpHolder holder)) return;
        int size = top.getSize();
        boolean touchesTop = false;
        for (int s : e.getRawSlots()) {
            if (s < size) {
                touchesTop = true;
                break;
            }
        }
        if (!touchesTop) return;
        switch (holder.type()) {
            case KITROOM_ADMIN -> { /* frei */ }
            case DUEL_EDIT -> {
                if (DuelGui.dragTouchesControls(e.getRawSlots(), size)) e.setCancelled(true);
            }
            default -> e.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof PvpHolder holder)) return;
        switch (holder.type()) {
            case KITROOM_ADMIN -> KitroomGui.onAdminClose(plugin, e);
            case DUEL_EDIT -> DuelGui.onEditClose(plugin, e);
            default -> { }
        }
    }
}
