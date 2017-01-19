package com.zixiken.dimdoors.items;

import java.util.HashMap;
import java.util.List;

import com.zixiken.dimdoors.DimDoors;
import com.zixiken.dimdoors.blocks.BlockDimDoorBase;
import com.zixiken.dimdoors.blocks.ModBlocks;
import com.zixiken.dimdoors.tileentities.DDTileEntityBase;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.block.SoundType;
import static net.minecraft.item.ItemDoor.placeDoor;
import net.minecraft.tileentity.TileEntity;

public abstract class ItemDoorBase extends ItemDoor {
    // Maps non-dimensional door items to their corresponding dimensional door item
    // Also maps dimensional door items to themselves for simplicity

    private static HashMap<ItemDoor, ItemDoorBase> doorItemMapping = new HashMap<ItemDoor, ItemDoorBase>();

    /**
     * door represents the non-dimensional door this item is associated with.
     * Leave null for none.
     *
     * @param vanillaDoor
     */
    public ItemDoorBase(Block block, ItemDoor vanillaDoor) {
        super(block);
        this.setMaxStackSize(64);
        this.setCreativeTab(DimDoors.dimDoorsCreativeTab);

        doorItemMapping.put(this, this);
        if (vanillaDoor != null) {
            doorItemMapping.put(vanillaDoor, this);
        }
    }

    @Override
    public abstract void addInformation(ItemStack stack, EntityPlayer playerIn, List<String> tooltip, boolean advanced);

    /**
     * Overridden in subclasses to specify which door block that door item will
     * place
     *
     * @return
     */
    protected abstract BlockDimDoorBase getDoorBlock();

    @Override
    public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World worldIn, EntityPlayer playerIn, EnumHand hand) {
        if (worldIn.isRemote) {
            return new ActionResult(EnumActionResult.FAIL, stack);
        }
        RayTraceResult hit = ItemDoorBase.doRayTrace(worldIn, playerIn, true);
        if (hit != null) {
            DimDoors.log(this.getClass(), "Hit is not null");
            BlockPos pos = hit.getBlockPos();
            if (worldIn.getBlockState(pos).getBlock() == ModBlocks.blockRift) {
                DimDoors.log(this.getClass(), "Block is a blockRift");
                EnumActionResult canDoorBePlacedOnGroundBelowRift
                        = onItemUse(stack, playerIn, worldIn, pos.down(2), hand, EnumFacing.UP,
                                (float) hit.hitVec.xCoord, (float) hit.hitVec.yCoord, (float) hit.hitVec.zCoord);
                return new ActionResult(canDoorBePlacedOnGroundBelowRift, stack);
            }
        }
        DimDoors.log(this.getClass(), "Hit is null");
        return new ActionResult(EnumActionResult.PASS, stack);
    }

    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer playerIn, World worldIn, BlockPos pos, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        //@todo also check for rift raytracing replacement;
        if (worldIn.isRemote) {
            return EnumActionResult.FAIL;
        }
        IBlockState iblockstate = worldIn.getBlockState(pos);
        Block block = iblockstate.getBlock();
        if (side != EnumFacing.UP || block == Blocks.AIR) {
            return EnumActionResult.FAIL;
        } else {

            if (!block.isReplaceable(worldIn, pos)) {
                pos = pos.offset(side); //we know that (side == EnumFacing.UP)
            }

            // Retrieve the actual door type that we want to use here.
            // It's okay if stack isn't an ItemDoor. In that case, the lookup will
            // return null, just as if the item was an unrecognized door type.
            ItemDoorBase mappedItem = doorItemMapping.get(stack.getItem());
            if (mappedItem == null) {
                return EnumActionResult.FAIL;
            }
            BlockDimDoorBase doorBlock = mappedItem.getDoorBlock();

            if (playerIn.canPlayerEdit(pos, side, stack) && playerIn.canPlayerEdit(pos.up(), side, stack)
                    && doorBlock.canPlaceBlockAt(worldIn, pos)) {

                TileEntity possibleOldRift = worldIn.getTileEntity(pos.up());
                //start logging code
                if (possibleOldRift instanceof DDTileEntityBase) { //
                    DDTileEntityBase oldRift = (DDTileEntityBase) possibleOldRift;
                    DimDoors.log(this.getClass(), "Old Rift rift-ID before placement: " + oldRift.riftID);
                }
                //end of logging code
                EnumFacing enumfacing = EnumFacing.fromAngle((double) playerIn.rotationYaw);
                int i = enumfacing.getFrontOffsetX();
                int j = enumfacing.getFrontOffsetZ();
                boolean flag = i < 0 && hitZ < 0.5F || i > 0 && hitZ > 0.5F || j < 0 && hitX > 0.5F || j > 0 && hitX < 0.5F;
                placeDoor(worldIn, pos, enumfacing, doorBlock, flag);
                SoundType soundtype = worldIn.getBlockState(pos).getBlock().getSoundType(worldIn.getBlockState(pos), worldIn, pos, playerIn);
                worldIn.playSound(playerIn, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
                --stack.stackSize;

                DDTileEntityBase newTileEntityDimDoor = (DDTileEntityBase) worldIn.getTileEntity(pos.up());
                if (possibleOldRift instanceof DDTileEntityBase) { //
                    DDTileEntityBase oldRift = (DDTileEntityBase) possibleOldRift;
                    DimDoors.log(this.getClass(), "Old Rift rift-ID after placement: " + oldRift.riftID);
                    newTileEntityDimDoor.loadDataFrom(oldRift);
                } else {
                    newTileEntityDimDoor.register();
                }
                DimDoors.log(this.getClass(), "New Door rift-ID after placement: " + newTileEntityDimDoor.riftID);

                return EnumActionResult.SUCCESS;
            } else {
                return EnumActionResult.FAIL;
            }
        }
    }

    public static boolean canPlace(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);

        return (state.getBlock() == ModBlocks.blockRift || state.equals(Blocks.AIR) || state.getMaterial().isReplaceable());
    }

    /**
     * Copied from minecraft Item.class TODO we probably can improve this
     *
     * @param world
     * @param player
     * @param useLiquids
     * @return
     */
    protected static RayTraceResult doRayTrace(World world, EntityPlayer player, boolean useLiquids) {
        float f = player.rotationPitch;
        float f1 = player.rotationYaw;
        double d0 = player.posX;
        double d1 = player.posY + (double) player.getEyeHeight();
        double d2 = player.posZ;
        Vec3d vec3 = new Vec3d(d0, d1, d2);
        float f2 = MathHelper.cos(-f1 * 0.017453292F - (float) Math.PI);
        float f3 = MathHelper.sin(-f1 * 0.017453292F - (float) Math.PI);
        float f4 = -MathHelper.cos(-f * 0.017453292F);
        float f5 = MathHelper.sin(-f * 0.017453292F);
        float f6 = f3 * f4;
        float f7 = f2 * f4;
        double d3 = 5.0D;
        if (player instanceof EntityPlayerMP) {
            d3 = ((EntityPlayerMP) player).interactionManager.getBlockReachDistance();
        }
        Vec3d vec31 = vec3.addVector((double) f6 * d3, (double) f5 * d3, (double) f7 * d3);
        return world.rayTraceBlocks(vec3, vec31, useLiquids, !useLiquids, false);
    }

    public void translateAndAdd(String key, List<String> list) {
        for (int i = 0; i < 10; i++) {
            /*if(StatCollector.canTranslate(key+Integer.toString(i))) {
				String line = StatCollector.translateToLocal(key + Integer.toString(i));
				list.add(line);
			} else */ break;
        }
    }
}