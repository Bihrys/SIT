package bhw.voident.xyz.sit.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import bhw.voident.xyz.sit.Sit;
import bhw.voident.xyz.sit.SitLogic;
import bhw.voident.xyz.sit.SitSeatEntity;

public class SitClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(Sit.SIT_ENTITY_TYPE, EmptyRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world == null) {
                return;
            }

            for (PlayerEntity player : client.world.getPlayers()) {
                SitLogic.updateRiderPose(player);
            }
        });
    }

    private static class EmptyRenderer extends EntityRenderer<SitSeatEntity> {
        protected EmptyRenderer(EntityRendererFactory.Context ctx) {
            super(ctx);
        }

        @Override
        public boolean shouldRender(SitSeatEntity entity, Frustum frustum, double x, double y, double z) {
            return false;
        }

        @Override
        public Identifier getTexture(SitSeatEntity entity) {
            return null;
        }
    }
}
