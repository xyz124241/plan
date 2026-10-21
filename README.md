# PvpPlugin

Ein Minecraft-PvP-Plugin für **Paper 1.21.x** (Java 21) mit Kitroom, Duellen, Party-System und persönlichen Kits.

## Features

- **Kitroom:** Ein Menü mit Items, die sich jeder Spieler holen kann. OPs bearbeiten es per Drag and Drop.
- **Duelle:** Spieler fordern sich gegenseitig heraus und wählen einen Modus. Admins legen Modi mit eigenem Kit und Spawnpunkten an.
- **Party:** Party mit Menü. Der Leader wählt Modus, FFA oder Teams und startet das Match.
- **Kits:** Jeder Spieler speichert eigene Kits und spawnt nach dem Tod damit.

## Befehle

| Befehl | Beschreibung | Recht |
|---|---|---|
| `/kitroom` | Kitroom öffnen, Klick auf ein Item gibt dir eine Kopie | alle |
| `/admin-kitman-kitroom` | Kitroom bearbeiten (Items hinein- oder herausziehen, Schließen speichert) | OP |
| `/duel <spieler>` | Menü mit allen Modi öffnen, Klick auf einen Modus sendet die Herausforderung | alle |
| `/duel accept <spieler>` | Herausforderung annehmen | alle |
| `/duel create <name> <item>` | Neuen Modus erstellen, z. B. `/duel create Sword DIAMOND_SWORD` | OP |
| `/duel delete <name>` | Modus löschen | OP |
| `/duel edit <name>` | Modus-Editor öffnen (Kit-Items und Spawn-Perlen) | OP |
| `/duel edit <name> icon <item>` | Anzeige-Item ändern | OP |
| `/duel edit <name> rename <neuer-name>` | Modus umbenennen | OP |
| `/duel admin` oder `/admin duel` | Liste aller Modi zum Bearbeiten | OP |
| `/duel setlobby` | Lobby-Punkt setzen (dorthin geht es nach jedem Match) | OP |
| `/party` | Party-Menü öffnen | alle |
| `/party create` / `delete` / `leave` | Party erstellen, auflösen, verlassen | alle |
| `/party invite <spieler>` / `accept` / `kick <spieler>` | Einladen, annehmen, rauswerfen | Leader bzw. alle |
| `/kits save <name>` | Aktuelles Inventar als Kit speichern | alle |
| `/kits delete <name>` | Kit löschen | alle |
| `/kits select <name>` | Kit auswählen, du spawnst nach dem Tod damit | alle |
| `/kits list` | Eigene Kits anzeigen | alle |

Permission für alle Admin-Befehle: `pvp.admin` (Standard: OP).

## Einen Duell-Modus einrichten

1. `/duel create Sword DIAMOND_SWORD`: Der Modus erscheint sofort im Menü von `/duel <spieler>`.
2. `/duel edit Sword`: Der Editor öffnet sich.
   - **Items:** Items in die Slots legen. Die Anordnung entspricht deinem Inventar: oben Hauptinventar, dann Hotbar, in Reihe 5 Schuhe, Hose, Brustpanzer, Helm und Offhand. Beim Schließen wird gespeichert.
   - **Spawn 1:** Unten auf die erste Perle klicken, an die Position gehen, Links- oder Rechtsklick mit der Perle.
   - **Spawn 2:** Unten auf die zweite Perle klicken, an die zweite Position gehen, Links- oder Rechtsklick.
3. Erst wenn beide Spawns gesetzt sind, kann der Modus gespielt werden.
4. Optional: `/duel setlobby`.

## Match-Regeln

- Countdown (Standard 3 Sekunden), in dem alle eingefroren sind.
- Beim Start wird das Inventar gesichert. Danach gibt es das Kit des Modus.
- Wer tödlichen Schaden nimmt, scheidet aus (ohne Todesbildschirm) und bekommt sofort sein Inventar zurück.
- Am Ende gehen alle zur Lobby (oder an ihren alten Ort) und bekommen Inventar, Leben und Effekte zurück.
- **Party ohne FFA:** Teams werden zufällig gemischt (3 Spieler: 2 gegen 1, 4 Spieler: 2 gegen 2). Team 1 spawnt bei Spawn 1, Team 2 bei Spawn 2.
- **Party mit FFA:** Alle spawnen in der Mitte zwischen Spawn 1 und Spawn 2, jeder gegen jeden.

## Bauen

Voraussetzungen: Java 21 und Maven (und Internet, damit die Paper-API geladen werden kann).

```bash
mvn clean package
```

Die fertige Datei liegt in `target/PvpPlugin-1.0.0.jar`. Sie kommt in den `plugins`-Ordner des Paper-Servers.

Bei jedem Push baut GitHub Actions das Plugin automatisch (`.github/workflows/build.yml`). Die `.jar` findest du im Actions-Tab unter *Artifacts*.

## Dateien des Plugins

| Datei | Inhalt |
|---|---|
| `plugins/PvpPlugin/config.yml` | Countdown, Timeouts, Party- und Kit-Limits |
| `plugins/PvpPlugin/messages.yml` | Alle Nachrichten (Farbcodes mit `&`) |
| `plugins/PvpPlugin/kitroom.yml` | Items des Kitroom |
| `plugins/PvpPlugin/duels.yml` | Modi, Kits, Spawns und Lobby |
| `plugins/PvpPlugin/kits.yml` | Kits der Spieler |

## Bekannte Einschränkungen

- Der Kitroom hat eine Seite mit 54 Slots.
- Bis zu 27 Duell-Modi (so viele passen in das 27-Slot-Menü).
- Das Inventar während eines Matches wird nur im Arbeitsspeicher gesichert. Bei einem Server-Absturz mitten im Match geht es verloren.
- Das Plugin wurde noch nicht auf einem echten Server getestet. Bitte einmal alle Befehle ausprobieren.

## Eigenen Namen einsetzen

Der Paketname ist `de.deinname.pvpplugin`. Wenn du ihn ändern willst, benenne den Ordner `src/main/java/de/deinname` um und passe `groupId` in der `pom.xml`, `main:` in der `plugin.yml` und die `package`-Zeilen an.

## Projektplan

Siehe [PLAN.md](PLAN.md).
