package myau.module.modules;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.MoveInputEvent;
import myau.events.UpdateEvent;
import myau.management.RotationState;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.util.MoveUtil;
import myau.util.PacketUtil;
import myau.util.RotationUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemEgg;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Comparator;

import static myau.util.BadPacketUtil.bad;

public class AutoProjectiles extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty minRange = new FloatProperty("MinRange", 3.0f, 2.0f, 6.0f);
    public final FloatProperty maxRange = new FloatProperty("MaxRange", 8.0f, 3.0f, 15.0f);
    public final BooleanProperty smartDelay = new BooleanProperty("smart-delay", true);
    public final IntProperty throwDelay = new IntProperty("throw-delay", 3, 1, 15, () -> !smartDelay.getValue());
    public final IntProperty fov = new IntProperty("fov", 90, 30, 360);
    public final BooleanProperty rotation = new BooleanProperty("rotation", true);
    public final BooleanProperty prediction = new BooleanProperty("Prediction", true);
    public final FloatProperty predictSize = new FloatProperty("PredictSize", 1.0f, 0.0f, 5.0f);
    public final BooleanProperty silentSwitch = new BooleanProperty("Silent-Switch", true);

    private EntityLivingBase target = null;
    private int lastSlot = -1;
    private long lastThrowTime = 0L;
    private int throwState = 0;
    private int throwsRemaining = 0;
    private boolean hasRotated = false;
    public AutoProjectiles() {
        super("AutoProjectiles", false);
    }

    private boolean isEntityHeightVisible(EntityLivingBase entity) {
        Vec3 eyePos = mc.thePlayer.getPositionEyes(1.0f);
        Vec3 top = new Vec3(entity.posX, entity.posY + entity.height, entity.posZ);
        Vec3 bottom = new Vec3(entity.posX, entity.posY, entity.posZ);
        return mc.theWorld.rayTraceBlocks(eyePos, top) == null || mc.theWorld.rayTraceBlocks(eyePos, bottom) == null;
    }

    private boolean isValidTarget(EntityLivingBase entity) {
        if (entity == mc.thePlayer || entity.deathTime > 0) {
            return false;
        }
        if (!(entity instanceof EntityOtherPlayerMP)) {
            return false;
        }
        double distance = mc.thePlayer.getDistanceToEntity(entity);
        if (distance > this.maxRange.getValue()) {
            return false;
        }
        EntityPlayer player = (EntityPlayer) entity;
        if (TeamUtil.isFriend(player)) {
            return false;
        }
        if (TeamUtil.isBot(player)) {
            return false;
        }
        if (!isEntityHeightVisible(entity)) {
            return false;
        }
        if (RotationUtil.angleToEntity(player) > (float) this.fov.getValue()) {
            return false;
        }
        return !TeamUtil.isSameTeam(player);
    }

    private int getThrowDelay() {
        if (!this.smartDelay.getValue()) {
            return this.throwDelay.getValue();
        }

        EntityLivingBase target = this.getTarget();
        if (target == null) {
            return this.throwDelay.getValue();
        }

        if (mc.gameSettings.keyBindBack.isKeyDown()) {
            return 1;
        }

        double distance = mc.thePlayer.getDistanceToEntity(target);

        if (distance <= 4.5) {
            return 1;
        }
        if (distance <= 6.0) {
            return 2;
        }
        if (distance <= 8.0) {
            return 3;
        }
        if (distance <= 9.0) {
            return 5;
        }
        if (distance <= 15.0) {
            return 8;
        }
        return 20;
    }

    private EntityLivingBase getTarget() {
        ArrayList<EntityLivingBase> targets = new ArrayList<>();

        for (Object obj : mc.theWorld.loadedEntityList) {
            if (obj instanceof EntityLivingBase) {
                EntityLivingBase entity = (EntityLivingBase) obj;
                if (isValidTarget(entity)) {
                    targets.add(entity);
                }
            }
        }

        if (targets.isEmpty()) {
            return null;
        }

        targets.sort(Comparator.comparingDouble(entity -> mc.thePlayer.getDistanceToEntity(entity)));
        return targets.get(0);
    }

    public boolean hasProjectile() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (isProjectile(stack)) {
                return true;
            }
        }
        return false;
    }

    private boolean isProjectile(ItemStack stack) {
        if (stack == null) {
            return false;
        }

        Item item = stack.getItem();
        return item instanceof ItemSnowball || item instanceof ItemEgg;
    }

    private int getProjectileSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (isProjectile(stack)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Kotlin faceTrajectory() 的目标位置预测。
     *
     * Prediction = false:
     *     使用目标当前位置。
     *
     * Prediction = true:
     *     当前坐标 + (当前坐标 - 上一 tick 坐标) * predictSize
     */
    private Vec3 getPredictedTargetPosition(EntityLivingBase target) {
        double x = target.posX;
        double y = target.posY;
        double z = target.posZ;

        if (prediction.getValue()) {
            x += (target.posX - target.prevPosX) * predictSize.getValue();
            y += (target.posY - target.prevPosY) * predictSize.getValue();
            z += (target.posZ - target.prevPosZ) * predictSize.getValue();
        }

        // 与 Kotlin faceTrajectory 的 target.entityBoundingBox.minY + eyeHeight - 0.15 对应
        y += target.getEyeHeight() - 0.15;

        return new Vec3(x, y, z);
    }

    /**
     * Kotlin faceTrajectory() 中对自身位置的预测。
     */
    private Vec3 getPredictedPlayerEyes() {
        double x = mc.thePlayer.posX;
        double y = mc.thePlayer.posY;
        double z = mc.thePlayer.posZ;

        if (prediction.getValue()) {
            x += mc.thePlayer.posX - mc.thePlayer.prevPosX;
            y += mc.thePlayer.posY - mc.thePlayer.prevPosY;
            z += mc.thePlayer.posZ - mc.thePlayer.prevPosZ;
        }

        return new Vec3(
                x,
                y + mc.thePlayer.getEyeHeight(),
                z
        );
    }

    /**
     * 计算雪球/鸡蛋的真实抛物线角度。
     *
     * Minecraft 1.8:
     * 初速度约 1.5
     * 重力约 0.03
     *
     * 返回:
     * [yaw, pitch]
     */
    private float[] getTrajectoryRotations(EntityLivingBase target) {
        Vec3 targetPos = getPredictedTargetPosition(target);
        Vec3 playerEyes = getPredictedPlayerEyes();

        double posX = targetPos.xCoord - playerEyes.xCoord;
        double posY = targetPos.yCoord - playerEyes.yCoord;
        double posZ = targetPos.zCoord - playerEyes.zCoord;

        double horizontalDistance = Math.sqrt(posX * posX + posZ * posZ);

        float yaw = (float) (Math.atan2(posZ, posX) * 180.0 / Math.PI) - 90.0F;

        if (horizontalDistance < 0.001) {
            return new float[]{yaw, posY > 0 ? -90.0F : 90.0F};
        }

        final double velocity = 1.5;
        final double gravity = 0.03;

        double velocitySquared = velocity * velocity;

        double discriminant = velocitySquared * velocitySquared
                - gravity * (gravity * horizontalDistance * horizontalDistance
                + 2.0 * posY * velocitySquared);

        double pitch;

        if (discriminant >= 0.0) {
            double sqrt = Math.sqrt(discriminant);

            // Low-angle trajectory，和 Kotlin faceTrajectory 使用的较低弹道解对应
            double tanTheta = (velocitySquared - sqrt) / (gravity * horizontalDistance);
            pitch = -Math.atan(tanTheta) * 180.0 / Math.PI;
        } else {
            // 超出弹道范围时，退回普通直线瞄准，避免 NaN
            pitch = -Math.atan2(posY, horizontalDistance) * 180.0 / Math.PI;
        }

        return new float[]{yaw, (float) pitch};
    }

    private long calculateSmartDelay() {
        if (target == null) {
            return 800L;
        }

        double distance = mc.thePlayer.getDistanceToEntity(target);

        if (distance <= 3.5) {
            return 0L;
        } else if (distance <= 3.8) {
            return 20L;
        } else if (distance <= 4.0) {
            return 70L;
        } else if (distance <= 4.5) {
            return 100L;
        } else if (distance <= 5.0) {
            return 200L;
        } else if (distance <= 10.0) {
            return 500L;
        } else {
            return 800L;
        }
    }

    private void switchToProjectile() {
        int projectileSlot = getProjectileSlot();

        if (projectileSlot != -1) {
            lastSlot = mc.thePlayer.inventory.currentItem;

            if (silentSwitch.getValue()) {
                PacketUtil.sendPacket(new C09PacketHeldItemChange(projectileSlot));
            } else {
                mc.thePlayer.inventory.currentItem = projectileSlot;
            }
        }
    }

    private void switchBack() {
        if (lastSlot != -1) {
            if (silentSwitch.getValue()) {
                PacketUtil.sendPacket(new C09PacketHeldItemChange(lastSlot));
            } else {
                mc.thePlayer.inventory.currentItem = lastSlot;
            }

            lastSlot = -1;
        }
    }

    private void throwProjectile() {
        int projectileSlot = getProjectileSlot();

        if (projectileSlot != -1) {
            ItemStack projectileStack = mc.thePlayer.inventory.getStackInSlot(projectileSlot);

            if (isProjectile(projectileStack)) {
                PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(projectileStack));
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onUpdate(UpdateEvent event) {
        if (!isEnabled() || event.getType() != EventType.PRE || bad()) {
            return;
        }

        if (!hasProjectile()) {
            target = null;
            throwState = 0;
            throwsRemaining = 0;
            hasRotated = false;
            switchBack();
            return;
        }

        if (throwState == 0) {
            if (System.currentTimeMillis() - lastThrowTime < getThrowDelay() * 50L) {
                return;
            }

            target = getTarget();

            if (target == null) {
                return;
            }

            KillAura killAura = (KillAura) Myau.moduleManager.getModule(KillAura.class);

            if (killAura != null && killAura.isEnabled()) {
                double distance = mc.thePlayer.getDistanceToEntity(target);

                if (distance <= minRange.getValue()) {
                    return;
                }
            }

            if (System.currentTimeMillis() - lastThrowTime < calculateSmartDelay()) {
                return;
            }

            throwsRemaining = 1;
            throwState = 1;
            hasRotated = false;
        }

        if (throwState == 1) {
            switchToProjectile();
            throwState = 2;
        } else if (throwState == 2) {
            if (throwsRemaining > 0) {
                if (rotation.getValue()) {
                    float[] rotations = getTrajectoryRotations(target);

                    event.setRotation(rotations[0], rotations[1], 2);
                    event.setPervRotation(rotations[0], 2);

                    hasRotated = true;
                } else {
                    hasRotated = false;
                }

                throwState = 3;
            } else {
                throwState = 4;
            }
        } else if (throwState == 3) {
            throwProjectile();
            throwsRemaining--;

            if (throwsRemaining > 0) {
                throwState = 2;
            } else {
                throwState = 4;
            }
        } else if (throwState == 4) {
            switchBack();
            target = null;
            throwState = 0;
            hasRotated = false;
            lastThrowTime = System.currentTimeMillis();
        }
    }

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (hasRotated && RotationState.isActived()
                && RotationState.getPriority() == 2.0F
                && MoveUtil.isForwardPressed()) {
            MoveUtil.fixStrafe(RotationState.getSmoothedYaw());
        }
    }

    @Override
    public void onEnabled() {
        target = null;
        lastSlot = -1;
        lastThrowTime = 0L;
        throwState = 0;
        throwsRemaining = 0;
        hasRotated = false;
    }

    @Override
    public void onDisabled() {
        switchBack();
        target = null;
        throwState = 0;
        throwsRemaining = 0;
        hasRotated = false;
    }
}    
