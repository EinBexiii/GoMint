package io.gomint.server.inventory.item;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 222 )
public class ItemMagentaGlazedTerracotta extends ItemStack {

    // CHECKSTYLE:OFF
    public ItemMagentaGlazedTerracotta( short data, int amount ) {
        super( 222, data, amount );
    }

    public ItemMagentaGlazedTerracotta( short data, int amount, NBTTagCompound nbt ) {
        super( 222, data, amount, nbt );
    }
    // CHECKSTYLE:ON

}
