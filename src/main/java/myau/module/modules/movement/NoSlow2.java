package myau.module.modules;

import io.netty.buffer.Unpooled;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.MoveInputEvent;
import myau.events.PacketEvent;
import myau.events.UpdateEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemBucketMilk;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSword;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class NoSlow2 extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    /*
     * Sword:
     *
     * 0 NONE
     * 1 NCP
     * 2 UPDATED_NCP
     * 3 AAC5
     * 4 SWITCH_ITEM
     * 5 INVALID_C08
     * 6 POST_PLACE
     * 7 GRIM_AC
     */
    public final ModeProperty swordMode = new ModeProperty(
            "sword-mode",
            0,
            new String[]{
                    "NONE",
                    "NCP",
                    "UPDATED_NCP",
                    "AAC5",
                    "SWITCH_ITEM",
                    "INVALID_C08",
                    "POST_PLACE",
                    "GRIM_AC"
            }
    );

    /*
     * Consume:
     *
     * 0 NONE
     * 1 UPDATED_NCP
     * 2 AAC5
     * 3 SWITCH_ITEM
     * 4 INVALID_C08
     * 5 INTAVE
     * 6 INTAVE_NEW
     */
    public final ModeProperty consumeMode = new ModeProperty(
            "consume-mode",
            0,
            new String[]{
                    "NONE",
                    "UPDATED_NCP",
                    "AAC5",
                    "SWITCH_ITEM",
                    "INVALID_C08",
                    "INTAVE",
                    "INTAVE_NEW"
            }
    );

    /*
     * Bow:
     *
     * 0 NONE
     * 1 UPDATED_NCP
     * 2 AAC5
     * 3 SWITCH_ITEM
     * 4 INVALID_C08
     */
    public final ModeProperty bowMode = new ModeProperty(
            "bow-mode",
            0,
            new String[]{
                    "NONE",
                    "UPDATED_NCP",
                    "AAC5",
                    "SWITCH_ITEM",
                    "INVALID_C08"
            }
    );

    /*
     * Movement multipliers.
     */
    public final PercentProperty swordForward =
            new PercentProperty(
                    "sword-forward",
                    100,
                    () -> this.swordMode.getValue() != 0
            );

    public final PercentProperty swordStrafe =
            new PercentProperty(
                    "sword-strafe",
                    100,
                    () -> this.swordMode.getValue() != 0
            );

    public final PercentProperty consumeForward =
            new PercentProperty(
                    "consume-forward",
                    100,
                    () -> this.consumeMode.getValue() != 0
            );

    public final PercentProperty consumeStrafe =
            new PercentProperty(
                    "consume-strafe",
                    100,
                    () -> this.consumeMode.getValue() != 0
            );

    public final PercentProperty bowForward =
            new PercentProperty(
                    "bow-forward",
                    100,
                    () -> this.bowMode.getValue() != 0
            );

    public final PercentProperty bowStrafe =
            new PercentProperty(
                    "bow-strafe",
                    100,
                    () -> this.bowMode.getValue() != 0
            );

    public final BooleanProperty consumeFood =
            new BooleanProperty(
                    "consume-food",
                    true,
                    () -> this.consumeMode.getValue() != 0
            );

    public final BooleanProperty consumeDrink =
            new BooleanProperty(
                    "consume-drink",
                    true,
                    () -> this.consumeMode.getValue() != 0
            );

    /*
     * UpdatedNCP state.
     */
    private boolean shouldSwap;

    /*
     * Used to prevent multiple InvalidC08 packets
     * during the same tick.
     */
    private int lastInvalidC08Tick = -1;

    public NoSlow2() {
        super("NoSlow2", false);
    }

    /*
     * =========================================================
     * Item checks
     * =========================================================
     */

    private Item getHeldItem() {
        if (mc.thePlayer == null || mc.thePlayer.getHeldItem() == null) {
            return null;
        }

        return mc.thePlayer.getHeldItem().getItem();
    }

    private boolean isHoldingSword() {
        return getHeldItem() instanceof ItemSword;
    }

    private boolean isHoldingBow() {
        return getHeldItem() instanceof ItemBow;
    }

    private boolean isHoldingFood() {
        return getHeldItem() instanceof ItemFood;
    }

    private boolean isHoldingDrink() {
        Item item = getHeldItem();

        return item instanceof ItemPotion
                || item instanceof ItemBucketMilk;
    }

    private boolean isUsingItem() {
        return mc.thePlayer != null
                && mc.thePlayer.isUsingItem();
    }

    private boolean isUsingSword() {
        return isUsingItem() && isHoldingSword();
    }

    private boolean isUsingBow() {
        return isUsingItem() && isHoldingBow();
    }

    private boolean isConsuming() {
        return isUsingItem()
                && (isHoldingFood() || isHoldingDrink());
    }

    /*
     * =========================================================
     * Active states
     * =========================================================
     */

    private boolean isSwordActive() {
        return swordMode.getValue() != 0
                && isUsingSword();
    }

    private boolean isConsumeActive() {
        if (consumeMode.getValue() == 0 || !isConsuming()) {
            return false;
        }

        if (isHoldingFood()) {
            return (Boolean) consumeFood.getValue();
        }

        if (isHoldingDrink()) {
            return (Boolean) consumeDrink.getValue();
        }

        return false;
    }

    private boolean isBowActive() {
        return bowMode.getValue() != 0
                && isUsingBow();
    }

    /*
     * This method is called by MixinEntityPlayerSP.
     *
     * When this returns true, the Mixin makes Minecraft's
     * onLivingUpdate() think that the player is not using an item.
     *
     * This is important because otherwise vanilla 1.8.9 can
     * reset the movement/sprint state after we modify movementInput.
     */
    public boolean isActive() {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return false;
        }

        return isSwordActive()
                || isConsumeActive()
                || isBowActive();
    }

    /*
     * =========================================================
     * Movement
     * =========================================================
     *
     * IMPORTANT:
     * This uses MoveInputEvent instead of LivingUpdateEvent.
     *
     * Your Mixin fires MoveInputEvent immediately after
     * MovementInput.updatePlayerMoveState().
     */

    @EventTarget
    public void onMoveInput(MoveInputEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (isSwordActive()) {
            applyMovement(
                    swordForward.getValue(),
                    swordStrafe.getValue()
            );
            return;
        }

        if (isConsumeActive()) {
            applyMovement(
                    consumeForward.getValue(),
                    consumeStrafe.getValue()
            );
            return;
        }

        if (isBowActive()) {
            applyMovement(
                    bowForward.getValue(),
                    bowStrafe.getValue()
            );
        }
    }

    private void applyMovement(int forward, int strafe) {
        mc.thePlayer.movementInput.moveForward *=
                (float) forward / 100.0F;

        mc.thePlayer.movementInput.moveStrafe *=
                (float) strafe / 100.0F;
    }

    /*
     * =========================================================
     * Update PRE / POST
     * =========================================================
     */

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (event.getType() == EventType.PRE) {
            handlePre();
        }

        if (event.getType() == EventType.POST) {
            handlePost();
        }
    }

    private void handlePre() {

        /*
         * -----------------------------------------------------
         * UpdatedNCP
         * -----------------------------------------------------
         */

        if (shouldSwap) {
            switchItem();
            sendBlockPlacement();
            shouldSwap = false;
        }

        /*
         * -----------------------------------------------------
         * Sword NCP
         * -----------------------------------------------------
         */

        if (isSwordActive() && swordMode.getValue() == 1) {
            sendReleaseUseItem();
        }

        /*
         * -----------------------------------------------------
         * Sword PostPlace
         * -----------------------------------------------------
         */

        if (isSwordActive() && swordMode.getValue() == 6) {
            sendReleaseUseItem();
        }

        /*
         * -----------------------------------------------------
         * Sword SwitchItem
         * -----------------------------------------------------
         */

        if (isSwordActive() && swordMode.getValue() == 4) {
            switchItem();
        }

        /*
         * -----------------------------------------------------
         * Consume SwitchItem
         * -----------------------------------------------------
         */

        if (isConsumeActive() && consumeMode.getValue() == 3) {
            switchItem();
        }

        /*
         * -----------------------------------------------------
         * Bow SwitchItem
         * -----------------------------------------------------
         */

        if (isBowActive() && bowMode.getValue() == 3) {
            switchItem();
        }

        /*
         * -----------------------------------------------------
         * Intave
         * -----------------------------------------------------
         */

        if (isConsumeActive() && consumeMode.getValue() == 5) {
            sendReleaseUseItem();
        }

        /*
         * -----------------------------------------------------
         * Intave New
         * -----------------------------------------------------
         */

        if (isConsumeActive() && consumeMode.getValue() == 6) {
            sendIntaveNew();
        }

        /*
         * -----------------------------------------------------
         * Invalid C08
         * -----------------------------------------------------
         */

        if (shouldSendInvalidC08()) {
            sendInvalidC08();
        }
    }

    private void handlePost() {

        /*
         * -----------------------------------------------------
         * Sword NCP
         * -----------------------------------------------------
         */

        if (isSwordActive() && swordMode.getValue() == 1) {
            sendBlockPlacement();
        }

        /*
         * -----------------------------------------------------
         * Sword UpdatedNCP
         * -----------------------------------------------------
         */

        if (isSwordActive() && swordMode.getValue() == 2) {
            sendBlockPlacement();
        }

        /*
         * -----------------------------------------------------
         * Sword AAC5
         * -----------------------------------------------------
         */

        if (isSwordActive() && swordMode.getValue() == 3) {
            sendAAC5Placement();
        }

        /*
         * -----------------------------------------------------
         * Sword PostPlace
         * -----------------------------------------------------
         */

        if (isSwordActive() && swordMode.getValue() == 6) {
            for (int i = 0; i < 5; i++) {
                sendAAC5Placement();
            }
        }

        /*
         * -----------------------------------------------------
         * Sword GrimAC
         * -----------------------------------------------------
         */

        if (isSwordActive() && swordMode.getValue() == 7) {
            sendGrimPackets();

            for (int i = 0; i < 5; i++) {
                sendAAC5Placement();
            }
        }

        /*
         * -----------------------------------------------------
         * Consume AAC5
         * -----------------------------------------------------
         */

        if (isConsumeActive() && consumeMode.getValue() == 2) {
            sendAAC5Placement();
        }

        /*
         * -----------------------------------------------------
         * Bow UpdatedNCP
         * -----------------------------------------------------
         */

        if (isBowActive() && bowMode.getValue() == 1) {
            sendBlockPlacement();
        }

        /*
         * -----------------------------------------------------
         * Bow AAC5
         * -----------------------------------------------------
         */

        if (isBowActive() && bowMode.getValue() == 2) {
            sendAAC5Placement();
        }
    }

    /*
     * =========================================================
     * Packet Event
     * =========================================================
     */

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()
                || mc.thePlayer == null
                || event.isCancelled()) {
            return;
        }

        if (event.getType() != EventType.SEND) {
            return;
        }

        /*
         * -----------------------------------------------------
         * C08 detection
         * -----------------------------------------------------
         *
         * LiquidBounce uses placedBlockDirection == 255
         * to identify item-use C08 packets.
         */

        if (event.getPacket() instanceof C08PacketPlayerBlockPlacement) {

            C08PacketPlayerBlockPlacement packet =
                    (C08PacketPlayerBlockPlacement) event.getPacket();

            if (packet.getStack() == null) {
                return;
            }

            Item packetItem = packet.getStack().getItem();
            Item heldItem = getHeldItem();

            if (packetItem == null || heldItem == null) {
                return;
            }

            if (packetItem != heldItem) {
                return;
            }

            /*
             * Consume UpdatedNCP.
             */
            if (consumeMode.getValue() == 1
                    && (packetItem instanceof ItemFood
                    || packetItem instanceof ItemPotion
                    || packetItem instanceof ItemBucketMilk)) {

                shouldSwap = true;
            }

            /*
             * Bow UpdatedNCP.
             */
            if (bowMode.getValue() == 1
                    && packetItem instanceof ItemBow) {

                shouldSwap = true;
            }
        }
    }

    /*
     * =========================================================
     * Packet helpers
     * =========================================================
     */

    private void sendReleaseUseItem() {
        mc.getNetHandler().addToSendQueue(
                new C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                        BlockPos.ORIGIN,
                        EnumFacing.DOWN
                )
        );
    }

    private void sendBlockPlacement() {
        if (mc.thePlayer == null
                || mc.thePlayer.getHeldItem() == null) {
            return;
        }

        mc.getNetHandler().addToSendQueue(
                new C08PacketPlayerBlockPlacement(
                        BlockPos.ORIGIN,
                        255,
                        mc.thePlayer.getHeldItem(),
                        0.0F,
                        0.0F,
                        0.0F
                )
        );
    }

    private void sendAAC5Placement() {
        if (mc.thePlayer == null
                || mc.thePlayer.getHeldItem() == null) {
            return;
        }

        mc.getNetHandler().addToSendQueue(
                new C08PacketPlayerBlockPlacement(
                        new BlockPos(-1, -1, -1),
                        255,
                        mc.thePlayer.getHeldItem(),
                        0.0F,
                        0.0F,
                        0.0F
                )
        );
    }

    private void sendInvalidC08() {
        mc.getNetHandler().addToSendQueue(
                new C08PacketPlayerBlockPlacement(
                        new BlockPos(-1, -1, -1),
                        1,
                        null,
                        0.0F,
                        0.0F,
                        0.0F
                )
        );
    }

    /*
     * =========================================================
     * SwitchItem
     * =========================================================
     */

    private void switchItem() {
        if (mc.thePlayer == null) {
            return;
        }

        int currentSlot =
                mc.thePlayer.inventory.currentItem;

        int nextSlot =
                (currentSlot + 1) % 9;

        mc.getNetHandler().addToSendQueue(
                new C09PacketHeldItemChange(nextSlot)
        );

        mc.getNetHandler().addToSendQueue(
                new C09PacketHeldItemChange(currentSlot)
        );
    }

    /*
     * =========================================================
     * GrimAC
     * =========================================================
     */

    private void sendGrimPackets() {
        if (mc.thePlayer == null) {
            return;
        }

        int currentSlot =
                mc.thePlayer.inventory.currentItem;

        int nextSlot =
                (currentSlot + 1) % 8;

        mc.getNetHandler().addToSendQueue(
                new C09PacketHeldItemChange(nextSlot)
        );

        mc.getNetHandler().addToSendQueue(
                new C17PacketCustomPayload(
                        "许锦良",
                        new PacketBuffer(
                                Unpooled.buffer()
                        )
                )
        );

        mc.getNetHandler().addToSendQueue(
                new C09PacketHeldItemChange(currentSlot)
        );
    }

    /*
     * =========================================================
     * Intave New
     * =========================================================
     */

    private void sendIntaveNew() {
        if (mc.thePlayer == null) {
            return;
        }

        mc.getNetHandler().addToSendQueue(
                new C07PacketPlayerDigging(
                        C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                        new BlockPos(
                                mc.thePlayer.posX,
                                mc.thePlayer.getPositionEyes(1.0F).yCoord,
                                mc.thePlayer.posZ
                        ),
                        EnumFacing.DOWN
                )
        );
    }

    /*
     * =========================================================
     * Invalid C08
     * =========================================================
     */

    private boolean shouldSendInvalidC08() {

        if (mc.thePlayer == null) {
            return false;
        }

        boolean sword =
                isSwordActive()
                        && swordMode.getValue() == 5;

        boolean consume =
                isConsumeActive()
                        && consumeMode.getValue() == 4;

        boolean bow =
                isBowActive()
                        && bowMode.getValue() == 4;

        if (!sword && !consume && !bow) {
            return false;
        }

        /*
         * LiquidBounce:
         *
         * player.ticksExisted % 3 == 0
         */
        if (mc.thePlayer.ticksExisted % 3 != 0) {
            return false;
        }

        if (lastInvalidC08Tick ==
                mc.thePlayer.ticksExisted) {
            return false;
        }

        lastInvalidC08Tick =
                mc.thePlayer.ticksExisted;

        return true;
    }

    /*
     * =========================================================
     * Disable
     * =========================================================
     */

    @Override
    public void onDisabled() {
        shouldSwap = false;
        lastInvalidC08Tick = -1;
    }
}