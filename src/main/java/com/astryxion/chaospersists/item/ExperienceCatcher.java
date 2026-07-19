package com.astryxion.chaospersists.item;

import com.astryxion.chaospersists.core.ChaosPersists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

public class ExperienceCatcher extends Item {

    public ExperienceCatcher(int i) {
        super(new Properties().stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        if (player == null) {
            return InteractionResult.FAIL;
        }
        InteractionHand hand = context.getHand();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        float hitX = (float) context.getClickLocation().x - pos.getX();
        float hitY = (float) context.getClickLocation().y - pos.getY();
        float hitZ = (float) context.getClickLocation().z - pos.getZ();

        player.swing(hand);

        if (!world.isClientSide()) {
            AABB bb = new AABB(
                    pos.getX() - 0.5D + hitX,
                    pos.getY(),
                    pos.getZ() - 0.5D + hitZ,
                    pos.getX() + 0.5D + hitX,
                    pos.getY() + 2.0D,
                    pos.getZ() + 0.5D + hitZ);

            List<ExperienceOrb> xpOrbs = world.getEntitiesOfClass(ExperienceOrb.class, bb);

            for (ExperienceOrb orb : xpOrbs) {
                if (orb.getValue() < 3 || world.getRandom().nextInt(5) == 1) {
                    continue;
                }

                orb.discard();

                spawnItem(world, pos, hitX, hitZ, new ItemStack(Items.EXPERIENCE_BOTTLE));
                spawnItem(world, pos, hitX, hitZ, new ItemStack(Items.STRING));
                spawnItem(world, pos, hitX, hitZ, new ItemStack(Items.STICK));

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }

                return InteractionResult.SUCCESS;
            }

            spawnItem(
                    world,
                    pos,
                    hitX,
                    hitZ,
                    new ItemStack((net.minecraft.world.level.ItemLike) (Object) ChaosPersists.MyExperienceCatcher));

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }

    private void spawnItem(Level world, BlockPos pos, float hitX, float hitZ, ItemStack stack) {
        ItemEntity entityItem =
                new ItemEntity(world, pos.getX() + hitX, pos.getY() + 1.0D, pos.getZ() + hitZ, stack);
        world.addFreshEntity(entityItem);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        player.swing(hand);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }
}
