package bhw.voident.xyz.sit;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import bhw.voident.xyz.sit.network.DismountRidePayload;

public class Sit implements ModInitializer {
    public static final String MOD_ID = "sit";

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(DismountRidePayload.ID, DismountRidePayload.CODEC);
        UseBlockCallback.EVENT.register(Sit::handleBlockUse);
        UseEntityCallback.EVENT.register(Sit::handleEntityUse);
        ServerPlayNetworking.registerGlobalReceiver(DismountRidePayload.ID, (payload, context) -> SitLogic.tryDismountModRide(context.player()));
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerManager().getPlayerList().forEach(SitLogic::handleSneak));
        ServerTickEvents.END_WORLD_TICK.register(SitLogic::cleanupSeats);
    }

    private static ActionResult handleBlockUse(PlayerEntity player, World world, Hand hand, BlockHitResult hitResult) {
        if (hand != Hand.MAIN_HAND || player.isSpectator()) {
            return ActionResult.PASS;
        }

        BlockState state = world.getBlockState(hitResult.getBlockPos());
        if (!SitLogic.canSitOn(state)) {
            return ActionResult.PASS;
        }

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(world instanceof ServerWorld serverWorld) || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        return SitLogic.trySitOnBlock(serverPlayer, serverWorld, hitResult.getBlockPos(), state, hitResult)
                ? ActionResult.SUCCESS
                : ActionResult.PASS;
    }

    private static ActionResult handleEntityUse(PlayerEntity player, World world, Hand hand, Entity entity, EntityHitResult hitResult) {
        if (hand != Hand.MAIN_HAND || player.isSpectator()) {
            return ActionResult.PASS;
        }

        if (!(entity instanceof PlayerEntity targetPlayer) || !SitLogic.canStack(player, hand, targetPlayer)) {
            return ActionResult.PASS;
        }

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return ActionResult.PASS;
        }

        return SitLogic.tryStackOnPlayer(serverPlayer, targetPlayer) ? ActionResult.SUCCESS : ActionResult.PASS;
    }
}
