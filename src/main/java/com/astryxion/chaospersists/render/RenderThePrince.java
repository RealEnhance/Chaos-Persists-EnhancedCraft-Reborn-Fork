package com.astryxion.chaospersists.render;

import com.astryxion.chaospersists.entity.ThePrince;
import com.astryxion.chaospersists.model.ModelThePrince;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RenderThePrince extends MobRenderer<ThePrince, ModelThePrince> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("chaospersists", "textures/entity/theprincetexture.png");
    private final float scale;

    public RenderThePrince(EntityRendererProvider.Context context, ModelThePrince model, float shadow, float scale) {
        super(context, model, shadow * scale);
        this.scale = scale;
    }

    @Override
    protected void scale(ThePrince entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(this.scale, this.scale, this.scale);
    }

    @Override
    public ResourceLocation getTextureLocation(ThePrince entity) {
        return TEXTURE;
    }
}
