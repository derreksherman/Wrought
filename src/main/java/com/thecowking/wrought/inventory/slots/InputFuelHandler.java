package com.thecowking.wrought.inventory.slots;

import com.thecowking.wrought.init.RecipeSerializerInit;
import com.thecowking.wrought.recipes.HoneyCombCokeOven.HoneyCombCokeOvenRecipe;
import com.thecowking.wrought.tileentity.MultiBlockControllerTile;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.util.Set;

import static com.thecowking.wrought.util.InventoryUtils.findRecipesByType;


public class InputFuelHandler extends ItemStackHandler {
    private static final Logger LOGGER = LogManager.getLogger();
    private MultiBlockControllerTile tile;
    private String id;


    public InputFuelHandler(int size, MultiBlockControllerTile tile, InputFuelHandler primary, String id)  {
        super(size);
        this.tile = tile;
        this.id = id;

    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate)  {
        return checkRecipe(slot,stack,simulate);
    }


    public ItemStack checkRecipe(int slot, @Nonnull ItemStack stack, boolean simulate)  {
        LOGGER.info("input stack is " + stack);
        LOGGER.info("id = " + id);

        HoneyCombCokeOvenRecipe currentRecipe = getRecipe(stack);
        if(currentRecipe == null)  {
            LOGGER.info("recipe is null");
            return stack;
        }

        LOGGER.info("hit bottom");
        return super.insertItem(slot, stack, simulate);
    }


    public HoneyCombCokeOvenRecipe getRecipe(ItemStack stack)  {
        if (stack == null) {
            return null;
        }
        Set<IRecipe<?>> recipes = findRecipesByType(RecipeSerializerInit.HONEY_COMB_OVEN_TYPE, tile.getWorld());

        for (IRecipe<?> iRecipe : recipes) {
            HoneyCombCokeOvenRecipe recipe = (HoneyCombCokeOvenRecipe) iRecipe;
            if (recipe.matches(stack))  {
                return recipe;
            }
        }
        return null;
    }




}

