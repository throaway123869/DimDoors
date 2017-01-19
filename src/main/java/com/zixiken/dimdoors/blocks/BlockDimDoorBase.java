package com.zixiken.dimdoors.blocks;

import java.util.Random;

import com.zixiken.dimdoors.DimDoors;
import com.zixiken.dimdoors.shared.RiftRegistry;
import com.zixiken.dimdoors.tileentities.DDTileEntityBase;
import com.zixiken.dimdoors.tileentities.TileEntityDimDoor;
import javax.annotation.Nullable;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class BlockDimDoorBase extends BlockDoor implements IDimDoor, ITileEntityProvider {

    public BlockDimDoorBase(Material material) {
        super(material);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
        enterDimDoor(world, pos, entity);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, @Nullable ItemStack heldItem, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!checkCanOpen(world, pos, player)) {
            return false;
        }

        if (state.getValue(BlockDoor.HALF) == EnumDoorHalf.UPPER) {
            pos = pos.down();
            state = world.getBlockState(pos);
        }

        if (state.getBlock() != this) {
            return false;
        } else {
            state = state.cycleProperty(BlockDoor.OPEN);
            world.setBlockState(pos, state, 2);
            world.markBlockRangeForRenderUpdate(pos, pos.up());
            return true;
        }
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return state.getValue(BlockDoor.HALF) == EnumDoorHalf.UPPER;
    }

    //Called to update the render information on the tile entity. Could probably implement a data watcher,
    //but this works fine and is more versatile I think. 
    public BlockDimDoorBase updateAttachedTile(World world, BlockPos pos) {
        DimDoors.proxy.updateDoorTE(this, world, pos);
        return this;
    }

    public boolean isDoorOnRift(World world, BlockPos pos) {
        return true;
    }

    @Override
    public void updateTick(World par1World, BlockPos pos, IBlockState state, Random rand) {
        updateAttachedTile(par1World, pos);
    }

    /**
     * only called by clickMiddleMouseButton , and passed to
     * inventory.setCurrentItem (along with isCreative)
     */
    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
        return new ItemStack(this.getItemDoor(), 1, 0);
    }

    /**
     * Returns the ID of the items to drop on destruction.
     */
    @Override
    public Item getItemDropped(IBlockState state, Random random, int fortune) {
        return isUpperDoorBlock(state) ? null : this.getItemDoor();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.getItemDoor(), 1, 0);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileEntityDimDoor();
    }

    @Override
    public void enterDimDoor(World world, BlockPos pos, Entity entity) {
        // Check that this is the top block of the door
        IBlockState state = world.getBlockState(pos.down());
        if (!world.isRemote && state.getBlock() == this) {
            if (state.getValue(BlockDoor.OPEN)
                    && entity instanceof EntityPlayer
                    && isEntityFacingDoor(state, (EntityLivingBase) entity)) {
                this.toggleDoor(world, pos, false);
                //DimDoors.log("RiftID = " + getRiftTile(world, pos, world.getBlockState(pos)).riftID);
            }
        } else {
            BlockPos up = pos.up();
            if (world.getBlockState(up).getBlock() == this) {
                enterDimDoor(world, up, entity);
            }
        }
    }

    public boolean isUpperDoorBlock(IBlockState state) {
        return state.getValue(BlockDoor.HALF) == EnumDoorHalf.UPPER;
    }

    public boolean checkCanOpen(World world, BlockPos pos, EntityPlayer player) {
        return true;
    }

    protected static boolean isEntityFacingDoor(IBlockState state, EntityLivingBase entity) {
        // Although any entity has the proper fields for this check,
        // we should only apply it to living entities since things
        // like Minecarts might come in backwards.
        return (state.getValue(BlockDoor.FACING) == EnumFacing.fromAngle(entity.rotationYaw));
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        DDTileEntityBase origRift = null;
        boolean isTopHalf = state.getValue(BlockDoor.HALF) == EnumDoorHalf.UPPER;
        if (isTopHalf) {
            origRift = (DDTileEntityBase) world.getTileEntity(pos);
            RiftRegistry.Instance.setLastChangedRift(origRift);
        }
        super.breakBlock(world, pos, state);
        if (isTopHalf) {
            world.setBlockState(pos, ModBlocks.blockRift.getDefaultState());
            DDTileEntityBase newRift = (DDTileEntityBase) world.getTileEntity(pos);
            newRift.loadDataFrom(origRift);
            DimDoors.log(this.getClass(), "New Rift rift-ID after breaking door " + newRift.riftID);
        }
    }

    public DDTileEntityBase getRiftTile(World world, BlockPos pos, IBlockState state) {
        TileEntity tileEntity;
        if (state.getValue(BlockDoor.HALF) == EnumDoorHalf.LOWER) {
            tileEntity = world.getTileEntity(pos.up());
        } else {
            tileEntity = world.getTileEntity(pos);
        }
        return (DDTileEntityBase) tileEntity;
    }

    @Override
    public boolean canPlaceBlockAt(World worldIn, BlockPos pos) {
        IBlockState stateBot = worldIn.getBlockState(pos);
        IBlockState stateTop = worldIn.getBlockState(pos.up());
        return pos.getY() >= worldIn.getHeight() - 1 ? false
                : worldIn.getBlockState(pos.down()).isSideSolid(worldIn, pos.down(), EnumFacing.UP)
                && canPlaceBottomAt(worldIn, pos, stateBot) && canPlaceTopAt(worldIn, pos, stateTop);
    }

    private boolean canPlaceBottomAt(World worldIn, BlockPos pos, IBlockState state) {
        return (state.equals(Blocks.AIR) || state.getBlock().isReplaceable(worldIn, pos));
    }

    private boolean canPlaceTopAt(World worldIn, BlockPos pos, IBlockState state) {
        return (state.getBlock() == ModBlocks.blockRift || state.equals(Blocks.AIR) || state.getMaterial().isReplaceable());
    }
}