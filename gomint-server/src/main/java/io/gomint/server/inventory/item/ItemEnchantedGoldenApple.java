package io.gomint.server.inventory.item;

import io.gomint.server.registry.RegisterInfo;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 466 )
public class ItemEnchantedGoldenApple extends ItemFood implements io.gomint.inventory.item.ItemEnchantedGoldenApple {

    // CHECKSTYLE:OFF
    public ItemEnchantedGoldenApple( short data, int amount ) {
        super( 466, data, amount );
    }

    public ItemEnchantedGoldenApple( short data, int amount, NBTTagCompound nbt ) {
        super( 466, data, amount, nbt );
    }
    // CHECKSTYLE:ON

    @Override
    public float getSaturation() {
        return 1.2f;
    }

    @Override
    public float getHunger() {
        return 4;
    }

}
