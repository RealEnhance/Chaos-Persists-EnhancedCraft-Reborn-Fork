package com.astryxion.chaospersists.item;

import com.astryxion.chaospersists.core.ChaosPersists;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Places a hoverboard entity. 1.7.10 used {@code EntityList.createEntityByName("Hoverboard", world)};
 * we construct {@link Elevator} via {@link ChaosPersists#ENTITY_TYPE_ELEVATOR} so spawn never depends on registry lookup succeeding.
 */
public class ItemElevator extends Item {

    public ItemElevator(int par1) {
        this(new Properties().stacksTo(1));
    }

    public ItemElevator(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide || player == null) {
            return InteractionResult.SUCCESS;
        }
        Elevator elevator = ChaosPersists.ENTITY_TYPE_ELEVATOR.get().create(level);
        if (elevator == null) {
            return InteractionResult.FAIL;
        }
        double x = (double) context.getClickedPos().getX() + 0.5;
        double y = (double) context.getClickedPos().getY() + 1.2;
        double z = (double) context.getClickedPos().getZ() + 0.5;
        elevator.moveTo(x, y, z, level.getRandom().nextFloat() * 360.0f, 0.0f);
        if (!level.addFreshEntity(elevator)) {
            return InteractionResult.FAIL;
        }
        if (!player.getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
