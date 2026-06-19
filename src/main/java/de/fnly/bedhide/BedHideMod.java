package de.fnly.bedhide;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BedHideMod implements ModInitializer {
    public static final String TAG = "bedhide_hidden";
    public static final String ORIGINAL_X = "bedhide_original_x";
    public static final String ORIGINAL_Y = "bedhide_original_y";
    public static final String ORIGINAL_Z = "bedhide_original_z";

    private final Map<UUID, Vec3d> oldPositions = new HashMap<>();
    private final Map<UUID, GameMode> oldModes = new HashMap<>();

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("bedhide")
            .then(CommandManager.literal("hide").executes(ctx -> hide(ctx.getSource().getPlayerOrThrow())))
            .then(CommandManager.literal("show").executes(ctx -> show(ctx.getSource().getPlayerOrThrow())))
            .then(CommandManager.literal("view").executes(ctx -> view(ctx.getSource().getPlayerOrThrow())))
            .executes(ctx -> hide(ctx.getSource().getPlayerOrThrow()))
        );
    }

    private int hide(ServerPlayerEntity player) {
        if (player.getScoreboardTags().contains(TAG)) {
            player.sendMessage(Text.literal("§eDu bist schon unter einem Bett versteckt. /bedhide show"), false);
            return 1;
        }

        BlockPos bed = findNearestBed(player, 6);
        if (bed == null) {
            player.sendMessage(Text.literal("§cKein Bett in der Nähe gefunden. Stell dich direkt neben ein Bett."), false);
            return 0;
        }

        oldPositions.put(player.getUuid(), player.getPos());
        oldModes.put(player.getUuid(), player.interactionManager.getGameMode());
        player.addScoreboardTag(TAG);

        // Echtes Spieler-Entity wird sicher aus dem Weg genommen.
        // Die Clients mit Mod rendern stattdessen ein liegendes Modell unter dem Bett.
        player.changeGameMode(GameMode.SPECTATOR);
        player.teleport((ServerWorld) player.getWorld(), bed.getX() + 0.5, bed.getY() + 0.1, bed.getZ() + 0.5, player.getYaw(), player.getPitch());
        player.sendMessage(Text.literal("§aBedHide aktiv. Du liegst jetzt als 3D-Modell unter dem Bett. /bedhide show"), false);
        return 1;
    }

    private int show(ServerPlayerEntity player) {
        player.removeScoreboardTag(TAG);
        Vec3d old = oldPositions.remove(player.getUuid());
        GameMode gm = oldModes.remove(player.getUuid());
        if (gm != null) player.changeGameMode(gm);
        if (old != null) {
            player.teleport((ServerWorld) player.getWorld(), old.x, old.y, old.z, player.getYaw(), player.getPitch());
        }
        player.sendMessage(Text.literal("§aBedHide beendet."), false);
        return 1;
    }

    private int view(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("§eView-Tipp: Du bist im Spectator-Modus. Flieg einfach frei herum, während dein Modell unter dem Bett angezeigt wird."), false);
        return 1;
    }

    private BlockPos findNearestBed(ServerPlayerEntity player, int radius) {
        BlockPos origin = player.getBlockPos();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterate(origin.add(-radius, -2, -radius), origin.add(radius, 2, radius))) {
            BlockState state = player.getWorld().getBlockState(pos);
            if (state.getBlock() instanceof BedBlock || state.isIn(BlockTags.BEDS)) {
                double d = pos.getSquaredDistance(origin);
                if (d < bestDist) {
                    bestDist = d;
                    best = pos.toImmutable();
                }
            }
        }
        return best;
    }
}
