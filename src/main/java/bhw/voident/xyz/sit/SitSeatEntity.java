package bhw.voident.xyz.sit;

import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class SitSeatEntity extends Entity {
    public SitSeatEntity(EntityType<SitSeatEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        setNoGravity(true);
        setInvisible(true);
    }

    public SitSeatEntity(World world) {
        this(Sit.SIT_ENTITY_TYPE, world);
    }

    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        if (passenger instanceof PlayerEntity player) {
            Vec3d resetPosition = SitUtil.getPreviousPlayerPosition(player, this);
            if (resetPosition != null) {
                discard();
                return resetPosition;
            }
        }

        discard();
        return super.updatePassengerForDismount(passenger);
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        SitUtil.removeSitEntity(getWorld(), getBlockPos());
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }

}
