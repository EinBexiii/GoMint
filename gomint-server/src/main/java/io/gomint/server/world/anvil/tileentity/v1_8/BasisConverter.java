/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.world.anvil.tileentity.v1_8;

import io.gomint.math.Location;
import io.gomint.server.inventory.MaterialMagicNumbers;
import io.gomint.server.inventory.item.ItemStack;
import io.gomint.server.inventory.item.Items;
import io.gomint.server.world.WorldAdapter;
import io.gomint.server.world.anvil.tileentity.TileEntityConverter;
import io.gomint.taglib.NBTTagCompound;

/**
 * @author geNAZt
 * @version 1.0
 * @param <T> type of tile entity which this converter should generate
 */
public abstract class BasisConverter<T> extends TileEntityConverter<T> {

    /**
     * Construct new converter
     *
     * @param worldAdapter for which we construct
     */
    public BasisConverter( WorldAdapter worldAdapter ) {
        super( worldAdapter );
    }

    /**
     * Read a position from the compound given
     *
     * @param compound which contains x, y and z position integers
     * @return block position object
     */
    protected Location getPosition( NBTTagCompound compound ) {
        return new Location(
            this.worldAdapter,
            compound.getInteger( "x", 0 ),
            compound.getInteger( "y", -1 ),
            compound.getInteger( "z", 0 )
        );
    }

    /**
     * Write the location to a compound
     *
     * @param location which should be written
     * @param compound which should be used to write to
     */
    protected void writePosition( Location location, NBTTagCompound compound ) {
        compound.addValue( "x", (int) location.getX() );
        compound.addValue( "y", (int) location.getY() );
        compound.addValue( "z", (int) location.getZ() );
    }

    /**
     * Get the item out of the compound
     *
     * @param compound which has serialized information about the item stack
     * @return the item stack which has been stored in the compound
     */
    protected ItemStack getItemStack( NBTTagCompound compound ) {
        // Check for correct ids
        int material = MaterialMagicNumbers.valueOfWithId( compound.getString( "id", "minecraft:air" ) );

        // Skip non existent items for PE
        if ( material == 0 ) {
            return Items.create( 0, (short) 0, (byte) 0, null );
        }

        short data = compound.getShort( "Damage", (short) 0 );
        byte amount = compound.getByte( "Count", (byte) 1 );

        return Items.create( material, data, amount, compound.getCompound( "tag", false ) );
    }

    /**
     * Write the item stack to a compound
     *
     * @param itemStack which should be written
     * @param compound to write to
     */
    protected void writeItemStack( ItemStack itemStack, NBTTagCompound compound ) {
        compound.addValue( "id", MaterialMagicNumbers.newIdFromValue( itemStack.getMaterial() ) );
        compound.addValue( "Count", itemStack.getAmount() );
        compound.addValue( "Damage", itemStack.getData() );

        if ( itemStack.getNbtData() != null ) {
            compound.addValue( "tag", itemStack.getNbtData().deepClone( "tag" ) );
        }
    }

}