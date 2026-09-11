package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.ModeProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.util.Vec3;

public class HitSelect extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final ModeProperty mode = new ModeProperty(
            "mode",
            0,
            new String[]{"Second", "Criticals", "Wtap"}
    );

    /*
     * 只表示 HitSelect 当前是否需要临时使用 KeepSprint。
     *
     * 不再通过 toggle() 操作 KeepSprint。
     */
    private boolean forceKeepSprint = false;

    private boolean sprintState = false;

    private int blockedHits = 0;
    private int allowedHits = 0;

    public HitSelect() {
        super("HitSelect", false);
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled()) {
            return;
        }

        /*
         * 一个攻击周期结束后解除临时 KeepSprint。
         */
        if (event.getType() == EventType.POST) {
            this.resetMotion();
        }
    }

    @EventTarget(Priority.HIGHEST)
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()
                || event.getType() != EventType.SEND
                || event.isCancelled()) {
            return;
        }

        /*
         * 记录玩家当前 sprint 状态。
         */
        if (event.getPacket() instanceof C0BPacketEntityAction) {
            C0BPacketEntityAction packet =
                    (C0BPacketEntityAction) event.getPacket();

            switch (packet.getAction()) {
                case START_SPRINTING:
                    this.sprintState = true;
                    break;

                case STOP_SPRINTING:
                    this.sprintState = false;
                    break;

                default:
                    break;
            }

            return;
        }

        /*
         * 只处理攻击包。
         */
        if (!(event.getPacket() instanceof C02PacketUseEntity)) {
            return;
        }

        C02PacketUseEntity use =
                (C02PacketUseEntity) event.getPacket();

        if (use.getAction() != C02PacketUseEntity.Action.ATTACK) {
            return;
        }

        Entity target = use.getEntityFromWorld(mc.theWorld);

        if (target == null || target instanceof EntityLargeFireball) {
            return;
        }

        if (!(target instanceof EntityLivingBase)) {
            return;
        }

        EntityLivingBase living = (EntityLivingBase) target;

        boolean allow = true;

        switch (this.mode.getValue()) {
            case 0:
                allow = this.prioritizeSecondHit(
                        mc.thePlayer,
                        living
                );
                break;

            case 1:
                allow = this.prioritizeCriticalHits(
                        mc.thePlayer
                );
                break;

            case 2:
                allow = this.prioritizeWTapHits(
                        mc.thePlayer,
                        this.sprintState
                );
                break;

            default:
                break;
        }

        if (!allow) {
            event.setCancelled(true);
            this.blockedHits++;
        } else {
            this.allowedHits++;
        }
    }

    /**
     * SECOND 模式。
     */
    private boolean prioritizeSecondHit(
            EntityLivingBase player,
            EntityLivingBase target
    ) {
        /*
         * Target 正处于 hurtTime，允许攻击。
         */
        if (target.hurtTime != 0) {
            return true;
        }

        /*
         * Player 自身还没有恢复，允许攻击。
         */
        if (player.hurtTime <= player.maxHurtTime - 1) {
            return true;
        }

        /*
         * 距离太近，允许攻击。
         */
        double dist = player.getDistanceToEntity(target);

        if (dist < 2.5) {
            return true;
        }

        /*
         * 双方没有朝向彼此移动，允许攻击。
         */
        if (!this.isMovingTowards(target, player, 60.0)) {
            return true;
        }

        if (!this.isMovingTowards(player, target, 60.0)) {
            return true;
        }

        /*
         * 当前攻击被 HitSelect 拦截。
         *
         * 不再开启 KeepSprint，只通知 Mixin：
         * 当前临时需要 KeepSprint。
         */
        this.fixMotion();

        return false;
    }

    /**
     * CRITICALS 模式。
     */
    private boolean prioritizeCriticalHits(
            EntityLivingBase player
    ) {
        /*
         * 地面允许攻击。
         */
        if (player.onGround) {
            return true;
        }

        /*
         * 受伤时允许攻击。
         */
        if (player.hurtTime != 0) {
            return true;
        }

        /*
         * 下落过程中允许攻击。
         */
        if (player.fallDistance > 0.0f) {
            return true;
        }

        /*
         * 等待暴击时，临时启用 KeepSprint 行为。
         */
        this.fixMotion();

        return false;
    }

    /**
     * W-TAP 模式。
     */
    private boolean prioritizeWTapHits(
            EntityLivingBase player,
            boolean sprinting
    ) {
        /*
         * 撞墙时允许攻击。
         */
        if (player.isCollidedHorizontally) {
            return true;
        }

        /*
         * 没有按前进键，允许攻击。
         */
        if (!mc.gameSettings.keyBindForward.isKeyDown()) {
            return true;
        }

        /*
         * 已经 sprint，允许攻击。
         */
        if (sprinting) {
            return true;
        }

        /*
         * 当前攻击被拦截。
         */
        this.fixMotion();

        return false;
    }

    /**
     * 通知 Mixin 当前攻击周期临时需要 KeepSprint。
     *
     * 这里不再：
     * - toggle KeepSprint
     * - 修改 KeepSprint.slowdown
     */
    private void fixMotion() {
        this.forceKeepSprint = true;
    }

    /**
     * 解除临时 KeepSprint。
     */
    private void resetMotion() {
        this.forceKeepSprint = false;
    }

    /**
     * MixinEntityPlayer 使用。
     *
     * 如果返回 true，代表 HitSelect 当前要求
     * 临时使用 KeepSprint。
     */
    public boolean isForceKeepSprint() {
        return this.forceKeepSprint;
    }

    /**
     * 判断 source 是否朝 target 移动。
     */
    private boolean isMovingTowards(
            EntityLivingBase source,
            EntityLivingBase target,
            double maxAngle
    ) {
        Vec3 currentPos = source.getPositionVector();

        Vec3 lastPos = new Vec3(
                source.lastTickPosX,
                source.lastTickPosY,
                source.lastTickPosZ
        );

        Vec3 targetPos = target.getPositionVector();

        /*
         * 计算移动向量。
         */
        double mx = currentPos.xCoord - lastPos.xCoord;
        double mz = currentPos.zCoord - lastPos.zCoord;

        double movementLength =
                Math.sqrt(mx * mx + mz * mz);

        if (movementLength == 0.0) {
            return false;
        }

        /*
         * Normalize movement vector。
         */
        mx /= movementLength;
        mz /= movementLength;

        /*
         * 计算目标方向。
         */
        double tx = targetPos.xCoord - currentPos.xCoord;
        double tz = targetPos.zCoord - currentPos.zCoord;

        double targetLength =
                Math.sqrt(tx * tx + tz * tz);

        if (targetLength == 0.0) {
            return false;
        }

        tx /= targetLength;
        tz /= targetLength;

        /*
         * Dot product。
         */
        double dotProduct = mx * tx + mz * tz;

        /*
         * 判断角度。
         */
        return dotProduct >= Math.cos(Math.toRadians(maxAngle));
    }

    @Override
    public void onDisabled() {
        this.forceKeepSprint = false;
        this.sprintState = false;

        this.blockedHits = 0;
        this.allowedHits = 0;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{
                this.mode.getModeString()
        };
    }
}