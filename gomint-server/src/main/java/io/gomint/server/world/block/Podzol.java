package io.gomint.server.world.block;

import io.gomint.server.registry.RegisterInfo;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 243 )
public class Podzol extends Block {

    @Override
    public int getBlockId() {
        return 243;
    }

    @Override
    public long getBreakTime() {
        return 750;
    }

}
