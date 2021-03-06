package io.gomint.server.inventory.item;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 262 )
 public class ItemArrow extends ItemStack implements io.gomint.inventory.item.ItemArrow {

    // CHECKSTYLE:OFF
    public ItemArrow( short data, int amount ) {
        super( 262, data, amount );
    }

    public ItemArrow( short data, int amount, NBTTagCompound nbt ) {
        super( 262, data, amount, nbt );
    }
    // CHECKSTYLE:ON

}
