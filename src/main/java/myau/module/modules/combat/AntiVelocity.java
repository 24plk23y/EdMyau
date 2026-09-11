package myau.module.modules;

import com.google.common.base.CaseFormat;
import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.AttackEvent;
import myau.events.KnockbackEvent;
import myau.events.LivingUpdateEvent;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class AntiVelocity extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty(
            "mode",
            0,
            new String[]{
                    "GRIM_NO_XZ",
                    "GRIM_REDUCE",
                    "GRIM_1_17_C06",
                    "CANCEL_C0F",
                    "MATRIX_REDUCE"
            }
    );

    public final IntProperty c02s = new IntProperty(
            "c02s",
            5,
            1,
            16,
            () -> this.mode.getValue() == 0
    );

    public final BooleanProperty legitSprint = new BooleanProperty(
            "legit-sprint",
            false,
            () -> this.mode.getValue() == 0
    );

    public final BooleanProperty stopSprint = new BooleanProperty(
            "stop-sprint",
            true,
            () -> this.mode.getValue() == 0
    );

    public final BooleanProperty setSprint = new BooleanProperty(
            "set-sprint",
            false,
            () -> this.mode.getValue() == 0
    );

    public final IntProperty reduceMotion = new IntProperty(
            "reduce-motion",
            5,
            1,
            16,
            () -> this.mode.getValue() == 0
    );

    public final BooleanProperty lagDebug = new BooleanProperty(
            "lag-debug",
            true
    );

    public final IntProperty cancelC0FCounts = new IntProperty(
            "cancel-c0f-counts",
            6,
            1,
            16,
            () -> this.mode.getValue() == 3
    );

    private boolean needVelocity;
    private int skipTicks;
    private int lastHurtTime;
    private Entity targetEntity;
    private double lastAttackReach;
    private int cancelC0FTicks;

    public AntiVelocity() {
        super("AntiVelocity", false);
    }

    @EventTarget
    public void onKnockback(KnockbackEvent event) {
        if (!isEnabled() || event.isCancelled()) {
            return;
        }

        switch (mode.getValue()) {
            case 0:
                targetEntity = getKillAuraTarget();
                if (targetEntity != null) {
                    needVelocity = true;
                }
                break;

            case 1:
                needVelocity = true;
                break;

            case 4:
                event.setX(event.getX() * 0.33);
                event.setZ(event.getZ() * 0.33);

                if (mc.thePlayer.onGround) {
                    event.setX(event.getX() * 0.86);
                    event.setZ(event.getZ() * 0.86);
                }
                break;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!isEnabled() || event.getType() != EventType.RECEIVE || event.isCancelled()) {
            return;
        }

        if (event.getPacket() instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity packet = (S12PacketEntityVelocity) event.getPacket();

            if (packet.getEntityID() != mc.thePlayer.getEntityId()) {
                return;
            }

            switch (mode.getValue()) {
                case 0:
                    targetEntity = getKillAuraTarget();
                    if (targetEntity != null) {
                        needVelocity = true;
                    }
                    break;

                case 2:
                    sendPositionLook();
                    sendDiggingPackets();
                    needVelocity = true;
                    event.setCancelled(true);
                    break;

                case 3:
                    cancelC0FTicks = cancelC0FCounts.getValue();
                    event.setCancelled(true);
                    break;

                case 1:
                    needVelocity = true;
                    break;
            }

            return;
        }

        if (cancelC0FTicks > 0 && event.getPacket() instanceof C0FPacketConfirmTransaction) {
            event.setCancelled(true);
            cancelC0FTicks--;
            return;
        }

        if (event.getPacket() instanceof S08PacketPlayerPosLook && lagDebug.getValue()) {
            ChatUtil.sendFormatted(
                    "Detect Lag AttackReach: " + lastAttackReach
            );
        }
    }

    @EventTarget
    public void onLivingUpdate(LivingUpdateEvent event) {
        if (!isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (mode.getValue() == 0) {
            handleGrimNoXZ();
        } else if (mode.getValue() == 2 && needVelocity) {
            sendPositionLook();
            sendDiggingPackets();
            needVelocity = false;
        }
    }

    @EventTarget
    public void onAttack(AttackEvent event) {
        if (!isEnabled() || mode.getValue() != 1 || mc.thePlayer == null) {
            return;
        }

        int hurtTime = mc.thePlayer.hurtTime;

        if (hurtTime == 0) {
            needVelocity = false;
        }

        if (needVelocity
                && hurtTime > 5
                && hurtTime != lastHurtTime
                && mc.thePlayer.onGround) {
            mc.thePlayer.jump();
            ChatUtil.sendFormatted("Player Jump");
        }

        if (!isMoving() || !mc.thePlayer.isSprinting()) {
            return;
        }

        if (hurtTime != lastHurtTime) {
            switch (hurtTime) {
                case 9:
                    mc.thePlayer.motionX *= 0.8;
                    mc.thePlayer.motionZ *= 0.8;
                    break;

                case 8:
                    mc.thePlayer.motionX *= 0.11;
                    mc.thePlayer.motionZ *= 0.11;
                    break;

                case 7:
                    mc.thePlayer.motionX *= 0.4;
                    mc.thePlayer.motionZ *= 0.4;
                    break;

                case 4:
                    mc.thePlayer.motionX *= 0.37;
                    mc.thePlayer.motionZ *= 0.37;
                    break;
            }

            lastHurtTime = hurtTime;
        }
    }

    private void handleGrimNoXZ() {
        if (!needVelocity || mc.thePlayer.hurtTime == 0 || targetEntity == null) {
            return;
        }

        boolean wasSprinting = mc.thePlayer.isSprinting();

        if (!wasSprinting && legitSprint.getValue()) {
            mc.thePlayer.sendQueue.addToSendQueue(
                    new C03PacketPlayer(mc.thePlayer.onGround)
            );
        }

        if (!wasSprinting) {
            mc.thePlayer.sendQueue.addToSendQueue(
                    new C0BPacketEntityAction(
                            mc.thePlayer,
                            C0BPacketEntityAction.Action.START_SPRINTING
                    )
            );

            if (setSprint.getValue()) {
                mc.thePlayer.setSprinting(true);
            }
        }

        for (int i = 0; i < c02s.getValue(); i++) {
            mc.thePlayer.sendQueue.addToSendQueue(
                    new C0APacketAnimation()
            );

            mc.thePlayer.sendQueue.addToSendQueue(
                    new C02PacketUseEntity(
                            targetEntity,
                            C02PacketUseEntity.Action.ATTACK
                    )
            );
        }

        if (!wasSprinting && stopSprint.getValue()) {
            mc.thePlayer.sendQueue.addToSendQueue(
                    new C0BPacketEntityAction(
                            mc.thePlayer,
                            C0BPacketEntityAction.Action.STOP_SPRINTING
                    )
            );
        }

        double multiplier = Math.pow(0.6, reduceMotion.getValue());

        mc.thePlayer.motionX *= multiplier;
        mc.thePlayer.motionZ *= multiplier;

        needVelocity = false;
    }

    private void sendPositionLook() {
        mc.thePlayer.sendQueue.addToSendQueue(
                new C03PacketPlayer.C06PacketPlayerPosLook(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY,
                        mc.thePlayer.posZ,
                        mc.thePlayer.rotationYaw,
                        mc.thePlayer.rotationPitch,
                        mc.thePlayer.onGround
                )
        );
    }

    private void sendDiggingPackets() {
        BlockPos pos = new BlockPos(mc.thePlayer).up();

        mc.thePlayer.sendQueue.addToSendQueue(
                new C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK,
                        pos,
                        EnumFacing.DOWN
                )
        );

        mc.thePlayer.sendQueue.addToSendQueue(
                new C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                        pos,
                        EnumFacing.DOWN
                )
        );
    }

    private Entity getKillAuraTarget() {
        KillAura killAura =
                (KillAura) Myau.moduleManager.getModule(KillAura.class);

        return killAura.getTarget();
    }

    private boolean isMoving() {
        return mc.thePlayer.moveForward != 0.0F
                || mc.thePlayer.moveStrafing != 0.0F;
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        onDisabled();
    }

    @Override
    public void onDisabled() {
        needVelocity = false;
        skipTicks = 0;
        lastHurtTime = 0;
        targetEntity = null;
        lastAttackReach = 0.0;
        cancelC0FTicks = 0;
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
}