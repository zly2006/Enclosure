package com.github.zly2006.enclosure.mixin;

import com.github.zly2006.enclosure.ServerMain;
import com.github.zly2006.enclosure.utils.Permission;
import com.github.zly2006.enclosure.utils.Utils;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FacingBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.fabricmc.api.EnvType.SERVER;

@Environment(SERVER)
@Mixin(PistonBlock.class)
public class MixinPistonBlock extends FacingBlock {
    @Shadow
    @Final
    public static BooleanProperty EXTENDED;

    protected MixinPistonBlock(Settings settings) {
        super(settings);
    }

    /**
     * 某个方块是否可以推动
     *
     * @param state     要推的方块的state
     * @param world     所在世界
     * @param pos       这个方块当前的位置
     * @param direction 要把这个方块推到哪里？
     * @param canBreak  是否接受破坏state对应的方块？
     * @param pistonDir 活塞的方向，值得注意的是当他和direction相反的时候，表示收回。
     */
    @Inject(at = @At("HEAD"), method = "isMovable", cancellable = true)
    private static void protectPiston(BlockState state, World world, BlockPos pos, Direction direction, boolean canBreak, Direction pistonDir, CallbackInfoReturnable<Boolean> cir) {
        if (world instanceof ServerWorld serverWorld) {
            BlockPos newPos = pos.offset(direction);
            BlockPos pistonPos;
            if (state.isAir()) {
                return;
            }
            if (pistonDir.equals(direction)) {
                // 活塞向外推
                // P B T
                pistonPos = pos.offset(pistonDir.getOpposite());
            } else {
                // P T B
                // 活塞向内收
                pistonPos = pos.offset(pistonDir.getOpposite(), 2);
            }
            if (!ServerMain.INSTANCE.checkPermissionInDifferentEnclosure(serverWorld, pistonPos, newPos, Permission.PISTON) ||
                !ServerMain.INSTANCE.checkPermissionInDifferentEnclosure(serverWorld, pos, newPos, Permission.PISTON)) {
                // this method will be called even the target pos is out of the world.
                Utils.mark4updateChecked(serverWorld, pos);
                Utils.mark4updateChecked(serverWorld, newPos);
                cir.setReturnValue(false);
            }
        }
    }

    @Redirect(method = "onSyncedBlockEvent", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    private boolean protectBlockEvent(World world, BlockPos pos, boolean move) {
        if (world instanceof ServerWorld serverWorld) {
            if (!ServerMain.INSTANCE.checkPermission(serverWorld, pos, null, Permission.BREAK_BLOCK) && !serverWorld.getBlockState(pos).isOf(Blocks.MOVING_PISTON)) {
                return false;
            }
        }
        return world.removeBlock(pos, move);
    }
}
