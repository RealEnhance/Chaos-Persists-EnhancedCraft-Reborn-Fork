/*
 * Decompiled with CFR 0_125.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.fml.relauncher.Side
 *  net.minecraftforge.fml.relauncher.SideOnly
 *  com.astryxion.chaospersists.ItemMinersDream
 *  com.astryxion.chaospersists.ChaosPersists
 *  com.astryxion.chaospersists.compat.minecraft.block.Block
 *  com.astryxion.chaospersists.compat.minecraft.block.BlockLiquid
 *  com.astryxion.chaospersists.compat.minecraft.block.BlockSand
 *  net.minecraft.client.renderer.texture.IIconRegister
 *  com.astryxion.chaospersists.compat.minecraft.creativetab.CreativeTabs
 *  net.minecraft.entity.Entity
 *  net.minecraft.entity.player.EntityPlayer
 *  net.minecraft.entity.player.PlayerCapabilities
 *  com.astryxion.chaospersists.compat.minecraft.init.Blocks
 *  net.minecraft.item.Item
 *  net.minecraft.item.ItemStack
 *  net.minecraft.util.IIcon
 *  com.astryxion.chaospersists.compat.minecraft.world.World
 *  com.astryxion.chaospersists.compat.minecraft.world.WorldProvider
 */
package com.astryxion.chaospersists.item;

import com.astryxion.chaospersists.core.ChaosPersists;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class ItemMinersDream extends Item {

    public ItemMinersDream(int i) {
        super(new Properties().stacksTo(16));
    }

    private static Block modBlock(Object block) {
        return (Block) block;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        if (player == null) {
            return InteractionResult.FAIL;
        }
        ItemStack par1ItemStack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        int cposx = pos.getX();
        int cposy = pos.getY();
        int cposz = pos.getZ();
        int deltax = 0;
        int deltaz = 0;
        int dirx = 0;
        int dirz = 0;
        int height = 5;
        int width = 5;
        int length = 64;
        int torches = 5;
        int solid_count = 0;
        if (cposx < 0) {
            dirx = -1;
        }
        if (cposz < 0) {
            dirz = -1;
        }
        int pposx = (int) (player.getX() + 0.99 * (double) dirx);
        int pposy = (int) player.getY();
        int pposz = (int) (player.getZ() + 0.99 * (double) dirz);
        if (cposx - pposx == 0 || cposz - pposz == 0) {
            Block bid;
            int k;
            int x = cposx;
            int y = pposy;
            int z = cposz;
            if (x - pposx < 0) {
                deltax = -1;
            }
            if (x - pposx > 0) {
                deltax = 1;
            }
            if (z - pposz < 0) {
                deltaz = -1;
            }
            if (z - pposz > 0) {
                deltaz = 1;
            }
            if (deltax == 0 && deltaz == 0) {
                return InteractionResult.FAIL;
            }
            if (deltax != 0 && deltaz != 0) {
                return InteractionResult.FAIL;
            }
            world.playSound(
                    player,
                    player.blockPosition(),
                    SoundEvents.GENERIC_EXPLODE,
                    SoundSource.PLAYERS,
                    1.0f,
                    1.5f);
            if (world.isClientSide()) {
                return InteractionResult.SUCCESS;
            }
            for (int i = 0; i < height; ++i) {
                for (k = 0; k < length; ++k) {
                    int j;
                    solid_count = 0;
                    for (j = -width; j <= width; ++j) {
                        BlockPos minePos = new BlockPos(x + k * deltax + j * deltaz, y + i, z + k * deltaz + j * deltax);
                        bid = world.getBlockState(minePos).getBlock();
                        if (bid == Blocks.STONE
                                || bid == Blocks.DIRT
                                || bid == Blocks.GRAVEL
                                || bid == Blocks.WATER
                                || bid == Blocks.LAVA
                                || bid == Blocks.NETHERRACK
                                || bid == Blocks.END_STONE
                                || bid == modBlock(ChaosPersists.CrystalStone)) {
                            world.setBlock(minePos, Blocks.AIR.defaultBlockState(), 2);
                        }
                        if (i != height - 1) {
                            continue;
                        }
                        BlockPos ceilingPos = new BlockPos(x + k * deltax + j * deltaz, y + i + 1, z + k * deltaz + j * deltax);
                        bid = world.getBlockState(ceilingPos).getBlock();
                        if (bid != Blocks.AIR) {
                            ++solid_count;
                        }
                        if (bid != Blocks.AIR
                                && bid != Blocks.GRAVEL
                                && bid != Blocks.SAND
                                && bid != Blocks.WATER
                                && bid != Blocks.LAVA) {
                            continue;
                        }
                        if (world.dimension().equals(ChaosPersists.getDimensionKey(5))) {
                            world.setBlock(
                                    ceilingPos,
                                    modBlock(ChaosPersists.CrystalStone).defaultBlockState(),
                                    2);
                            continue;
                        }
                        world.setBlock(ceilingPos, Blocks.COBBLESTONE.defaultBlockState(), 2);
                    }
                    if (i != height - 1 || solid_count != 0) {
                        continue;
                    }
                    for (j = -width; j <= width; ++j) {
                        world.setBlock(
                                new BlockPos(x + k * deltax + j * deltaz, y + i + 1, z + k * deltaz + j * deltax),
                                Blocks.AIR.defaultBlockState(),
                                2);
                    }
                }
            }
            for (k = 0; k < length; k += torches) {
                BlockPos floorPos = new BlockPos(x + k * deltax, y - 1, z + k * deltaz);
                BlockPos torchPos = new BlockPos(x + k * deltax, y, z + k * deltaz);
                bid = world.getBlockState(floorPos).getBlock();
                if ((bid == Blocks.STONE
                                || bid == Blocks.DIRT
                                || bid == Blocks.GRAVEL
                                || bid == Blocks.NETHERRACK
                                || bid == Blocks.END_STONE
                                || bid == Blocks.BEDROCK)
                        && world.getBlockState(torchPos).isAir()) {
                    world.setBlock(
                            torchPos,
                            modBlock(ChaosPersists.ExtremeTorch).defaultBlockState(),
                            2);
                }
                if (bid != modBlock(ChaosPersists.CrystalStone) || !world.getBlockState(torchPos).isAir()) {
                    continue;
                }
                world.setBlock(
                        torchPos,
                        modBlock(ChaosPersists.CrystalTorch).defaultBlockState(),
                        2);
            }
            if (!player.getAbilities().instabuild) {
                par1ItemStack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}
