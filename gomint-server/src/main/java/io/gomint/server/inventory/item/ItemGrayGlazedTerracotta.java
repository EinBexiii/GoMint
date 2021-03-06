package io.gomint.server.inventory.item;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 227 )
public class ItemGrayGlazedTerracotta extends ItemStack {

    // CHECKSTYLE:OFF
    public ItemGrayGlazedTerracotta( short data, int amount ) {
        super( 227, data, amount );
    }

    public ItemGrayGlazedTerracotta( short data, int amount, NBTTagCompound nbt ) {
        super( 227, data, amount, nbt );
    }
    // CHECKSTYLE:ON

}
