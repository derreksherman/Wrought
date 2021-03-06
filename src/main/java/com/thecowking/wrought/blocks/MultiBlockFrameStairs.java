package com.thecowking.wrought.blocks;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;

import static com.thecowking.wrought.data.MultiblockData.FORMED;

import static com.thecowking.wrought.data.MultiblockData.getUnderlyingBlock;

public class MultiBlockFrameStairs extends StairsBlock implements IMultiBlockFrame  {


    public MultiBlockFrameStairs(BlockState state) {
        super(state, Properties.create(Material.IRON)
                .sound(SoundType.METAL)
                .hardnessAndResistance(2.0f)
                .harvestTool(ToolType.PICKAXE));
    }

    /*
          Looks below it to grab a Tile Entity that can find the controller and start deconstructing the multi-blocks
         */
    @Override
    public void onBlockHarvested(World worldIn, BlockPos posIn, BlockState state, PlayerEntity player) {
        // Jump out if we are not Server Side
        if(!(player instanceof ServerPlayerEntity))  {
            return;
        }
        if(state.get(FORMED))  {
            BlockPos pos = getUnderlyingBlock(posIn);
            if(pos != null) {
                Block b = worldIn.getBlockState(pos).getBlock();
                if(b instanceof MultiBlockFrameBlock)  {
                    MultiBlockFrameBlock frame = (MultiBlockFrameBlock) b;
                    frame.updateMultiBlock(worldIn, pos);
                }
            }
        }
        super.onBlockHarvested(worldIn, posIn, state, player);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, SHAPE, WATERLOGGED);
        builder.add(FORMED);
    }

    @Override
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos posIn, PlayerEntity player, Hand hand, BlockRayTraceResult trace) {
        if (player instanceof ServerPlayerEntity && worldIn.getBlockState(posIn).get(FORMED)) {
            BlockPos frameBlockPos = getUnderlyingBlock(posIn);
            Block frameBlock = worldIn.getBlockState(frameBlockPos).getBlock();
            if (frameBlock instanceof MultiBlockFrameBlock) {
                ((MultiBlockFrameBlock) frameBlock).onBlockActivated(worldIn.getBlockState(frameBlockPos), worldIn, frameBlockPos, player, hand, trace);
            }
        }
        return super.onBlockActivated(state, worldIn, posIn, player, hand, trace);
    }

    public void addingToMultblock(BlockState blockState, BlockPos posIn, World worldIn) {
        worldIn.setBlockState(posIn, blockState.with(FORMED, true));
    }

    @Override
    public void removeFromMultiBlock(BlockState blockState, BlockPos posIn, World worldIn) {
        worldIn.setBlockState(posIn, blockState.with(FORMED, false));
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState();
    }

}
