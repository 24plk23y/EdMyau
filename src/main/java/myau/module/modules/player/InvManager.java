package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.UpdateEvent;
import myau.events.WindowClickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.util.ItemUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings.GameType;
import org.apache.commons.lang3.RandomUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;

public class InvManager extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private int actionDelay = 0;
    private int oDelay = 0;
    private int fishRodSlot = -1;
    private boolean inventoryOpen = false;

    private final TimerUtil autoArmorTime = new TimerUtil();

    /*
     * Mode:
     * 0 = OpenInv
     * 1 = Silent
     */
    public final ModeProperty mode = new ModeProperty(
            "mode",
            0,
            new String[]{"OpenInv", "Silent"}
    );

    public final BooleanProperty instant =
            new BooleanProperty(
                    "instant",
                    false
            );

    public final IntProperty minDelay =
            new IntProperty(
                    "min-delay",
                    1,
                    0,
                    20,
                    () -> !this.instant.getValue()
            );

    public final IntProperty maxDelay =
            new IntProperty(
                    "max-delay",
                    2,
                    0,
                    20,
                    () -> !this.instant.getValue()
            );

    /*
     * 只在 OpenInv 模式下使用。
     */
    public final IntProperty openDelay =
            new IntProperty(
                    "open-delay",
                    1,
                    0,
                    20
            );

    public final BooleanProperty autoArmor =
            new BooleanProperty(
                    "auto-armor",
                    true
            );

    public final IntProperty autoArmorInterval =
            new IntProperty(
                    "auto-armor-interval",
                    0,
                    0,
                    100,
                    this.autoArmor::getValue
            );

    public final BooleanProperty dropTrash =
            new BooleanProperty(
                    "drop-trash",
                    false
            );

    public final BooleanProperty checkDurability =
            new BooleanProperty(
                    "check-durability",
                    true
            );

    public final IntProperty swordSlot =
            new IntProperty(
                    "sword-slot",
                    1,
                    0,
                    9
            );

    public final IntProperty pickaxeSlot =
            new IntProperty(
                    "pickaxe-slot",
                    3,
                    0,
                    9
            );

    public final IntProperty shovelSlot =
            new IntProperty(
                    "shovel-slot",
                    4,
                    0,
                    9
            );

    public final IntProperty axeSlot =
            new IntProperty(
                    "axe-slot",
                    5,
                    0,
                    9
            );

    public final IntProperty blocksSlot =
            new IntProperty(
                    "blocks-slot",
                    2,
                    0,
                    9
            );

    public final IntProperty blocks =
            new IntProperty(
                    "blocks",
                    128,
                    64,
                    2304
            );

    public final IntProperty projectileSlot =
            new IntProperty(
                    "projectile-slot",
                    7,
                    0,
                    9
            );

    public final IntProperty projectiles =
            new IntProperty(
                    "projectiles",
                    64,
                    16,
                    2304
            );

    public final IntProperty goldAppleSlot =
            new IntProperty(
                    "gold-apple-slot",
                    9,
                    0,
                    9
            );

    public final IntProperty arrow =
            new IntProperty(
                    "arrow",
                    256,
                    0,
                    2304
            );

    public final IntProperty bowSlot =
            new IntProperty(
                    "bow-slot",
                    8,
                    0,
                    9
            );

    public InvManager() {
        super("InvManager", false);
    }

    /**
     * 当前是否为 Silent 模式。
     */
    private boolean isSilentMode() {
        return this.mode.getValue() == 1;
    }

    /**
     * 当前是否为 OpenInv 模式。
     */
    private boolean isOpenInvMode() {
        return this.mode.getValue() == 0;
    }

    /**
     * Module suffix。
     *
     * OpenInv -> InvManager [OpenInv]
     * Silent  -> InvManager [Silent]
     */
    @Override
    public String[] getSuffix() {
        return new String[]{
                this.mode.getModeString()
        };
    }

    /**
     * 只允许 Survival / Adventure。
     */
    private boolean isValidGameMode() {
        GameType gameType =
                mc.playerController.getCurrentGameType();

        return gameType == GameType.SURVIVAL
                || gameType == GameType.ADVENTURE;
    }

    /**
     * 判断玩家当前是否打开了自己的背包。
     *
     * OpenInv 模式使用。
     *
     * Silent 模式不会调用这个方法作为运行条件。
     */
    private boolean isPlayerInventoryOpen() {
        if (!(mc.currentScreen instanceof GuiInventory)) {
            return false;
        }

        return ((GuiInventory) mc.currentScreen)
                .inventorySlots instanceof ContainerPlayer;
    }

    /**
     * 将 PlayerInventory slot 转换成 ContainerPlayer slot。
     */
    private int convertSlotIndex(int slot) {
        if (slot >= 36) {
            return 8 - (slot - 36);
        }

        return slot <= 8
                ? slot + 36
                : slot;
    }

    /**
     * 执行 ContainerPlayer windowClick。
     */
    private void clickSlot(
            int windowId,
            int slotId,
            int mouseButtonClicked,
            int mode
    ) {
        mc.playerController.windowClick(
                windowId,
                slotId,
                mouseButtonClicked,
                mode,
                mc.thePlayer
        );
    }

    /**
     * 获取指定 PlayerInventory slot 的数量。
     */
    private int getStackSize(int slot) {
        if (slot == -1) {
            return 0;
        }

        ItemStack stack =
                mc.thePlayer.inventory.getStackInSlot(slot);

        return stack != null
                ? stack.stackSize
                : 0;
    }

    /**
     * 执行一次 InvManager action。
     *
     * @return true  = 执行了一个操作
     * @return false = 当前没有需要执行的操作
     */
    private boolean doAction() {
        ArrayList<Integer> equippedArmorSlots =
                new ArrayList<>(
                        Arrays.asList(-1, -1, -1, -1)
                );

        ArrayList<Integer> inventoryArmorSlots =
                new ArrayList<>(
                        Arrays.asList(-1, -1, -1, -1)
                );

        /*
         * 查找四种护甲。
         */
        for (int i = 0; i < 4; i++) {
            equippedArmorSlots.set(
                    i,
                    ItemUtil.findArmorInventorySlot(i, true)
            );

            inventoryArmorSlots.set(
                    i,
                    ItemUtil.findArmorInventorySlot(i, false)
            );
        }

        /*
         * Sword
         */
        int preferredSwordHotbarSlot =
                this.swordSlot.getValue() - 1;

        int inventorySwordSlot =
                ItemUtil.findSwordInInventorySlot(
                        preferredSwordHotbarSlot,
                        this.checkDurability.getValue()
                );

        if (inventorySwordSlot == -1) {
            inventorySwordSlot =
                    ItemUtil.findSwordInInventorySlot(
                            preferredSwordHotbarSlot,
                            false
                    );
        }

        /*
         * Pickaxe
         */
        int preferredPickaxeHotbarSlot =
                this.pickaxeSlot.getValue() - 1;

        int inventoryPickaxeSlot =
                ItemUtil.findInventorySlot(
                        "pickaxe",
                        preferredPickaxeHotbarSlot,
                        this.checkDurability.getValue()
                );

        if (inventoryPickaxeSlot == -1) {
            inventoryPickaxeSlot =
                    ItemUtil.findInventorySlot(
                            "pickaxe",
                            preferredPickaxeHotbarSlot,
                            false
                    );
        }

        /*
         * Shovel
         */
        int preferredShovelHotbarSlot =
                this.shovelSlot.getValue() - 1;

        int inventoryShovelSlot =
                ItemUtil.findInventorySlot(
                        "shovel",
                        preferredShovelHotbarSlot,
                        this.checkDurability.getValue()
                );

        if (inventoryShovelSlot == -1) {
            inventoryShovelSlot =
                    ItemUtil.findInventorySlot(
                            "shovel",
                            preferredShovelHotbarSlot,
                            false
                    );
        }

        /*
         * Axe
         */
        int preferredAxeHotbarSlot =
                this.axeSlot.getValue() - 1;

        int inventoryAxeSlot =
                ItemUtil.findInventorySlot(
                        "axe",
                        preferredAxeHotbarSlot,
                        this.checkDurability.getValue()
                );

        if (inventoryAxeSlot == -1) {
            inventoryAxeSlot =
                    ItemUtil.findInventorySlot(
                            "axe",
                            preferredAxeHotbarSlot,
                            false
                    );
        }

        /*
         * Blocks
         */
        int preferredBlocksHotbarSlot =
                this.blocksSlot.getValue() - 1;

        int inventoryBlocksSlot =
                ItemUtil.findInventorySlot(
                        preferredBlocksHotbarSlot,
                        ItemUtil.ItemType.Block
                );

        /*
         * Projectile
         */
        int preferredProjectileHotbarSlot =
                this.projectileSlot.getValue() - 1;

        int inventoryProjectileSlot =
                ItemUtil.findInventorySlot(
                        preferredProjectileHotbarSlot,
                        ItemUtil.ItemType.Projectile
                );

        /*
         * Fishing Rod
         */
        this.fishRodSlot =
                ItemUtil.findInventorySlot(
                        preferredProjectileHotbarSlot,
                        ItemUtil.ItemType.FishRod
                );

        /*
         * 没有投掷物时使用鱼竿。
         */
        if (inventoryProjectileSlot == -1) {
            inventoryProjectileSlot = this.fishRodSlot;
        }

        /*
         * Golden Apple
         */
        int preferredGoldAppleHotbarSlot =
                this.goldAppleSlot.getValue() - 1;

        int inventoryGoldAppleSlot =
                ItemUtil.findInventorySlot(
                        preferredGoldAppleHotbarSlot,
                        ItemUtil.ItemType.GoldApple
                );

        /*
         * Bow
         */
        int preferredBowHotbarSlot =
                this.bowSlot.getValue() - 1;

        int inventoryBowSlot =
                ItemUtil.findBowInventorySlot(
                        preferredBowHotbarSlot,
                        this.checkDurability.getValue()
                );

        if (inventoryBowSlot == -1) {
            inventoryBowSlot =
                    ItemUtil.findBowInventorySlot(
                            preferredBowHotbarSlot,
                            false
                    );
        }

        /*
         * =========================================================
         * AutoArmor
         * =========================================================
         *
         * OpenInv:
         *     打开背包后执行。
         *
         * Silent:
         *     不需要打开背包，直接执行。
         */
        if (this.autoArmor.getValue()
                && this.autoArmorTime.hasTimeElapsed(
                        this.autoArmorInterval.getValue() * 50L
                )) {

            for (int i = 0; i < 4; i++) {
                int equippedSlot =
                        equippedArmorSlots.get(i);

                int inventorySlot =
                        inventoryArmorSlots.get(i);

                if (equippedSlot != -1
                        || inventorySlot != -1) {

                    int playerArmorSlot =
                            39 - i;

                    /*
                     * 当前最佳护甲不是已经装备的位置。
                     */
                    if (equippedSlot != playerArmorSlot
                            && inventorySlot != playerArmorSlot) {

                        /*
                         * 当前护甲栏有物品。
                         */
                        if (mc.thePlayer.inventory
                                .getStackInSlot(playerArmorSlot)
                                != null) {

                            /*
                             * 有空位：
                             * Shift Click 把旧护甲放回背包。
                             */
                            if (mc.thePlayer.inventory
                                    .getFirstEmptyStack() != -1) {

                                this.clickSlot(
                                        mc.thePlayer.inventoryContainer.windowId,
                                        this.convertSlotIndex(
                                                playerArmorSlot
                                        ),
                                        0,
                                        1
                                );

                            } else {

                                /*
                                 * 没空位：
                                 * 直接丢掉旧护甲。
                                 */
                                this.clickSlot(
                                        mc.thePlayer.inventoryContainer.windowId,
                                        this.convertSlotIndex(
                                                playerArmorSlot
                                        ),
                                        1,
                                        4
                                );
                            }

                        } else {

                            /*
                             * 优先使用已经装备区域找到的最佳护甲，
                             * 否则使用普通背包中的最佳护甲。
                             */
                            int armorToEquipSlot =
                                    equippedSlot != -1
                                            ? equippedSlot
                                            : inventorySlot;

                            this.clickSlot(
                                    mc.thePlayer.inventoryContainer.windowId,
                                    this.convertSlotIndex(
                                            armorToEquipSlot
                                    ),
                                    0,
                                    1
                            );

                            this.autoArmorTime.reset();
                        }

                        return true;
                    }
                }
            }
        }

        /*
         * 防止多个物品竞争同一个 Hotbar Slot。
         */
        LinkedHashSet<Integer> usedHotbarSlots =
                new LinkedHashSet<>();

        /*
         * =========================================================
         * Sword
         * =========================================================
         */
        if (preferredSwordHotbarSlot >= 0
                && preferredSwordHotbarSlot <= 8
                && inventorySwordSlot != -1) {

            usedHotbarSlots.add(
                    preferredSwordHotbarSlot
            );

            if (inventorySwordSlot
                    != preferredSwordHotbarSlot) {

                this.clickSlot(
                        mc.thePlayer.inventoryContainer.windowId,
                        this.convertSlotIndex(
                                inventorySwordSlot
                        ),
                        preferredSwordHotbarSlot,
                        2
                );

                return true;
            }
        }

        /*
         * =========================================================
         * Pickaxe
         * =========================================================
         */
        if (preferredPickaxeHotbarSlot >= 0
                && preferredPickaxeHotbarSlot <= 8
                && !usedHotbarSlots.contains(
                        preferredPickaxeHotbarSlot
                )
                && inventoryPickaxeSlot != -1) {

            usedHotbarSlots.add(
                    preferredPickaxeHotbarSlot
            );

            if (inventoryPickaxeSlot
                    != preferredPickaxeHotbarSlot) {

                this.clickSlot(
                        mc.thePlayer.inventoryContainer.windowId,
                        this.convertSlotIndex(
                                inventoryPickaxeSlot
                        ),
                        preferredPickaxeHotbarSlot,
                        2
                );

                return true;
            }
        }

        /*
         * =========================================================
         * Shovel
         * =========================================================
         */
        if (preferredShovelHotbarSlot >= 0
                && preferredShovelHotbarSlot <= 8
                && !usedHotbarSlots.contains(
                        preferredShovelHotbarSlot
                )
                && inventoryShovelSlot != -1) {

            usedHotbarSlots.add(
                    preferredShovelHotbarSlot
            );

            if (inventoryShovelSlot
                    != preferredShovelHotbarSlot) {

                this.clickSlot(
                        mc.thePlayer.inventoryContainer.windowId,
                        this.convertSlotIndex(
                                inventoryShovelSlot
                        ),
                        preferredShovelHotbarSlot,
                        2
                );

                return true;
            }
        }

        /*
         * =========================================================
         * Axe
         * =========================================================
         */
        if (preferredAxeHotbarSlot >= 0
                && preferredAxeHotbarSlot <= 8
                && !usedHotbarSlots.contains(
                        preferredAxeHotbarSlot
                )
                && inventoryAxeSlot != -1) {

            usedHotbarSlots.add(
                    preferredAxeHotbarSlot
            );

            if (inventoryAxeSlot
                    != preferredAxeHotbarSlot) {

                this.clickSlot(
                        mc.thePlayer.inventoryContainer.windowId,
                        this.convertSlotIndex(
                                inventoryAxeSlot
                        ),
                        preferredAxeHotbarSlot,
                        2
                );

                return true;
            }
        }

        /*
         * =========================================================
         * Blocks
         * =========================================================
         */
        if (preferredBlocksHotbarSlot >= 0
                && preferredBlocksHotbarSlot <= 8
                && !usedHotbarSlots.contains(
                        preferredBlocksHotbarSlot
                )
                && inventoryBlocksSlot != -1) {

            usedHotbarSlots.add(
                    preferredBlocksHotbarSlot
            );

            if (inventoryBlocksSlot
                    != preferredBlocksHotbarSlot) {

                this.clickSlot(
                        mc.thePlayer.inventoryContainer.windowId,
                        this.convertSlotIndex(
                                inventoryBlocksSlot
                        ),
                        preferredBlocksHotbarSlot,
                        2
                );

                return true;
            }
        }

        /*
         * =========================================================
         * Projectile / Fishing Rod
         * =========================================================
         */
        if (preferredProjectileHotbarSlot >= 0
                && preferredProjectileHotbarSlot <= 8
                && !usedHotbarSlots.contains(
                        preferredProjectileHotbarSlot
                )
                && inventoryProjectileSlot != -1) {

            usedHotbarSlots.add(
                    preferredProjectileHotbarSlot
            );

            if (inventoryProjectileSlot
                    != preferredProjectileHotbarSlot) {

                this.clickSlot(
                        mc.thePlayer.inventoryContainer.windowId,
                        this.convertSlotIndex(
                                inventoryProjectileSlot
                        ),
                        preferredProjectileHotbarSlot,
                        2
                );

                return true;
            }
        }

        /*
         * =========================================================
         * Golden Apple
         * =========================================================
         */
        if (preferredGoldAppleHotbarSlot >= 0
                && preferredGoldAppleHotbarSlot <= 8
                && !usedHotbarSlots.contains(
                        preferredGoldAppleHotbarSlot
                )
                && inventoryGoldAppleSlot != -1) {

            usedHotbarSlots.add(
                    preferredGoldAppleHotbarSlot
            );

            if (inventoryGoldAppleSlot
                    != preferredGoldAppleHotbarSlot) {

                this.clickSlot(
                        mc.thePlayer.inventoryContainer.windowId,
                        this.convertSlotIndex(
                                inventoryGoldAppleSlot
                        ),
                        preferredGoldAppleHotbarSlot,
                        2
                );

                return true;
            }
        }

        /*
         * =========================================================
         * Bow
         * =========================================================
         */
        if (preferredBowHotbarSlot >= 0
                && preferredBowHotbarSlot <= 8
                && !usedHotbarSlots.contains(
                        preferredBowHotbarSlot
                )
                && inventoryBowSlot != -1) {

            usedHotbarSlots.add(
                    preferredBowHotbarSlot
            );

            if (inventoryBowSlot
                    != preferredBowHotbarSlot) {

                this.clickSlot(
                        mc.thePlayer.inventoryContainer.windowId,
                        this.convertSlotIndex(
                                inventoryBowSlot
                        ),
                        preferredBowHotbarSlot,
                        2
                );

                return true;
            }
        }

        /*
         * =========================================================
         * Drop Trash
         * =========================================================
         */
        if (this.dropTrash.getValue()) {
            int currentBlockCount =
                    this.getStackSize(
                            inventoryBlocksSlot
                    );

            int currentProjectileCount =
                    this.getStackSize(
                            inventoryProjectileSlot
                    );

            for (int i = 0; i < 36; i++) {

                if (!equippedArmorSlots.contains(i)
                        && !inventoryArmorSlots.contains(i)
                        && inventorySwordSlot != i
                        && inventoryPickaxeSlot != i
                        && inventoryShovelSlot != i
                        && inventoryAxeSlot != i
                        && inventoryBlocksSlot != i
                        && inventoryProjectileSlot != i
                        && this.fishRodSlot != i
                        && inventoryGoldAppleSlot != i
                        && inventoryBowSlot != i) {

                    ItemStack stack =
                            mc.thePlayer.inventory
                                    .getStackInSlot(i);

                    if (stack != null) {

                        boolean isBlock =
                                ItemUtil.isBlock(stack);

                        boolean isProjectile =
                                ItemUtil.isProjectile(stack);

                        if (isBlock) {
                            currentBlockCount +=
                                    stack.stackSize;
                        }

                        if (isProjectile) {
                            currentProjectileCount +=
                                    stack.stackSize;
                        }

                        boolean shouldDrop =
                                isBlock
                                        ? currentBlockCount
                                        > this.blocks.getValue()

                                        : isProjectile
                                        ? currentProjectileCount
                                        > this.projectiles.getValue()

                                        : ItemUtil.isNotSpecialItem(
                                                stack
                                        );

                        if (shouldDrop) {

                            this.clickSlot(
                                    mc.thePlayer.inventoryContainer.windowId,
                                    this.convertSlotIndex(i),
                                    1,
                                    4
                            );

                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {

        if (event.getType() != EventType.PRE) {
            return;
        }

        /*
         * 普通 action delay。
         */
        if (this.actionDelay > 0) {
            this.actionDelay--;
        }

        /*
         * OpenInv 的 open-delay。
         */
        if (this.oDelay > 0) {
            this.oDelay--;
        }

        /*
         * =========================================================
         * OpenInv Mode
         * =========================================================
         *
         * 必须打开自己的背包。
         */
        if (this.isOpenInvMode()) {

            if (!this.isPlayerInventoryOpen()) {
                this.inventoryOpen = false;
                return;
            }

            /*
             * 每次重新打开背包时初始化。
             */
            if (!this.inventoryOpen) {
                this.inventoryOpen = true;

                this.oDelay =
                        this.openDelay.getValue() + 1;

                this.autoArmorTime.reset();
            }

            /*
             * 等待 open-delay。
             */
            if (this.oDelay > 0) {
                return;
            }
        }

        /*
         * =========================================================
         * Silent Mode
         * =========================================================
         *
         * 完全不检查 currentScreen。
         *
         * 不需要打开背包 GUI。
         *
         * 不使用 open-delay。
         */
        else {

            this.inventoryOpen = true;
        }

        /*
         * 游戏模式检查。
         */
        if (!this.isEnabled()
                || !this.isValidGameMode()) {
            return;
        }

        /*
         * =========================================================
         * Normal Action
         * =========================================================
         *
         * instant = false:
         * 每次 Update 最多执行一个 action。
         */
        if (!this.instant.getValue()) {

            if (this.actionDelay > 0) {
                return;
            }

            this.doAction();
            return;
        }

        /*
         * =========================================================
         * Instant Action
         * =========================================================
         *
         * 一次 Update 连续执行多个 action。
         */
        final int maxActions = 100;

        for (int i = 0; i < maxActions; i++) {

            /*
             * OpenInv 模式：
             * 如果整理过程中关闭背包，立即停止。
             *
             * Silent 模式：
             * 永远不检查 GUI。
             */
            if (this.isOpenInvMode()) {

                if (!this.isPlayerInventoryOpen()) {
                    this.inventoryOpen = false;
                    break;
                }
            }

            boolean acted =
                    this.doAction();

            if (!acted) {
                break;
            }
        }
    }

    @EventTarget
    public void onClick(WindowClickEvent event) {

        /*
         * Instant 模式不使用 actionDelay。
         */
        if (this.instant.getValue()) {
            this.actionDelay = 0;
            return;
        }

        /*
         * OpenInv / Silent 都使用相同的随机 delay。
         *
         * Silent 和 OpenInv 的唯一核心区别：
         * 是否需要打开背包 GUI。
         */
        this.actionDelay =
                RandomUtils.nextInt(
                        this.minDelay.getValue() + 1,
                        this.maxDelay.getValue() + 2
                );
    }

    @Override
    public void verifyValue(String mode) {

        switch (mode) {

            case "min-delay":

                if (this.minDelay.getValue()
                        > this.maxDelay.getValue()) {

                    this.maxDelay.setValue(
                            this.minDelay.getValue()
                    );
                }

                break;

            case "max-delay":

                if (this.minDelay.getValue()
                        > this.maxDelay.getValue()) {

                    this.minDelay.setValue(
                            this.maxDelay.getValue()
                    );
                }

                break;
        }
    }
}