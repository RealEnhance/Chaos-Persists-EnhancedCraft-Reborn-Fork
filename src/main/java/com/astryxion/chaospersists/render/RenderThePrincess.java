package com.astryxion.chaospersists.render;

import com.astryxion.chaospersists.entity.ThePrincess;
import com.astryxion.chaospersists.model.ModelThePrincess;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RenderThePrincess extends MobRenderer<ThePrincess, ModelThePrincess> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("chaospersists", "textures/entity/theprincesstexture.png");
    private final float scale;

    public RenderThePrincess(
            EntityRendererProvider.Context context, ModelThePrincess model, float shadow, float scale) {
        super(context, model, shadow * scale);
        this.scale = scale;
    }

    @Override
    protected void scale(ThePrincess entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(this.scale, this.scale, this.scale);
    }

    @Override
    public ResourceLocation getTextureLocation(ThePrincess entity) {
        return TEXTURE;
    }
}
