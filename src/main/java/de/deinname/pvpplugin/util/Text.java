package de.deinname.pvpplugin.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public final class Text {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private Text() {}

    /** Wandelt Text mit &-Farbcodes in eine Component um. */
    public static Component c(String legacy) {
        return LEGACY.deserialize(legacy);
    }

    /** Wie {@link #c(String)}, aber ohne Kursiv (für Item-Namen und Lore). */
    public static Component item(String legacy) {
        return LEGACY.deserialize(legacy).decoration(TextDecoration.ITALIC, false);
    }
}
