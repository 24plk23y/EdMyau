package myau.module.modules;

import java.awt.Color;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.MoveInputEvent;
import myau.events.PacketEvent;
import myau.events.Render2DEvent;
import myau.events.UpdateEvent;
import myau.events.StrafeEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import myau.util.PacketUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemAppleGold;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.world.WorldSettings;
import myau.util.MoveUtil;

public class Gapple2 extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final IntProperty c03Packets = new IntProperty("c03-packets", 32, 32, 40);
    public final IntProperty health = new IntProperty("health", 12, 1, 40);
    public final BooleanProperty autoEat = new BooleanProperty("auto-eat", true);
    public final IntProperty sendDelay = new IntProperty("Send-Delay", 3, 3, 10);
    public final BooleanProperty progressBar = new BooleanProperty("progress-bar", true);
    public final BooleanProperty alwaysAttack = new BooleanProperty("always-attack", false);
    private double savedMotionX;
    private double savedMotionY;
    private double savedMotionZ;
    private boolean motionSaved;
    private boolean cancelMove;
    private int ticks;
    private int stuckTicks;
    private int pauseTicks;
    private float yaw;
    private float pitch;
    private boolean shouldEat;
    private boolean eating;
    private float lastHealth = 40.0F;
    private int gappleSlot = -1;

    public Gapple2() {
        super("Gapple-edit", false, false);
    }

@Override
public void onEnabled() {
    this.resetState();
    super.onEnabled();
}

@Override
public void onDisabled() {
    this.resetState();
    super.onDisabled();
}
    @EventTarget
    public void onStrafe(StrafeEvent event) {
        if (this.isEnabled()) {
            event.setForward(0.0f);
            event.setStrafe(0.0f);
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (this.isEnabled() && mc.thePlayer != null && event.getType() == EventType.SEND) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof C03PacketPlayer && this.cancelMove) {
                stuckTicks++;
                if (stuckTicks >= 19) {
                stuckTicks = 0;
                pauseTicks++;
                    }
            }
            if (packet instanceof C03PacketPlayer && this.cancelMove && this.ticks < (Integer)this.c03Packets.getValue()) {
                this.yaw = mc.thePlayer.rotationYaw;
                this.pitch = mc.thePlayer.rotationPitch;
                ++this.ticks;
                event.setCancelled(true);
            }
        }
        if (this.isEnabled() && mc.thePlayer != null && event.getType() == EventType.RECEIVE) {
            Packet<?> packet = event.getPacket();
            if (packet instanceof S12PacketEntityVelocity && ((S12PacketEntityVelocity) packet).getEntityID() == mc.thePlayer.getEntityId()) {
                pauseTicks++;
            }
        }
    }

    @EventTarget
    public void onMove(MoveInputEvent event) {
        if (this.isEnabled() && this.cancelMove && mc.thePlayer != null) {
            mc.thePlayer.movementInput.moveStrafe = 0.0F;
            mc.thePlayer.movementInput.moveForward = 0.0F;
            mc.thePlayer.movementInput.jump = false;
            mc.thePlayer.setSprinting(false);
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionY = 0.0;
            mc.thePlayer.motionZ = 0.0;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (mc.thePlayer != null && mc.theWorld != null && mc.thePlayer.isEntityAlive()) {
                if (this.isEnabled() && this.cancelMove && mc.thePlayer != null) {
            mc.thePlayer.movementInput.moveStrafe = 0.0F;
            mc.thePlayer.movementInput.moveForward = 0.0F;
            mc.thePlayer.movementInput.jump = false;
            mc.thePlayer.setSprinting(false);
            mc.thePlayer.motionX = 0.0;
            mc.thePlayer.motionY = 0.0;
            mc.thePlayer.motionZ = 0.0;
                }
                if (mc.playerController.getCurrentGameType() != net.minecraft.world.WorldSettings.GameType.SURVIVAL) {
                    this.resetState();
                } else {
                    this.yaw = event.getNewYaw();
                    this.pitch = event.getNewPitch();
                    this.gappleSlot = this.findGappleSlot();
                    boolean wantsEat = this.checkHealthCondition();
                    if (wantsEat && this.gappleSlot >= 0) {
                        if (this.pauseTicks == 0) {
                            this.startStuck();
                        } else {
                            this.stopStuck();
                            --this.pauseTicks;
                        }

                        if (this.ticks >= (Integer)this.c03Packets.getValue()) {
                            this.consumeGappleBurst();
                        }

                    } else {
                        this.stopStuck();
                        this.ticks = 0;
                        this.pauseTicks = 0;
                    }

                    if ((mc.thePlayer.ticksExisted % sendDelay.getValue()) == 0) {
                        PacketUtil.sendPacketNoEvent(new C03PacketPlayer.C05PacketPlayerLook(this.yaw, this.pitch, mc.thePlayer.onGround));
                        ticks--;
                    }
                }
            } else {
                this.resetState();
            }
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled() && this.eating && (Boolean)this.progressBar.getValue() && mc.currentScreen == null) {
            ScaledResolution sr = new ScaledResolution(mc);
            int width = 140;
            int barHeight = 7;
            int x = sr.getScaledWidth() / 2 - width / 2;
            int y = sr.getScaledHeight() * 3 / 4;
            float progress = Math.min(1.0F, (float)this.ticks / (float)(Integer)this.c03Packets.getValue());
            int filled = x + Math.round((float)width * progress);
            Gui.drawRect(x - 2, y - 2, x + width + 2, y + barHeight + 2, (new Color(0, 0, 0, 140)).getRGB());
            Gui.drawRect(x, y, x + width, y + barHeight, (new Color(20, 20, 20, 180)).getRGB());
            if (filled > x) {
                Gui.drawRect(x, y, filled, y + barHeight, (new Color(76, 157, 240, 220)).getRGB());
            }

            String text = "Gapple " + (int)(progress * 100.0F) + "%";
            mc.fontRendererObj.drawString(text,
                    (int)((float)sr.getScaledWidth() / 2.0F - (float)mc.fontRendererObj.getStringWidth(text) / 2.0F),
                    (int)((float)y - 10.0F),
                    -1);
        }
    }

    public boolean isEatingGapple() {
        return this.eating;
    }

    public boolean isSilentEating() {
        return this.eating;
    }

    public int getGappleSlot() {
        return this.gappleSlot;
    }

    private void startStuck() {
    if (!this.motionSaved) {
        this.savedMotionX = mc.thePlayer.motionX;
        this.savedMotionY = mc.thePlayer.motionY;
        this.savedMotionZ = mc.thePlayer.motionZ;
        this.motionSaved = true;
    }

    if (!this.cancelMove) {
        this.cancelMove = true;
        MoveUtil.lockMovement();
    }
}

    private void stopStuck() {
    if (this.cancelMove) {
        this.cancelMove = false;
        MoveUtil.unlockMovement();
    }

    if (this.motionSaved && mc.thePlayer != null) {
        mc.thePlayer.motionX = this.savedMotionX;
        mc.thePlayer.motionY = this.savedMotionY;
        mc.thePlayer.motionZ = this.savedMotionZ;
        this.motionSaved = false;
    }
}

    private void releaseBufferedPackets() {
        PacketUtil.sendPacketNoEvent(new C03PacketPlayer.C05PacketPlayerLook(this.yaw, this.pitch, mc.thePlayer.onGround));

        for(int i = 1; i < this.ticks; ++i) {
            PacketUtil.sendPacketNoEvent(new C03PacketPlayer(mc.thePlayer.onGround));
        }

    }

    private void consumeGappleBurst() {
        int currentSlot = mc.thePlayer.inventory.currentItem;
        ItemStack gappleStack = mc.thePlayer.inventory.getStackInSlot(this.gappleSlot);
        if (gappleStack != null && gappleStack.getItem() instanceof ItemAppleGold) {
            PacketUtil.sendPacketNoEvent(new C09PacketHeldItemChange(this.gappleSlot));
            PacketUtil.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(gappleStack));
            this.releaseBufferedPackets();
            PacketUtil.sendPacketNoEvent(new C09PacketHeldItemChange(currentSlot));
            ItemStack heldStack = mc.thePlayer.inventory.getStackInSlot(currentSlot);
            if (heldStack != null) {
                PacketUtil.sendPacketNoEvent(new C08PacketPlayerBlockPlacement(heldStack));
            }

            ChatUtil.sendRaw("吃了一个苹果");
            //++this.pauseTicks;
            this.ticks = 0;
        } else {
            this.gappleSlot = -1;
        }
    }

    private int findGappleSlot() {
        for(int i = 0; i < 9; ++i) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemAppleGold) {
                return i;
            }
        }

        return -1;
    }

    private boolean checkHealthCondition() {
        if (!(Boolean)this.autoEat.getValue()) {
            this.shouldEat = false;
            this.eating = false;
            return false;
        } else {
            float currentHealth = mc.thePlayer.getHealth();
            float maxHealth = mc.thePlayer.getMaxHealth();
            if (currentHealth <= (float)(Integer)this.health.getValue()) {
                this.shouldEat = true;
                this.eating = true;
            }

            this.lastHealth = currentHealth;
            return this.shouldEat;
        }
    }

    private void resetState() {
        this.stopStuck();
        this.ticks = 0;
        this.pauseTicks = 0;
        this.shouldEat = false;
        this.eating = false;
        this.gappleSlot = -1;
        this.lastHealth = 40.0F;
    }
    public boolean isStoppingMovement() {
        return this.isEnabled() && this.cancelMove;
    }

    public String[] getSuffix() {
        return this.eating ? new String[]{String.valueOf(this.ticks)} : new String[0];
    }
}
