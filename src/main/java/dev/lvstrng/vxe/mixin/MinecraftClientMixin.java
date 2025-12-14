package dev.lvstrng.vxe.mixin;

import dev.lvstrng.vxe.Vxe;
import dev.lvstrng.vxe.event.EventManager;
import dev.lvstrng.vxe.event.events.AttackListener;
import dev.lvstrng.vxe.event.events.BlockBreakingListener;
import dev.lvstrng.vxe.event.events.ItemUseListener;
import dev.lvstrng.vxe.event.events.TickListener;
import dev.lvstrng.vxe.utils.MouseSimulation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
	@Shadow
	@Nullable
	public ClientWorld world;

        @Inject(method = "tick", at = @At("HEAD"))
        private void onTick(CallbackInfo ci) {
                if (world != null) {
                        TickListener.TickEvent event = new TickListener.TickEvent();

                        EventManager.fire(event);
                }
        }

	@Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
	private void onItemUse(CallbackInfo ci) {
		ItemUseListener.ItemUseEvent event = new ItemUseListener.ItemUseEvent();

		EventManager.fire(event);
		if (event.isCancelled()) ci.cancel();

		if (MouseSimulation.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_RIGHT)) {
			MouseSimulation.mouseButtons.put(GLFW.GLFW_MOUSE_BUTTON_RIGHT, false);
			ci.cancel();
		}
	}

	@Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
        private void onAttack(CallbackInfoReturnable<Boolean> cir) {
                AttackListener.AttackEvent event = new AttackListener.AttackEvent();

                EventManager.fire(event);
                if (event.isCancelled()) cir.setReturnValue(false);

                if (MouseSimulation.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_1)) {
                        MouseSimulation.mouseButtons.put(GLFW.GLFW_MOUSE_BUTTON_1, false);
                        cir.setReturnValue(false);
                }
        }

        @Inject(method = "handleBlockBreaking", at = @At("HEAD"), cancellable = true)
        private void onBlockBreaking(boolean breaking, CallbackInfo ci) {
                BlockBreakingListener.BlockBreakingEvent event = new BlockBreakingListener.BlockBreakingEvent();

                EventManager.fire(event);
                if (event.isCancelled()) ci.cancel();

                if (MouseSimulation.isMouseButtonPressed(GLFW.GLFW_MOUSE_BUTTON_1)) {
                        MouseSimulation.mouseButtons.put(GLFW.GLFW_MOUSE_BUTTON_1, false);
                        ci.cancel();
                }
        }

        @Inject(method = "stop", at = @At("HEAD"))
        private void onClose(CallbackInfo ci) {
                Vxe.INSTANCE.getProfileManager().saveProfile();
	}
}
