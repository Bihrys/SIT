package bhw.voident.xyz.sit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class SitLogic {
    private static final double SEAT_SEARCH_RADIUS = 0.35D;
    private static final Set<UUID> ACTIVE_SNEAKS = new HashSet<>();

    private SitLogic() {
    }

    public static boolean canSitOn(BlockState state) {
        if (state.getBlock() instanceof SlabBlock) {
            return state.get(SlabBlock.TYPE) == SlabType.BOTTOM;
        }

        if (state.getBlock() instanceof StairsBlock) {
            return state.get(StairsBlock.HALF) == BlockHalf.BOTTOM;
        }

        return false;
    }

    public static boolean canStack(PlayerEntity rider, Hand hand, PlayerEntity target) {
        return rider.isSneaking()
                && hand == Hand.MAIN_HAND
                && rider.getStackInHand(hand).isEmpty()
                && rider != target
                && target.isAlive()
                && rider.getVehicle() == null;
    }

    public static boolean trySitOnBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, BlockHitResult hitResult) {
        if (!canAttemptSit(player, world, pos, state, hitResult)) {
            return false;
        }

        SitSeatEntity seat = findSeat(world, pos);
        if (seat == null) {
            seat = new SitSeatEntity(world);
            seat.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, getSeatYaw(state, player), 0.0F);
            SitUtil.addSitEntity(world, pos, seat, player.getPos());
            world.spawnEntity(seat);
        } else if (seat.hasPassengers()) {
            return false;
        } else {
            SitUtil.addSitEntity(world, pos, seat, player.getPos());
        }

        boolean mounted = player.startRiding(seat, true);
        if (!mounted) {
            seat.discard();
        }

        return mounted;
    }

    public static boolean canAttemptSit(PlayerEntity player, World world, BlockPos pos, BlockState state, BlockHitResult hitResult) {
        if (!canSitOn(state)) {
            return false;
        }

        if (hitResult.getSide() != Direction.UP) {
            return false;
        }

        if (player.isSneaking() || SitUtil.isPlayerSitting(player)) {
            return false;
        }

        if (!player.getStackInHand(Hand.MAIN_HAND).isEmpty()) {
            return false;
        }

        if (!world.canPlayerModifyAt(player, pos)) {
            return false;
        }

        if (!isPlayerInRange(player, pos)) {
            return false;
        }

        return !isSeatOccupied(world, pos);
    }

    public static boolean tryStackOnPlayer(ServerPlayerEntity rider, PlayerEntity target) {
        if (!target.isAlive()) {
            return false;
        }

        if (target.hasPassengerDeep(rider) || rider.isConnectedThroughVehicle(target) || rider.hasPassengerDeep(target)) {
            return false;
        }

        return rider.startRiding(target, true);
    }

    public static void handleSneak(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        if (!player.isSneaking()) {
            ACTIVE_SNEAKS.remove(playerId);
            return;
        }

        if (!ACTIVE_SNEAKS.add(playerId)) {
            return;
        }

        Entity passenger = player.getFirstPassenger();
        if (passenger instanceof PlayerEntity) {
            passenger.stopRiding();
            passenger.setSneaking(false);
        }
    }

    public static void tickServerPlayer(ServerPlayerEntity player) {
        handleSneak(player);
        updateRiderPose(player);
    }

    public static void cleanupSeats(ServerWorld world) {
        if (world.getTime() % 20L != 0L) {
            return;
        }

        for (Entity entity : world.iterateEntities()) {
            if (!isSeatEntity(entity)) {
                continue;
            }

            if (!entity.hasPassengers() || !isValidSeatBlock(world, entity.getBlockPos())) {
                entity.discard();
            }
        }
    }

    public static boolean shouldShiftBeHandledByMod(PlayerEntity player) {
        return player.hasPassengers();
    }

    public static boolean isModRide(Entity vehicle) {
        return isSeatEntity(vehicle) || vehicle instanceof PlayerEntity;
    }

    public static boolean isSeatEntity(Entity entity) {
        return entity instanceof SitSeatEntity;
    }

    public static boolean isSeatOccupied(World world, BlockPos pos) {
        SitSeatEntity seat = SitUtil.getSitEntity(world, pos);
        if (seat != null) {
            return seat.hasPassengers();
        }

        seat = findSeat(world, pos);
        return seat != null && seat.hasPassengers();
    }

    public static void updateRiderPose(PlayerEntity rider) {
        Entity vehicle = rider.getVehicle();
        if (vehicle == null) {
            return;
        }

        if (vehicle instanceof PlayerEntity) {
            alignBodyToHead(rider);
            return;
        }

        if (!isSeatEntity(vehicle)) {
            return;
        }

        World world = rider.getWorld();
        BlockState state = world.getBlockState(vehicle.getBlockPos());
        if (state.getBlock() instanceof StairsBlock) {
            rider.setBodyYaw(getStairSeatYaw(state));
            return;
        }

        alignBodyToHead(rider);
    }

    private static void alignBodyToHead(PlayerEntity rider) {
        rider.setBodyYaw(rider.getHeadYaw());
    }

    private static float getSeatYaw(BlockState state, PlayerEntity player) {
        if (state.getBlock() instanceof StairsBlock) {
            return getStairSeatYaw(state);
        }

        return player.getYaw();
    }

    private static float getStairSeatYaw(BlockState state) {
        Direction facing = state.get(StairsBlock.FACING);
        return facing.getOpposite().asRotation();
    }

    public static SitSeatEntity findSeat(World world, BlockPos pos) {
        Vec3d center = Vec3d.ofCenter(pos);
        Box searchBox = Box.of(center, SEAT_SEARCH_RADIUS, 0.6D, SEAT_SEARCH_RADIUS);
        List<SitSeatEntity> seats = world.getEntitiesByType(Sit.SIT_ENTITY_TYPE, searchBox, entity -> true);
        return seats.isEmpty() ? null : seats.get(0);
    }

    private static boolean isValidSeatBlock(World world, BlockPos pos) {
        return canSitOn(world.getBlockState(pos));
    }

    private static boolean isPlayerInRange(PlayerEntity player, BlockPos pos) {
        int blockReachDistance = SitConfig.get().getBlockReachDistance();
        BlockPos playerPos = player.getBlockPos();

        if (blockReachDistance == 0) {
            return playerPos.getY() - pos.getY() <= 1
                    && playerPos.getX() == pos.getX()
                    && playerPos.getZ() == pos.getZ();
        }

        return Math.abs(playerPos.getX() - pos.getX()) <= blockReachDistance
                && Math.abs(playerPos.getY() - pos.getY()) <= blockReachDistance
                && Math.abs(playerPos.getZ() - pos.getZ()) <= blockReachDistance;
    }
}
