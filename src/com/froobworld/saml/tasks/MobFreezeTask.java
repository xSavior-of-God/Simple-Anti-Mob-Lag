package com.froobworld.saml.tasks;

import com.froobworld.saml.*;
import com.froobworld.saml.events.SamlMobFreezeEvent;
import com.froobworld.saml.events.SamlPreMobFreezeEvent;
import com.froobworld.saml.utils.TpsSupplier;
import com.froobworld.saml.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MobFreezeTask implements Runnable {
    private Saml saml;
    private SamlConfiguration config;
    private SamlConfiguration messages;
    private TpsSupplier tpsSupplier;
    private FrozenChunkCache frozenChunkCache;

    public MobFreezeTask(Saml saml) {
        this.saml = saml;
        this.config = saml.getSamlConfig();
        this.messages = saml.getSamlMessages();
        this.tpsSupplier = new TpsSupplier(saml);
        if(saml.getSamlConfig().getBoolean("keep-frozen-chunk-cache")) {
            createChunkCacheIfNotExist();
        } else {
            frozenChunkCache = null;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(saml, this);
    }


    public FrozenChunkCache getFrozenChunkCache() {
        return frozenChunkCache;
    }

    public void createChunkCacheIfNotExist() {
        if(frozenChunkCache == null) {
            frozenChunkCache = new FrozenChunkCache(new File(saml.getDataFolder(), ".chunk-cache"), saml, false);
        }
    }

    @Override
    public void run() {
        Bukkit.getScheduler().scheduleSyncDelayedTask(saml, this, config.getLong("ticks-per-operation"));
        double tps = tpsSupplier.get();
        long startTime = System.currentTimeMillis();
        long maxOperationTime = config.getLong("maximum-operation-time");

        if(tps > config.getDouble("tps-unfreezing-threshold")) {
            int unfrozen = 0;
            double unfreezeLimit = config.getDouble("unfreeze-limit");
            for(World world : Bukkit.getWorlds()) {
                for(LivingEntity entity : world.getLivingEntities()) {
                    if(unfrozen >= unfreezeLimit) {
                        break;
                    }
                    if(!entity.hasAI()) {
                        unfrozen++;
                        entity.setAI(true);
                    }
                }
            }
            return;
        }
        double thresholdTps = config.getDouble("tps-freezing-threshold");
        if(tps > thresholdTps) {
            return;
        }
        SamlPreMobFreezeEvent preMobFreezeEvent = new SamlPreMobFreezeEvent(tps);
        Bukkit.getPluginManager().callEvent(preMobFreezeEvent);
        if(preMobFreezeEvent.isCancelled()) {
            return;
        }

        MessageUtils.broadcastToOpsAndConsole(messages.getString("starting-freezing-operation")
                .replaceAll("%TPS", "" + tps)
                , config);
        int numberFrozen = 0;
        int totalFrozen = 0;
        int totalMobs = 0;
        boolean groupBias = config.getBoolean("group-bias");
        boolean smartScaling = config.getBoolean("use-smart-scaling");
        double baseMinimumSize = config.getDouble("group-minimum-size");
        double baseMaximumRadiusSq = Math.pow(config.getDouble("group-maximum-radius"), 2);

        double minimumSize = smartScaling ? baseMinimumSize * (1 - (thresholdTps - tps) / thresholdTps) : baseMinimumSize;
        double maximumRadiusSq = smartScaling ? baseMaximumRadiusSq / Math.pow((1 - (thresholdTps - tps) / thresholdTps), 2) : baseMaximumRadiusSq;

        Set<String> alwaysFreeze = new HashSet<String>(config.getStringList("always-freeze"));

        List<LivingEntity> mobsToFreeze = new ArrayList<LivingEntity>();
        for(World world : Bukkit.getWorlds()) {
            if(config.getStringList("ignore-worlds").contains(world.getName())) {
                continue;
            }
            if(!groupBias) {
                for(LivingEntity entity : world.getLivingEntities()) {
                    totalMobs++;
                    if(!entity.hasAI()) {
                        totalFrozen++;
                        continue;
                    }
                    if(System.currentTimeMillis() - startTime > maxOperationTime) {
                        continue;
                    }
                    if(preMobFreezeEvent.getShouldIgnorePredicate().test(entity)) {
                        continue;
                    }
                    mobsToFreeze.add(entity);
                }
                continue;
            }

            List<NeighbouredEntity> neighbouredEntities = new ArrayList<NeighbouredEntity>();
            for(LivingEntity entity : world.getLivingEntities()) {
                totalMobs++;
                if(!entity.hasAI()) {
                    totalFrozen++;
                    continue;
                }
                if(System.currentTimeMillis() - startTime > maxOperationTime) {
                    continue;
                }
                if(preMobFreezeEvent.getShouldIgnorePredicate().test(entity)) {
                    continue;
                }
                NeighbouredEntity thisEntity = new NeighbouredEntity(entity);
                if(alwaysFreeze.contains(entity.getType().name())) {
                    neighbouredEntities.add(thisEntity);
                    thisEntity.freezeByDefault = true;
                    continue;
                }
                for(NeighbouredEntity otherEntity : neighbouredEntities) {
                    if(thisEntity.mostPopularNeighbour.neighbours >= minimumSize && otherEntity.mostPopularNeighbour.neighbours >= minimumSize) {
                        continue;
                    }
                    if(thisEntity.entity.getLocation().distanceSquared(otherEntity.entity.getLocation())  < maximumRadiusSq) {
                        thisEntity.neighbours++;
                        otherEntity.neighbours++;
                        if(thisEntity.mostPopularNeighbour.neighbours < otherEntity.neighbours) {
                            thisEntity.mostPopularNeighbour = otherEntity;
                        }
                        if(thisEntity.mostPopularNeighbour.neighbours < thisEntity.neighbours) {
                            thisEntity.mostPopularNeighbour = thisEntity;
                        }
                        if(otherEntity.mostPopularNeighbour.neighbours < thisEntity.neighbours) {
                            otherEntity.mostPopularNeighbour = thisEntity;
                        }
                        if(otherEntity.mostPopularNeighbour.neighbours < otherEntity.neighbours) {
                            otherEntity.mostPopularNeighbour = otherEntity;
                        }
                    }
                }
                neighbouredEntities.add(thisEntity);
            }

            for(NeighbouredEntity neighbouredEntity : neighbouredEntities) {
                if(neighbouredEntity.mostPopularNeighbour.neighbours >= minimumSize || neighbouredEntity.freezeByDefault) {
                    mobsToFreeze.add(neighbouredEntity.entity);
                }
            }
        }

        SamlMobFreezeEvent mobFreezeEvent = new SamlMobFreezeEvent(mobsToFreeze);
        Bukkit.getPluginManager().callEvent(mobFreezeEvent);
        if(!mobFreezeEvent.isCancelled()) {
            for(LivingEntity entity : mobFreezeEvent.getMobsToFreeze()) {
                if(entity.hasAI()) {
                    entity.setAI(false);
                    if(frozenChunkCache != null) {
                        frozenChunkCache.addChunk(entity.getLocation());
                    }
                    numberFrozen++;
                    totalFrozen++;
                }
            }
            if(frozenChunkCache != null) {
                frozenChunkCache.saveToFile();
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;
        MessageUtils.broadcastToOpsAndConsole(messages.getString("freezing-operation-complete")
                        .replaceAll("%TIME", "" + elapsedTime)
                        .replaceAll("%NUMBER_FROZEN", "" + numberFrozen)
                        .replaceAll("%TOTAL_FROZEN", "" + totalFrozen)
                        .replaceAll("%TOTAL_MOBS", "" + totalMobs)
                , config);
    }

    private class NeighbouredEntity {
        private LivingEntity entity;
        private int neighbours;
        private NeighbouredEntity mostPopularNeighbour;
        private boolean freezeByDefault;

        public NeighbouredEntity(LivingEntity entity) {
            this.entity = entity;
            this.neighbours = 1;
            this.mostPopularNeighbour = this;
            this.freezeByDefault = false;
        }
    }

}
