package myau.mixin;

import myau.Myau;
import myau.module.modules.ItemRotate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class MixinItemRotate {

    @Inject(
            method = "renderItemInFirstPerson(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ItemRenderer;transformFirstPersonItem(FF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void injectItemRotate(
            float partialTicks,
            CallbackInfo ci
    ) {
        ItemRotate itemRotate =
                (ItemRotate) Myau.moduleManager.getModule(ItemRotate.class);

        if (itemRotate == null || !itemRotate.shouldRotate()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        // 正在使用物品时不旋转
        if (mc.thePlayer.getItemInUseCount() > 0) {
            return;
        }

        GlStateManager.rotate(
                itemRotate.getRotation(),
                0.0F,
                1.0F,
                0.0F
        );
    }
}