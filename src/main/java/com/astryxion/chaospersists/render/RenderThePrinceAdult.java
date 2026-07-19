package com.astryxion.chaospersists.render;

import com.astryxion.chaospersists.entity.ThePrinceAdult;
import com.astryxion.chaospersists.model.ModelThePrinceAdult;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RenderThePrinceAdult extends MobRenderer<ThePrinceAdult, ModelThePrinceAdult> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("chaospersists", "textures/entity/thekingtexture.png");
    private final float scale;

    public RenderThePrinceAdult(
            EntityRendererProvider.Context context, ModelThePrinceAdult model, float shadow, float scale) {
        super(context, model, shadow * scale);
        this.scale = scale;
    }

    @Override
    protected void scale(ThePrinceAdult entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(this.scale, this.scale, this.scale);
    }

    @Override
    public ResourceLocation getTextureLocation(ThePrinceAdult entity) {
        return TEXTURE;
    }
}
