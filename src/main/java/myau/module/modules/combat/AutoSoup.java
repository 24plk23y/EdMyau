package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.mixin.IAccessorPlayerControllerMP;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.PacketUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class AutoSoup extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final TimerUtil timer = new TimerUtil();

    private int previousSlot = -1;
    private int soupSlot = -1;
    private boolean waitingForDrop = false;

    public final IntProperty health = new IntProperty("health", 15, 0, 20);
    public final IntProperty delay = new IntProperty("delay", 150, 0, 500);
    public final BooleanProperty silent = new BooleanProperty("silent", true);
    public final ModeProperty bowl = new ModeProperty("bowl", 0, new String[]{"Drop"});

    public AutoSoup() {
        super("AutoSoup", false);
    }

    private int findSoup() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() == Items.mushroom_stew) {
                return i;
            }
        }
        return -1;
    }

    private void syncSlot() {
        ((IAccessorPlayerControllerMP) mc.playerController).callSyncCurrentPlayItem();
    }

    private void selectSlot(int slot) {
        if (silent.getValue()) {
            PacketUtil.sendPacket(new C09PacketHeldItemChange(slot));
        } else {
            mc.thePlayer.inventory.currentItem = slot;
            syncSlot();
        }
    }

    private void restoreSlot() {
        if (previousSlot == -1) {
            return;
        }

        if (silent.getValue()) {
            PacketUtil.sendPacket(new C09PacketHeldItemChange(previousSlot));
        } else {
            mc.thePlayer.inventory.currentItem = previousSlot;
            syncSlot();
        }

        previousSlot = -1;
    }

    private void useSoup(int slot) {
        ItemStack stack = mc.thePlayer.inventory.getStackInSlot(slot);
        if (stack == null || stack.getItem() != Items.mushroom_stew) {
            return;
        }

        previousSlot = mc.thePlayer.inventory.currentItem;
        soupSlot = slot;

        selectSlot(slot);

        PacketUtil.sendPacket(new C08PacketPlayerBlockPlacement(stack));

        waitingForDrop = true;
        timer.reset();
    }

    private void dropBowl() {
        if (!waitingForDrop || soupSlot == -1) {
            return;
        }

        selectSlot(soupSlot);

        PacketUtil.sendPacket(new C07PacketPlayerDigging(
                C07PacketPlayerDigging.Action.DROP_ITEM,
                BlockPos.ORIGIN,
                EnumFacing.DOWN
        ));

        waitingForDrop = false;
        soupSlot = -1;

        restoreSlot();
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            waitingForDrop = false;
            soupSlot = -1;
            previousSlot = -1;
            return;
        }

        switch (event.getType()) {
            case PRE:
                if (waitingForDrop) {
                    dropBowl();
                    return;
                }

                if (!timer.hasTimeElapsed(delay.getValue())) {
                    return;
                }

                if (mc.thePlayer.getHealth() <= health.getValue()) {
                    int slot = findSoup();
                    if (slot != -1) {
                        useSoup(slot);
                    }
                }
                break;

            case POST:
                break;
        }
    }

    public void disabled() {
        waitingForDrop = false;
        soupSlot = -1;

        if (previousSlot != -1 && mc.thePlayer != null) {
            restoreSlot();
        }

        previousSlot = -1;
    }
}