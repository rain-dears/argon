package dev.lvstrng.argon.module.modules.combat;

import dev.lvstrng.argon.event.events.TickListener;
import dev.lvstrng.argon.module.Category;
import dev.lvstrng.argon.module.Module;
import dev.lvstrng.argon.module.setting.BooleanSetting;
import dev.lvstrng.argon.module.setting.KeybindSetting;
import dev.lvstrng.argon.module.setting.NumberSetting;
import dev.lvstrng.argon.utils.BlockUtils;
import dev.lvstrng.argon.utils.EncryptedString;
import dev.lvstrng.argon.utils.InventoryUtils;
import dev.lvstrng.argon.utils.KeyUtils;
import dev.lvstrng.argon.utils.TimerUtils;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.lwjgl.glfw.GLFW;

/**
 * Charges anchors on demand without detonating them.
 */
public final class AnchorFill extends Module implements TickListener {
        private final KeybindSetting activationKey = new KeybindSetting(EncryptedString.of("Hold Key"), GLFW.GLFW_MOUSE_BUTTON_RIGHT, true)
                        .setDescription(EncryptedString.of("Fills anchors only while this key is held"));
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
                addSettings(activationKey, autoSwitch, placeDelay);
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

                if (!KeyUtils.isKeyPressed(activationKey.getValue()))
                        return;

                if (!(mc.crosshairTarget instanceof BlockHitResult hit))
                        return;

                if (!BlockUtils.isBlock(hit.getBlockPos(), Blocks.RESPAWN_ANCHOR))
                        return;

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
