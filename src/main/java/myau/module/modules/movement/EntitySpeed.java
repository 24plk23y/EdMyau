package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.event.EventTarget;
import myau.events.LivingUpdateEvent;
import myau.module.Module;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.util.AxisAlignedBB;

public class EntitySpeed extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty(
            "mode",
            0,
            new String[]{
                    "ENTITY_COLLIDE",
                    "GRIM"
            }
    );

    public final IntProperty speed = new IntProperty(
            "speed",
            5,
            1,
            8
    );

    public EntitySpeed() {
        super("SpeedNew", false);
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!isEnabled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        switch (mode.getValue()) {
            case 0:
                handleEntityCollide();
                break;

            case 1:
                handleGrim();
                break;
        }
    }

    private void handleEntityCollide() {
        if (!isMoving()) {
            return;
        }

        EntityLivingBase player = mc.thePlayer;
        int count = 0;

        AxisAlignedBB playerBox =
                player.getEntityBoundingBox().expand(1.0, 1.0, 1.0);

        for (Object object : mc.theWorld.loadedEntityList) {
            Entity entity = (Entity) object;

            if (entity instanceof EntityArmorStand) {
                continue;
            }

            if (entity != player
                    && entity instanceof EntityLivingBase
                    && playerBox.intersectsWith(
                    entity.getEntityBoundingBox())) {
                count++;
            }
        }

        if (count <= 0) {
            return;
        }

        double yawRadians =
                Math.toRadians(getMoveYaw(player.rotationYaw));

        double boost =
                speed.getValue() * 0.01 * count;

        double motionX =
                -Math.sin(yawRadians) * boost;

        double motionZ =
                Math.cos(yawRadians) * boost;

        player.addVelocity(
                motionX,
                0.0,
                motionZ
        );
    }

    private void handleGrim() {
        EntityLivingBase player = mc.thePlayer;
        int count = 0;

        AxisAlignedBB playerBox =
                player.getEntityBoundingBox().expand(1.0, 1.0, 1.0);

        for (Object object : mc.theWorld.loadedEntityList) {
            Entity entity = (Entity) object;

            if (!(entity instanceof EntityLivingBase)
                    && !(entity instanceof EntityBoat)
                    && !(entity instanceof EntityMinecart)
                    && !(entity instanceof EntityFishHook)) {
                continue;
            }

            if (entity instanceof EntityArmorStand) {
                continue;
            }

            if (entity.getEntityId() == player.getEntityId()) {
                continue;
            }

            if (!playerBox.intersectsWith(
                    entity.getEntityBoundingBox())) {
                continue;
            }

            if (entity.getEntityId() == -8
                    || entity.getEntityId() == -1337) {
                continue;
            }

            count++;
        }

        if (count > 0 && isMoving()) {
            double strafeOffset =
                    Math.min(count, 4)
                            * speed.getValue()
                            * 0.01;

            double yawRadians =
                    Math.toRadians(getMoveYaw(player.rotationYaw));

            double motionX =
                    -Math.sin(yawRadians) * strafeOffset;

            double motionZ =
                    Math.cos(yawRadians) * strafeOffset;

            player.addVelocity(
                    motionX,
                    0.0,
                    motionZ
            );

            if (count < 4
                    && getKillAuraTarget() != null
                    && mc.gameSettings.keyBindSprint.isKeyDown()) {

                KeyBinding.setKeyBindState(
                        mc.gameSettings.keyBindSprint.getKeyCode(),
                        true
                );

                return;
            }
        }

        KeyBinding.setKeyBindState(
                mc.gameSettings.keyBindSprint.getKeyCode(),
                mc.gameSettings.keyBindSprint.isKeyDown()
        );
    }

    private double getMoveYaw(float yaw) {
        double result = yaw;

        if (mc.thePlayer.moveForward < 0.0F) {
            result += 180.0;
        }

        double forward = 1.0;

        if (mc.thePlayer.moveForward < 0.0F) {
            forward = -0.5;
        } else if (mc.thePlayer.moveForward > 0.0F) {
            forward = 0.5;
        }

        if (mc.thePlayer.moveStrafing > 0.0F) {
            result -= 90.0 * forward;
        }

        if (mc.thePlayer.moveStrafing < 0.0F) {
            result += 90.0 * forward;
        }

        return result;
    }

    private boolean isMoving() {
        return mc.thePlayer.moveForward != 0.0F
                || mc.thePlayer.moveStrafing != 0.0F;
    }

    private Entity getKillAuraTarget() {
        KillAura killAura =
                (KillAura) Myau.moduleManager.getModule(KillAura.class);

        if (killAura == null) {
            return null;
        }

        return killAura.getTarget();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{
                CaseFormat.UPPER_UNDERSCORE.to(
                        CaseFormat.UPPER_CAMEL,
                        mode.getModeString()
                )
        };
    }

    @Override
    public void onDisabled() {
        if (mc.gameSettings != null
                && mc.gameSettings.keyBindSprint != null) {
            KeyBinding.setKeyBindState(
                    mc.gameSettings.keyBindSprint.getKeyCode(),
                    mc.gameSettings.keyBindSprint.isKeyDown()
            );
        }
    }
}