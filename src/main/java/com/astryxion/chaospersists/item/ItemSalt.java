package com.astryxion.chaospersists.item;

import net.minecraft.world.item.Item;

/** Simple material item; reused for many drops/ingredients in ChaosPersists. */
public class ItemSalt extends Item {

    public ItemSalt() {
        super(new Properties());
    }

    /** Legacy hub offset constructor (unused). */
    public ItemSalt(int ignored) {
        this();
    }
}
