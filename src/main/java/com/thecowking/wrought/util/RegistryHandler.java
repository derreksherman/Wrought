package com.thecowking.wrought.util;

import com.thecowking.wrought.Wrought;
import com.thecowking.wrought.blocks.blast_furance.BlastFurnaceBrickControllerBlock;
import com.thecowking.wrought.blocks.blast_furance.BlastFurnaceBrickFrameBlock;
import com.thecowking.wrought.blocks.coke_block.CokeBlock;
import com.thecowking.wrought.blocks.honey_comb_coke_oven.*;
import com.thecowking.wrought.init.FluidInit;
import com.thecowking.wrought.inventory.containers.HCCokeOvenContainer;
import com.thecowking.wrought.inventory.containers.HCCokeOvenContainerMultiblock;
import com.thecowking.wrought.items.blocks.BlockItemBase;
import com.thecowking.wrought.items.blocks.CokeBlockItem;
import com.thecowking.wrought.items.items.AshItem;
import com.thecowking.wrought.items.items.CokeBrickItem;
import com.thecowking.wrought.items.items.CokeItem;
import com.thecowking.wrought.items.items.SootItem;
import com.thecowking.wrought.tileentity.blast_furance.BlastFurnaceBrickControllerTile;
import com.thecowking.wrought.tileentity.blast_furance.BlastFurnaceBrickFrameTile;
import com.thecowking.wrought.tileentity.honey_comb_coke_oven.HCCokeOvenControllerTile;
import com.thecowking.wrought.tileentity.honey_comb_coke_oven.HCCokeOvenFrameTile;
import net.minecraft.block.Block;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class RegistryHandler {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Wrought.MODID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Wrought.MODID);
    private static final DeferredRegister<TileEntityType<?>> TILES = DeferredRegister.create(ForgeRegistries.TILE_ENTITIES, Wrought.MODID);
    private static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS,  Wrought.MODID);




    public static void init()  {
        FluidInit.FLUIDS.register(FMLJavaModLoadingContext.get().getModEventBus());
        ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        BLOCKS.register(FMLJavaModLoadingContext.get().getModEventBus());
        TILES.register(FMLJavaModLoadingContext.get().getModEventBus());
        CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());
        RecipeSerializerInit.RECIPE_SERIALIZERS.register(FMLJavaModLoadingContext.get().getModEventBus());
    }

    //Honey Comb Coke Controller
    public static final RegistryObject<Block> H_C_COKE_CONTROLLER_BLOCK = BLOCKS.register("h_c_coke_controller_block", HCCokeOvenControllerBlock::new);
    public static final RegistryObject<Item> H_C_COKE_CONTROLLER_BLOCK_ITEM = ITEMS.register("h_c_coke_controller_block", () -> new BlockItemBase(H_C_COKE_CONTROLLER_BLOCK.get()));
    public static final RegistryObject<TileEntityType<HCCokeOvenControllerTile>> H_C_COKE_CONTROLLER_TILE = TILES.register("h_c_coke_controller_block", () -> TileEntityType.Builder.create(HCCokeOvenControllerTile::new, H_C_COKE_CONTROLLER_BLOCK.get()).build(null));

    //Honey Comb Coke Frame
    public static final RegistryObject<Block> H_C_COKE_FRAME_BLOCK = BLOCKS.register("h_c_coke_frame_block", HCCokeOvenFrameBlock::new);
    public static final RegistryObject<Item> H_C_COKE_FRAME_BLOCK_ITEM = ITEMS.register("h_c_coke_frame_block", () -> new BlockItemBase(H_C_COKE_FRAME_BLOCK.get()));
    public static final RegistryObject<TileEntityType<HCCokeOvenFrameTile>> H_C_COKE_FRAME_TILE = TILES.register("h_c_coke_frame_block", () -> TileEntityType.Builder.create(HCCokeOvenFrameTile::new, H_C_COKE_FRAME_BLOCK.get()).build(null));


    //Honey Comb Coke Frame Slab
    public static final RegistryObject<Block> H_C_COKE_FRAME_SLAB = BLOCKS.register("h_c_coke_frame_slab", HCCokeOvenFrameSlab::new);
    public static final RegistryObject<Item> H_C_COKE_FRAME_SLAB_ITEM = ITEMS.register("h_c_coke_frame_slab", () -> new BlockItemBase(H_C_COKE_FRAME_SLAB.get()));

    //Honey Comb Coke Frame Stairs
    public static final RegistryObject<Block> H_C_COKE_FRAME_STAIR = BLOCKS.register("h_c_coke_frame_stair", HCCokeOvenFrameStairs::new);
    public static final RegistryObject<Item> H_C_COKE_FRAME_STAIR_ITEM = ITEMS.register("h_c_coke_frame_stair", () -> new BlockItemBase(H_C_COKE_FRAME_STAIR.get()));

    //Honey Comb Coke Multi-Block
    public static final RegistryObject<ContainerType<HCCokeOvenContainerMultiblock>> H_C_CONTAINER = CONTAINERS.register("h_c_coke_controller_block", () -> IForgeContainerType.create((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        World world = inv.player.getEntityWorld();
        return new HCCokeOvenContainerMultiblock(windowId, world, pos, inv);
    }));

    //Honey Comb Coke Multi-Block
    public static final RegistryObject<ContainerType<HCCokeOvenContainer>> H_C_CONTAINER_BUILDER = CONTAINERS.register("h_c_coke_controller_block_builder", () -> IForgeContainerType.create((windowId, inv, data) -> {
        BlockPos pos = data.readBlockPos();
        World world = inv.player.getEntityWorld();
        return new HCCokeOvenContainer(windowId, world, pos, inv);
    }));
    //Coke Item
    public static final RegistryObject<Item> COKE = ITEMS.register("coke", CokeItem::new);

    //CokeBrick Item
    public static final RegistryObject<Item> COKE_BRICK_ITEM = ITEMS.register("coke_brick_item", CokeBrickItem::new);

    //Soot Item
    public static final RegistryObject<Item> SOOT = ITEMS.register("soot", SootItem::new);

    //Ash Item
    public static final RegistryObject<Item> ASH = ITEMS.register("ash", AshItem::new);

    //Coke Block
    public static final RegistryObject<Block> COKE_BLOCK = BLOCKS.register("coke_block", CokeBlock::new);
    public static final RegistryObject<Item> COKE_BLOCK_ITEM = ITEMS.register("coke_block", () -> new CokeBlockItem(COKE_BLOCK.get()));


    //Blast Furnace Controller
    public static final RegistryObject<Block> BLAST_FURANCE_BRICK_CONTROLLER = BLOCKS.register("blast_furnace_brick_controller", BlastFurnaceBrickControllerBlock::new);
    public static final RegistryObject<Item> BLAST_FURANCE_BRICK_CONTROLLER_ITEM = ITEMS.register("blast_furnace_brick_controller", () -> new BlockItemBase(BLAST_FURANCE_BRICK_CONTROLLER.get()));
    public static final RegistryObject<TileEntityType<BlastFurnaceBrickControllerTile>> BLAST_FURANCE_BRICK_CONTROLLER_TILE = TILES.register("blast_furnace_brick_controller", () -> TileEntityType.Builder.create(BlastFurnaceBrickControllerTile::new, BLAST_FURANCE_BRICK_CONTROLLER.get()).build(null));



    //Blast Furnace Frame
    public static final RegistryObject<Block> BLAST_FURANCE_BRICK_FRAME = BLOCKS.register("blast_furnace_brick_frame", BlastFurnaceBrickFrameBlock::new);
    public static final RegistryObject<Item> BLAST_FURANCE_BRICK_FRAME_ITEM = ITEMS.register("blast_furnace_brick_frame", () -> new BlockItemBase(BLAST_FURANCE_BRICK_FRAME.get()));
    public static final RegistryObject<TileEntityType<BlastFurnaceBrickFrameTile>> BLAST_FURANCE_BRICK_FRAME_TILE = TILES.register("blast_furnace_brick_frame", () -> TileEntityType.Builder.create(BlastFurnaceBrickFrameTile::new, BLAST_FURANCE_BRICK_FRAME.get()).build(null));





    //Creosote Bucket
    public static final RegistryObject<BucketItem> CREOSOTE_BUCKET = ITEMS.register("creosote_bucket", () -> new BucketItem(() -> FluidInit.CREOSOTE_FLUID.get(), new Item.Properties().maxStackSize(1)) );

}
