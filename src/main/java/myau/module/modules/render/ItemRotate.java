package myau.module.modules;

import myau.event.EventTarget;
import myau.events.TickEvent;
import myau.event.types.EventType;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;

public class ItemRotate extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    public final FloatProperty speed =
            new FloatProperty("speed", 2.00F, -10.00F, 10.00F);

    public final BooleanProperty ignoreBlocking =
            new BooleanProperty("ignore-blocking", true);

    /*
     * 旋转开始的时间。
     *
     * 原本是每 Tick 更新 rotation，
     * 现在改成根据时间在渲染时计算。
     */
    private long rotationStartTime = System.nanoTime();

    private boolean blockingLastTick = false;

    public ItemRotate() {
        super("ItemRotate", false, false);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!isEnabled()) {
            return;
        }

        if (event.getType() != EventType.POST) {
            return;
        }

        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        boolean blocking = mc.thePlayer.isBlocking();

        if (ignoreBlocking.getValue() && blocking) {

            // 刚开始格挡，重置旋转
            if (!blockingLastTick) {
                rotationStartTime = System.nanoTime();
            }

            blockingLastTick = true;
            return;
        }

        // 刚结束格挡，重新开始计算旋转
        if (blockingLastTick) {
            rotationStartTime = System.nanoTime();
        }

        blockingLastTick = false;
    }

    @Override
    public void onEnabled() {
        rotationStartTime = System.nanoTime();
        blockingLastTick = false;
    }

    @Override
    public void onDisabled() {
        rotationStartTime = System.nanoTime();
        blockingLastTick = false;
    }

    /**
     * 获取当前旋转角度。
     *
     * Speed = 2.0
     * → 200 degrees / second
     *
     * 与原来的：
     *
     * rotation += speed * 100 * delta
     *
     * 保持相同速度。
     */
    public float getRotation() {
        if (!isEnabled()) {
            return 0.0F;
        }

        if (mc.thePlayer == null || mc.theWorld == null) {
            return 0.0F;
        }

        if (ignoreBlocking.getValue()
                && mc.thePlayer.isBlocking()) {
            return 0.0F;
        }

        float elapsed =
                (System.nanoTime() - rotationStartTime)
                        / 1_000_000_000.0F;

        float rotation =
                speed.getValue() * 100.0F * elapsed;

        rotation %= 360.0F;

        if (rotation < 0.0F) {
            rotation += 360.0F;
        }

        return rotation;
    }

    public boolean shouldRotate() {
        if (!isEnabled()) {
            return false;
        }

        if (mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }

        if (ignoreBlocking.getValue()
                && mc.thePlayer.isBlocking()) {
            return false;
        }

        return mc.gameSettings.thirdPersonView == 0;
    }
}