package bhw.voident.xyz.sit;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class SitUtil {
    private static final Map<RegistryKey<World>, Map<BlockPos, SeatData>> OCCUPIED = new HashMap<>();

    private SitUtil() {
    }

    public static boolean addSitEntity(World world, BlockPos blockPos, SitSeatEntity entity, Vec3d playerPos) {
        if (world.isClient()) {
            return false;
        }

        RegistryKey<World> key = world.getRegistryKey();
        OCCUPIED.computeIfAbsent(key, unused -> new HashMap<>())
                .put(blockPos, new SeatData(entity, playerPos));
        return true;
    }

    public static boolean removeSitEntity(World world, BlockPos pos) {
        if (world.isClient()) {
            return false;
        }

        RegistryKey<World> key = world.getRegistryKey();
        Map<BlockPos, SeatData> map = OCCUPIED.get(key);
        if (map == null) {
            return false;
        }

        map.remove(pos);
        return true;
    }

    public static SitSeatEntity getSitEntity(World world, BlockPos pos) {
        if (world.isClient()) {
            return null;
        }

        RegistryKey<World> key = world.getRegistryKey();
        Map<BlockPos, SeatData> map = OCCUPIED.get(key);
        if (map == null) {
            return null;
        }

        SeatData data = map.get(pos);
        if (data == null) {
            return null;
        }

        if (data.entity().isRemoved()) {
            map.remove(pos);
            return null;
        }

        return data.entity();
    }

    public static Vec3d getPreviousPlayerPosition(PlayerEntity player, SitSeatEntity sitEntity) {
        if (player.getWorld().isClient()) {
            return null;
        }

        RegistryKey<World> key = player.getWorld().getRegistryKey();
        Map<BlockPos, SeatData> map = OCCUPIED.get(key);
        if (map == null) {
            return null;
        }

        for (SeatData data : map.values()) {
            if (data.entity() == sitEntity) {
                return data.playerPos();
            }
        }

        return null;
    }

    public static boolean isPlayerSitting(PlayerEntity player) {
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof SitSeatEntity || vehicle instanceof PlayerEntity) {
            return true;
        }

        if (player.getWorld().isClient()) {
            return false;
        }

        RegistryKey<World> key = player.getWorld().getRegistryKey();
        Map<BlockPos, SeatData> map = OCCUPIED.get(key);
        if (map == null) {
            return false;
        }

        for (SeatData data : map.values()) {
            if (data.entity().hasPassenger(player)) {
                return true;
            }
        }

        return false;
    }

    private record SeatData(SitSeatEntity entity, Vec3d playerPos) {
    }
}
