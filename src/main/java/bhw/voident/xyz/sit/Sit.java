package bhw.voident.xyz.sit;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

public class Sit implements ModInitializer {
    public static final String MOD_ID = "sit";
    public static final EntityType<SitSeatEntity> SIT_ENTITY_TYPE = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(MOD_ID, "entity_sit"),
            FabricEntityTypeBuilder.<SitSeatEntity>create(SpawnGroup.MISC, SitSeatEntity::new)
                    .dimensions(EntityDimensions.fixed(0.001F, 0.001F))
                    .trackRangeBlocks(16)
                    .trackedUpdateRate(1)
                    .build()
    );

    @Override
    public void onInitialize() {
        SitConfig.get();
        UseBlockCallback.EVENT.register(Sit::handleBlockUse);
        UseEntityCallback.EVENT.register(Sit::handleEntityUse);
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClient()) {
                return;
            }

            SitSeatEntity seat = SitUtil.getSitEntity(world, pos);
            if (seat == null) {
                seat = SitLogic.findSeat(world, pos);
            }

            if (seat != null) {
                SitUtil.removeSitEntity(world, pos);
                seat.removeAllPassengers();
                seat.discard();
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerManager().getPlayerList().forEach(SitLogic::tickServerPlayer));
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

        if (state.getBlock() instanceof StairsBlock && SitLogic.isSeatOccupied(world, hitResult.getBlockPos())) {
            return ActionResult.SUCCESS;
        }

        if (world.isClient()) {
            return SitLogic.canAttemptSit(player, world, hitResult.getBlockPos(), state, hitResult)
                    ? ActionResult.SUCCESS
                    : ActionResult.PASS;
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
