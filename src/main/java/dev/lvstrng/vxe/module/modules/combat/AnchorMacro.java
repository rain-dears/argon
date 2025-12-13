package dev.lvstrng.vxe.module.modules.combat;

import dev.lvstrng.vxe.event.events.ItemUseListener;
import dev.lvstrng.vxe.event.events.TickListener;
import dev.lvstrng.vxe.module.Category;
import dev.lvstrng.vxe.module.Module;
import dev.lvstrng.vxe.module.setting.BooleanSetting;
import dev.lvstrng.vxe.module.setting.KeybindSetting;
import dev.lvstrng.vxe.module.setting.NumberSetting;
import dev.lvstrng.vxe.utils.*;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

//not mine
public final class AnchorMacro extends Module implements TickListener, ItemUseListener {
	private final BooleanSetting whileUse = new BooleanSetting(EncryptedString.of("While Use"), true).setDescription(EncryptedString.of("If it should trigger while eating/using shield"));
	private final BooleanSetting stopOnKill = new BooleanSetting(EncryptedString.of("Stop on Kill"), false).setDescription(EncryptedString.of("Doesn't anchor if body nearby"));
	private final BooleanSetting clickSimulation = new BooleanSetting(EncryptedString.of("Click Simulation"), false).setDescription(EncryptedString.of("Makes the CPS hud think you're legit"));
	private final NumberSetting switchDelay = new NumberSetting(EncryptedString.of("Switch Delay"), 0, 20, 0, 1);
	private final NumberSetting switchChance = new NumberSetting(EncryptedString.of("Switch Chance"), 0, 100, 100, 1);
	private final NumberSetting placeChance = new NumberSetting(EncryptedString.of("Place Chance"), 0, 100, 100, 1).setDescription(EncryptedString.of("Randomization"));
	private final NumberSetting glowstoneDelay = new NumberSetting(EncryptedString.of("Glowstone Delay"), 0, 20, 0, 1);
        private final NumberSetting glowstoneChance = new NumberSetting(EncryptedString.of("Glowstone Chance"), 0, 100, 100, 1);
        private final NumberSetting explodeDelay = new NumberSetting(EncryptedString.of("Explode Delay"), 0, 20, 0, 1);
        private final NumberSetting explodeChance = new NumberSetting(EncryptedString.of("Explode Chance"), 0, 100, 100, 1);
        private final NumberSetting explodeSlot = new NumberSetting(EncryptedString.of("Explode Slot"), 1, 9, 1, 1);
        private final BooleanSetting onlyOwn = new BooleanSetting(EncryptedString.of("Only Own"), false);
        private final BooleanSetting onlyCharge = new BooleanSetting(EncryptedString.of("Only Charge"), false);
        private final KeybindSetting activationKey = new KeybindSetting(EncryptedString.of("Hold Key"), GLFW.GLFW_MOUSE_BUTTON_4, true)
                        .setDescription(EncryptedString.of("Macro runs only while this key is held (Mouse Button 5 by default)"));

        private int switchClock = 0;
        private int glowstoneClock = 0;
        private int explodeClock = 0;

        private MacroStep step = MacroStep.IDLE;
        private BlockPos targetAnchor;
        private BlockHitResult targetHit;

	//hashset cuz in a hashset stuff cant repeat iirc
	private final Set<BlockPos> ownedAnchors = new HashSet<>();

        public AnchorMacro() {
                super(EncryptedString.of("Anchor Macro"),
                                EncryptedString.of("Automatically blows up respawn anchors for you"),
                                -1,
                                Category.COMBAT);
                addSettings(whileUse, stopOnKill, clickSimulation, placeChance, switchDelay, switchChance, glowstoneDelay, glowstoneChance, explodeDelay, explodeChance, explodeSlot, onlyOwn, onlyCharge, activationKey);
        }

	@Override
        public void onEnable() {
                eventManager.add(TickListener.class, this);
                eventManager.add(ItemUseListener.class, this);
                switchClock = 0;
                glowstoneClock = 0;
                explodeClock = 0;
                step = MacroStep.IDLE;
                super.onEnable();
        }

	@Override
        public void onDisable() {
                eventManager.remove(TickListener.class, this);
                eventManager.remove(ItemUseListener.class, this);
                resetMacroState();
                super.onDisable();
        }

	@Override
        public void onTick() {
                if (mc.currentScreen != null)
                        return;

                if (!KeyUtils.isKeyPressed(activationKey.getValue())) {
                        resetMacroState();
                        return;
                }

                if (!whileUse.getValue() && mc.player.isUsingItem())
                        return;

                if (stopOnKill.getValue() && WorldUtils.isDeadBodyNearby())
                        return;

                if (!(mc.crosshairTarget instanceof BlockHitResult hit))
                        return;

                if (step == MacroStep.IDLE) {
                        targetHit = resolvePlacementHit(hit);
                        targetAnchor = targetHit.getBlockPos();
                        switchClock = glowstoneClock = explodeClock = 0;
                        step = MacroStep.PLACE_ANCHOR;
                }

                if (targetAnchor == null || targetHit == null)
                        return;

                if (onlyOwn.getValue() && BlockUtils.isBlock(targetAnchor, Blocks.RESPAWN_ANCHOR) && !ownedAnchors.contains(targetAnchor)) {
                        resetMacroState();
                        return;
                }

                switch (step) {
                        case PLACE_ANCHOR -> handleAnchorPlacement();
                        case CHARGE_ANCHOR -> handleAnchorCharging();
                        case EXPLODE -> handleAnchorExplosion();
                }
        }

	@Override
        public void onItemUse(ItemUseEvent event) {
                if (mc.crosshairTarget instanceof BlockHitResult hitResult && hitResult.getType() == HitResult.Type.BLOCK) {
                        if (mc.player.getMainHandStack().getItem() == Items.RESPAWN_ANCHOR) {
                                Direction dir = hitResult.getSide();
                                BlockPos pos = hitResult.getBlockPos();

				if (!mc.world.getBlockState(pos).isReplaceable()) {
					switch (dir) {
						case UP: {
							ownedAnchors.add(pos.add(0, 1, 0));
							break;
						}
						case DOWN: {
							ownedAnchors.add(pos.add(0, -1, 0));
							break;
						}
						case EAST: {
							ownedAnchors.add(pos.add(1, 0, 0));
							break;
						}
						case WEST: {
							ownedAnchors.add(pos.add(-1, 0, 0));
							break;
						}
						case NORTH: {
							ownedAnchors.add(pos.add(0, 0, -1));
							break;
						}
						case SOUTH: {
							ownedAnchors.add(pos.add(0, 0, 1));
							break;
						}
					}
				} else ownedAnchors.add(pos);
			}

			BlockPos bp = hitResult.getBlockPos();

                        if(BlockUtils.isAnchorCharged(bp))
                                ownedAnchors.remove(bp);
                }
        }

        private void handleAnchorPlacement() {
                if (BlockUtils.isBlock(targetAnchor, Blocks.RESPAWN_ANCHOR)) {
                        step = MacroStep.CHARGE_ANCHOR;
                        glowstoneClock = 0;
                        return;
                }

                if (!InventoryUtils.selectItemFromHotbar(Items.RESPAWN_ANCHOR))
                        return;

                if (switchClock < switchDelay.getValueInt()) {
                        switchClock++;
                        return;
                }

                switchClock = 0;

                if (MathUtils.randomInt(1, 100) > placeChance.getValueInt())
                        return;

                if (clickSimulation.getValue())
                        MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);

                WorldUtils.placeBlock(targetHit, true);
                ownedAnchors.add(targetAnchor);
        }

        private void handleAnchorCharging() {
                if (!BlockUtils.isBlock(targetAnchor, Blocks.RESPAWN_ANCHOR)) {
                        resetMacroState();
                        return;
                }

                if (BlockUtils.isAnchorCharged(targetAnchor)) {
                        step = MacroStep.EXPLODE;
                        explodeClock = 0;
                        return;
                }

                if (!selectGlowstone())
                        return;

                if (glowstoneClock < glowstoneDelay.getValueInt()) {
                        glowstoneClock++;
                        return;
                }

                glowstoneClock = 0;

                if (MathUtils.randomInt(1, 100) > glowstoneChance.getValueInt())
                        return;

                if (clickSimulation.getValue())
                        MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);

                WorldUtils.placeBlock(targetHit, true);
        }

        private void handleAnchorExplosion() {
                if (!BlockUtils.isAnchorCharged(targetAnchor)) {
                        step = MacroStep.CHARGE_ANCHOR;
                        return;
                }

                if (onlyCharge.getValue())
                        return;

                int slot = explodeSlot.getValueInt() - 1;

                if (mc.player.getInventory().selectedSlot != slot) {
                        if (switchClock < switchDelay.getValueInt()) {
                                switchClock++;
                                return;
                        }

                        switchClock = 0;

                        if (MathUtils.randomInt(1, 100) <= switchChance.getValueInt())
                                mc.player.getInventory().selectedSlot = slot;

                        return;
                }

                if (explodeClock < explodeDelay.getValueInt()) {
                        explodeClock++;
                        return;
                }

                explodeClock = 0;

                if (MathUtils.randomInt(1, 100) > explodeChance.getValueInt())
                        return;

                if (clickSimulation.getValue())
                        MouseSimulation.mouseClick(GLFW.GLFW_MOUSE_BUTTON_RIGHT);

                WorldUtils.placeBlock(targetHit, true);
                ownedAnchors.remove(targetAnchor);
                resetMacroState();
        }

        private boolean selectGlowstone() {
                if (mc.player.getMainHandStack().isOf(Items.GLOWSTONE))
                        return true;

                if (switchClock < switchDelay.getValueInt()) {
                        switchClock++;
                        return false;
                }

                switchClock = 0;
                return MathUtils.randomInt(1, 100) <= switchChance.getValueInt() && InventoryUtils.selectItemFromHotbar(Items.GLOWSTONE);
        }

        private BlockHitResult resolvePlacementHit(BlockHitResult hit) {
                BlockPos base = hit.getBlockPos();

                if (BlockUtils.isBlock(base, Blocks.RESPAWN_ANCHOR))
                        return new BlockHitResult(Vec3d.ofCenter(base), hit.getSide(), base, false);

                BlockPos placePos = mc.world.getBlockState(base).isReplaceable() ? base : base.offset(hit.getSide());
                return new BlockHitResult(Vec3d.ofCenter(placePos), hit.getSide(), placePos, false);
        }

        private void resetMacroState() {
                step = MacroStep.IDLE;
                targetAnchor = null;
                targetHit = null;
                switchClock = 0;
                glowstoneClock = 0;
                explodeClock = 0;
        }

        private enum MacroStep {
                IDLE,
                PLACE_ANCHOR,
                CHARGE_ANCHOR,
                EXPLODE
        }
}
