package com.astryxion.chaospersists.util;

import com.astryxion.chaospersists.container.ContainerCrystalFurnace;
import com.astryxion.chaospersists.tileentity.TileEntityCrystalFurnace;
import net.minecraft.client.gui.screens.inventory.FurnaceScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CrystalFurnaceGUI extends FurnaceScreen {

    public CrystalFurnaceGUI(Inventory playerInventory, TileEntityCrystalFurnace furnace) {
        super(new ContainerCrystalFurnace(0, playerInventory, furnace), playerInventory, furnace.getDisplayName());
    }
}
