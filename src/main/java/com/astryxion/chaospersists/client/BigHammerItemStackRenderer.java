package com.astryxion.chaospersists.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Flat handheld hammer texture with in-hand scale/pose tuned separately from {@link ChainsawItemStackRenderer}
 * (swords/axes use that path; hammer head is offset to reduce clipping through the player).
 */
public class BigHammerItemStackRenderer extends BlockEntityWithoutLevelRenderer {

    private final BakedModel flatModel;

    public BigHammerItemStackRenderer(BakedModel flatModel) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.flatModel = flatModel;
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext ctx,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            int packedOverlay) {
        if (ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || ctx == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND) {
            boolean left = ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.5f, 0.5f);
            if (left) {
                poseStack.scale(-1.0f, 1.0f, 1.0f);
            }
            applyFirstPerson(poseStack);
            FlatItemModelRenderer.render(flatModel, stack, poseStack, buffer, packedLight, packedOverlay);
            poseStack.popPose();
        } else if (ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || ctx == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
            boolean left = ctx == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.5f, 0.5f);
            if (left) {
                poseStack.scale(-1.0f, 1.0f, 1.0f);
            }
            applyThirdPerson(poseStack);
            FlatItemModelRenderer.render(flatModel, stack, poseStack, buffer, packedLight, packedOverlay);
            poseStack.popPose();
        } else {
            FlatItemModelRenderer.render(flatModel, stack, poseStack, buffer, packedLight, packedOverlay);
        }
    }

    private static void applyFirstPerson(PoseStack poseStack) {
        poseStack.translate(0.55f, -0.12f, 0.18f);
        poseStack.scale(0.32f, 0.32f, 0.32f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-8.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(102.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f));
    }

    private static void applyThirdPerson(PoseStack poseStack) {
        poseStack.translate(0.42f, -0.42f, 0.52f);
        poseStack.scale(0.38f, 0.38f, 0.38f);
        poseStack.mulPose(Axis.XP.rotationDegrees(-22.0f));
        poseStack.mulPose(Axis.YP.rotationDegrees(-38.0f));
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0f));
    }
}
