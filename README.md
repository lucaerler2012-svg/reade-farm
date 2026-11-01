# MultiRaid (Fabric, Minecraft Java 26.3)

Erlaubt **mehrere gleichzeitige Raids**, jeder mit **eigener Bossbar**.

> **Status:** Der Quellcode wurde noch **nicht gebaut oder im Spiel getestet**. Der erste Build
> läuft über GitHub Actions. Falls der Build Fehler meldet (die API von 26.3 konnte nicht gegen
> die Quellen geprüft werden), das Log aus dem Actions-Tab weitergeben und korrigieren lassen.

## Voraussetzungen

| Komponente | Version |
| --- | --- |
| Minecraft Java | 26.3 |
| Java | 25 |
| Fabric Loader | 0.19.5 oder neuer |
| Fabric API | 0.165.0+26.3 (oder neuer für 26.3) |

## Funktion

- Ein Spieler trinkt eine Ominous Bottle und steht in der Nähe eines Dorfes (mindestens ein
  Dorfbewohner im Umkreis von 64 Blöcken): Es startet ein Raid.
- Trinkt er (oder ein anderer Spieler) weitere Flaschen, starten weitere Raids. Laufende Raids
  werden nie abgebrochen oder zurückgesetzt. Maximal 32 gleichzeitige Raids (`RaidManager.MAX_RAIDS`).
- Jeder Raid hat 5 Wellen, eigene Gegner, eigenen Fortschritt und eigenen Abschlussstatus.
- Jeder Raid hat seine eigene Bossbar, z. B. `Raid 2 – Welle 1/5`. Alle sind gleichzeitig sichtbar.
  Endet ein Raid, verschwindet nur seine Bossbar.
- Sieg: alle 5 Wellen besiegt (Held des Dorfes für Spieler in der Nähe).
  Niederlage: keine Dorfbewohner mehr im Umkreis. Abbruch: 60 Sekunden lang kein Spieler in der Nähe.

## Technik / Einschränkungen

Das Vanilla-Raid-System erlaubt pro Dorf nur einen Raid. Statt es per Mixin umzubauen, ersetzt die Mod
es durch ein eigenes System:

1. Pro Tick wird geprüft, ob ein Spieler den Effekt *Bad Omen* besitzt und ein Dorf in der Nähe ist.
2. Dann wird der Effekt entfernt (Vanilla startet also keinen eigenen Raid) und ein `ActiveRaid`
   in der Liste des `RaidManager` angelegt. Pro Spieler gilt ein Cooldown von 2 Sekunden gegen Doppelstarts.
3. Jeder `ActiveRaid` besitzt eine eigene `ServerBossEvent`. Das Spiel überträgt diese Bossbars
   selbst an die Clients. Auf Clients ist keine Mod nötig.
4. Beim Stoppen des Servers oder Verlassen der Einzelspielerwelt werden alle Raids beendet, Bossbars
   entfernt und Raid-Gegner gelöscht.

Einschränkungen:

- Es wird nicht zwischen Ominous Bottle und anderen Quellen von Bad Omen unterschieden (z. B. Kapitän
  erschlagen). Beides startet einen Raid.
- Raids sind **nicht speichernd**: Nach einem Neustart sind laufende Raids weg.
- Es sind eigene Raids (Wellenzusammensetzung eigener Code), nicht die Vanilla-Raid-Objekte.
  Vanilla-Raid-Mechaniken wie Banner-Kapitäne gibt es nicht.

## Bauen mit GitHub Actions

1. Neues Repository auf GitHub anlegen (leer, ohne README).
2. Den **Inhalt** dieses Ordners hochladen (Add file → Upload files; wichtig: auch den
   versteckten Ordner `.github`).
3. Tab **Actions** öffnen, Workflow **Build** läuft automatisch (oder „Run workflow“).
4. Nach dem Lauf im Lauf-Eintrag unter **Artifacts** `multiraid-jar` herunterladen und entpacken.

## Installation und Test

1. Fabric Loader für 26.3 installieren (fabricmc.net) und Fabric API in den `mods`-Ordner legen.
2. `multiraid-1.0.0.jar` in den `mods`-Ordner legen (Einzelspieler: Client; Server: Server-Ordner).
3. Welt mit Dorf betreten, Creative/OP:
   - `/give @s minecraft:ominous_bottle` und trinken, oder zum Testen `/effect give @s minecraft:bad_omen 1000 0`.
   - Erneut ausführen: Es erscheint eine zweite Bossbar. Die erste läuft unverändert weiter.
4. In `logs/latest.log` stehen Zeilen `Raid N gestartet ...`.
