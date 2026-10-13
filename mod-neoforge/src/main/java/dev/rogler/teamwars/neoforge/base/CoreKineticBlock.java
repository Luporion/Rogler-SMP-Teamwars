package dev.rogler.teamwars.neoforge.base;

import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The physical base core remains an immutable, registered TeamWars core.
 *  Its horizontal Create shaft is real: an attached Create shaft receives RPM/SU.
 *  Owning team has to purchase the engine before speed becomes nonzero.
 */
public class CoreKineticBlock extends RotatedPillarKineticBlock implements IBE<CoreKineticBlockEntity> {
    public CoreKineticBlock(Properties properties) { super(properties); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
    }
    @Override public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }
    @Override public Direction.Axis getRotationAxis(BlockState state) {return state.getValue(AXIS);}
    @Override public Class<CoreKineticBlockEntity> getBlockEntityClass() {return CoreKineticBlockEntity.class;}
    @Override public BlockEntityType<? extends CoreKineticBlockEntity> getBlockEntityType() {
        return BaseCoreRegistries.CORE_BE.get();
    }
}
