/*
 * Copyright (c) 2017, GoMint, BlackyPaw and geNAZt
 *
 * This code is licensed under the BSD license found in the
 * LICENSE file in the root directory of this source tree.
 */

package io.gomint.server.world;

import io.gomint.entity.EntityPlayer;
import io.gomint.inventory.item.ItemAir;
import io.gomint.inventory.item.ItemStack;
import io.gomint.math.AxisAlignedBB;
import io.gomint.math.BlockPosition;
import io.gomint.math.Location;
import io.gomint.math.Vector;
import io.gomint.server.GoMintServer;
import io.gomint.server.async.Delegate;
import io.gomint.server.async.Delegate2;
import io.gomint.entity.Entity;
import io.gomint.server.entity.passive.EntityItem;
import io.gomint.server.entity.tileentity.TileEntity;
import io.gomint.server.network.packet.*;
import io.gomint.server.util.EnumConnectors;
import io.gomint.server.util.collection.PlayerMap;
import io.gomint.server.util.random.FastRandom;
import io.gomint.server.world.block.Air;
import io.gomint.server.world.block.Blocks;
import io.gomint.server.world.generator.ChunkGenerator;
import io.gomint.server.world.storage.TemporaryStorage;
import io.gomint.util.Numbers;
import io.gomint.world.*;
import io.gomint.world.block.Block;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/**
 * @author BlackyPaw
 * @version 1.0
 */
public abstract class WorldAdapter implements World {

    // Shared objects
    @Getter
    protected final GoMintServer server;
    protected final Logger logger;

    // World properties
    protected final File worldDir;
    protected String levelName;
    protected Location spawn;
    @Getter
    protected Map<Gamerule, Object> gamerules = new HashMap<>();

    /**
     * Get the difficulty of this world
     */
    @Getter protected Difficulty difficulty;

    // Chunk Handling
    protected ChunkCache chunkCache;
    protected ChunkGenerator chunkGenerator;

    // Entity Handling
    protected EntityManager entityManager;

    // Block ticking
    int randomUpdateNumber = FastRandom.current().nextInt();
    TickList tickQueue = new TickList();
    private final Set<Long> neighbourUpdates = new HashSet<>();

    // I/O
    private boolean asyncWorkerRunning;
    private BlockingQueue<AsyncChunkTask> asyncChunkTasks;
    private Queue<AsyncChunkPackageTask> chunkPackageTasks;

    // EntityPlayer handling
    private PlayerMap players;

    protected WorldAdapter( GoMintServer server, File worldDir ) {
        this.server = server;
        this.logger = LoggerFactory.getLogger( "World-" + worldDir.getName() );
        this.worldDir = worldDir;
        this.entityManager = new EntityManager( this );
        this.players = PlayerMap.withExpectedSize( this.server.getServerConfig().getMaxPlayers() );
        this.asyncChunkTasks = new LinkedBlockingQueue<>();
        this.chunkPackageTasks = new ConcurrentLinkedQueue<>();
        this.startAsyncWorker( server.getExecutorService() );
        this.initGamerules();
    }

    // ==================================== GENERAL ACCESSORS ==================================== //

    /**
     * Get the current view of players on this world.
     *
     * @return The Collection View of the Players currently on this world
     */
    public PlayerMap getPlayers0() {
        return players;
    }

    /**
     * Get a collection (set) of all players online on this world
     *
     * @return collection of all players online on this world
     */
    public Collection<EntityPlayer> getPlayers() {
        Collection<EntityPlayer> playerReturn = new HashSet<>();
        playerReturn.addAll( players.keySet() );
        return playerReturn;
    }

    @Override
    public void playSound( Location location, Sound sound, byte pitch, int extraData ) {
        PacketWorldSoundEvent soundPacket = new PacketWorldSoundEvent();
        soundPacket.setType( EnumConnectors.SOUND_CONNECTOR.convert( sound ) );
        soundPacket.setPitch( pitch );
        soundPacket.setExtraData( extraData );
        soundPacket.setPosition( location );

        sendToVisible( location.toBlockPosition(), soundPacket, new Predicate<Entity>() {
            @Override
            public boolean test( Entity entity ) {
                return true;
            }
        } );
    }

    @Override
    public String getWorldName() {
        return this.worldDir.getName();
    }

    @Override
    public String getLevelName() {
        return this.levelName;
    }

    @Override
    public Location getSpawnLocation() {
        return this.spawn.clone();
    }

    @Override
    public <T extends Block> T getBlockAt( BlockPosition pos ) {
        return this.getBlockAt( pos.getX(), pos.getY(), pos.getZ() );
    }

    @Override
    public <T extends Block> T getBlockAt( int x, int y, int z ) {
        // Secure location
        if ( y < 0 || y > 255 ) {
            return (T) Blocks.get( 0, (byte) 0, (byte) ( y > 255 ? 15 : 0 ), (byte) 0, null, new Location( this, x, y, z ) );
        }

        int xChunk = CoordinateUtils.fromBlockToChunk( x );
        int zChunk = CoordinateUtils.fromBlockToChunk( z );

        ChunkAdapter chunk = this.loadChunk( xChunk, zChunk, true );
        return chunk.getBlockAt( x & 0xF, y, z & 0xF );
    }

    /**
     * Set the block id for the given location
     *
     * @param position Position of the block
     * @param blockId  The new block id
     */
    public void setBlockId( BlockPosition position, int blockId ) {
        final ChunkAdapter chunk = this.loadChunk(
            CoordinateUtils.fromBlockToChunk( position.getX() ),
            CoordinateUtils.fromBlockToChunk( position.getZ() ),
            true );

        chunk.setBlock( position.getX() & 0xF, position.getY(), position.getZ() & 0xF, blockId );
    }

    /**
     * Set the data byte for the given block
     *
     * @param position Position of the block
     * @param data     The new data of the block
     */
    public void setBlockData( BlockPosition position, byte data ) {
        int xChunk = CoordinateUtils.fromBlockToChunk( position.getX() );
        int zChunk = CoordinateUtils.fromBlockToChunk( position.getZ() );

        final ChunkAdapter chunk = this.loadChunk( xChunk, zChunk, true );
        chunk.setData( position.getX() & 0xF, position.getY(), position.getZ() & 0xF, data );
    }

    private void initGamerules() {
        this.setGamerule( Gamerule.DO_DAYLIGHT_CYCLE, false );
    }

    @Override
    public <T> void setGamerule( Gamerule<T> gamerule, T value ) {
        this.gamerules.put( gamerule, value );
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public <T> T getGamerule( Gamerule<T> gamerule ) {
        return (T) this.gamerules.get( gamerule );
    }

    // ==================================== UPDATING ==================================== //

    /**
     * Ticks the world and updates what needs to be updated.
     *
     * @param currentTimeMS The current time in milliseconds. Used to reduce the number of calls to System#currentTimeMillis()
     * @param dT            The delta from the full second which has been calculated in the last tick
     */
    public void update( long currentTimeMS, float dT ) {
        // ---------------------------------------
        // Tick the chunk cache to get rid of Chunks
        this.chunkCache.tick( currentTimeMS );

        // ---------------------------------------
        // Update all blocks

        // Random blocks
        for ( long chunkHash : this.chunkCache.getChunkHashes() ) {
            if ( chunkHash != 0 ) {
                ChunkAdapter chunkAdapter = this.chunkCache.getChunkInternal( chunkHash );
                chunkAdapter.update( currentTimeMS, dT );
            }
        }

        // Scheduled blocks
        while ( this.tickQueue.getNextTaskTime() < currentTimeMS ) {
            long blockToUpdate = this.tickQueue.getNextElement();
            if ( blockToUpdate == Long.MIN_VALUE ) {
                break;
            }

            // Get the block
            Block block = getBlockAt( CoordinateUtils.fromLong( blockToUpdate ) );
            if ( block != null ) {
                // CHECKSTYLE:OFF
                try {
                    io.gomint.server.world.block.Block block1 = (io.gomint.server.world.block.Block) block;
                    long next = block1.update( UpdateReason.SCHEDULED, currentTimeMS, dT );

                    // Reschedule if needed
                    if ( next > currentTimeMS ) {
                        this.tickQueue.add( next, blockToUpdate );
                    }
                } catch ( Exception e ) {
                    logger.error( "Error whilst ticking block @ " + blockToUpdate, e );
                }
                // CHECKSTYLE:ON
            }
        }

        // Neighbour updates
        if ( this.neighbourUpdates.size() > 0 ) {
            for ( Long blockToUpdate : this.neighbourUpdates ) {
                Block block = getBlockAt( CoordinateUtils.fromLong( blockToUpdate ) );
                if ( block != null ) {
                    // CHECKSTYLE:OFF
                    try {
                        io.gomint.server.world.block.Block block1 = (io.gomint.server.world.block.Block) block;
                        long next = block1.update( UpdateReason.NEIGHBOUR_UPDATE, currentTimeMS, dT );

                        // Reschedule if needed
                        if ( next > currentTimeMS ) {
                            this.tickQueue.add( next, blockToUpdate );
                        }
                    } catch ( Exception e ) {
                        logger.error( "Error whilst ticking block @ " + blockToUpdate, e );
                    }
                    // CHECKSTYLE:ON
                }
            }

            this.neighbourUpdates.clear();
        }

        // ---------------------------------------
        // Update all entities
        this.entityManager.update( currentTimeMS, dT );

        // ---------------------------------------
        // Chunk packages are done in main thread in order to be able to
        // cache packets without possibly getting into race conditions:
        while ( !this.chunkPackageTasks.isEmpty() ) {
            // One chunk per tick at max:
            AsyncChunkPackageTask task = this.chunkPackageTasks.poll();
            ChunkAdapter chunk = this.getChunk( task.getX(), task.getZ() );
            if ( chunk == null ) {
                final Object lock = new Object();

                this.getOrLoadChunk( task.getX(), task.getZ(), false, new Delegate<ChunkAdapter>() {
                    @Override
                    public void invoke( ChunkAdapter arg ) {
                        synchronized ( lock ) {
                            packageChunk( arg, task.getCallback() );
                            lock.notifyAll();
                        }
                    }
                } );

                // Wait until the chunk is loaded
                synchronized ( lock ) {
                    try {
                        lock.wait();
                    } catch ( InterruptedException e ) {
                        // Ignored .-.
                    }
                }
            } else {
                packageChunk( chunk, task.getCallback() );
            }
        }

        // ---------------------------------------
        // Perform regular updates:
    }

    // ==================================== ENTITY MANAGEMENT ==================================== //

    /**
     * Adds a new player to this world and schedules all world chunk packets required for spawning
     * the player for send.
     *
     * @param player The player entity to add to the world
     */
    public void addPlayer( io.gomint.server.entity.EntityPlayer player ) {
        // Schedule sending spawn region chunks:
        final int minChunkX = CoordinateUtils.fromBlockToChunk( (int) this.spawn.getX() ) - 4;
        final int minChunkZ = CoordinateUtils.fromBlockToChunk( (int) this.spawn.getZ() ) - 4;
        final int maxChunkX = CoordinateUtils.fromBlockToChunk( (int) this.spawn.getX() ) + 4;
        final int maxChunkZ = CoordinateUtils.fromBlockToChunk( (int) this.spawn.getZ() ) + 4;

        for ( int i = minChunkZ; i <= maxChunkZ; ++i ) {
            for ( int j = minChunkX; j <= maxChunkX; ++j ) {
                this.sendChunk( j, i, player, false );
            }
        }
    }

    /**
     * Removes a player from this world and cleans up its references
     *
     * @param player The player entity which should be removed from the world
     */
    public void removePlayer( io.gomint.server.entity.EntityPlayer player ) {
        ChunkAdapter chunkAdapter = this.players.remove( player );
        if ( chunkAdapter != null ) {
            chunkAdapter.removePlayer( player );
        }
    }

    /**
     * Gets an entity given its unique ID.
     *
     * @param entityId The entity's unique ID
     * @return The entity if found or null otherwise
     */
    public Entity findEntity( long entityId ) {
        return this.entityManager.findEntity( entityId );
    }

    /**
     * Spawns the given entity at the specified position.
     *
     * @param entity The entity to spawn
     * @param vector The vector which contains the position of the spawn
     */
    public void spawnEntityAt( Entity entity, Vector vector ) {
        this.spawnEntityAt( entity, vector.getX(), vector.getY(), vector.getZ() );
    }

    /**
     * Spawns the given entity at the specified position.
     *
     * @param entity    The entity to spawn
     * @param positionX The x coordinate to spawn the entity at
     * @param positionY The y coordinate to spawn the entity at
     * @param positionZ The z coordinate to spawn the entity at
     */
    public void spawnEntityAt( Entity entity, float positionX, float positionY, float positionZ ) {
        this.entityManager.spawnEntityAt( entity, positionX, positionY, positionZ );
    }

    /**
     * Spawns the given entity at the specified position with the specified rotation.
     *
     * @param entity    The entity to spawn
     * @param positionX The x coordinate to spawn the entity at
     * @param positionY The y coordinate to spawn the entity at
     * @param positionZ The z coordinate to spawn the entity at
     * @param yaw       The yaw value of the entity ; will be applied to both the entity's body and head
     * @param pitch     The pitch value of the entity
     */
    public void spawnEntityAt( Entity entity, float positionX, float positionY, float positionZ, float yaw, float pitch ) {
        this.entityManager.spawnEntityAt( entity, positionX, positionY, positionZ, yaw, pitch );
    }

    // ==================================== CHUNK MANAGEMENT ==================================== //

    /**
     * Gets the chunk at the specified coordinates. If the chunk is currently not available
     * it will be loaded or generated.
     *
     * @param x The x-coordinate of the chunk
     * @param z The z-coordinate of the chunk
     * @return The chunk if available or null otherwise
     */
    public ChunkAdapter getChunk( int x, int z ) {
        return this.chunkCache.getChunk( x, z );
    }

    /**
     * Gets a chunk asynchronously. This allows to load or generate the chunk if it is not yet available
     * and then return it once it gets available. The callback is guaranteed to be invoked: if the chunk
     * could not be loaded nor be generated it will be passed null as its argument.
     *
     * @param x        The x-coordinate of the chunk
     * @param z        The z-coordinate of the chunk
     * @param generate Whether or not to generate teh chunk if it does not yet exist
     * @param callback The callback to be invoked once the chunk is available
     */
    public void getOrLoadChunk( int x, int z, boolean generate, Delegate<ChunkAdapter> callback ) {
        // Early out:
        ChunkAdapter chunk = this.chunkCache.getChunk( x, z );
        if ( chunk != null ) {
            callback.invoke( chunk );
            return;
        }

        // Schedule this chunk for asynchronous loading:
        AsyncChunkLoadTask task = new AsyncChunkLoadTask( x, z, generate, callback );
        this.asyncChunkTasks.offer( task );
    }

    /**
     * Send a chunk of this world to the client
     *
     * @param x      The x-coordinate of the chunk
     * @param z      The z-coordinate of the chunk
     * @param player The player we want to send the chunk to
     * @param sync   Force sync chunk loading
     */
    public void sendChunk( int x, int z, io.gomint.server.entity.EntityPlayer player, boolean sync ) {
        Delegate2<Long, ChunkAdapter> sendDelegate = new Delegate2<Long, ChunkAdapter>() {
            @Override
            public void invoke( Long chunkHash, ChunkAdapter chunk ) {
                player.getChunkSendQueue().offer( chunk );
            }
        };

        if ( !sync ) {
            this.getOrLoadChunk( x, z, true, new Delegate<ChunkAdapter>() {
                @Override
                public void invoke( ChunkAdapter chunk ) {
                    chunk.packageChunk( sendDelegate );
                }
            } );
        } else {
            ChunkAdapter chunkAdapter = this.loadChunk( x, z, true );
            if ( chunkAdapter.dirty || chunkAdapter.cachedPacket == null || chunkAdapter.cachedPacket.get() == null ) {
                packageChunk( chunkAdapter, sendDelegate );
            } else {
                sendDelegate.invoke( CoordinateUtils.toLong( x, z ), chunkAdapter );
            }
        }
    }

    /**
     * Move a player to a new chunk. This is done so we know which player is in which chunk so we can unload unneeded
     * Chunks better and faster.
     *
     * @param x      The x-coordinate of the chunk
     * @param z      The z-coordinate of the chunk
     * @param player The player which should be set into the chunk
     */
    public void movePlayerToChunk( int x, int z, io.gomint.server.entity.EntityPlayer player ) {
        ChunkAdapter oldChunk = this.players.get( player );
        ChunkAdapter newChunk = this.loadChunk( x, z, true );

        if ( oldChunk == null ) {
            newChunk.addPlayer( player );
            this.players.justPut( player, newChunk );
        }

        if ( oldChunk != null && !oldChunk.equals( newChunk ) ) {
            oldChunk.removePlayer( player );
            newChunk.addPlayer( player );
            this.players.justPut( player, newChunk );
        }
    }

    /**
     * Prepares the region surrounding the world's spawn point.
     *
     * @throws IOException Throws in case the spawn region could not be loaded nor generated
     */
    protected void prepareSpawnRegion() throws IOException {
        final int spawnRadius = this.server.getServerConfig().getAmountOfChunksForSpawnArea();
        if ( spawnRadius == 0 ) {
            return;
        }

        final int chunkX = CoordinateUtils.fromBlockToChunk( (int) this.spawn.getX() );
        final int chunkZ = CoordinateUtils.fromBlockToChunk( (int) this.spawn.getZ() );

        for ( int i = chunkX - spawnRadius; i <= chunkX + spawnRadius; i++ ) {
            for ( int j = chunkZ - spawnRadius; j <= chunkZ + spawnRadius; j++ ) {
                this.loadChunk( i, j, true );
            }
        }
    }

    /**
     * Load a Chunk from the underlying implementation
     *
     * @param x        The x coordinate of the chunk we want to load
     * @param z        The x coordinate of the chunk we want to load
     * @param generate A boolean which decides whether or not the chunk should be generated when not found
     * @return The loaded or generated Chunk
     */
    protected abstract ChunkAdapter loadChunk( int x, int z, boolean generate );

    /**
     * Saves the given chunk to its respective region file. The respective region file
     * is created automatically if it does not yet exist.
     *
     * @param chunk The chunk to be saved
     */
    protected abstract void saveChunk( ChunkAdapter chunk );

    /**
     * Saves the given chunk to its region file asynchronously.
     *
     * @param chunk The chunk to save
     */
    void saveChunkAsynchronously( ChunkAdapter chunk ) {
        AsyncChunkSaveTask task = new AsyncChunkSaveTask( chunk );
        this.asyncChunkTasks.add( task );
    }

    /**
     * Notifies the world that the given chunk was told to package itself. This will effectively
     * produce an asynchronous chunk task which will be completed by the asynchronous worker thread.
     *
     * @param x        The x coordinate of the chunk we want to package
     * @param z        The z coordinate of the chunk we want to package
     * @param callback The callback to be invoked once the chunk is packaged
     */
    void notifyPackageChunk( int x, int z, Delegate2<Long, ChunkAdapter> callback ) {
        AsyncChunkPackageTask task = new AsyncChunkPackageTask( x, z, callback );
        this.chunkPackageTasks.add( task );
    }

    /**
     * Package a Chunk into a ChunkData Packet for Raknet. This is done to enable caching of those packets.
     *
     * @param chunk    The chunk which should be packed
     * @param callback The callback which should be invoked when the packing has been done
     */
    private void packageChunk( ChunkAdapter chunk, Delegate2<Long, ChunkAdapter> callback ) {
        PacketWorldChunk packet = chunk.createPackagedData();
        chunk.setCachedPacket( packet );
        callback.invoke( CoordinateUtils.toLong( chunk.getX(), chunk.getZ() ), chunk );
    }

    // ==================================== NETWORKING HELPERS ==================================== //

    /**
     * Send a packet to all players which can see the position
     *
     * @param position  where the packet will have its impact
     * @param packet    which should be sent
     * @param predicate which decides over each entity if they will get the packet sent or not
     */
    public void sendToVisible( BlockPosition position, Packet packet, Predicate<Entity> predicate ) {
        int posX = CoordinateUtils.fromBlockToChunk( position.getX() );
        int posZ = CoordinateUtils.fromBlockToChunk( position.getZ() );

        for ( EntityPlayer player : this.getPlayers() ) {
            Location location = player.getLocation();
            int currentX = CoordinateUtils.fromBlockToChunk( (int) location.getX() );
            int currentZ = CoordinateUtils.fromBlockToChunk( (int) location.getZ() );

            if ( Math.abs( posX - currentX ) <= player.getViewDistance() &&
                Math.abs( posZ - currentZ ) <= player.getViewDistance() &&
                predicate.test( (Entity) player ) ) {
                ( (io.gomint.server.entity.EntityPlayer) player ).getConnection().addToSendQueue( packet );
            }
        }
    }

    // ==================================== ASYNCHRONOUS WORKER ==================================== //

    /**
     * Starts the asynchronous worker thread used by the world to perform I/O operations for chunks.
     */
    private void startAsyncWorker( ExecutorService executorService ) {
        executorService.execute( new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName( Thread.currentThread().getName() + " [Async World I/O: " + WorldAdapter.this.getWorldName() + "]" );
                WorldAdapter.this.asyncWorkerLoop();
            }
        } );

        this.asyncWorkerRunning = true;
    }

    /**
     * Main loop of the world's asynchronous worker thread.
     */
    private void asyncWorkerLoop() {
        while ( this.asyncWorkerRunning ) {
            try {
                AsyncChunkTask task = this.asyncChunkTasks.poll( 500, TimeUnit.MILLISECONDS );
                if ( task == null ) {
                    continue;
                }

                ChunkAdapter chunk;
                switch ( task.getType() ) {
                    case LOAD:
                        AsyncChunkLoadTask load = (AsyncChunkLoadTask) task;
                        chunk = this.loadChunk( load.getX(), load.getZ(), load.isGenerate() );
                        load.getCallback().invoke( chunk );
                        break;

                    case SAVE:
                        AsyncChunkSaveTask save = (AsyncChunkSaveTask) task;
                        chunk = save.getChunk();
                        this.saveChunk( chunk );
                        break;

                    default:
                        // Log some error when this happens

                        break;
                }
            } catch ( Throwable cause ) {
                // Catching throwable in order to make sure no uncaught exceptions puts
                // the asynchronous worker into nirvana:
                logger.error( "Error whilst doing async work: ", cause );
            }
        }
    }

    /**
     * Send the block given under the position to all players in the chunk of the block
     *
     * @param pos The position of the block to update
     */
    public void updateBlock( BlockPosition pos ) {
        io.gomint.server.world.block.Block block = getBlockAt( pos );

        logger.debug( "Updating block @ " + pos + " data: " + block.getBlockData() );

        // Update the block
        PacketUpdateBlock updateBlock = new PacketUpdateBlock();
        updateBlock.setPosition( pos );
        updateBlock.setBlockId( block.getBlockId() );
        updateBlock.setPrioAndMetadata( (byte) ( ( PacketUpdateBlock.FLAG_ALL_PRIORITY << 4 ) | ( block.getBlockData() ) ) );

        sendToVisible( pos, updateBlock, new Predicate<Entity>() {
            @Override
            public boolean test( Entity entity ) {
                return true;
            }
        } );

        // Check for tile entity
        if ( block.getTileEntity() != null ) {
            PacketTileEntityData tileEntityData = new PacketTileEntityData();
            tileEntityData.setPosition( pos );
            tileEntityData.setTileEntity( block.getTileEntity() );

            sendToVisible( pos, tileEntityData, new Predicate<Entity>() {
                @Override
                public boolean test( Entity entity ) {
                    return true;
                }
            } );
        }
    }

    /**
     * Get the amount of players online on this world
     *
     * @return amount of players online on this world
     */
    public int getAmountOfPlayers() {
        return players.size();
    }

    /**
     * Get all entities which touch or are inside this bounding box
     *
     * @param bb        the bounding box which should be used to collect entities in
     * @param exception a entity which should not be included in the list
     * @return either null if there are no entities or a collection of entities
     */
    public Collection<io.gomint.entity.Entity> getNearbyEntities( AxisAlignedBB bb, io.gomint.entity.Entity exception ) {
        Set<io.gomint.entity.Entity> nearby = null;

        int minX = Numbers.fastFloor( ( bb.getMinX() - 2 ) / 4 );
        int maxX = Numbers.fastCeil( ( bb.getMaxX() + 2 ) / 4 );
        int minZ = Numbers.fastFloor( ( bb.getMinZ() - 2 ) / 4 );
        int maxZ = Numbers.fastCeil( ( bb.getMaxZ() + 2 ) / 4 );

        for ( int x = minX; x < maxX; ++x ) {
            for ( int z = minZ; z < maxZ; ++z ) {
                int chunkX = x >> 2;
                int chunkZ = z >> 2;

                Chunk chunk = this.getChunk( chunkX, chunkZ );
                if ( chunk != null ) {
                    Collection<io.gomint.entity.Entity> entities = chunk.getEntities();
                    if ( entities != null ) {
                        for ( io.gomint.entity.Entity entity : entities ) {
                            if ( !entity.equals( exception ) && entity.getBoundingBox().intersectsWith( bb ) ) {
                                if ( nearby == null ) {
                                    nearby = new HashSet<>();
                                }

                                nearby.add( entity );
                            }
                        }
                    }
                }
            }
        }

        return nearby;
    }

    @Override
    public List<AxisAlignedBB> getCollisionCubes( io.gomint.entity.Entity entity, AxisAlignedBB bb, boolean includeEntities ) {
        int minX = Numbers.fastFloor( bb.getMinX() );
        int minY = Numbers.fastFloor( bb.getMinY() );
        int minZ = Numbers.fastFloor( bb.getMinZ() );
        int maxX = Numbers.fastCeil( bb.getMaxX() );
        int maxY = Numbers.fastCeil( bb.getMaxY() );
        int maxZ = Numbers.fastCeil( bb.getMaxZ() );

        List<AxisAlignedBB> collisions = null;

        for ( int z = minZ; z < maxZ; ++z ) {
            for ( int x = minX; x < maxX; ++x ) {
                for ( int y = minY; y < maxY; ++y ) {
                    Block block = this.getBlockAt( x, y, z );

                    if ( !block.canPassThrough() ) {
                        AxisAlignedBB blockBox = block.getBoundingBox();
                        if ( blockBox.intersectsWith( bb ) ) {
                            if ( collisions == null ) {
                                collisions = new ArrayList<>();
                            }

                            collisions.add( blockBox );
                        }
                    }
                }
            }
        }

        if ( includeEntities ) {
            Collection<io.gomint.entity.Entity> entities = getNearbyEntities( bb.grow( 0.25f, 0.25f, 0.25f ), entity );
            if ( entities != null ) {
                for ( io.gomint.entity.Entity entity1 : entities ) {
                    if ( collisions == null ) {
                        collisions = new ArrayList<>();
                    }

                    collisions.add( entity1.getBoundingBox() );
                }
            }
        }

        return collisions;
    }

    /**
     * Use a item on a block to interact / place it down
     *
     * @param itemInHand    of the player which wants to interact
     * @param blockPosition on which we want to use the item
     * @param face          on which we interact
     * @param clickPosition the exact position on the block we interact with
     * @param entity        which interacts with the block
     * @return true when interaction was successful, false when not
     */
    public boolean useItemOn( ItemStack itemInHand, BlockPosition blockPosition, int face, Vector clickPosition, io.gomint.server.entity.EntityPlayer entity ) {
        Block blockClicked = this.getBlockAt( blockPosition );
        if ( blockClicked instanceof Air ) {
            return false;
        }

        // TODO: Event stuff and spawn protection / Adventure gamemode

        io.gomint.server.world.block.Block clickedBlock = (io.gomint.server.world.block.Block) blockClicked;
        boolean interacted = false;
        if ( !entity.isSneaking() ) {
            interacted = clickedBlock.interact( entity, face, clickPosition, itemInHand );
        }

        if ( !interacted || entity.isSneaking() ) {
            boolean canBePlaced = ( (io.gomint.server.inventory.item.ItemStack) itemInHand ).getBlockId() < 256 && !( itemInHand instanceof ItemAir );
            if ( canBePlaced ) {
                Block blockReplace = blockClicked.getSide( face );
                io.gomint.server.world.block.Block replaceBlock = (io.gomint.server.world.block.Block) blockReplace;

                if ( clickedBlock.canBeReplaced( itemInHand ) ) {
                    replaceBlock = clickedBlock;
                } else if ( !replaceBlock.canBeReplaced( itemInHand ) ) {
                    return false;
                }

                // We got the block we want to replace
                // Let the item build up the block
                boolean success = Blocks.replaceWithItem( entity, replaceBlock, itemInHand, clickPosition );
                if ( success ) {
                    // Schedule neighbour updates

                }

                return success;
            }
        }

        return false;
    }

    public EntityItem createItemDrop( Location location, ItemStack item ) {
        EntityItem entityItem = new EntityItem( item, this );
        spawnEntityAt( entityItem, location );
        return entityItem;
    }

    public void close() {
        // Stop async worker
        this.asyncWorkerRunning = false;
    }

    public TemporaryStorage getTemporaryBlockStorage( BlockPosition position ) {
        // Get chunk
        int x = position.getX(), y = position.getY(), z = position.getZ();
        int xChunk = CoordinateUtils.fromBlockToChunk( x );
        int zChunk = CoordinateUtils.fromBlockToChunk( z );

        ChunkAdapter chunk = this.loadChunk( xChunk, zChunk, true );
        return chunk.getTemporaryStorage( x & 0xF, y, z & 0xF );
    }

    public ChunkAdapter generate( int x, int z ) {
        if ( this.chunkGenerator != null ) {
            ChunkAdapter chunkAdapter = this.chunkGenerator.generate( x, z );
            if ( chunkAdapter != null ) {
                this.chunkCache.putChunk( chunkAdapter );
                return chunkAdapter;
            }
        }

        return null;
    }

    public void sendLevelEvent( Vector position, int levelEvent, int data ) {
        PacketWorldEvent worldEvent = new PacketWorldEvent();
        worldEvent.setData( data );
        worldEvent.setEventId( levelEvent );
        worldEvent.setPosition( position );

        sendToVisible( position.toBlockPosition(), worldEvent, new Predicate<Entity>() {
            @Override
            public boolean test( Entity entity ) {
                return true;
            }
        } );
    }

    public void storeTileEntity( BlockPosition position, TileEntity tileEntity ) {
        // Get chunk
        int x = position.getX(), y = position.getY(), z = position.getZ();
        int xChunk = CoordinateUtils.fromBlockToChunk( x );
        int zChunk = CoordinateUtils.fromBlockToChunk( z );

        ChunkAdapter chunk = this.loadChunk( xChunk, zChunk, true );
        chunk.setTileEntity( x & 0xF, y, z & 0xF, tileEntity );
    }

    public boolean breakBlock( BlockPosition position, boolean drops ) {
        io.gomint.server.world.block.Block block = getBlockAt( position );
        if ( block.onBreak() ) {
            if ( drops ) {
                for ( ItemStack itemStack : block.getDrops() ) {
                    EntityItem item = this.createItemDrop( block.getLocation(), itemStack );
                    item.setVelocity( new Vector( 0.1f, 0.3f, 0.1f ) );
                }
            }

            // Break animation (this also plays the break sound in the client)
            sendLevelEvent( position.toVector().add( .5f, .5f, .5f ), LevelEvent.PARTICLE_DESTROY, block.getBlockId() | (block.getBlockData() << 8) );

            block.setType( io.gomint.world.block.Air.class, (byte) 0 );

            return true;
        } else {
            return false;
        }
    }

    public void resetTemporaryStorage( BlockPosition position ) {
        // Get chunk
        int x = position.getX(), y = position.getY(), z = position.getZ();
        int xChunk = CoordinateUtils.fromBlockToChunk( x );
        int zChunk = CoordinateUtils.fromBlockToChunk( z );

        ChunkAdapter chunk = this.loadChunk( xChunk, zChunk, true );
        chunk.resetTemporaryStorage( x & 0xF, y, z & 0xF );
    }

}
