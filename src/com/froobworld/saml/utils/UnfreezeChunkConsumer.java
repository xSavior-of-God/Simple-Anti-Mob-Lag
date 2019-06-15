package com.froobworld.saml.utils;

import com.froobworld.saml.FrozenChunkCache;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

import java.util.function.Consumer;

public class UnfreezeChunkConsumer implements Consumer<Chunk> {
    private FrozenChunkCache frozenChunkCache;

    public UnfreezeChunkConsumer(FrozenChunkCache frozenChunkCache) {
        this.frozenChunkCache = frozenChunkCache;
    }


    @Override
    public void accept(Chunk chunk) {
        if(!chunk.isLoaded()) {
            chunk.load(false);
        }
        for(Entity entity : chunk.getEntities()) {
            if(entity instanceof LivingEntity) {
                if(EntityFreezer.isFrozen((LivingEntity) entity)) {
                    EntityFreezer.unfreezeEntity((LivingEntity) entity);
                }
            }
        }
        frozenChunkCache.removeChunk(chunk);
    }
}
