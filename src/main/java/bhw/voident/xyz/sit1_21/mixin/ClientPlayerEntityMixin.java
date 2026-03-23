package bhw.voident.xyz.sit1_21.mixin;

import bhw.voident.xyz.sit1_21.SeatOrientation;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "getBodyYaw", at = @At("HEAD"), cancellable = true)
    private void sit1_21$lockBodyYawForStairSeat(CallbackInfoReturnable<Float> cir) {
        ClientPlayerEntity player = (ClientPlayerEntity) (Object) this;
        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            return;
        }

        if (SeatOrientation.getRideMode(player.getWorld(), vehicle) != SeatOrientation.RideMode.LOCKED_STAIR) {
            return;
        }

        cir.setReturnValue(SeatOrientation.getLockedBodyYaw(player.getWorld(), vehicle, vehicle.getYaw()));
    }
}
