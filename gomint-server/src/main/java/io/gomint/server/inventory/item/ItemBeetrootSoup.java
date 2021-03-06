package io.gomint.server.inventory.item;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 459 )
public class ItemBeetrootSoup extends ItemFood implements io.gomint.inventory.item.ItemBeetrootSoup {

    // CHECKSTYLE:OFF
    public ItemBeetrootSoup( short data, int amount ) {
        super( 459, data, amount );
    }

    public ItemBeetrootSoup( short data, int amount, NBTTagCompound nbt ) {
        super( 459, data, amount, nbt );
    }
    // CHECKSTYLE:ON

    @Override
    public float getSaturation() {
        return 0.6f;
    }

    @Override
    public float getHunger() {
        return 6;
    }

}
