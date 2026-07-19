package com.astryxion.chaospersists.render;

import com.astryxion.chaospersists.entity.TheQueen;
import com.astryxion.chaospersists.model.ModelTheQueen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class RenderTheQueen extends MobRenderer<TheQueen, ModelTheQueen> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("chaospersists", "textures/entity/thequeentexture.png");
    private static final ResourceLocation TEXTURE_HAPPY =
            ResourceLocation.fromNamespaceAndPath("chaospersists", "textures/entity/thequeentexture2.png");
    private final float scale;

    public RenderTheQueen(EntityRendererProvider.Context context, ModelTheQueen model, float shadow, float scale) {
        super(context, model, shadow * scale);
        this.scale = scale;
    }

    @Override
    protected void scale(TheQueen entity, PoseStack poseStack, float partialTick) {
        if (entity.getPlayNicely() != 0) {
            float s = this.scale / 4.0f;
            poseStack.scale(s, s, s);
        } else {
            poseStack.scale(this.scale, this.scale, this.scale);
        }
    }

    @Override
    public ResourceLocation getTextureLocation(TheQueen entity) {
        if (entity.isHappy()) {
            return TEXTURE_HAPPY;
        }
        return TEXTURE;
    }
}
