package com.thecowking.wrought.recipes;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import mekanism.api.recipes.inputs.FluidStackIngredient;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class WroughtSerializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<WroughtRecipe>  {
    protected int numInputs;
    protected int numOutputs;
    protected int numFluidInputs;
    protected int numFluidOutputs;
    protected boolean needFuel;
    protected boolean needHeat;
    protected ResourceLocation recipeTypeID;



    public WroughtSerializer(int numInputs, int numOutputs, int numFluidInputs, int numFluidOutputs, boolean needFuel,  boolean needHeat, ResourceLocation recipeTypeID)  {
        this.numInputs = numInputs;
        this.numOutputs = numOutputs;
        this.numFluidInputs = numFluidInputs;
        this.numFluidOutputs = numFluidOutputs;
        this.needFuel = needFuel;
        this.needHeat = needHeat;
        this.recipeTypeID = recipeTypeID;

    }


    @Override
    public WroughtRecipe read(ResourceLocation recipeId, JsonObject json) {

        /*
        ArrayList<ItemStack> itemOutputs = new ArrayList<>();
        for(int i = 0; i < this.numOutputs; i++)  {
            String searchString = "output_" + i;
            ItemStack current = CraftingHelper.getItemStack(JSONUtils.getJsonObject(json, searchString), true);
            itemOutputs.add(current);
        }

         */


        ArrayList<Ingredient> itemOutputs = new ArrayList<>();
        for(int i = 0; i < this.numOutputs; i++)  {
            String searchString = "output_" + i;
            Ingredient current = Ingredient.deserialize(JSONUtils.getJsonObject(json, searchString));
            itemOutputs.add(current);
        }


        ArrayList<Ingredient> itemInputs  = new ArrayList<>();
        for(int i = 0; i < this.numInputs; i++)  {
            String searchString = "input_" + i;
            Ingredient current = Ingredient.deserialize(JSONUtils.getJsonObject(json, searchString));
            itemInputs.add(current);
        }

        ArrayList<FluidStack> fluidOutputs = new ArrayList<>();
        for(int i = 0; i < this.numFluidOutputs; i++)  {
            String searchString = "fluid_output_" + i;
            ResourceLocation currentFluid = new ResourceLocation(JSONUtils.getString(json, searchString));
            int currentAmount = JSONUtils.getInt(json, "amount_" + searchString);
            FluidStack currentStack = getFluidStackFromID(currentFluid, currentAmount);
            fluidOutputs.add(currentStack);
        }

        ArrayList<FluidStack> fluidInputs = new ArrayList<>();
        for(int i = 0; i < this.numFluidInputs; i++)  {
            String searchString = "fluid_input_" + i;
            ResourceLocation currentFluid = new ResourceLocation(JSONUtils.getString(json, searchString));
            int currentAmount = JSONUtils.getInt(json, "amount_" + searchString);
            FluidStack currentStack = getFluidStackFromID(currentFluid, currentAmount);
            fluidInputs.add(currentStack);
        }

        Ingredient fuel = Ingredient.EMPTY;
        int heat = 0;
        if(this.needHeat)  {
            heat = JSONUtils.getInt(json, "heat");
        }
        int burnTime = JSONUtils.getInt(json, "burnTime");



        return new WroughtRecipe(recipeId, itemInputs, itemOutputs, fluidOutputs, fluidInputs, fuel, burnTime, heat, recipeTypeID);
    }

    @Nullable
    @Override
    public WroughtRecipe read(ResourceLocation recipeId, PacketBuffer buffer) {

        ArrayList<Ingredient> itemOutputs  = new ArrayList<>();
        for(int i = 0; i < this.numOutputs; i++)  {
            itemOutputs.add(Ingredient.read(buffer));
        }


        ArrayList<Ingredient> itemInputs  = new ArrayList<>();
        for(int i = 0; i < this.numInputs; i++)  {
            itemInputs.add(Ingredient.read(buffer));
        }

        ArrayList<FluidStack> fluidOutputs = new ArrayList<>();
        for(int i = 0; i < this.numFluidOutputs; i++)  {
            fluidOutputs.add(buffer.readFluidStack());
        }

        ArrayList<FluidStack> fluidInputs = new ArrayList<>();
        for(int i = 0; i < this.numFluidInputs; i++)  {
            fluidInputs.add(buffer.readFluidStack());
        }

        Ingredient fuel = Ingredient.EMPTY;

        int burnTime = buffer.readInt();
        int heat  = 0;
        if(this.needHeat)  {
            heat = buffer.readInt();
        }
        return new WroughtRecipe(recipeId, itemInputs, itemOutputs, fluidOutputs, fluidInputs, fuel, burnTime, heat, recipeTypeID);
    }

    @Override
    public void write(PacketBuffer buffer, WroughtRecipe recipe) {

        List<Ingredient> itemOutputs = recipe.getItemInputs();
        for(int i = 0; i < itemOutputs.size(); i++)  {
            itemOutputs.get(i).write(buffer);
        }

        List<Ingredient> itemInputs = recipe.getItemInputs();
        for(int i = 0; i < itemInputs.size(); i++)  {
            itemInputs.get(i).write(buffer);
        }

        List<FluidStack> fluidOutputs = recipe.getFluidOutputs();
        for(int i = 0; i < fluidOutputs.size(); i++)  {
            buffer.writeFluidStack(fluidOutputs.get(i));
        }

        List<FluidStack> fluidInputs = recipe.getFluidInputs();
        for(int i = 0; i < fluidOutputs.size(); i++)  {
            buffer.writeFluidStack(fluidOutputs.get(i));
        }
        buffer.writeInt(recipe.getHeat());
        buffer.writeInt(recipe.getBurnTime());
    }

    public FluidStack getFluidStackFromID(ResourceLocation fluidID, int amount)  {
        FluidStack fluidStack = FluidStack.EMPTY;
        if(amount != 0)  {
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidID);
               if (fluid == null || fluid == FluidStack.EMPTY.getFluid()) {
                   throw new JsonSyntaxException("Unknown fluid: " + fluidID);
              }
            fluidStack = new FluidStack(fluid, amount);
        }
        return fluidStack;
    }
}
