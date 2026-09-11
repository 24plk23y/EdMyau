package myau.mixin;

import net.minecraft.client.gui.GuiIngame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GuiIngame.class)
public interface IAccessorGuiInGame {
    @Accessor("updateCounter")
    int getUpdateCounter();
    
    @Accessor("lastPlayerHealth")
    int getLastPlayerHealth();

    @Accessor("healthUpdateCounter")
    long getHealthUpdateCounter();
}