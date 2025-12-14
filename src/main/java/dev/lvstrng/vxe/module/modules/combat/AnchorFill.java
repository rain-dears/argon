package dev.lvstrng.vxe.module.modules.combat;

import dev.lvstrng.vxe.event.events.TickListener;
import dev.lvstrng.vxe.module.Category;
import dev.lvstrng.vxe.module.Module;
import dev.lvstrng.vxe.module.setting.BooleanSetting;
import dev.lvstrng.vxe.module.setting.KeybindSetting;
import dev.lvstrng.vxe.module.setting.NumberSetting;
import dev.lvstrng.vxe.utils.BlockUtils;
import dev.lvstrng.vxe.utils.EncryptedString;
import dev.lvstrng.vxe.utils.InventoryUtils;
import dev.lvstrng.vxe.utils.KeyUtils;
import dev.lvstrng.vxe.utils.TimerUtils;
import dev.lvstrng.vxe.utils.WorldUtils;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * Charges anchors on demand without detonating them.
 */
public final class AnchorFill extends Module implements TickListener {
        private final KeybindSetting activationKey = new KeybindSetting(EncryptedString.of("Hold Key"), GLFW.GLFW_MOUSE_BUTTON_RIGHT, true)
                        .setDescription(EncryptedString.of("Fills anchors only while this key is held"));
        private final BooleanSetting useRightClickLogic = new BooleanSetting(EncryptedString.of("Right Click Flow"), true)
                        .setDescription(EncryptedString.of("If enabled, runs the fill flow while holding right-click instead of the hotkey"));
        private final BooleanSetting autoSwitch = new BooleanSetting(EncryptedString.of("Auto Switch"), true)
                        .setDescription(EncryptedString.of("Swap to glowstone automatically"));
        private final NumberSetting placeDelay = new NumberSetting(EncryptedString.of("Delay"), 0, 10, 2, 1)
                        .setDescription(EncryptedString.of("Ticks between glowstone placements"));

        private final TimerUtils timer = new TimerUtils();

        public AnchorFill() {
                super(EncryptedString.of("Anchor Fill"),
                                EncryptedString.of("Only charges anchors when you right-click"),
                                -1,
                                Category.COMBAT);
                addSettings(activationKey, useRightClickLogic, autoSwitch, placeDelay);
        }

        @Override
        public void onEnable() {
                eventManager.add(TickListener.class, this);
                timer.reset();
                super.onEnable();
        }

        @Override
        public void onDisable() {
                eventManager.remove(TickListener.class, this);
                super.onDisable();
        }

        @Override
        public void onTick() {
                if (mc.currentScreen != null || mc.player == null || mc.world == null)
                        return;

                if (useRightClickLogic.getValue()) {
                        runRightClickFlow();
                        return;
                }

                if (!KeyUtils.isKeyPressed(activationKey.getValue()))
                        return;

                if (!(mc.crosshairTarget instanceof BlockHitResult hit))
                        return;

                if (!BlockUtils.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR))
                        return;

                mc.options.useKey.setPressed(false);

                if (BlockUtils.isAnchorCharged(hit.getBlockPos()))
                        return;

                if (!timer.hasReached(placeDelay.getValueInt() * 50L))
                        return;

                Hand glowstoneHand = getGlowstoneHand();

                if (glowstoneHand == null && autoSwitch.getValue() && InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE)) {
                        glowstoneHand = Hand.MAIN_HAND;
                }

                if (glowstoneHand == null)
                        return;

                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(glowstoneHand, hit, 0));
                mc.player.swingHand(glowstoneHand);
                timer.reset();
        }

        private void runRightClickFlow() {
                if (!mc.options.useKey.isPressed())
                        return;

                if (!(mc.crosshairTarget instanceof BlockHitResult hit))
                        return;

                mc.options.useKey.setPressed(false);

                BlockHitResult placementHit = resolvePlacementHit(hit);

                if (!BlockUtils.isBlock(placementHit.getBlockPos(), Blocks.RESPAWN_ANCHOR)) {
                        attemptAnchorPlacement(placementHit);
                        return;
                }

                if (BlockUtils.isAnchorCharged(placementHit.getBlockPos()))
                        return;

                attemptAnchorCharge(placementHit);
        }

        private void attemptAnchorPlacement(BlockHitResult placementHit) {
                if (!timer.hasReached(placeDelay.getValueInt() * 50L))
                        return;

                if (!InventoryUtils.selectItemFromHotbar(Items.RESPAWN_ANCHOR))
                        return;

                WorldUtils.placeBlock(placementHit, true);
                timer.reset();
        }

        private void attemptAnchorCharge(BlockHitResult placementHit) {
                if (!timer.hasReached(placeDelay.getValueInt() * 50L))
                        return;

                Hand glowstoneHand = getGlowstoneHand();

                if (glowstoneHand == null && autoSwitch.getValue() && InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE)) {
                        glowstoneHand = Hand.MAIN_HAND;
                }

                if (glowstoneHand == null)
                        return;

                mc.getNetworkHandler().sendPacket(new PlayerInteractBlockC2SPacket(glowstoneHand, placementHit, 0));
                mc.player.swingHand(glowstoneHand);
                timer.reset();
        }

        private BlockHitResult resolvePlacementHit(BlockHitResult hit) {
                BlockPos base = hit.getBlockPos();
                BlockPos placePos = mc.world.getBlockState(base).isReplaceable() ? base : base.offset(hit.getSide());
                return new BlockHitResult(Vec3d.ofCenter(placePos), hit.getSide(), placePos, false);
        }

        private Hand getGlowstoneHand() {
                Item mainHandItem = mc.player.getMainHandStack().getItem();
                if (mainHandItem == Items.GLOWSTONE)
                        return Hand.MAIN_HAND;

                Item offHandItem = mc.player.getOffHandStack().getItem();
                if (offHandItem == Items.GLOWSTONE)
                        return Hand.OFF_HAND;

                return null;
        }
}
