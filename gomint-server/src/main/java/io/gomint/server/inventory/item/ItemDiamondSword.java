package io.gomint.server.inventory.item;

import io.gomint.inventory.item.ItemSword;
import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 276 )
 public class ItemDiamondSword extends ItemReduceTierDiamond implements io.gomint.inventory.item.ItemDiamondSword, ItemSword {

    // CHECKSTYLE:OFF
    public ItemDiamondSword( short data, int amount ) {
        super( 276, data, amount );
    }

    public ItemDiamondSword( short data, int amount, NBTTagCompound nbt ) {
        super( 276, data, amount, nbt );
    }
    // CHECKSTYLE:ON

}
