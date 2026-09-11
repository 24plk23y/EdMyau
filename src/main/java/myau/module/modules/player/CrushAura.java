package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.IntProperty;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CrushAura extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty range =
            new IntProperty("range", 5, 1, 8);

    public final IntProperty delay =
            new IntProperty("delay", 10, 1, 30);

    private final Map<UUID, Long> blacklist =
            new HashMap<>();

    public CrushAura() {
        super("CrushAura", false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() != EventType.PRE) {
            return;
        }

        if (!this.isEnabled()) {
            return;
        }

        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        long now = System.currentTimeMillis();

        // 清理已经过期的冷却
        blacklist.entrySet().removeIf(entry ->
                now - entry.getValue() >= delay.getValue() * 1000L
        );

        EntityAnimal target = null;

        for (Entity entity : mc.theWorld.loadedEntityList) {

            if (!(entity instanceof EntityAnimal)) {
                continue;
            }

            EntityAnimal animal = (EntityAnimal) entity;

            if (!isValidTarget(animal)) {
                continue;
            }

            target = animal;
            break;
        }

        if (target != null) {
            feed(target);
        }
    }

    private boolean isValidTarget(EntityAnimal animal) {

        // 距离
        if (mc.thePlayer.getDistanceToEntity(animal)
                > range.getValue()) {
            return false;
        }

        // 只处理成年动物
        if (animal.getGrowingAge() != 0) {
            return false;
        }

        // 已经进入繁殖状态
        if (animal.isInLove()) {
            return false;
        }

        // 冷却中
        if (isBlocked(animal.getUniqueID())) {
            return false;
        }

        // 快捷栏没有对应食物
        return findBreedingSlot(animal) != -1;
    }

    private void feed(EntityAnimal animal) {

        if (mc.thePlayer == null) {
            return;
        }

        int oldSlot =
                mc.thePlayer.inventory.currentItem;

        int slot = findBreedingSlot(animal);

        if (slot == -1) {
            return;
        }

        // 切换到繁殖物品
        mc.thePlayer.inventory.currentItem = slot;

        // 对动物进行交互
        mc.playerController.interactWithEntitySendPacket(
                mc.thePlayer,
                animal
        );

        // 恢复原来的 Slot
        mc.thePlayer.inventory.currentItem = oldSlot;

        // 加入冷却
        blacklist.put(
                animal.getUniqueID(),
                System.currentTimeMillis()
        );
    }

    private int findBreedingSlot(EntityAnimal animal) {

        if (mc.thePlayer == null) {
            return -1;
        }

        for (int i = 0; i < 9; i++) {

            ItemStack stack =
                    mc.thePlayer.inventory.getStackInSlot(i);

            if (stack == null) {
                continue;
            }

            if (animal.isBreedingItem(stack)) {
                return i;
            }
        }

        return -1;
    }

    private boolean isBlocked(UUID uuid) {

        Long time = blacklist.get(uuid);

        if (time == null) {
            return false;
        }

        return System.currentTimeMillis() - time
                < delay.getValue() * 1000L;
    }

    @Override
    public void onEnabled() {
        blacklist.clear();
    }

    @Override
    public String[] getSuffix() {
        return new String[]{
                range.getValue().toString()
        };
    }
}