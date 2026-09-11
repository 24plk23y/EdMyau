package myau.mixin.viaversion.MixinMinecraftAttackOrder;

import de.florianmichael.viamcp.ViaMCP;
import de.florianmichael.viamcp.fixes.AttackOrder;
import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import net.minecraft.block.material.Material;
import net.minecraft.util.MovingObjectPosition;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.particle.EffectRenderer;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Minecraft.class)
@SideOnly(Side.CLIENT)
public abstract class MixinMinecraftAttackOrder {

    @Shadow
    private int leftClickCounter;
    @Shadow
    public MovingObjectPosition objectMouseOver;
    @Shadow
    public static Logger logger;
    @Shadow
    public PlayerControllerMP playerController;
    @Shadow
    public WorldClient theWorld;
    @Shadow
    public EntityPlayerSP thePlayer;
    @Shadow
    public EffectRenderer effectRenderer;
    
    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
private void onClickMouse(CallbackInfo info) {
    if (this.leftClickCounter <= 0) {
        AttackOrder.sendConditionalSwing(this.objectMouseOver);

        if (this.objectMouseOver == null) {
            logger.error("Null returned as 'hitResult', this shouldn't happen!");

            if (this.playerController.isNotCreative()) {
                this.leftClickCounter = 10;
            }
        } else if (this.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            AttackOrder.sendFixedAttack(
                this.thePlayer,
                this.objectMouseOver.entityHit
            );

        } else if (this.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos blockpos = this.objectMouseOver.getBlockPos();

            if (this.theWorld.getBlockState(blockpos).getBlock().getMaterial() != Material.air) {
                this.playerController.clickBlock(
                    blockpos,
                    this.objectMouseOver.sideHit
                );
            } else {
                if (this.playerController.isNotCreative()) {
                    this.leftClickCounter = 10;
                }
            }

        } else {
            // 对应原 switch 中的 MISS / default
            if (this.playerController.isNotCreative()) {
                this.leftClickCounter = 10;
            }
        }
    }

    info.cancel();
}
    
    @Inject(method = "sendClickBlockToController", at = @At("HEAD"), cancellable = true)
    public void sendClickBlockToController(boolean leftClick, CallbackInfo ci) {
        if (!leftClick)
            leftClickCounter = 0;

        if (leftClickCounter <= 0 && !thePlayer.isUsingItem()) {
            if (leftClick && objectMouseOver != null && objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                BlockPos blockPos = objectMouseOver.getBlockPos();

                if (theWorld.getBlockState(blockPos).getBlock().getMaterial() != Material.air && playerController.onPlayerDamageBlock(blockPos, objectMouseOver.sideHit)) {
                    effectRenderer.addBlockHitEffects(blockPos, objectMouseOver.sideHit);
                    AttackOrder.sendConditionalSwing(objectMouseOver);
                }
            }
        }
        
        ci.cancel();
    }
}