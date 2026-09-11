package me.ksyz.accountmanager.gui;

import me.ksyz.accountmanager.AccountManager;
import me.ksyz.accountmanager.auth.Account;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiOfflineLogin extends GuiScreen {

    private final GuiScreen previousScreen;

    private GuiTextField usernameField;

    private GuiButton randomButton;
    private GuiButton saveButton;

    /*
     * Random username length.
     *
     * static means the value will survive:
     * Offline GUI -> Account Manager -> Offline GUI
     *
     * It will reset when Minecraft itself is restarted.
     */
    private static int usernameLength = 8;

    /*
     * Slider
     */
    private int sliderX;
    private int sliderY;
    private final int sliderWidth = 200;
    private final int sliderHeight = 20;

    private boolean draggingSlider = false;

    public GuiOfflineLogin(GuiScreen previousScreen) {
        this.previousScreen = previousScreen;
    }

    /**
     * Generate a random Minecraft username.
     */
    private String generateRandomUsername(int length) {

        final String chars =
                "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_____";

        StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            result.append(
                    chars.charAt(
                            (int) (Math.random() * chars.length())
                    )
            );
        }

        return result.toString();
    }

    @Override
    public void initGui() {

        Keyboard.enableRepeatEvents(true);

        /*
         * Username
         */
        usernameField = new GuiTextField(
                1,
                mc.fontRendererObj,
                width / 2 - 100,
                height / 2 - 20,
                200,
                20
        );

        usernameField.setMaxStringLength(16);
        usernameField.setFocused(true);

        /*
         * Slider
         */
        sliderX = width / 2 - 100;
        sliderY = height / 2 + 15;

        /*
         * Random
         */
        randomButton = new GuiButton(
                3,
                width / 2 + 105,
                height / 2 - 20,
                70,
                20,
                "Random"
        );

        /*
         * Save
         */
        saveButton = new GuiButton(
                4,
                width / 2 - 100,
                height / 2 + 50,
                200,
                20,
                "Save Offline Account"
        );

        buttonList.add(randomButton);
        buttonList.add(saveButton);

        super.initGui();
    }

    /**
     * Get slider knob X position.
     */
    private int getSliderKnobX() {

        float progress =
                (usernameLength - 1) / 15.0F;

        return sliderX +
                Math.round(
                        progress * (sliderWidth - 8)
                );
    }

    /**
     * Update length according to mouse position.
     */
    private void updateSlider(int mouseX) {

        /*
         * Center the calculation around the knob.
         */
        float progress =
                (mouseX - sliderX - 4)
                        / (float) (sliderWidth - 8);

        progress =
                Math.max(
                        0.0F,
                        Math.min(
                                1.0F,
                                progress
                        )
                );

        usernameLength =
                1 + Math.round(
                        progress * 15.0F
                );

        usernameLength =
                Math.max(
                        1,
                        Math.min(
                                16,
                                usernameLength
                        )
                );
    }

    @Override
    protected void actionPerformed(GuiButton button) {

        if (button == null || !button.enabled) {
            return;
        }

        /*
         * Random
         */
        if (button.id == 3) {

            usernameField.setText(
                    generateRandomUsername(
                            usernameLength
                    )
            );

            return;
        }

        /*
         * Save Offline Account
         */
        if (button.id == 4) {

            String username =
                    usernameField.getText().trim();

            if (username.isEmpty()) {
                return;
            }

            /*
             * Only save the account.
             *
             * DO NOT change Minecraft Session here.
             */
            Account account =
                    new Account(
                            username,
                            true
                    );

            AccountManager.accounts.add(account);
            AccountManager.save();

            /*
             * Return to Account Manager.
             */
            mc.displayGuiScreen(
                    previousScreen
            );
        }
    }

    @Override
    protected void keyTyped(
            char typedChar,
            int keyCode
    ) throws IOException {

        if (keyCode == Keyboard.KEY_ESCAPE) {

            mc.displayGuiScreen(
                    previousScreen
            );

            return;
        }

        if (keyCode == Keyboard.KEY_RETURN) {

            actionPerformed(
                    saveButton
            );

            return;
        }

        if (usernameField.isFocused()) {

            usernameField.textboxKeyTyped(
                    typedChar,
                    keyCode
            );
        }

        super.keyTyped(
                typedChar,
                keyCode
        );
    }

    @Override
    protected void mouseClicked(
            int mouseX,
            int mouseY,
            int mouseButton
    ) throws IOException {

        /*
         * Username field
         */
        usernameField.mouseClicked(
                mouseX,
                mouseY,
                mouseButton
        );

        /*
         * Slider
         */
        if (mouseButton == 0 &&
                mouseX >= sliderX - 4 &&
                mouseX <= sliderX + sliderWidth + 4 &&
                mouseY >= sliderY &&
                mouseY <= sliderY + sliderHeight) {

            draggingSlider = true;

            updateSlider(mouseX);
        }

        super.mouseClicked(
                mouseX,
                mouseY,
                mouseButton
        );
    }

    @Override
    protected void mouseClickMove(
            int mouseX,
            int mouseY,
            int clickedMouseButton,
            long timeSinceLastClick
    ) {

        if (draggingSlider &&
                clickedMouseButton == 0) {

            updateSlider(mouseX);
        }

        super.mouseClickMove(
                mouseX,
                mouseY,
                clickedMouseButton,
                timeSinceLastClick
        );
    }

    @Override
    protected void mouseReleased(
            int mouseX,
            int mouseY,
            int state
    ) {

        if (state == 0) {
            draggingSlider = false;
        }

        super.mouseReleased(
                mouseX,
                mouseY,
                state
        );
    }

    @Override
    public void drawScreen(
            int mouseX,
            int mouseY,
            float partialTicks
    ) {

        drawDefaultBackground();

        /*
         * Title
         */
        drawCenteredString(
                fontRendererObj,
                "Offline Account",
                width / 2,
                height / 2 - 60,
                -1
        );

        /*
         * Username
         */
        usernameField.drawTextBox();

        /*
         * Slider label
         */
        drawCenteredString(
                fontRendererObj,
                "Username Length: " + usernameLength,
                width / 2,
                height / 2 + 5,
                -1
        );

        /*
         * Slider background
         */
        drawRect(
                sliderX,
                sliderY + 8,
                sliderX + sliderWidth,
                sliderY + 12,
                0xFF808080
        );

        /*
         * Slider knob
         */
        int knobX =
                getSliderKnobX();

        drawRect(
                knobX,
                sliderY,
                knobX + 8,
                sliderY + sliderHeight,
                0xFFFFFFFF
        );

        /*
         * Min
         */
        drawString(
                fontRendererObj,
                "1",
                sliderX - 8,
                sliderY + 6,
                -1
        );

        /*
         * Max
         */
        drawString(
                fontRendererObj,
                "16",
                sliderX + sliderWidth + 5,
                sliderY + 6,
                -1
        );

        super.drawScreen(
                mouseX,
                mouseY,
                partialTicks
        );
    }

    @Override
    public void onGuiClosed() {

        Keyboard.enableRepeatEvents(
                false
        );

        /*
         * usernameLength is static,
         * so we intentionally don't reset it here.
         */
    }
}