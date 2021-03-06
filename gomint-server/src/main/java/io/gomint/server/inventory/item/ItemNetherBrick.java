package io.gomint.server.inventory.item;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 405 )
 public class ItemNetherBrick extends ItemStack implements io.gomint.inventory.item.ItemNetherBrick {

    // CHECKSTYLE:OFF
    public ItemNetherBrick( short data, int amount ) {
        super( 405, data, amount );
    }

    public ItemNetherBrick( short data, int amount, NBTTagCompound nbt ) {
        super( 405, data, amount, nbt );
    }
    // CHECKSTYLE:ON

}
