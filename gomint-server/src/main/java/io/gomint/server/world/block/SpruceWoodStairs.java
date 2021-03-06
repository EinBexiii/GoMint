package io.gomint.server.world.block;

import io.gomint.server.registry.RegisterInfo;

/**
 * @author geNAZt
 * @version 1.0
 */
@RegisterInfo( id = 134 )
public class SpruceWoodStairs extends Stairs {

    @Override
    public int getBlockId() {
        return 134;
    }

    @Override
    public long getBreakTime() {
        return 3000;
    }

}
