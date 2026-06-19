package de.fnly.bedhide.mixin;

import de.fnly.bedhide.BedHideClient;
import de.fnly.bedhide.BedHideMod;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public class PlayerRendererMixin {
    // Hinweis: Je nach Yarn-Version kann diese Mixin-Signatur angepasst werden müssen.
    // Ziel: normales Rendern verstecken, aber unser Fake-Render unter dem Bett erlauben.
    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("HEAD"), cancellable = true)
    private void bedhide$cancelNormal(PlayerEntityRenderState state, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (BedHideClient.RENDERING_FAKE) return;
        // In neueren Minecraft-Versionen ist der Player im RenderState nicht direkt public verfügbar.
        // Falls dein Mapping hier meckert, entferne diese Mixin-Datei und nutze die Spectator-Unsichtbarkeit.
    }
}
