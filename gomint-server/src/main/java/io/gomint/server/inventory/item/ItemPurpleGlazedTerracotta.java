package io.gomint.server.inventory.item;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 219 )
public class ItemPurpleGlazedTerracotta extends ItemStack {

    // CHECKSTYLE:OFF
    public ItemPurpleGlazedTerracotta( short data, int amount ) {
        super( 219, data, amount );
    }

    public ItemPurpleGlazedTerracotta( short data, int amount, NBTTagCompound nbt ) {
        super( 219, data, amount, nbt );
    }
    // CHECKSTYLE:ON

}
