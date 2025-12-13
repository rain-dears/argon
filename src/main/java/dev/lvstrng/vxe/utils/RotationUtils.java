package dev.lvstrng.vxe.utils;

import dev.lvstrng.vxe.Vxe;
import dev.lvstrng.vxe.utils.rotation.Rotation;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RotationUtils {

        public static Rotation getDirection(Entity entity, Vec3d vec) {
                double dx = vec.x - entity.getX(),
                                dy = vec.y - entity.getY(),
                                dz = vec.z - entity.getZ(),
                                dist = MathHelper.sqrt((float) (dx * dx + dz * dz));

		return new Rotation(MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dz, dx)) - 90.0), -MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(dy, dist))));
	}

        public static double getAngleToRotation(Rotation rotation) {
                double currentYaw = MathHelper.wrapDegrees(entityYaw());
                double currentPitch = MathHelper.wrapDegrees(entityPitch());

                double diffYaw = MathHelper.wrapDegrees(currentYaw - rotation.yaw());
                double diffPitch = MathHelper.wrapDegrees(currentPitch - rotation.pitch());

                return Math.sqrt(diffYaw * diffYaw + diffPitch * diffPitch);
        }

        private static float entityYaw() {
                return Vxe.mc.player.getYaw();
        }

        private static float entityPitch() {
                return Vxe.mc.player.getPitch();
        }
}