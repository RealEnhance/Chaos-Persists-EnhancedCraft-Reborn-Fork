package com.astryxion.chaospersists.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.world.entity.LivingEntity;

/**
 * 1.12.2 {@code ModelBiped} UV layout for 64x32 entity skins (boyfriend, girlfriend, etc.).
 * Modern {@link net.minecraft.client.model.geom.ModelLayers#PLAYER} expects 64x64 skins.
 */
public class LegacySkinModel<T extends LivingEntity> extends HumanoidModel<T> {
    public LegacySkinModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 32);
    }
}
