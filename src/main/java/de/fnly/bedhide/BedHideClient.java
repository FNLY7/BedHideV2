package de.fnly.bedhide;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;

public class BedHideClient implements ClientModInitializer {
    public static boolean RENDERING_FAKE = false;

    @Override
    public void onInitializeClient() {
        WorldRenderEvents.AFTER_ENTITIES.register(this::renderHiddenPlayers);
    }

    private void renderHiddenPlayers(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        MatrixStack matrices = context.matrixStack();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null) return;

        Vec3d camera = context.camera().getPos();
        EntityRenderDispatcher dispatcher = client.getEntityRenderDispatcher();

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (!player.getScoreboardTags().contains(BedHideMod.TAG)) continue;

            BlockPos bed = findNearestBedClient(player.getBlockPos(), 4);
            if (bed == null) continue;

            BlockState state = client.world.getBlockState(bed);
            Direction facing = Direction.NORTH;
            if (state.contains(Properties.HORIZONTAL_FACING)) {
                facing = state.get(Properties.HORIZONTAL_FACING);
            }

            double x = bed.getX() + 0.5 - camera.x;
            double y = bed.getY() - 0.58 - camera.y;
            double z = bed.getZ() + 0.5 - camera.z;

            matrices.push();
            matrices.translate(x, y, z);

            // Unter das Bett legen: Richtung Bett-Facing, dann um 90 Grad flachlegen.
            float yaw = facing.asRotation();
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f - yaw));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0f));

            // Etwas kleiner als normal, damit er wirklich unter dem Bett liegt.
            matrices.scale(0.62f, 0.62f, 0.62f);
            matrices.translate(0.0, -0.65, 0.0);

            try {
                RENDERING_FAKE = true;
                dispatcher.render(player, 0, 0, 0, 0, context.tickCounter().getTickDelta(false), matrices, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE);
            } finally {
                RENDERING_FAKE = false;
            }

            matrices.pop();
        }
    }

    private BlockPos findNearestBedClient(BlockPos origin, int radius) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return null;
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterate(origin.add(-radius, -2, -radius), origin.add(radius, 2, radius))) {
            BlockState state = client.world.getBlockState(pos);
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
