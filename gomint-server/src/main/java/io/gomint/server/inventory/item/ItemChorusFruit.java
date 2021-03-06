package io.gomint.server.inventory.item;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 432 )
public class ItemChorusFruit extends ItemFood implements io.gomint.inventory.item.ItemChorusFruit {

    // CHECKSTYLE:OFF
    public ItemChorusFruit( short data, int amount ) {
        super( 432, data, amount );
    }

    public ItemChorusFruit( short data, int amount, NBTTagCompound nbt ) {
        super( 432, data, amount, nbt );
    }
    // CHECKSTYLE:ON

    @Override
    public float getSaturation() {
        return 0.3f;
    }

    @Override
    public float getHunger() {
        return 4;
    }

}
