package io.gomint.server.inventory.item;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 233 )
public class ItemGreenGlazedTerracotta extends ItemStack {

    // CHECKSTYLE:OFF
    public ItemGreenGlazedTerracotta( short data, int amount ) {
        super( 233, data, amount );
    }

    public ItemGreenGlazedTerracotta( short data, int amount, NBTTagCompound nbt ) {
        super( 233, data, amount, nbt );
    }
    // CHECKSTYLE:ON
    
}
