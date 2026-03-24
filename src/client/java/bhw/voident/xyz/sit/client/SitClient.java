package bhw.voident.xyz.sit.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import bhw.voident.xyz.sit.SitLogic;
import bhw.voident.xyz.sit.network.DismountRidePayload;

public class SitClient implements ClientModInitializer {
    private static boolean jumpPressedLastTick;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean jumpPressed = client.options.jumpKey.isPressed();

            if (client.player == null || client.currentScreen != null) {
                jumpPressedLastTick = jumpPressed;
                return;
            }

            if (jumpPressed && !jumpPressedLastTick && SitLogic.isModRide(client.player.getVehicle())) {
                ClientPlayNetworking.send(DismountRidePayload.INSTANCE);
            }

            jumpPressedLastTick = jumpPressed;
        });
    }
}
