package com.thecowking.wrought.tileentity.honey_comb_coke_oven;

import com.thecowking.wrought.tileentity.MultiBlockFrameTile;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

import static com.thecowking.wrought.util.RegistryHandler.H_C_COKE_FRAME_TILE;


public class HCCokeOvenFrameTile extends MultiBlockFrameTile {
    private static final Logger LOGGER = LogManager.getLogger();

    public HCCokeOvenFrameTile() {
        super(H_C_COKE_FRAME_TILE.get());
        //LOGGER.info("FRAME TILE MADE");
    }

    public HCCokeOvenControllerTile getControllerTile()  {
        if(frameGetControllerPos() != null) {
            TileEntity te = this.world.getTileEntity(frameGetControllerPos());
            if (te instanceof HCCokeOvenControllerTile) {
                HCCokeOvenControllerTile controllerTile = (HCCokeOvenControllerTile) te;
                return controllerTile;
            }
        }
        return null;
    }


    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        HCCokeOvenControllerTile controllerTile = getControllerTile();
        if(controllerTile != null)  {
                return controllerTile.getCapability(cap, Direction.WEST);
        }
        return super.getCapability(cap, side);
    }



}
