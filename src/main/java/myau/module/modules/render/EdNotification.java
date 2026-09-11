package myau.module.modules;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EdNotification extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final int POS_X = 12;
    private static final int POS_Y = 12;

    private static final float WIDTH = 120.0F;
    private static final float HEIGHT = 34.0F;

    private static final long ANIMATION_TIME = 300L;
    private static final long DISPLAY_TIME = 2000L;
    private static final long LIFE_TIME = 2300L;

    private final List<Notify> notifications = new ArrayList<>();

    public EdNotification() {
        super("EdNotification", true, true);
    }

    /**
     * Called by Module when a module is enabled/disabled.
     */
    public void moduleToggle(Module module, boolean enabled) {
        if (module == this) {
            return;
        }

        this.notifications.add(
                new Notify(
                        module.getName(),
                        enabled,
                        System.currentTimeMillis()
                )
        );
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);

        if (mc.fontRendererObj == null) {
            return;
        }

        long now = System.currentTimeMillis();

        for (Iterator<Notify> iterator = notifications.iterator(); iterator.hasNext(); ) {
            Notify notify = iterator.next();

            long life = now - notify.time;

            if (life > LIFE_TIME) {
                iterator.remove();
                continue;
            }

            float targetX =
                    sr.getScaledWidth()
                            - WIDTH
                            - POS_X;

            float startX =
                    sr.getScaledWidth()
                            + WIDTH;

            float x;

            if (life < ANIMATION_TIME) {
                float progress =
                        life / (float) ANIMATION_TIME;

                x = startX
                        - (startX - targetX)
                        * easeOut(progress);

            } else if (life > DISPLAY_TIME) {
                float progress =
                        (life - DISPLAY_TIME)
                                / (float) ANIMATION_TIME;

                x = targetX
                        + (startX - targetX)
                        * easeIn(progress);

            } else {
                x = targetX;
            }

            int index = notifications.indexOf(notify);

            float targetY =
                    sr.getScaledHeight()
                            - HEIGHT
                            - POS_Y
                            - index * 38.0F;

            if (notify.y == -1.0F) {
                notify.y = targetY;
            }

            notify.y +=
                    (targetY - notify.y) * 0.25F;

            float y = notify.y;

            /*
             * Background
             */
            RenderUtil.enableRenderState();

            RenderUtil.drawRect(
                    x,
                    y,
                    x + WIDTH,
                    y + HEIGHT,
                    0xB0000000
            );

            RenderUtil.disableRenderState();

            /*
             * Module name
             */
            mc.fontRendererObj.drawStringWithShadow(
                    notify.title,
                    x + 6.0F,
                    y + 6.0F,
                    Color.WHITE.getRGB()
            );

            /*
             * Status
             */
            mc.fontRendererObj.drawStringWithShadow(
                    notify.enabled ? "Enabled" : "Disabled",
                    x + 6.0F,
                    y + 18.0F,
                    notify.enabled
                            ? 0xFF55FF55
                            : 0xFFFF5555
            );

            /*
             * Progress bar
             */
            float progress =
                    life / (float) LIFE_TIME;

            if (progress > 1.0F) {
                progress = 1.0F;
            }

            RenderUtil.enableRenderState();

            RenderUtil.drawRect(
                    x,
                    y + HEIGHT - 2.0F,
                    x + WIDTH * progress,
                    y + HEIGHT,
                    Color.WHITE.getRGB()
            );

            RenderUtil.disableRenderState();
        }
    }

    private float easeOut(float t) {
        return 1.0F
                - (float) Math.pow(1.0F - t, 3.0D);
    }

    private float easeIn(float t) {
        return (float) Math.pow(t, 3.0D);
    }

    private static class Notify {

        private final String title;
        private final boolean enabled;
        private final long time;

        private float y = -1.0F;

        private Notify(
                String title,
                boolean enabled,
                long time
        ) {
            this.title = title;
            this.enabled = enabled;
            this.time = time;
        }
    }
}