package bhw.voident.xyz.sit.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.player.PlayerEntity;

import bhw.voident.xyz.sit.SitLogic;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "shouldDismount", at = @At("HEAD"), cancellable = true)
    private void sit$handleShiftForModRides(CallbackInfoReturnable<Boolean> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (SitLogic.shouldShiftBeHandledByMod(player)) {
            cir.setReturnValue(false);
        }
    }
}
