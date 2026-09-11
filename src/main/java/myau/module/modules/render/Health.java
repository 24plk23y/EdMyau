package myau.module.modules;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.mixin.IAccessorGuiChat;
import myau.util.RenderUtil;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.potion.Potion;
import net.minecraft.util.MathHelper;
import myau.mixin.IAccessorGuiContainer;
import myau.mixin.IAccessorGuiInGame;

import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Random;

public class Health extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    private static final DecimalFormat decimalFormat =
            new DecimalFormat(
                    "0.#",
                    DecimalFormatSymbols.getInstance(Locale.ENGLISH)
            );

    private static final Random random = new Random();

    public Health() {
        super("Health", false, false);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        renderHealth();
    }

    private void renderHealth() {

        if (mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        final net.minecraft.client.entity.EntityPlayerSP player =
                mc.thePlayer;

        final net.minecraft.world.World world =
                mc.theWorld;

        final GuiScreen screen =
                mc.currentScreen;

        final ScaledResolution scaledResolution =
                new ScaledResolution(mc);

        /*
         * ==========================================
         * Absorption Health
         * ==========================================
         */

        final float absorptionHealth =
                player.getAbsorptionAmount();

        /*
         * ==========================================
         * Health String
         * ==========================================
         */

        StringBuilder healthString =
                new StringBuilder();

        healthString.append(
                decimalFormat.format(
                        player.getHealth() / 2.0F
                )
        );

        healthString.append("§c❤ ");

        if (absorptionHealth > 0.0F) {

            healthString.append("§e");

            healthString.append(
                    decimalFormat.format(
                            absorptionHealth / 2.0F
                    )
            );

            healthString.append("§6❤");
        }

        /*
         * ==========================================
         * GUI Offset
         * ==========================================
         */

        int offsetY = 0;

        if (screen instanceof GuiInventory) {

            offsetY = 70;

        } else if (screen instanceof GuiContainerCreative) {

            offsetY = 80;

        } else if (screen instanceof GuiChest) {

            /*
             * 假设你的项目存在：
             *
             * IAccessorGuiContainer
             *
             * 并且有：
             *
             * getYSize()
             */

            offsetY =
                    ((IAccessorGuiContainer) screen)
                            .getYSize() / 2 - 15;
        }

        /*
         * ==========================================
         * Health Color
         * ==========================================
         */

        final float healthRatio;

        if (player.getMaxHealth() > 0.0F) {

            healthRatio =
                    Math.max(
                            0.0F,
                            Math.min(
                                    1.0F,
                                    player.getHealth()
                                            / player.getMaxHealth()
                            )
                    );

        } else {

            healthRatio = 0.0F;
        }

        final Color color =
                getHealthColor(healthRatio);

        /*
         * ==========================================
         * Text Position
         * ==========================================
         */

        final float x =
                scaledResolution.getScaledWidth()
                        / 2.0F
                        - 3.0F;

        final float textX =
                absorptionHealth > 0.0F
                        ? x - 15.5F
                        : x - 3.5F;

        final float textY =
                scaledResolution.getScaledHeight()
                        / 2.0F
                        + 25.0F
                        + offsetY;

        mc.fontRendererObj.drawString(
                healthString.toString(),
                textX,
                textY,
                color.getRGB(),
                true
        );

        /*
         * ==========================================
         * Vanilla Heart Rendering
         * ==========================================
         */

        GL11.glPushMatrix();

        try {

            /*
             * Vanilla GUI icons texture
             */

            mc.getTextureManager().bindTexture(
                    Gui.icons
            );

            /*
             * ==========================================
             * Update Counter
             * ==========================================
             */

            final int updateCounter =
        ((IAccessorGuiInGame) mc.ingameGUI)
                .getUpdateCounter();

            random.setSeed(
                    (long) updateCounter * 312871L
            );

            /*
             * ==========================================
             * Accessor
             * ==========================================
             *
             * 暂时假设你的项目存在：
             *
             * IAccessorGuiInGame
             *
             * getLastPlayerHealth()
             * getHealthUpdateCounter()
             */

            final IAccessorGuiInGame guiAccessor =
                    (IAccessorGuiInGame) mc.ingameGUI;

            final float lastPlayerHealth =
                    guiAccessor.getLastPlayerHealth();

            final long healthUpdateCounter =
                    guiAccessor.getHealthUpdateCounter();

            /*
             * ==========================================
             * Max Health
             * ==========================================
             */

            final float maxHealth =
                    player.getMaxHealth();

            /*
             * ==========================================
             * Current Health
             * ==========================================
             */

            final int healthInt =
                    MathHelper.ceiling_float_int(
                            player.getHealth()
                    );

            /*
             * ==========================================
             * Damage Animation
             * ==========================================
             */

            final long healthDifference =
                    healthUpdateCounter
                            - updateCounter;

            final boolean flag =
                    healthUpdateCounter > updateCounter
                            && healthDifference / 3L % 2L == 1L;

            /*
             * ==========================================
             * Regeneration Animation
             * ==========================================
             */

            int regenerationIndex = -1;

            if (player.isPotionActive(
                    Potion.regeneration
            )) {

                regenerationIndex =
                        updateCounter %
                                MathHelper.ceiling_float_int(
                                        maxHealth + 5.0F
                                );
            }

            /*
             * ==========================================
             * OpenGL Color
             * ==========================================
             */

            GL11.glColor4f(
                    1.0F,
                    1.0F,
                    1.0F,
                    1.0F
            );

            /*
             * ==========================================
             * Number Of Hearts
             * ==========================================
             */

            final int heartCount =
                    MathHelper.ceiling_float_int(
                            maxHealth / 2.0F
                    );

            /*
             * ==========================================
             * Heart Start X
             * ==========================================
             */

            final float heartStartX =
                    scaledResolution.getScaledWidth()
                            / 2.0F
                            - maxHealth
                            / 2.5F
                            * 10.0F
                            / 2.0F;

            /*
             * ==========================================
             * Render Hearts
             * ==========================================
             */

            for (
                    int i6 = heartCount - 1;
                    i6 >= 0;
                    i6--
            ) {

                /*
                 * ======================================
                 * Potion Heart Texture
                 * ======================================
                 */

                int xOffset = 16;

                if (player.isPotionActive(
                        Potion.poison
                )) {

                    xOffset += 36;

                } else if (player.isPotionActive(
                        Potion.wither
                )) {

                    xOffset += 72;
                }

                /*
                 * ======================================
                 * Damage Animation Texture
                 * ======================================
                 */

                final int k3 =
                        flag ? 1 : 0;

                /*
                 * ======================================
                 * Heart Position
                 * ======================================
                 */

                final float renX =
                        heartStartX
                                + i6 % 10 * 8.0F;

                float renY =
                        scaledResolution.getScaledHeight()
                                / 2.0F
                                + 15.0F
                                + offsetY;

                /*
                 * ======================================
                 * Low Health Shake
                 * ======================================
                 */

                if (healthInt <= 4) {

                    renY +=
                            random.nextInt(2);
                }

                /*
                 * ======================================
                 * Regeneration Bounce
                 * ======================================
                 */

                if (i6 == regenerationIndex) {

                    renY -= 2.0F;
                }

                /*
                 * ======================================
                 * Hardcore
                 * ======================================
                 */

                final int yOffset =
                        world.getWorldInfo()
                                .isHardcoreModeEnabled()
                                ? 5
                                : 0;

                /*
                 * ======================================
                 * Empty Heart
                 * ======================================
                 */

                RenderUtil.drawTexturedModalRect(
                        (int) renX,
                        (int) renY,
                        16 + k3 * 9,
                        9 * yOffset,
                        9,
                        9,
                        0.0F
                );

                /*
                 * ======================================
                 * Damage Animation
                 * ======================================
                 */

                if (flag) {

                    /*
                     * Damage Full Heart
                     */

                    if (i6 * 2 + 1 <
                            lastPlayerHealth) {

                        RenderUtil.drawTexturedModalRect(
                                (int) renX,
                                (int) renY,
                                xOffset + 54,
                                9 * yOffset,
                                9,
                                9,
                                0.0F
                        );
                    }

                    /*
                     * Damage Half Heart
                     */

                    if (i6 * 2 + 1 ==
                            lastPlayerHealth) {

                        RenderUtil.drawTexturedModalRect(
                                (int) renX,
                                (int) renY,
                                xOffset + 63,
                                9 * yOffset,
                                9,
                                9,
                                0.0F
                        );
                    }
                }

                /*
                 * ======================================
                 * Full Heart
                 * ======================================
                 */

                if (i6 * 2 + 1 <
                        healthInt) {

                    RenderUtil.drawTexturedModalRect(
                            (int) renX,
                            (int) renY,
                            xOffset + 36,
                            9 * yOffset,
                            9,
                            9,
                            0.0F
                    );
                }

                /*
                 * ======================================
                 * Half Heart
                 * ======================================
                 */

                if (i6 * 2 + 1 ==
                        healthInt) {

                    RenderUtil.drawTexturedModalRect(
                            (int) renX,
                            (int) renY,
                            xOffset + 45,
                            9 * yOffset,
                            9,
                            9,
                            0.0F
                    );
                }
            }

        } finally {

            GL11.glPopMatrix();
        }
    }

    /*
     * ==========================================
     * Health Color
     * ==========================================
     *
     * 0%   -> Red
     * 50%  -> Yellow
     * 100% -> Green
     */

    private Color getHealthColor(
            float progress
    ) {

        final float p =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                progress
                        )
                );

        /*
         * Red -> Yellow
         */

        if (p <= 0.5F) {

            final float t =
                    p / 0.5F;

            return new Color(
                    interpolate(
                            255,
                            255,
                            t
                    ),
                    interpolate(
                            37,
                            255,
                            t
                    ),
                    interpolate(
                            0,
                            0,
                            t
                    )
            );

        }

        /*
         * Yellow -> Green
         */

        final float t =
                (p - 0.5F) / 0.5F;

        return new Color(
                interpolate(
                        255,
                        0,
                        t
                ),
                interpolate(
                        255,
                        255,
                        t
                ),
                interpolate(
                        0,
                        0,
                        t
                )
        );
    }

    private int interpolate(
            int start,
            int end,
            float progress
    ) {

        final float p =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                progress
                        )
                );

        return (int) (
                start
                        + (end - start) * p
        );
    }

    /*
     * ==========================================
     * Strip Minecraft Color Codes
     * ==========================================
     */

    private String stripColorCodes(
            String text
    ) {

        return text.replaceAll(
                "§.",
                ""
        );
    }
}