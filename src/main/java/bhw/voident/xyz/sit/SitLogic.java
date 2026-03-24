package bhw.voident.xyz.sit;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

public final class SitLogic {
    private static final String SEAT_NAME = "sit$seat";
    private static final double SEAT_SEARCH_RADIUS = 0.35D;
    private static final double BOX_EPSILON = 1.0E-5D;
    private static final Set<UUID> ACTIVE_SNEAKS = new HashSet<>();

    private SitLogic() {
    }

    public static boolean canSitOn(BlockState state) {
        if (state.getBlock() instanceof SlabBlock) {
            return state.get(SlabBlock.TYPE) != SlabType.DOUBLE;
        }

        return state.getBlock() instanceof StairsBlock;
    }

    public static boolean canStack(PlayerEntity rider, Hand hand, PlayerEntity target) {
        return rider.isSneaking()
                && hand == Hand.MAIN_HAND
                && rider.getStackInHand(hand).isEmpty()
                && rider != target
                && target.isAlive();
    }

    public static boolean trySitOnBlock(ServerPlayerEntity player, ServerWorld world, BlockPos pos, BlockState state, BlockHitResult hitResult) {
        Vec3d seatPos = getSeatPos(world, pos, state, hitResult);
        if (seatPos == null) {
            return false;
        }

        ArmorStandEntity seat = findSeat(world, seatPos);
        boolean created = false;

        if (seat == null) {
            seat = createSeat(world, seatPos, player.getYaw());
            created = true;
        } else if (seat.hasPassengers()) {
            return false;
        }

        boolean mounted = player.startRiding(seat, true);
        if (!mounted && created) {
            seat.discard();
        }

        return mounted;
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

    public static void tryDismountModRide(ServerPlayerEntity player) {
        Entity vehicle = player.getVehicle();
        if (!isModRide(vehicle)) {
            return;
        }

        player.stopRiding();

        if (isSeatEntity(vehicle) && vehicle != null && !vehicle.hasPassengers()) {
            vehicle.discard();
        }
    }

    public static void cleanupSeats(ServerWorld world) {
        if (world.getTime() % 20L != 0L) {
            return;
        }

        for (Entity entity : world.iterateEntities()) {
            if (!isSeatEntity(entity)) {
                continue;
            }

            if (!entity.hasPassengers() || !isValidSeatBlock(world, entity.getPos())) {
                entity.discard();
            }
        }
    }

    public static boolean shouldShiftBeHandledByMod(PlayerEntity player) {
        return isModRide(player.getVehicle()) || player.hasPassengers();
    }

    public static boolean isModRide(Entity vehicle) {
        return isSeatEntity(vehicle) || vehicle instanceof PlayerEntity;
    }

    public static boolean isSeatEntity(Entity entity) {
        if (!(entity instanceof ArmorStandEntity armorStand)) {
            return false;
        }

        if (!armorStand.isMarker() || !armorStand.isInvisible()) {
            return false;
        }

        Text name = armorStand.getCustomName();
        return name != null && SEAT_NAME.equals(name.getString());
    }

    private static ArmorStandEntity createSeat(ServerWorld world, Vec3d seatPos, float yaw) {
        ArmorStandEntity seat = new ArmorStandEntity(world, seatPos.x, seatPos.y, seatPos.z);
        seat.setYaw(yaw);
        seat.setInvisible(true);
        seat.setMarker(true);
        seat.setNoGravity(true);
        seat.setSilent(true);
        seat.setInvulnerable(true);
        seat.setCustomName(Text.literal(SEAT_NAME));
        seat.setCustomNameVisible(false);
        world.spawnEntity(seat);
        return seat;
    }

    private static ArmorStandEntity findSeat(ServerWorld world, Vec3d seatPos) {
        Box searchBox = Box.of(seatPos, SEAT_SEARCH_RADIUS, 0.6D, SEAT_SEARCH_RADIUS);
        List<ArmorStandEntity> seats = world.getEntitiesByType(
                TypeFilter.instanceOf(ArmorStandEntity.class),
                searchBox,
                SitLogic::isSeatEntity
        );

        for (ArmorStandEntity seat : seats) {
            if (seat.squaredDistanceTo(seatPos) < 0.0025D) {
                return seat;
            }
        }

        return null;
    }

    private static Vec3d getSeatPos(ServerWorld world, BlockPos pos, BlockState state, BlockHitResult hitResult) {
        VoxelShape shape = state.getCollisionShape(world, pos, ShapeContext.absent());
        if (shape.isEmpty()) {
            return null;
        }

        double localX = Math.clamp(hitResult.getPos().x - pos.getX(), 0.1D, 0.9D);
        double localZ = Math.clamp(hitResult.getPos().z - pos.getZ(), 0.1D, 0.9D);
        double seatX = 0.5D;
        double seatZ = 0.5D;
        double topY = shape.getMax(Direction.Axis.Y);
        boolean matched = false;

        for (Box box : shape.getBoundingBoxes()) {
            if (!covers(box, localX, localZ)) {
                continue;
            }

            if (!matched || box.maxY > topY + BOX_EPSILON) {
                matched = true;
                topY = box.maxY;
                seatX = clampInside(localX, box.minX, box.maxX);
                seatZ = clampInside(localZ, box.minZ, box.maxZ);
            }
        }

        return new Vec3d(pos.getX() + seatX, pos.getY() + topY, pos.getZ() + seatZ);
    }

    private static boolean isValidSeatBlock(ServerWorld world, Vec3d seatPos) {
        BlockPos blockPos = BlockPos.ofFloored(seatPos.x, seatPos.y - 0.05D, seatPos.z);
        return canSitOn(world.getBlockState(blockPos));
    }

    private static boolean covers(Box box, double x, double z) {
        return x >= box.minX - BOX_EPSILON
                && x <= box.maxX + BOX_EPSILON
                && z >= box.minZ - BOX_EPSILON
                && z <= box.maxZ + BOX_EPSILON;
    }

    private static double clampInside(double value, double min, double max) {
        if (max - min < 0.1D) {
            return (min + max) * 0.5D;
        }

        return Math.clamp(value, min + 0.05D, max - 0.05D);
    }
}
