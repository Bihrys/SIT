package bhw.voident.xyz.sit1_21;

import net.minecraft.block.BlockState;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class SeatOrientation {

    public static final String SEAT = "sit_seat";
    public static final String SLAB = "slab";
    public static final String STAIR = "stair";

    public enum RideMode {
        NONE,
        FREE,
        LOCKED_STAIR
    }

    private SeatOrientation() {
    }

    public static boolean isSeat(Entity entity) {
        return entity instanceof ArmorStandEntity && entity.getCommandTags().contains(SEAT);
    }

    public static float getPlacementYaw(BlockState state, float fallbackYaw) {
        if (state.getBlock() instanceof StairsBlock && state.contains(Properties.HORIZONTAL_FACING)) {
            return state.get(Properties.HORIZONTAL_FACING).asRotation();
        }

        return fallbackYaw;
    }

    public static RideMode getRideMode(World world, Entity vehicle) {
        if (vehicle instanceof PlayerEntity) {
            return RideMode.FREE;
        }

        if (!(vehicle instanceof ArmorStandEntity)) {
            return RideMode.NONE;
        }

        BlockState state = getSeatBlockState(world, vehicle);
        if (state.getBlock() instanceof StairsBlock) {
            return RideMode.LOCKED_STAIR;
        }

        if (state.getBlock() instanceof SlabBlock) {
            return RideMode.FREE;
        }

        return RideMode.NONE;
    }

    public static float getLockedBodyYaw(World world, Entity vehicle, float fallbackYaw) {
        return getPlacementYaw(getSeatBlockState(world, vehicle), fallbackYaw);
    }

    public static BlockPos getSeatBlockPos(Entity vehicle) {
        return BlockPos.ofFloored(vehicle.getX(), vehicle.getY() - 0.2, vehicle.getZ());
    }

    public static BlockState getSeatBlockState(World world, Entity vehicle) {
        return world.getBlockState(getSeatBlockPos(vehicle));
    }

    public static void syncFreeRotation(LivingEntity entity, float yaw) {
        syncYaw(entity, yaw);
        syncHeadYaw(entity, yaw);
    }

    public static void syncBodyYaw(LivingEntity entity, float yaw) {
        entity.setBodyYaw(yaw);
        entity.prevBodyYaw = yaw;
    }

    public static void syncYaw(LivingEntity entity, float yaw) {
        entity.setYaw(yaw);
        entity.prevYaw = yaw;
        syncBodyYaw(entity, yaw);
    }

    public static void syncHeadYaw(LivingEntity entity, float yaw) {
        entity.setHeadYaw(yaw);
        entity.prevHeadYaw = yaw;
    }
}
