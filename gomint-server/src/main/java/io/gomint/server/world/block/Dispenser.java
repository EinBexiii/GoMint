package io.gomint.server.world.block;

import io.gomint.server.registry.RegisterInfo;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 23 )
public class Dispenser extends Block {

    @Override
    public int getBlockId() {
        return 23;
    }

    @Override
    public long getBreakTime() {
        return 5250;
    }

}
