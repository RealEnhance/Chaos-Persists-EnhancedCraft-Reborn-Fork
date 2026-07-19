package com.astryxion.chaospersists.entity;

import com.astryxion.chaospersists.core.ChaosPersists;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ChaosPersists.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class Bertha extends SwordItem {
    /** Right-click interactions in 1.20.1 also call swing(); skip projectile spawn that tick only. */
    private static final Map<Player, Integer> SUPPRESS_INTERACT_SWING_TICK = new WeakHashMap<>();

    public Bertha(Tier tier) {
        super(tier, 3, -2.4f, new Properties().stacksTo(1).durability(9000));
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        if (this == ChaosPersists.MyRoyal) {
            stack.enchant(Enchantments.UNBREAKING, 5);
        } else if (this != ChaosPersists.MyHammy) {
            stack.enchant(Enchantments.KNOCKBACK, 5);
            stack.enchant(Enchantments.BANE_OF_ARTHROPODS, 1);
            stack.enchant(Enchantments.FIRE_ASPECT, 1);
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        this.ensureEnchantments(stack);
    }

    private void ensureEnchantments(ItemStack stack) {
        int lvl = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.KNOCKBACK, stack);
        if (lvl == 0) {
            lvl = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.UNBREAKING, stack);
        }
        if (lvl <= 0) {
            if (this == ChaosPersists.MyRoyal) {
                stack.enchant(Enchantments.UNBREAKING, 5);
            } else if (this != ChaosPersists.MyHammy) {
                stack.enchant(Enchantments.KNOCKBACK, 5);
                stack.enchant(Enchantments.BANE_OF_ARTHROPODS, 1);
                stack.enchant(Enchantments.FIRE_ASPECT, 1);
            }
        }
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity entity) {
        if (entity != null && ChaosPersists.big_bertha_pvp == 0) {
            if (entity instanceof Player
                    || isGirlfriendOrBoyfriend(entity)
                    || (entity instanceof TamableAnimal t && t.isTame())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onEntitySwing(ItemStack stack, LivingEntity entityLiving) {
        if (entityLiving instanceof Player player && !player.level().isClientSide) {
            Integer suppressTick = SUPPRESS_INTERACT_SWING_TICK.get(player);
            if (suppressTick == null || suppressTick != player.tickCount) {
                InteractionHand hand = InteractionHand.MAIN_HAND;
                if (ItemStack.isSameItemSameTags(stack, player.getOffhandItem())) {
                    hand = InteractionHand.OFF_HAND;
                }
                spawnProjectile(player, stack, hand);
            }
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getLevel().isClientSide) {
            suppressInteractSwing(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide) {
            suppressInteractSwing(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!event.getLevel().isClientSide) {
            suppressInteractSwing(event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!event.getLevel().isClientSide) {
            suppressInteractSwing(event.getEntity());
        }
    }

    private static void suppressInteractSwing(Player player) {
        SUPPRESS_INTERACT_SWING_TICK.put(player, player.tickCount);
    }

    private void spawnProjectile(Player player, ItemStack stack, InteractionHand hand) {
        double xzoff = 2.0;
        double yoff = 1.55;
        BerthaHit lb = new BerthaHit(ChaosPersists.ENTITY_TYPE_BERTHA_HIT.get(), player, player.level());
        lb.moveTo(
                player.getX() - xzoff * Mth.sin(player.getYHeadRot() * Mth.DEG_TO_RAD),
                player.getY() + yoff,
                player.getZ() + xzoff * Mth.cos(player.getYHeadRot() * Mth.DEG_TO_RAD),
                player.getYHeadRot(),
                player.getXRot());
        var dm = lb.getDeltaMovement();
        lb.setDeltaMovement(dm.x * 2.0, dm.y * 2.0, dm.z * 2.0);
        if (this == ChaosPersists.MyRoyal) {
            lb.setHitType(2);
        }
        if (this == ChaosPersists.MyHammy) {
            lb.setHitType(3);
        }
        if (!lb.tryHitAlongPath()) {
            player.level().addFreshEntity(lb);
        }
        stack.hurtAndBreak(
                1,
                player,
                e -> e.broadcastBreakEvent(hand == InteractionHand.MAIN_HAND
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND));
    }

    public String getMaterialName() {
        return "Uranium/Titanium";
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, e -> e.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        return true;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 9000;
    }

    private static boolean isGirlfriendOrBoyfriend(Entity e) {
        String n = e.getClass().getSimpleName();
        return "Girlfriend".equals(n) || "Boyfriend".equals(n);
    }

    @Override
    public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        com.astryxion.chaospersists.client.BigWeaponClientExtensions.register(consumer, this);
    }
}
