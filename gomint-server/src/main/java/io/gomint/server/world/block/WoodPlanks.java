package io.gomint.server.world.block;

import io.gomint.inventory.item.*;
import io.gomint.server.registry.RegisterInfo;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 5 )
public class WoodPlanks extends Block {

    @Override
    public int getBlockId() {
        return 5;
    }

    @Override
    public long getBreakTime() {
        return 3000;
    }

    @Override
    public Class<? extends ItemStack>[] getToolInterfaces() {
        return new Class[]{
                ItemWoodenAxe.class,
                ItemIronAxe.class,
                ItemDiamondAxe.class,
                ItemGoldenAxe.class,
                ItemStoneAxe.class
        };
    }

}
