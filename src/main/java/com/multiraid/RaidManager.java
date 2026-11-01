package com.multiraid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Verwaltet beliebig viele gleichzeitige Raids.
 *
 * Funktionsweise: Sobald ein Spieler den Effekt "Bad Omen" besitzt (z. B. durch eine Ominous Bottle)
 * und sich in der Naehe eines Dorfes (Dorfbewohner) befindet, wird der Effekt entfernt - damit das
 * Vanilla-Raid-System gar nicht erst eingreift - und stattdessen ein eigener {@link ActiveRaid}
 * gestartet. Jeder ActiveRaid hat seine eigene Bossbar, Wellen und Gegner.
 */
public final class RaidManager {
    /** Sicherheitsgrenze, damit ein Server nicht durch Flaschen-Spam ueberlastet wird. */
    public static final int MAX_RAIDS = 32;
    /** Mindestabstand zwischen zwei Raid-Starts desselben Spielers (Ticks). */
    private static final int PLAYER_COOLDOWN_TICKS = 40;
    /** Suchradius fuer Dorfbewohner beim Start. */
    private static final double VILLAGE_RADIUS = 64.0;
    private static final double VILLAGE_HEIGHT = 48.0;

    private static final List<ActiveRaid> RAIDS = new ArrayList<>();
    private static final Map<UUID, Long> COOLDOWNS = new HashMap<>();
    private static int nextId = 1;
    private static long ticks = 0L;

    private RaidManager() {
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        scanForOmens(server);

        Iterator<ActiveRaid> iterator = RAIDS.iterator();
        while (iterator.hasNext()) {
            ActiveRaid raid = iterator.next();
            boolean finished;
            try {
                finished = raid.tick(server);
            } catch (RuntimeException exception) {
                MultiRaidMod.LOGGER.error("Raid {} ist abgestuerzt und wird beendet.", raid.getId(), exception);
                raid.forceClose(server);
                finished = true;
            }
            if (finished) {
                iterator.remove();
            }
        }

        if (ticks % 1200L == 0L) {
            COOLDOWNS.values().removeIf(last -> ticks - last > PLAYER_COOLDOWN_TICKS);
        }
    }

    private static void scanForOmens(MinecraftServer server) {
        for (ServerPlayer player : new ArrayList<>(server.getPlayerList().getPlayers())) {
            if (!player.isAlive()) {
                continue;
            }
            MobEffectInstance omen = player.getEffect(MobEffects.BAD_OMEN);
            if (omen == null) {
                continue;
            }
            if (!(player.level() instanceof ServerLevel level)) {
                continue;
            }
            BlockPos position = player.blockPosition();
            if (!isNearVillage(level, position)) {
                // Wie in Vanilla: Der Effekt bleibt bestehen, bis der Spieler ein Dorf erreicht.
                continue;
            }
            Long last = COOLDOWNS.get(player.getUUID());
            if (last != null && ticks - last < PLAYER_COOLDOWN_TICKS) {
                continue;
            }
            if (RAIDS.size() >= MAX_RAIDS) {
                COOLDOWNS.put(player.getUUID(), ticks);
                player.sendSystemMessage(Component.literal(
                        "Es sind bereits " + MAX_RAIDS + " Raids aktiv. Der Bad-Omen-Effekt bleibt erhalten."));
                continue;
            }

            int omenLevel = Math.min(5, omen.getAmplifier() + 1);
            player.removeEffect(MobEffects.BAD_OMEN);
            COOLDOWNS.put(player.getUUID(), ticks);

            ActiveRaid raid = new ActiveRaid(nextId++, level.dimension(), position, omenLevel);
            RAIDS.add(raid);
            raid.announceStart(server, level, player);
            MultiRaidMod.LOGGER.info("Raid {} gestartet von {} bei {} (Omen-Stufe {}). Aktive Raids: {}",
                    raid.getId(), player.getName().getString(), position.toShortString(), omenLevel, RAIDS.size());
        }
    }

    /** Ein Dorf gilt als gueltig, wenn mindestens ein Dorfbewohner in der Naehe lebt. */
    static boolean isNearVillage(ServerLevel level, BlockPos center) {
        return VillageCheck.countVillagers(level, center, VILLAGE_RADIUS, VILLAGE_HEIGHT) > 0;
    }

    public static int getActiveRaidCount() {
        return RAIDS.size();
    }

    /** Wird beim Beenden des Servers bzw. beim Verlassen der Einzelspieler-Welt aufgerufen. */
    public static void shutdown(MinecraftServer server) {
        for (ActiveRaid raid : RAIDS) {
            raid.forceClose(server);
        }
        RAIDS.clear();
        COOLDOWNS.clear();
    }

    public static void reset() {
        RAIDS.clear();
        COOLDOWNS.clear();
        nextId = 1;
        ticks = 0L;
    }
}
