# PvP-Plugin: Projektplan

Ein Minecraft-PvP-Plugin mit Kitroom, Duellen, Party-System und Kits.

## 1. Tech-Stack

| Punkt | Wahl |
|---|---|
| Server-Software | Paper (kompatibel mit Spigot) |
| Minecraft-Version | 1.21.x |
| Sprache | Java 21 |
| Build-Tool | Gradle oder Maven (Empfehlung: Maven, einfacher für Einsteiger) |
| Speicherung | YAML-Dateien im Plugin-Ordner (später optional SQLite) |
| Paketname | `de.deinname.pvpplugin` |

---

## 2. Projektstruktur

```
pvp-plugin/
├── pom.xml
├── README.md
├── PLAN.md
├── .gitignore
├── .github/workflows/build.yml      (automatischer Build)
└── src/main/
    ├── java/de/deinname/pvpplugin/
    │   ├── PvpPlugin.java           (Main-Klasse)
    │   ├── commands/
    │   │   ├── KitroomCommand.java  (/kitroom und /admin-kitman-kitroom)
    │   │   ├── DuelCommand.java
    │   │   ├── AdminCommand.java    (/admin duel)
    │   │   ├── PartyCommand.java
    │   │   └── KitsCommand.java
    │   ├── gui/
    │   │   ├── PvpHolder.java
    │   │   ├── KitroomGui.java
    │   │   ├── DuelGui.java         (Auswahl, Admin-Liste, Editor)
    │   │   └── PartyGui.java
    │   ├── duel/
    │   │   ├── DuelMode.java        (Name, Icon, Items, Spawns)
    │   │   ├── DuelManager.java
    │   │   ├── Match.java           (laufender Kampf)
    │   │   └── MatchManager.java
    │   ├── party/
    │   │   ├── Party.java
    │   │   └── PartyManager.java
    │   ├── kit/
    │   │   └── KitManager.java
    │   ├── kitroom/
    │   │   └── KitroomManager.java
    │   ├── listener/
    │   │   ├── GuiClickListener.java
    │   │   ├── DeathRespawnListener.java
    │   │   ├── PearlSpawnListener.java
    │   │   └── MatchListener.java
    │   └── util/
    │       ├── Items.java
    │       └── Text.java
    └── resources/
        ├── plugin.yml
        ├── config.yml
        └── messages.yml
```

---

## 3. Befehle und Rechte

| Befehl | Beschreibung | Recht |
|---|---|---|
| `/kitroom` | Öffnet den Kitroom (Items zum Mitnehmen) | alle |
| `/admin-kitman-kitroom` | Kitroom bearbeiten (Items hinzufügen/entfernen) | nur OP |
| `/duel <spieler>` | Öffnet das Menü mit allen Spielmodi (27 Slots), Klick auf einen Modus sendet die Herausforderung | alle |
| `/duel accept <spieler>` | Herausforderung von diesem Spieler annehmen | alle |
| `/duel create <name> <item>` | Neuen Duell-Modus erstellen | nur OP |
| `/duel delete <name>` | Duell-Modus löschen | nur OP |
| `/duel edit <name>` | Duell-Modus bearbeiten | nur OP |
| `/duel admin` bzw. `/admin duel` | Admin-GUI zum Einstellen von Items und Spawns | nur OP |
| `/party` | Öffnet die Party-GUI | alle |
| `/party create` | Party erstellen | alle |
| `/party delete` | Party auflösen (nur Leader) | alle |
| `/party invite <spieler>` | Spieler einladen | Leader |
| `/party accept` | Einladung annehmen | alle |
| `/party kick <spieler>` | Spieler rauswerfen | Leader |
| `/party leave` | Party verlassen | alle |
| `/kits save <name>` | Aktuelles Inventar als Kit speichern | alle |
| `/kits delete <name>` | Kit löschen | alle |
| `/kits select <name>` | Kit auswählen (Respawn-Kit) | alle |

Permissions in `plugin.yml`: `pvp.admin` (Standard: op) für alle Admin-Befehle.

---

## 4. Features im Detail

### 4.1 Kitroom

- `/kitroom`: GUI (54 Slots, bei Bedarf mehrere Seiten). Spieler nehmen sich **Kopien** der Items heraus, die Items im Kitroom bleiben erhalten.
- `/admin-kitman-kitroom`: Admin-Modus (nur OP). Dort kann man:
  - Items aus dem eigenen Inventar per Drag and Drop in den Kitroom ziehen (hinzufügen)
  - Items wieder aus dem Kitroom herausnehmen (entfernen)
- Beim Schließen der Admin-GUI wird der Inhalt in `kitroom.yml` gespeichert.
- Items werden mit vollständigen NBT-Daten gespeichert (Verzauberungen, Namen, Lore).

### 4.2 Duelle

**Herausfordern (`/duel <spieler>`)**
- Der Befehl öffnet ein Menü mit **27 Slots**, in dem alle Duell-Modi angezeigt werden.
- Jeder erstellte Modus erscheint als Icon (das beim Erstellen angegebene Item) mit Namen.
- Klick auf ein Icon: Die Herausforderung (Modus + Absender) wird an den Zielspieler geschickt.
- Der Zielspieler bekommt eine Nachricht (klickbar) und nimmt mit `/duel accept <spieler>` an.
- Die Anfrage läuft nach 60 Sekunden ab. Man kann sich nicht selbst herausfordern, und niemand im laufenden Match kann herausgefordert werden.

**Verwaltung per Befehl (nur OP)**
- `/duel create <name> <item>`: legt einen Modus mit Namen und Anzeige-Item an (z. B. `/duel create Sword DIAMOND_SWORD`). Danach erscheint er sofort in der GUI.
- `/duel delete <name>`: entfernt den Modus.
- `/duel edit <name>`: ändert Name und Icon (und öffnet bei Bedarf die Admin-GUI).

**Admin-GUI (`/duel admin`)**
- Zuerst Modus auswählen, dann zwei Bereiche:
  1. **Kit-Items**: Hier legt man fest, welche Items die Spieler im Duell bekommen (Items per Drag and Drop hineinlegen, wird gespeichert).
  2. **Spawn-Perlen**: Zwei Enderperlen, **Spawn 1** und **Spawn 2**.
     - Perle 1 nehmen, an die gewünschte Position gehen, Links- oder Rechtsklick: Spawn 1 wird gesetzt.
     - Perle 2 nehmen, zur zweiten Position gehen, Klick: Spawn 2 wird gesetzt.
     - Der Wurf der Perle wird abgebrochen (`PlayerInteractEvent` cancellen), die Position wird aus `player.getLocation()` gespeichert.
- Jeder Modus hat also: Name, Icon, Kit-Items, Spawn 1, Spawn 2.

**Match-Ablauf**
1. Beide Spieler werden zu Spawn 1 bzw. Spawn 2 teleportiert.
2. Inventar wird vorher gesichert und geleert, danach werden die Kit-Items des Modus gegeben.
3. 3-Sekunden-Countdown (Spieler eingefroren).
4. Kampf. Wer stirbt, verliert. Danach Inventar zurück und Teleport zum Lobby-Spawn.

### 4.3 Party

**Befehle:** `create`, `delete`, `invite`, `accept`, `kick`, `leave`.

- Einladungen laufen nach 60 Sekunden ab.
- Verlässt der Leader die Party, wird sie aufgelöst oder der Leader wechselt (in `config.yml` einstellbar).
- Kein Spieler kann in zwei Parties gleichzeitig sein.

**Party-GUI (`/party`)**
- Mitte: Köpfe aller Party-Mitglieder (Spielerköpfe).
- **Unten rechts: Schwert.** Klick wechselt durch die vorhandenen Duell-Modi.
- **Daneben: Start-Button.** Startet das Party-Match.
- **Links davon: FFA-Schalter** (an/aus).

**Match-Modi**
- **FFA aus (Teams):** Spieler werden gleichmäßig aufgeteilt.
  - 2 Spieler: 1 gegen 1
  - 3 Spieler: 2 gegen 1
  - 4 Spieler: 2 gegen 2
  - usw. (Teams zufällig gemischt)
  - Das Team gewinnt, wenn alle Gegner tot sind.
- **FFA an:** Alle spawnen in der Mitte, jeder kämpft gegen jeden. Der letzte Überlebende gewinnt.

### 4.4 Kits

- `/kits save <name>`: speichert Rüstung, Hauptinventar und Offhand in `kits.yml` (pro Spieler per UUID).
- `/kits delete <name>`: löscht das Kit.
- `/kits select <name>`: markiert das Kit als aktiv.
- **Respawn:** Im `PlayerRespawnEvent` bekommt der Spieler automatisch das ausgewählte Kit (mit 1 Tick Verzögerung per Scheduler).
- Optional: `/kits list` zum Anzeigen aller eigenen Kits.

---

## 5. Datenspeicherung

| Datei | Inhalt |
|---|---|
| `kitroom.yml` | Items des Kitroom |
| `duels.yml` | Modi: Name, Icon, Items, Spawn 1, Spawn 2 |
| `kits.yml` | Spieler-Kits und das ausgewählte Kit |
| `config.yml` | Einstellungen (Countdown, Lobby-Spawn, Party-Optionen) |

Items werden als Base64 gespeichert, Locations direkt von Bukkit (Welt, x, y, z, yaw, pitch). Nachrichten stehen im Code.

---

## 6. Meilensteine

1. **Setup:** Projekt, `pom.xml`, `plugin.yml`, Main-Klasse, GitHub-Repo, `.gitignore`
2. **Kits:** `/kits save/delete/select` und Respawn-Listener (einfachster Teil, gut zum Einstieg)
3. **Kitroom:** GUI und Admin-Modus mit Speicherung
4. **Duelle (Verwaltung):** Create/Delete/Edit, 27-Slot-GUI, Admin-GUI mit Spawn-Perlen
5. **Duelle (Kampf):** Herausforderung (`/duel <spieler>`, `/duel accept`), Match-Ablauf, Inventar sichern/zurückgeben
6. **Party:** Befehle, Einladungssystem, Party-GUI
7. **Party-Matches:** Teams und FFA
8. **Feinschliff:** Nachrichten, Rechte, Edge-Cases (Disconnect im Match, Tod, Server-Stop), Tests
9. **Release:** README, GitHub Actions Build, Release-Tag mit `.jar`

---

## 7. Annahmen und offene Fragen

Diese Punkte waren in der Beschreibung nicht ganz eindeutig. Aktuelle Annahme in Klammern:

- Befehl für Admin-Duell-Menü: `/duel admin` (zusätzlich Alias `/admin duel`)
- Wo spawnen alle bei **FFA**? (Mittelpunkt zwischen Spawn 1 und Spawn 2; alternativ eine dritte Perle "Mitte" pro Modus)
- Dürfen normale Spieler im Kitroom Items nur kopieren? (Ja, Items bleiben im Kitroom)
- Wohin kommen Spieler nach einem Match? (Lobby-Spawn aus `config.yml`)
- Gibt es pro Duell-Modus mehrere Arenen, oder genau eine? (genau eine, also ein Spawn-Paar pro Modus)

---

## 8. GitHub einrichten

1. Neues Repo erstellen (z. B. `pvp-plugin`)
2. `README.md` mit Beschreibung, Befehlsliste und Installationsanleitung
3. `.gitignore` für Java/Maven (`target/`, `.idea/`, `*.iml`)
4. `.github/workflows/build.yml`: baut bei jedem Push die `.jar` mit Maven
5. Issues für jeden Meilenstein anlegen und nacheinander abarbeiten
6. Releases mit Tags (`v1.0.0`) und angehängter `.jar`
