package com.thecowking.wrought.tileentity;

import com.thecowking.wrought.data.IMultiblockData;
import com.thecowking.wrought.data.MultiblockData;
import com.thecowking.wrought.inventory.slots.*;
import com.thecowking.wrought.recipes.IWroughtRecipe;
import com.thecowking.wrought.tileentity.honey_comb_coke_oven.HCCokeOvenFrameTile;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SUpdateTileEntityPacket;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.thecowking.wrought.data.MultiblockData.*;

public class MultiBlockControllerTile extends MultiBlockTile implements ITickableTileEntity {
    private static final Logger LOGGER = LogManager.getLogger();

    protected BlockPos redstoneIn;
    protected BlockPos redstoneOut;

    // tracks if the tile entity needs a block update
    protected boolean needUpdate = false;

    // holds the string that is displayed on the status buttona
    protected String status;

    // used to track when we can start an operations
    // -> didnt want something that processes too fast
    protected int tickCounter;

    public int timeElapsed = 0;
    public int timeComplete = 0;
    public int currentHeatLevel = 0;
    public int maxHeatLevel = 3200; // iron melts at  2800



    protected int needsUpdate = 0;

    protected IMultiblockData data;

    protected final int TICKSPEROPERATION = 20;

    protected boolean isRunning = false;
    protected boolean clogged = false;

    protected int fuelIndicator;


    //Input Slots
    protected InputItemHandler inputSlots;
    //Output Slots
    protected OutputItemHandler outputSlots;
    //Items currently being cooked
    protected ItemStack[] processingItemStacks;
    //Backlog Items
    protected ItemStack[] itemBacklogs;

    /*
        Basic operation
        item inserted into input slot -> moves into processing stack -> attempt insert into output slot -> leftovers in backlog
     */

    //Fuel Slot
    protected InputFuelHandler fuelInputSlot;

    //Holds all the handlers
    protected List<IItemHandlerModifiable> allHandlers;
    //Handlers for when the user uses the GUI to interact with slots
    protected LazyOptional<IItemHandler> everything;
    //Handlers when the world interacts with the multiblock
    protected LazyOptional<IItemHandler> automation;



    public int numInputSlots;
    public int numOutputSlots;
    public boolean hasFuelSlot;


    public MultiBlockControllerTile(TileEntityType<?> tileEntityTypeIn, int numberInputSlots, int numberOutputSlots, boolean fuelSlot, IMultiblockData data) {
        super(tileEntityTypeIn);
        this.data = data;
        this.status = "not init";
        this.numInputSlots = numberInputSlots;
        this.numOutputSlots = numberOutputSlots;
        this.hasFuelSlot = fuelSlot;

        initSlots();



    }

    public void initSlots()  {

        // input
        this.inputSlots = new InputItemHandler(numInputSlots, this, null, "input_slots");
        // output
        this.outputSlots = new OutputItemHandler(numOutputSlots);

        //fuel
        if(this.hasFuelSlot)  {
            this.fuelInputSlot = new InputFuelHandler(1, this, null, "fuel");
        }

        // processing slots
        this.processingItemStacks = new ItemStack[data.getNumberItemOutputSlots()];

        //backlogs
        this.itemBacklogs = new ItemStack[data.getNumberItemOutputSlots()];

        for(int i = 0; i < this.outputSlots.getSlots(); i++)  {
            processingItemStacks[i] = ItemStack.EMPTY;
            itemBacklogs[i] = ItemStack.EMPTY;
        }
    }

    public void buildAllHandlers()  {
        this.allHandlers = new ArrayList<>();
        this.allHandlers.add(inputSlots);
        this.allHandlers.add(outputSlots);
        if(this.hasFuelSlot)  {
            allHandlers.add(fuelInputSlot);
        }
    }





    /*
    Does the needed checks and casting to see if current BlockPos holds a correct member of multi-blocks
 */
    private boolean checkIfCorrectFrame(BlockPos currentPos)  {
        Block currentBlock = world.getBlockState(currentPos).getBlock();
        BlockState currentState = world.getBlockState(currentPos);
        if( currentState.hasTileEntity() || !(currentState.isAir(world, currentPos))) {
            return checkIfCorrectFrame(currentBlock);
        }
        return false;
    }

    public String getStatus()  {
        return this.status;
    }



    /*
      Grabs the Frame Tile Entity
     */
    private MultiBlockFrameTile getFrameTile(BlockPos posIn)  {
        TileEntity te = getTileFromPos(this.world, posIn);
        if( te != null && te instanceof MultiBlockFrameTile) {
            return (MultiBlockFrameTile) te;
        }
        return null;
    }



    /*
        Launches the GUI for the completed multiblock
     */
    public void openGUI(World world, PlayerEntity player, boolean isFormed) {

        NetworkHooks.openGui((ServerPlayerEntity) player, this.data.getContainerProvider(world, this.pos, isFormed), this.pos);
    }




    public IMultiblockData getData()  {
        return this.data;
    }
    public BlockPos getPos()  {return this.pos;}


    /*
      This is called when a controller is right clicked by a player when the multi-blocks is not formed
      Checks to make sure that the player is holding the correct item in hand to form the multi-blocks.
     */
    public boolean isValidMultiBlockFormer(Item item)  {
        return item == Items.STICK;
    }

    /*
      Returns the controllers facing direction
     */
    public Direction getDirectionFacing()  {
        return this.world.getBlockState(this.pos).get(BlockStateProperties.FACING);
    }

    /*
      Checks a frame blocks blockstate to see if it is powered by redstone
     */
    public boolean isRedstonePowered(BlockPos posIn)  {
        if(this.redstoneIn != null)  {
            MultiBlockFrameTile frameTile = getFrameTile(this.redstoneIn);
            return this.world.isBlockPowered(posIn);
            }
        return false;
    }

    /*
      Sets the value for the frame blocks REDSTONE blockstate
     */
    public void sendOutRedstone(int power)  {
        if(this.redstoneOut != null)  {
            MultiBlockFrameTile frameTile = getFrameTile(this.redstoneOut);
            if(frameTile != null)  {
                frameTile.setRedstonePower(power);
            }
        }
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
      Lets nearby blocks know we need an update
     */
    protected void blockUpdate() {
        this.world.notifyBlockUpdate(this.pos, this.getBlockState(), this.getBlockState(), Constants.BlockFlags.BLOCK_UPDATE);
    }

    public boolean checkIfCorrectFrame(Block block)  {
        return true;
    }

    public BlockPos getControllerPos() {
        return this.pos;
    }

    public boolean isFormed()  {
        return this.getBlockState().get(MultiblockData.FORMED);
    }


    @Override
    public void tick() {
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


    protected boolean processAllBackLog()  {
        for(int i = 0; i < itemBacklogs.length; i++)  {
            if(!(processItemBackLog(i)))  {
                return false;
            }
        }
        return true;
    }

    protected boolean processItemBackLog(int index)  {
        if(this.itemBacklogs[index] == ItemStack.EMPTY)  {return true;}

        // used to check if anything was processed
        ItemStack oldBacklog = this.itemBacklogs[index];

        // attempt to insert backlog into the item output slot
        // whatever is leftover is saved into itemBacklog
        this.itemBacklogs[index] = outputSlots.internalInsertItem(index, this.itemBacklogs[index].copy(), false);

        // check for changes
        if(this.itemBacklogs[index] != oldBacklog)  {this.needUpdate = true;}

        if(this.itemBacklogs[index] == ItemStack.EMPTY)  {return true;}

        this.status = "Output is full";
        return false;
    }


    /*
        Called to check if the processing item(s) have cooked long enough to be finished
     */
    protected boolean processing()  {
        boolean localClog = false;
        if(this.isRunning) {
            this.needUpdate = true;
            this.timeElapsed = 0;
            // go thru all processingItemStacks and move into output slots
            for(int i = 0; i < processingItemStacks.length; i++)  {
                if(this.processingItemStacks[i] == ItemStack.EMPTY)  {
                    LOGGER.info("attempted to process empty itemstack");
                    continue;
                }
                this.itemBacklogs[i] = outputSlots.internalInsertItem(i, processingItemStacks[i].copy(), false);
                // check if somehow something got left over
                if(this.itemBacklogs[i] != ItemStack.EMPTY)  {
                    localClog = true;
                }
                processingItemStacks[i] = ItemStack.EMPTY;
            }
        }
        if(localClog)  {
            this.clogged = true;
            this.status = "item clogged";
            return false;
        }
        return true;
    }


    public boolean finishedProcessingCurrentOperation()  {
        // Check if there is a previous item and the item has "cooked" long enough
        if (this.isRunning && this.timeElapsed++ < this.timeComplete) {
            this.needUpdate = true;
            this.timeElapsed++;
            return false;
        }
        // item has cooked long enough -> insert outputs and move onto next operation
        return true;
    }

    // should overwrite
    protected boolean areOutputsFull()  {
        for(int i = 0; i < data.getNumberItemOutputSlots(); i++)  {
            if(outputSlots.getStackInSlot(i).getCount() >= outputSlots.getStackInSlot(i).getMaxStackSize())  {
                this.status = "Not enough output room to process current recipe";
                return true;
            }
        }
        return false;
    }


    protected boolean consumeFuel(IWroughtRecipe currentRecipe)  {
        ItemStack fuelStack = this.fuelInputSlot.getStackInSlot(0);
        if(!validFuel(currentRecipe, fuelStack))  {
            LOGGER.info("NOT VALID FUEL");
            this.status = "Not a valid fuel";
            return false;
        }
        int burnTime = ForgeHooks.getBurnTime(fuelStack);
        if(raiseHeatLevel(burnTime))  {
            this.fuelInputSlot.getStackInSlot(0).shrink(1);
        }
        return true;
    }

    protected boolean validFuel(IWroughtRecipe currentRecipe, ItemStack fuelStack)  {
        LOGGER.info("valid fuel");

        // Check if the recipe only wants specific fuels
        if(currentRecipe.getFuel() != Ingredient.EMPTY)  {
            if(currentRecipe.getFuel().test(fuelStack))  {
                return true;
            }
            return false;
        }
        // Check if the itemstack is burnable
        if(fuelStack.getBurnTime() != 0)  {
            return true;
        }
        return false;
    }


    // I know this isnt right. Ill try to brush off my physics skills someday
    protected void heatDisspation()  {
        if(this.currentHeatLevel < 0)  {
            this.currentHeatLevel = 0;
        }  else  {
            this.currentHeatLevel -= this.currentHeatLevel / 100;
        }
    }


    protected boolean raiseHeatLevel(int burnTime)  {
        if(this.currentHeatLevel + burnTime > this.maxHeatLevel)  return false;
        this.currentHeatLevel += burnTime / 100;
        return true;
    }

    protected boolean heatHighEnough(IWroughtRecipe currentRecipe)  {
        int currentHeat = currentRecipe.getHeat();
        if(currentHeat == 0)  { return true;}  // don't care about heat here
        if(currentHeat < this.currentHeatLevel)  {
            if(!consumeFuel(currentRecipe))  return false;
            if(currentHeat < this.currentHeatLevel)  {
                this.status = "Not enough heat";
                return false;
            }
        }
        return true;
    }


    /*
  Flips states if machine is changing from off -> on or from on -> off
 */
    protected void machineChangeOperation(boolean online) {
        if (online == this.isRunning) {
            return;
        }
        this.isRunning = online;
        setOn(online);
        if(online)  {
            sendOutRedstone(15);
            this.status = "Processing";
        }  else  {
            sendOutRedstone(0);
        }
    }

    protected boolean isRunning()  {
        return this.isRunning;
    }



    /*
    Checks if redstone signal is on / off and turns off machine if on
 */
    protected boolean redstonePowered()  {

        if(isRedstonePowered(this.redstoneIn)) {
            machineChangeOperation(false);
            this.status = "Red stone Turning off";
            return true;
        }
        return false;
    }

    /*
      Assigns out "jobs" to frame blocks that the controller needs to keep track of
      eg: what blocks output / watch input for redstone
     */
    public void assignJobs() {
        BlockPos inputPos = data.getRedstoneInBlockPos(this.pos);
        BlockPos outputPos = data.getRedstoneOutBlockPos(this.pos);
        TileEntity te = MultiblockData.getTileFromPos(this.world, inputPos);
        if (te instanceof HCCokeOvenFrameTile) {
            ((HCCokeOvenFrameTile) te).setJob(JOB_REDSTONE_IN);
        }
        te = MultiblockData.getTileFromPos(this.world, outputPos);
        if (te instanceof HCCokeOvenFrameTile) {
            ((HCCokeOvenFrameTile) te).setJob(JOB_REDSTONE_OUT);
        }
    }

    public boolean isPrimarySlotEmpty()  {
        return this.inputSlots.getStackInSlot(0).isEmpty();
    }


    @Nullable
    public IWroughtRecipe getRecipe() {
        LOGGER.info("machine get recipe");
        Set<IRecipe<?>> recipes = data.getRecipesByType(this.world);
        LOGGER.info("num recipes = " + recipes.size());
        for (IRecipe<?> iRecipe : recipes) {
            IWroughtRecipe recipe = (IWroughtRecipe) iRecipe;
            //LOGGER.info(recipe.getInput(0).getMatchingStacks()[0]);


            if (recipe.matches(new RecipeWrapper(this.inputSlots), this.world)) {
                return recipe;
            }
        }
        LOGGER.info("could not find recipe");
        return null;
    }


    public boolean itemUsedInRecipe(ItemStack input, int index) {
        Set<IRecipe<?>> recipes = data.getRecipesByType(this.world);
        LOGGER.info(recipes);
        for (IRecipe<?> iRecipe : recipes) {
            IWroughtRecipe recipe = (IWroughtRecipe) iRecipe;
            LOGGER.info("testing " + recipe.getInput(index).toString());
            LOGGER.info(" with " + input.getTranslationKey());
            LOGGER.info( " slot = " + index  );
            if(recipe.getInput(index).test(input))  {
                return true;
            }
        }
        return false;
    }


    /*
    Check if a new item has a recipe that the oven can use
 */
    public boolean recipeChecker(IWroughtRecipe currentRecipe)  {
        // check if we have a recipe for item
        if (currentRecipe == null) {
            machineChangeOperation(false);
            this.status = "No Recipe for Item";
            return false;
        }
        // check if it all matches
        if(currentRecipe.matches(new RecipeWrapper(this.inputSlots), this.world))  {
            return true;
        }
        return false;
    }




    //TODO - cut down lag by making state machine based off of insertion

    /*

     */
    public void attemptRunOperation() {
        heatDisspation();
        LOGGER.info("backlog");

        // Check if any of the item backlogs is clogged - note that another operation will not happen until tickCount has passed if these fail
        if(!processAllBackLog())  { return; }
        // check if redstone is turning machine off
        LOGGER.info("redstone");

        if(redstonePowered())  { return; }
        // increment how long current item has cooked
        LOGGER.info("current process");

        if(!finishedProcessingCurrentOperation())  { return; }
        // check to make sure output is not full before starting another operation
        LOGGER.info("output full check");

        if(areOutputsFull())  {return; }
        LOGGER.info("processing");

        // moves things in processingItemStacks into OutputSlots
        if(!processing())  {return; }
        LOGGER.info("RECIPE");
        // New operation and new recipe
        IWroughtRecipe currentRecipe = this.getRecipe();
        if (!(recipeChecker(currentRecipe))) { return; }
        LOGGER.info("good recipe");
        if(!heatHighEnough(currentRecipe))  return;
        LOGGER.info("enough heat");
        mutliBlockOperation(currentRecipe);


    }

    /*
        Moves Recipes into processingItemStacks
     */
    public void mutliBlockOperation(IWroughtRecipe currentRecipe)  {
        if(currentRecipe == null)  {return;}

        //FluidStack fluidOutput = this.getRecipe(getPrimaryItemInput()).getRecipeFluidStackOutput();
        this.timeComplete = currentRecipe.getBurnTime();

        // consume all inputs that are needed
        for(int i = 0; i < currentRecipe.getNumInputs(); i++)  {
            // TODO - shrink by dynamic num
            this.inputSlots.getStackInSlot(i).shrink(1);
        }

        // insert outputs into processing
        for(int i = 0; i < currentRecipe.getNumOutputs(); i++)  {
            this.processingItemStacks[i] = currentRecipe.getOutput(i);
        }
        this.needUpdate = true;
        machineChangeOperation(true);
    }




    @Override
    public CompoundNBT getUpdateTag()  {
        return this.write(new CompoundNBT());
    }
    @Override
    public SUpdateTileEntityPacket getUpdatePacket() {
        CompoundNBT nbt = new CompoundNBT();
        this.write(nbt);

        // the number here is generally ignored for non-vanilla TileEntities, 0 is safest
        return new SUpdateTileEntityPacket(this.getPos(), 0, nbt);
    }

    @Override
    public void onDataPacket(NetworkManager net, SUpdateTileEntityPacket packet) {
        this.read(world.getBlockState(packet.getPos()), packet.getNbtCompound());
    }


    /*
        Tells the server what to save to disk
     */
    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        this.timeElapsed = nbt.getInt(BURN_TIME);
        this.timeComplete = nbt.getInt(BURN_COMPLETE_TIME);
        this.status = nbt.getString(STATUS);
        if(this.hasFuelSlot)  {
            this.currentHeatLevel = nbt.getInt(HEAT_LEVEL);
        }
    }

    /*
        Tells the server what to read from disk on chunk load
     */
    @Override
    public CompoundNBT write(CompoundNBT tag) {
        tag = super.write(tag);
        tag.putInt(BURN_TIME, this.timeElapsed);
        tag.putInt(BURN_COMPLETE_TIME, this.timeComplete);
        tag.putString(STATUS, this.status);
        if(this.hasFuelSlot)  {
            tag.putInt(HEAT_LEVEL, this.currentHeatLevel);
        }
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
        return super.getCapability(cap, side);
    }


    @SuppressWarnings("resource")
    @OnlyIn(Dist.CLIENT)
    public static Set<IRecipe<?>> findRecipesByType(IRecipeType<?> typeIn) {
        ClientWorld world = Minecraft.getInstance().world;
        return world != null ? world.getRecipeManager().getRecipes().stream()
                .filter(recipe -> recipe.getType() == typeIn).collect(Collectors.toSet()) : Collections.emptySet();
    }



}
