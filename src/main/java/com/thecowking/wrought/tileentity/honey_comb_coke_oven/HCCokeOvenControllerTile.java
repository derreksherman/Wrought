package com.thecowking.wrought.tileentity.honey_comb_coke_oven;

import com.thecowking.wrought.blocks.IMultiBlockFrame;
import com.thecowking.wrought.inventory.containers.HCCokeOvenContainer;
import com.thecowking.wrought.blocks.honey_comb_coke_oven.HCCokeOvenFrameBlock;
import com.thecowking.wrought.blocks.Multiblock;
import com.thecowking.wrought.inventory.containers.OutputFluidTank;
import com.thecowking.wrought.inventory.slots.*;
import com.thecowking.wrought.recipes.HoneyCombCokeOven.HoneyCombCokeOvenRecipe;
import com.thecowking.wrought.tileentity.MultiBlockControllerTile;
import com.thecowking.wrought.util.*;
import javafx.util.Pair;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.BucketItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;
import java.util.stream.Collectors;

import static com.thecowking.wrought.blocks.Multiblock.*;
import static com.thecowking.wrought.util.RegistryHandler.*;

public class HCCokeOvenControllerTile extends MultiBlockControllerTile implements INamedContainerProvider {
    private static final Logger LOGGER = LogManager.getLogger();


    // Members that make up the multi-block
    protected static Block frameBlock = H_C_COKE_FRAME_BLOCK.get();
    private static Block controllerBlock = H_C_COKE_CONTROLLER_BLOCK.get();
    private static Block hatchBlock = H_C_COKE_FRAME_BLOCK.get();
    private static Block frameStairs = H_C_COKE_FRAME_STAIR.get();
    private static Block frameSlab = H_C_COKE_FRAME_SLAB.get();

    // used to track info for the progress bar which gets sent to the client
    public final HCStateData stateData = new HCStateData();

    // tracks if the tile entity needs a block update
    private boolean needUpdate = false;

    /*
    array holding the blocks location of all members in the multi-blocks
    split up by the y level where posArray[0][x][z] = the bottom most layer
     */
    private final Block[][][] posArray = {
            // bottom level
            {
                    {null, null,       null,       null,       null,       null,       null},
                    {null, null,       frameBlock, frameBlock, frameBlock, null,       null},
                    {null, frameBlock, frameBlock, frameBlock, frameBlock, frameBlock, null},
                    {null, frameBlock, frameBlock, frameBlock, frameBlock, frameBlock, null},
                    {null, frameBlock, frameBlock, frameBlock, frameBlock, frameBlock, null},
                    {null, null,       frameBlock, frameBlock, frameBlock, null,       null},
                    {null, null,       null,       null,       null,       null,       null}
            },
            {
                    {null,  null, frameBlock, frameBlock, frameBlock, null, null},
                    {null,  frameBlock, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock, null},
                    {frameBlock,  Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock},
                    {frameBlock,  Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock},
                    {frameBlock,  Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock},
                    {null,  frameBlock, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock, null},
                    {null,  null, frameBlock, frameBlock, frameBlock, null, null}
            },
            {
                    {null,  null, frameBlock, controllerBlock, frameBlock, null, null},
                    {null,  frameBlock, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock, null},
                    {frameBlock,  Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock},
                    {frameBlock,  Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock},
                    {frameBlock,  Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock},
                    {null,  frameBlock, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock, null},
                    {null,  null, frameBlock, frameBlock, frameBlock, null, null}
            },
            {
                    {null,  null, frameBlock, frameBlock, frameBlock, null, null},
                    {null,  frameBlock, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock, null},
                    {frameBlock,  Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock},
                    {frameBlock,  Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock},
                    {frameBlock,  Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock},
                    {null,  frameBlock, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock, null},
                    {null,  null, frameBlock, frameBlock, frameBlock, null, null}
            },
            {
                    {null, null, frameStairs, frameStairs, frameStairs, null, null},
                    {null, frameStairs, frameBlock, frameBlock, frameBlock, frameStairs, null},
                    {frameStairs, frameBlock, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock, frameStairs},
                    {frameStairs, frameBlock, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock, frameStairs},
                    {frameStairs, frameBlock, Blocks.AIR, Blocks.AIR, Blocks.AIR, frameBlock, frameStairs},
                    {null, frameStairs, frameBlock, frameBlock, frameBlock, frameStairs, null},
                    {null, null, frameStairs, frameStairs, frameStairs, null, null}
            },
            {
                    {null, null, null, null, null, null, null},
                    {null, null, frameSlab, frameStairs, frameSlab, null, null},
                    {null, frameSlab, frameBlock, frameBlock, frameBlock, frameSlab, null},
                    {null, frameStairs, frameBlock, hatchBlock, frameBlock, frameStairs, null},
                    {null, frameSlab, frameBlock, frameBlock, frameBlock, frameSlab, null},
                    {null, null, frameSlab, frameStairs, frameSlab, null, null},
                    {null, null, null, null, null, null, null}
            }
    };

    // used to track when we can start an operations
    // -> didnt want something that processes too fast
    private int tickCounter;

    // tracks for the light level and blockstate
    private boolean isSmelting = false;

    // input and outputs
    protected FluidHandlerItemStack fluidOutput;
    protected InputItemHandler inputSlot;
    protected OutputItemHandler primaryOutputSlot;
    protected OutputItemHandler secondaryOutputSlot;
    protected FluidItemInputHandler itemFluidInputSlot;
    protected FluidItemOutputHandler itemFluidOutputSlot;

    // main tank
    private OutputFluidTank fluidTank;

    // used when player is directly accessing multi-block
    private final LazyOptional<IItemHandler> everything = LazyOptional.of(() -> new CombinedInvWrapper(inputSlot, primaryOutputSlot, secondaryOutputSlot, itemFluidInputSlot, itemFluidOutputSlot));

    // used when in world things interact with multi-block
    private final LazyOptional<IItemHandler> automation = LazyOptional.of(() -> new AutomationCombinedInvWrapper(inputSlot, primaryOutputSlot, secondaryOutputSlot, itemFluidInputSlot, itemFluidOutputSlot));

    // used to stop operation for when an item cannot be inserted into output slot
    private ItemStack itemBacklog;

    // used to stop operation for when a fluid cannot be inserted into output slot
    private FluidStack fluidBacklog;

    // holds information about the current oven operation
    private ItemStack processingPrimaryItemStack;
    private ItemStack processingSecondaryItemStack;
    private FluidStack processingFluidStack;
    private boolean isProcessing = false;

    // used to stop operations if the bucket slot cannot be used
    private ItemStack fluidItemBacklog;

    // lets us know how long the current item has cooked for
    public int operationProgression;

    // lets us know when the current item is done cooking
    public int operationComplete;

    // used to let autobuilding know what blocks are still needed
    HashMap<Block, Integer> failures;

    public HCCokeOvenControllerTile() {

        super(H_C_COKE_CONTROLLER_TILE.get());
        this.inputSlot = new InputItemHandler(1, this);
        this.primaryOutputSlot = new OutputItemHandler(1);
        this.secondaryOutputSlot = new OutputItemHandler(1);
        this.itemFluidInputSlot = new FluidItemInputHandler(1);
        this.itemFluidOutputSlot = new FluidItemOutputHandler(1);

        this.fluidTank = new OutputFluidTank(16000);
        this.itemBacklog = ItemStack.EMPTY;
        this.fluidBacklog = FluidStack.EMPTY;
        this.fluidItemBacklog = ItemStack.EMPTY;
        this.processingPrimaryItemStack = ItemStack.EMPTY;
        this.processingSecondaryItemStack = ItemStack.EMPTY;
        this.processingFluidStack = FluidStack.EMPTY;

        // TODO - get rid of state data
        this.operationProgression = 0;
        this.operationComplete = 0;
        this.stateData.timeElapsed = 0;
        this.stateData.timeComplete = 0;

        this.height = posArray.length;
        this.length = posArray[0].length;
        this.width = posArray[0][0].length;
        this.tickCounter = 0;
    }

    /*
        Getters
     */
    public World getWorld()  {return this.world;}
    public FluidStack getFluidInTank()  {return fluidTank.getFluid();}


    /*
        Runs every 1/20 seconds
     */
    @Override
    public void tick()  {
        super.tick();
        // check if we are in correct instance
        if (this.world == null || this.world.isRemote) {
            finishOperation();
            return;
        }

        // check if we have a multi-block
        if (!isFormed(getControllerPos())) {
            finishOperation();
            return;
        }

        // Check if enough time passed for an operation
        // note - don't really care about writing this to mem
        if(tickCounter < this.TICKSPEROPERATION)  {
            tickCounter++;
            finishOperation();
            return;
        }
        tickCounter = 0;

        attemptRunOperation();

        finishOperation();
    }

    /*
        Main operation driver - checks all instances to make sure that we can actually run an operation
     */
    public void attemptRunOperation() {
        LOGGER.info("Attempt Oven Operation");


        // Checks if we can fill the item in the fluid input with the tanks fluid
        processFluidContainerItem();

        // Check if the item output is clogged - note that another operation will not happen until tickCount has passed if these fail
        if(!processItemBackLog())  { return; }

        // check if redstone is turning machine off
        if(redstonePowered())  { return; }

        // either increment how long current item has cooked or get ready to move onto next item
        if(!processItem())  { return; }

        // check to make sure output is not full before starting another operation
        if (primaryOutputSlot.getStackInSlot(0).getCount() >= primaryOutputSlot.getStackInSlot(0).getMaxStackSize()) {
            machineChangeOperation(false);
            LOGGER.info("cannot insert item off");
            return;
        }

        // yank the current recipe for an item in
        HoneyCombCokeOvenRecipe currentRecipe = this.getRecipe(inputSlot.getStackInSlot(0));

        // check if we have a recipe for item
        if (!recipeChecker(currentRecipe)) { return; }
        if (!fluidRecipeChecker(currentRecipe))  {return;}

        ovenOperation(currentRecipe);
    }


    /*
        Attempts to clear item backlogs
        false if it could not - thus i want the machine to halt
        true if we should continue on
     */
    private boolean processItemBackLog()  {
        LOGGER.info("Process Item Backlog");


        if(this.itemBacklog == ItemStack.EMPTY)  {return true;}

        // used to check if anything was processed
        ItemStack oldBacklog = this.itemBacklog;

        // attempt to insert backlog into the item output slot
        // whatever is leftover is saved into itemBacklog
        this.itemBacklog = primaryOutputSlot.internalInsertItem(0, this.itemBacklog.copy(), false);


        // check for changes
        if(this.itemBacklog != oldBacklog)  {this.needUpdate = true;}

        if(this.itemBacklog == ItemStack.EMPTY)  {return true;}

        return false;
    }

    /*
        Checks if redstone signal is on / off and turns off machine if on
     */
    private boolean redstonePowered()  {
        LOGGER.info("Check Redstone Power");

        if(isRedstonePowered(this.redstoneIn)) {
            LOGGER.info("redstone turn off");
            machineChangeOperation(false);
            return true;
        }
        return false;
    }

    /*
        Increments the timer for an operation if it has not finished yet
        If finished attempts to move item+fluid into output slot
     */

    private boolean processItem()  {
        LOGGER.info("Process Item");

        // Check if there is a previous item and the item has "cooked" long enough
        if (processingPrimaryItemStack != ItemStack.EMPTY && this.stateData.timeElapsed++ < this.stateData.timeComplete) {
            LOGGER.info("Burn Time = " + this.stateData.timeElapsed + " finishtime = " + this.stateData.timeComplete);

            this.needUpdate = true;
            this.stateData.timeElapsed++;
            return false;

            // item has cooked long enough -> insert outputs and move onto next operation
        }  else if(processingPrimaryItemStack != ItemStack.EMPTY) {
            this.needUpdate = true;
            this.stateData.timeElapsed = 0;
            // attempt to insert fluid from craft and fill leftovers into backlog tank
            fluidBacklog = fluidTank.internalFill(processingFluidStack, IFluidHandler.FluidAction.EXECUTE);
            // attempt to insert item from craft and fill leftovers into backlog container
            LOGGER.info("inserting -> " + processingPrimaryItemStack);

            // unsure why but if i do not .copy() the outputs randomly multiplies by two on each successful operation
            itemBacklog = primaryOutputSlot.internalInsertItem(0, processingPrimaryItemStack.copy(), false);
            secondaryOutputSlot.internalInsertItem(0, processingSecondaryItemStack.copy(), false);
            processingPrimaryItemStack = ItemStack.EMPTY;
            processingSecondaryItemStack = ItemStack.EMPTY;

            processingFluidStack = FluidStack.EMPTY;
            isProcessing = false;
        }
        return true;
    }



    /*
        Check if a new item has a recipe that the oven can use
     */
    private boolean recipeChecker(HoneyCombCokeOvenRecipe currentRecipe)  {
        LOGGER.info("Recipe Check");


        // check if we have a recipe for item
        if (currentRecipe == null) {
            machineChangeOperation(false);
            //LOGGER.info("no recipe");
            return false;
        }
        return true;
    }

    /*
        Check if we can store a fluid for a new operation
     */
    private boolean fluidRecipeChecker(HoneyCombCokeOvenRecipe currentRecipe)  {
        LOGGER.info("Fluid Reciepe Check");

        // get the fluid output from recipe
        FluidStack recipeFluidOutput = currentRecipe.getRecipeFluidStackOutput();

        // check if recipe has a fluid output
        if(recipeFluidOutput != null && !fluidTank.isEmpty())  {

            // check if fluid matches tank and if tank has space for fluid
            if(fluidTank.getFluidAmount() + recipeFluidOutput.getAmount() > fluidTank.getCapacity())  {
                LOGGER.info(fluidTank.getFluidAmount() + recipeFluidOutput.getAmount() + " is not smaller than " + fluidTank.getCapacity());
                LOGGER.info("fluid cannot insert as there is not enough tank space");
                finishOperation();
                return false;
            }

            // check to see if that fluids match
            if (recipeFluidOutput.getFluid() != fluidTank.getFluid().getFluid()  )  {
                LOGGER.info("fluid cannot insert fluid is not equal");
                LOGGER.info(recipeFluidOutput.getDisplayName());
                LOGGER.info(fluidTank.getFluid().getDisplayName());
                finishOperation();
                return false;
            }

        }
        return true;
    }


    /*
     Method to run a single oven operation
     */
    private void ovenOperation(HoneyCombCokeOvenRecipe currentRecipe) {
        LOGGER.info("Oven Operation");
        if(currentRecipe == null)  {return;}

        ItemStack primaryOutput = currentRecipe.getPrimaryOutput();

        FluidStack fluidOutput = this.getRecipe(this.inputSlot.getStackInSlot(0)).getRecipeFluidStackOutput();
        this.stateData.timeComplete = currentRecipe.getBurnTime();
        this.operationComplete = currentRecipe.getBurnTime();


        if (primaryOutput != null && primaryOutput.getItem() != Items.AIR) {

            if(!this.isSmelting)  {
                machineChangeOperation(true);
            }
            this.isProcessing = true;

            // take item out of input
            // TODO - multi item input
            this.inputSlot.getStackInSlot(0).shrink(1);

            this.processingPrimaryItemStack = currentRecipe.getPrimaryOutput();
            this.processingSecondaryItemStack = currentRecipe.getSecondaryOutput();

            this.processingFluidStack = fluidOutput;
            this.needUpdate = true;
        }
    }

    /*
      Flips states if machine is changing from off -> on or from on -> off
     */
    private void machineChangeOperation(boolean online) {
        if (online == isSmelting) {
            return;
        }
        this.isSmelting = online;
        setOn(online);
        if(online)  {
            sendOutRedstone(15);
        }  else  {
            sendOutRedstone(0);
        }
    }

    /*
    TODO - add support for tanks
    Processes items in the "bucket" slot
     */
    protected void processFluidContainerItem()  {
        LOGGER.info(fluidTank.getFluidAmount());
        if(fluidTank.getFluidAmount() < 1000)  return;
        if(itemFluidInputSlot.getStackInSlot(0).isEmpty())  return;
        if(!(itemFluidOutputSlot.getStackInSlot(0).isEmpty()))  return;

        ItemStack fluidContainer = itemFluidInputSlot.getStackInSlot(0);

        //LazyOptional <net.minecraftforge.fluids.capability.IFluidHandler> itemFluidCapability = fluidContainer.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
        LazyOptional <net.minecraftforge.fluids.capability.IFluidHandlerItem> itemFluidCapability = fluidContainer.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY);


        // check to see if somehow a non fluid container got in -> this check is also in SlotInputFluid
        if(!itemFluidCapability.isPresent())  {
            LOGGER.info("not a container!");
            return;
        }

        // if we have a bucket
        if(fluidContainer.getItem() instanceof BucketItem)  {
            ItemStack fluidBucket = InventoryUtils.fillBucketOrFluidContainer(fluidContainer, fluidTank.getFluid());
            if(fluidBucket.isEmpty())  return;

            itemFluidInputSlot.getStackInSlot(0).shrink(1);

            ItemStack filledContainer = InventoryUtils.fillBucketOrFluidContainer(fluidContainer, fluidTank.getFluid());
            if (filledContainer.isEmpty())  {
                LOGGER.info("could not get filledcontainer");
                return;
            }
            LOGGER.info("container is ");
            LOGGER.info(filledContainer);

            LOGGER.info("inserting into itemfluid ouptut");
            fluidTank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
            fluidItemBacklog = itemFluidOutputSlot.internalInsertItem(0, filledContainer.copy(), false);
            this.needUpdate = true;

            // we have some sort of container
        }  else  {
            LOGGER.info("processing!");
            IFluidHandlerItem fluidItemHandler = itemFluidCapability.resolve().get();
            FluidStack back = FluidUtil.tryFluidTransfer(fluidItemHandler, fluidTank, fluidTank.getFluid(), true);
            LOGGER.info(back.getDisplayName());
            if (back.isEmpty())  {
                ItemStack f = itemFluidInputSlot.getStackInSlot(0).copy();
                itemFluidInputSlot.getStackInSlot(0).shrink(1);
                itemFluidOutputSlot.internalInsertItem(0, f, false);
                this.needUpdate = true;
            }
        }
    }


    public BlockPos getMultiBlockCenter()  {
        return calcCenterBlock(this.getDirectionFacing());
    }

    /*
        Method to set off updates
        This way markDirty is not called multiple times in one operation
     */
    public void finishOperation()  {
        if (this.needUpdate)  {
            this.needUpdate = false;
            blockUpdate();
            markDirty();
        }
    }


    /*
        Tells the server what to save to disk
     */
    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        inputSlot.deserializeNBT(nbt.getCompound(INVENTORY_IN));
        primaryOutputSlot.deserializeNBT(nbt.getCompound(PRIMARY_INVENTORY_OUT));
        secondaryOutputSlot.deserializeNBT(nbt.getCompound(SECONDARY_INVENTORY_OUT));
        itemFluidInputSlot.deserializeNBT(nbt.getCompound(FLUID_INVENTORY_IN));
        itemFluidOutputSlot.deserializeNBT(nbt.getCompound(FLUID_INVENTORY_OUT));
        fluidTank.readFromNBT(nbt.getCompound(FLUID_TANK));
        stateData.readFromNBT(nbt);
    }

    /*
        Tells the server what to read from disk on  chunk load
     */
    @Override
    public CompoundNBT write(CompoundNBT tag) {
        tag = super.write(tag);
        tag.put(INVENTORY_IN, inputSlot.serializeNBT());
        tag.put(PRIMARY_INVENTORY_OUT, primaryOutputSlot.serializeNBT());
        tag.put(SECONDARY_INVENTORY_OUT, secondaryOutputSlot.serializeNBT());
        tag.put(FLUID_INVENTORY_IN, itemFluidInputSlot.serializeNBT());
        tag.put(FLUID_INVENTORY_OUT, itemFluidOutputSlot.serializeNBT());
        tag.put(FLUID_TANK, fluidTank.writeToNBT(new CompoundNBT()));
        stateData.putIntoNBT(tag);
        return tag;
    }

    /*
        lets the world around it know what can be automated
     */
    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull final Capability<T> cap, @Nullable final Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            if (world != null && world.getBlockState(pos).getBlock() != this.getBlockState().getBlock()) {//if the blocks at myself isn't myself, allow full access (Block Broken)
                return everything.cast();
            }
            if (side == null) {
                return everything.cast();
            } else {
                return automation.cast();
            }
        }
        if(cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY)  {
            return LazyOptional.of(() -> fluidTank).cast();
        }
        return super.getCapability(cap, side);
    }

    @Nullable
    @Override
    public Container createMenu(final int windowID, final PlayerInventory playerInv, final PlayerEntity playerIn) {
        return new HCCokeOvenContainer(windowID, this.world, getControllerPos(), playerInv, stateData);
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TranslationTextComponent("Coke Oven Controller");
    }

    /*
        Finds a recipe for a given input
     */
    @Nullable
    public HoneyCombCokeOvenRecipe getRecipe(ItemStack stack) {
        if (stack == null) {
            return null;
        }
        Set<IRecipe<?>> recipes = findRecipesByType(RecipeSerializerInit.HONEY_COMB_OVEN_TYPE, this.world);

        for (IRecipe<?> iRecipe : recipes) {
            HoneyCombCokeOvenRecipe recipe = (HoneyCombCokeOvenRecipe) iRecipe;
            if (recipe.matches(new RecipeWrapper(this.inputSlot), this.world)) {
                return recipe;
            }
        }
        return null;
    }

    public static Set<IRecipe<?>> findRecipesByType(IRecipeType<?> typeIn, World world) {
        LOGGER.info("findRecipesByType - server");
        return world != null ? world.getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.getType() == typeIn).collect(Collectors.toSet()) : Collections.emptySet();
    }

    @SuppressWarnings("resource")
    @OnlyIn(Dist.CLIENT)
    public static Set<IRecipe<?>> findRecipesByType(IRecipeType<?> typeIn) {
        LOGGER.info("findRecipesByType - client");
        ClientWorld world = Minecraft.getInstance().world;
        return world != null ? world.getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.getType() == typeIn).collect(Collectors.toSet()) : Collections.emptySet();
    }


    /*
        Launches the GUI
     */
    public void openGUI(World worldIn, BlockPos pos, PlayerEntity player, HCCokeOvenControllerTile tileEntity) {
        INamedContainerProvider containerProvider = new INamedContainerProvider() {
            @Override
            public ITextComponent getDisplayName() {
                return new TranslationTextComponent("Honey Comb Coke Oven");
            }

            @Override
            public Container createMenu(int i, PlayerInventory playerInventory, PlayerEntity playerEntity) {
                return new HCCokeOvenContainer(i, worldIn, getControllerPos(), playerInventory, stateData);
            }
        };
        NetworkHooks.openGui((ServerPlayerEntity) player, containerProvider, ((HCCokeOvenControllerTile) tileEntity).getPos());
    }

    // ------------------------MULTI-BLOCK STUFFS ------------------------------------------------


    public void checkIfPlayerHasBlocksNeeded()  {

    }

    public HashMap<Block, Integer> getBlocksNeeded()  {
        this.failures = new HashMap<>();
        getMultiBlockMembers(getWorld(), null, false, this.getDirectionFacing());
        return this.failures;
    }

    public void autoBuildMultiBlock()  {

    }



    // override the always return true method
    @Override
    public boolean checkIfCorrectFrame(Block block) {
        return (block instanceof HCCokeOvenFrameBlock);
    }

    /*
  Updates all information in all multiblock members
 */
    private void updateMultiBlockMemberTiles(List<BlockPos> memberArray, boolean destroy) {
        for (int i = 0; i < memberArray.size(); i++) {
            BlockPos current = memberArray.get(i);
            Block currentBlock = world.getBlockState(current).getBlock();                   //check blocks
            if(currentBlock instanceof IMultiBlockFrame)  {
                IMultiBlockFrame frameBlock = (IMultiBlockFrame) currentBlock;
                if(destroy)  {
                    frameBlock.removeFromMultiBlock(world.getBlockState(current),current, world);
                }  else  {
                    frameBlock.addingToMultblock(world.getBlockState(current), current, world);          // change blockstate and create TE
                }
                TileEntity currentTile = getTileFromPos(world, current);                    // get new TE
                if (currentTile instanceof HCCokeOvenFrameTile) {
                    HCCokeOvenFrameTile castedCurrent = (HCCokeOvenFrameTile) currentTile;
                    if (destroy) {
                        castedCurrent.destroyMultiBlock();   // remove blockstate
                    } else {
                        castedCurrent.setupMultiBlock(getControllerPos());  // dp TE things needed for multiblock setup
                    }
                }
            }
        }
    }

        /*
      This attempts to find all the frame blocks in the multi-blocks to determine if we should form the multi-blocks or used to update frame blocks
      that the multi-blocks is being formed or destroyed.
     */
    public List<BlockPos> getMultiBlockMembers(World worldIn, PlayerEntity player, boolean destroy, Direction direction) {
        BlockPos centerPos = calcCenterBlock(direction);
        BlockPos lowCorner = findLowsestValueCorner(centerPos, direction, this.length, this.height, this.width);
        BlockPos correctLowCorner = new BlockPos(lowCorner.getX(), lowCorner.getY() + 1, lowCorner.getZ());
        List<BlockPos> multiblockMembers = new ArrayList();

        // checks the central slice part of the structure to ensure the correct blocks exist
        for (int y = 0; y < posArray.length; y++) {
            for (int z = 0; z < posArray[0].length; z++) {
                for (int x = 0; x < posArray[0][0].length; x++) {
                    Block correctBlock = posArray[y][z][x];                            // get the blocks that should be at these coord's
                    if (correctBlock == null) {                                // skip the "null" positions (don't care whats in here)
                        continue;
                    }
                    // get current blocks - adjusted for Direction
                    BlockPos current = indexShifterBlockPos(getDirectionFacing(), correctLowCorner, x, y, z, length, width);
                    Block currentBlock = world.getBlockState(current).getBlock();   // get the actual blocks at pos
                    if (currentBlock != correctBlock && !destroy) {
                        if (!destroy) {
                            if (player != null) {
                                String msg = "Could not form because of block at Coord at X:" + current.getX() + " Y:" + current.getY() + " Z:" + current.getZ();
                                player.sendStatusMessage(new TranslationTextComponent(msg), false);
                                msg = "Should be " + correctBlock.getBlock() + " not " + currentBlock.getBlock();
                                player.sendStatusMessage(new TranslationTextComponent(msg), true);
                                player.sendStatusMessage(new TranslationTextComponent(msg), false);
                            }
                            // increment block
                            if(this.failures != null)  {
                                failures.put(correctBlock, failures.get(correctBlock)+1);
                            }
                            LOGGER.info("Could not form because of " + current);
                            LOGGER.info("should be " + correctBlock + " not " + currentBlock);
                            return null;
                        }
                    }  else  {
                        // add blocks of things to be formed/deleted
                        multiblockMembers.add(current);
                    }
                }
            }
        }  //end loop
        return multiblockMembers;
    }

    /*
      Moves what blocks we are looking at with respect to the posArray
     */
    public BlockPos indexShifterBlockPos(Direction inputDirection, BlockPos low, int x, int y, int z, int length, int width)  {

        switch (inputDirection)  {
            case NORTH:
                return new BlockPos(low.getX() + x, low.getY() + y, low.getZ() + z);
            case SOUTH:
                return new BlockPos(low.getX() + x, low.getY() + y, low.getZ() + length - z - 1);
            case WEST:
                return new BlockPos(low.getX() + z, low.getY() + y, low.getZ() + x);
            case EAST:
                return new BlockPos(low.getX() + length - z - 1, low.getY() + y, low.getZ() + width - x - 1);
        }
        return null;
    }

    /*
      Driver for forming the multiblock
     */
    public void tryToFormMultiBlock(World worldIn, PlayerEntity player, BlockPos posIn) {
        List<BlockPos> multiblockMembers = getMultiBlockMembers(worldIn, player,false, getDirectionFacing());             // calc if every location has correct blocks
        if (multiblockMembers != null) {                                                                            // if above check has no errors then it will not be null
            setFormed(true);                                                                                        // change blocks state of controller
            updateMultiBlockMemberTiles(multiblockMembers, false);                                            // change blocks state of frames
            assignJobs();                                                                                           // sets "jobs" on frame members as needed
        }
    }

    /*
      Driver for destroying multi-blocks
     */
    public void destroyMultiBlock(World worldIn, BlockPos posIn) {
        if (!isFormed(getControllerPos())) {
            return;
        }
        setFormed(false);
        List<BlockPos> multiblockMembers = getMultiBlockMembers(worldIn, null, true, getDirectionFacing());
        if (multiblockMembers != null) {
            updateMultiBlockMemberTiles(multiblockMembers, true);
        }
    }

    /*
      Assigns out "jobs" to frame blocks that the controller needs to keep track of
     */
    public void assignJobs() {
        BlockPos inputPos = getRedstoneInBlockPos();
        BlockPos outputPos = getRedstoneOutBlockPos();
        TileEntity te = Multiblock.getTileFromPos(this.world, inputPos);
        if (te instanceof HCCokeOvenFrameTile) {
            ((HCCokeOvenFrameTile) te).setJob(JOB_REDSTONE_IN);
        }
        te = Multiblock.getTileFromPos(this.world, outputPos);
        if (te instanceof HCCokeOvenFrameTile) {
            ((HCCokeOvenFrameTile) te).setJob(JOB_REDSTONE_OUT);
        }
    }


    /*
      Calc's the position of the redstone input frame
     */
    public BlockPos getRedstoneInBlockPos() {
        if (this.redstoneIn == null) {
            this.redstoneIn = new BlockPos(getControllerPos().getX(), getControllerPos().getY() + 1, getControllerPos().getZ());
        }
        return this.redstoneIn;
    }

    /*
  Calc's the position of the redstone output frame
  */
    public BlockPos getRedstoneOutBlockPos() {
        if (this.redstoneOut == null) {
            this.redstoneOut = new BlockPos(getControllerPos().getX(), getControllerPos().getY() - 1, getControllerPos().getZ());
        }
        return this.redstoneOut;
    }


    public int getTankMaxSize()  {
        return fluidTank.getCapacity();
    }


    @Override
    public CompoundNBT getUpdateTag()  {
        return this.write(new CompoundNBT());
    }
    @Override
    public SUpdateTileEntityPacket getUpdatePacket()
    {
        CompoundNBT nbt = new CompoundNBT();
        this.write(nbt);

        // the number here is generally ignored for non-vanilla TileEntities, 0 is safest
        return new SUpdateTileEntityPacket(this.getPos(), 0, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet)
    {
        this.read(world.getBlockState(packet.getPos()), packet.getNbtCompound());
    }


}