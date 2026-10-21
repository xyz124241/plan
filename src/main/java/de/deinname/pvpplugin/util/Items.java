package de.deinname.pvpplugin.util;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public final class Items {

    /** 36 Inventar-Slots + 4 Rüstung + 1 Offhand. */
    public static final int INV_SIZE = 41;

    private Items() {}

    public static String encode(ItemStack item) {
        if (item == null || item.getType().isAir()) return "";
        return Base64.getEncoder().encodeToString(item.serializeAsBytes());
    }

    public static ItemStack decode(String s) {
        if (s == null || s.isEmpty()) return null;
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(s));
        } catch (Exception e) {
            return null;
        }
    }

    public static List<String> encodeAll(ItemStack[] items) {
        List<String> out = new ArrayList<>(items.length);
        for (ItemStack item : items) out.add(encode(item));
        return out;
    }

    public static ItemStack[] decodeAll(List<String> list, int size) {
        ItemStack[] out = new ItemStack[size];
        for (int i = 0; i < size && i < list.size(); i++) out[i] = decode(list.get(i));
        return out;
    }

    public static ItemStack cloneOrNull(ItemStack item) {
        return (item == null || item.getType().isAir()) ? null : item.clone();
    }

    /** Kopie des kompletten Spieler-Inventars (inkl. Rüstung und Offhand). */
    public static ItemStack[] snapshot(Player p) {
        ItemStack[] src = p.getInventory().getContents();
        ItemStack[] out = new ItemStack[INV_SIZE];
        for (int i = 0; i < INV_SIZE && i < src.length; i++) out[i] = cloneOrNull(src[i]);
        return out;
    }

    /** Ersetzt das komplette Spieler-Inventar durch die übergebenen Items. */
    public static void apply(Player p, ItemStack[] items) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        for (int i = 0; i < INV_SIZE; i++) {
            ItemStack item = (items != null && i < items.length) ? cloneOrNull(items[i]) : null;
            if (item != null) inv.setItem(i, item);
        }
    }

    public static ItemStack button(Material mat, String name, String... lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(Text.item(name));
        if (lore.length > 0) m.lore(Arrays.stream(lore).map(Text::item).toList());
        m.addItemFlags(ItemFlag.values());
        it.setItemMeta(m);
        return it;
    }

    public static ItemStack tag(ItemStack item, NamespacedKey key, String value) {
        ItemMeta m = item.getItemMeta();
        m.getPersistentDataContainer().set(key, PersistentDataType.STRING, value);
        item.setItemMeta(m);
        return item;
    }

    public static String tagOf(ItemStack item, NamespacedKey key) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }
}
