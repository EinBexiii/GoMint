package io.gomint.server.inventory.item;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 411 )
public class ItemRawRabbit extends ItemFood implements io.gomint.inventory.item.ItemRawRabbit {

    // CHECKSTYLE:OFF
    public ItemRawRabbit( short data, int amount ) {
        super( 411, data, amount );
    }

    public ItemRawRabbit( short data, int amount, NBTTagCompound nbt ) {
        super( 411, data, amount, nbt );
    }
    // CHECKSTYLE:ON

    @Override
    public float getSaturation() {
        return 0.3f;
    }

    @Override
    public float getHunger() {
        return 3;
    }

}
