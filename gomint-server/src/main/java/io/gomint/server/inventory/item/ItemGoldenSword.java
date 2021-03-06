package io.gomint.server.inventory.item;

import io.gomint.inventory.item.ItemSword;
import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 283 )
 public class ItemGoldenSword extends ItemReduceTierGolden implements io.gomint.inventory.item.ItemGoldenSword, ItemSword {

    // CHECKSTYLE:OFF
    public ItemGoldenSword( short data, int amount ) {
        super( 283, data, amount );
    }

    public ItemGoldenSword( short data, int amount, NBTTagCompound nbt ) {
        super( 283, data, amount, nbt );
    }
    // CHECKSTYLE:ON

}
