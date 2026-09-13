package com.cartprac.battleroyalspawn.config;

import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Vector;

import java.util.Collections;
import java.util.List;

/**
 * Data-driven description of the visible cart: one optional scaled item, one optional banner item and any
 * number of block boxes. All offsets are in cart-local coordinates (x sideways, y up, z forward).
 */
public final class RigSpec {

    /** One ItemDisplay part. */
    public static final class ItemPart {
        public final boolean enabled;
        public final Material item;
        public final float scale;
        public final Vector offset;
        public final float yaw;
        public final ItemDisplay.ItemDisplayTransform mode;

        public ItemPart(boolean enabled, Material item, float scale, Vector offset, float yaw,
                        ItemDisplay.ItemDisplayTransform mode) {
            this.enabled = enabled;
            this.item = item;
            this.scale = scale;
            this.offset = offset;
            this.yaw = yaw;
            this.mode = mode;
        }
    }

    /** One BlockDisplay box. {@code offset} is the centre of the bottom face. */
    public static final class BlockPart {
        public final Material block;
        public final Vector offset;
        public final Vector size;

        public BlockPart(Material block, Vector offset, Vector size) {
            this.block = block;
            this.offset = offset;
            this.size = size;
        }
    }

    public final ItemPart cart;
    public final ItemPart banner;
    public final List<BlockPart> blocks;

    public RigSpec(ItemPart cart, ItemPart banner, List<BlockPart> blocks) {
        this.cart = cart;
        this.banner = banner;
        this.blocks = Collections.unmodifiableList(blocks);
    }

    /** Number of display entities this rig spawns. */
    public int partCount() {
        return blocks.size() + (cart.enabled ? 1 : 0) + (banner.enabled ? 1 : 0);
    }
}
