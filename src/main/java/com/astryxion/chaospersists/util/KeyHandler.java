package com.astryxion.chaospersists.util;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public class KeyHandler {
    public static final String KEY_CATEGORY = "key.categories.chaospersists";
    /** 1.12 key code 56 — unchanged for 1:1 default binding. */
    public static final KeyMapping KEY_FLY_UP =
            new KeyMapping("chaospersists UP/FAST", InputConstants.Type.KEYSYM, 56, KEY_CATEGORY);
}
