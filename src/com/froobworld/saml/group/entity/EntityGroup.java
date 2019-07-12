package com.froobworld.saml.group.entity;

import com.froobworld.saml.group.Group;
import com.froobworld.saml.group.GroupStatusUpdater;
import com.froobworld.saml.group.ProtoGroup;
import org.bukkit.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public interface EntityGroup extends Group<SnapshotEntity> {
    public Map<String, Object> getSnapshotProperties(LivingEntity entity);
    public default void scaleToTps(double tps, double expectedTps) {}

    public static BiFunction<EntityGroup, EntityGroup, EntityGroup> transformGroupOperation(BiFunction<EntityGroup, EntityGroup, Group<SnapshotEntity>> operation) {
        return new BiFunction<EntityGroup, EntityGroup, EntityGroup>() {
            @Override
            public EntityGroup apply(EntityGroup entityGroup1, EntityGroup entityGroup2) {
                Group<SnapshotEntity> snapshotEntityGroup = operation.apply(entityGroup1, entityGroup2);

                return new EntityGroup() {
                    @Override
                    public Map<String, Object> getSnapshotProperties(LivingEntity entity) {
                        Map<String, Object> allProperties = new HashMap<String, Object>();
                        Map<String, Object> snapshotProperties1 = entityGroup1.getSnapshotProperties(entity);
                        Map<String, Object> snapshotProperties2 = entityGroup2.getSnapshotProperties(entity);
                        if(snapshotProperties1 != null) {
                            allProperties.putAll(snapshotProperties1);
                        }
                        if(snapshotProperties2 != null) {
                            allProperties.putAll(snapshotProperties2);
                        }

                        return allProperties;
                    }

                    @Override
                    public String getName() {
                        return null;
                    }

                    @Override
                    public boolean inProtoGroup(SnapshotEntity entity, ProtoGroup<SnapshotEntity> protoGroup) {
                        return snapshotEntityGroup.inProtoGroup(entity, protoGroup);
                    }

                    @Override
                    public boolean canBeMember(SnapshotEntity candidate) {
                        return snapshotEntityGroup.canBeMember(candidate);
                    }

                    @Override
                    public GroupStatusUpdater<SnapshotEntity> groupStatusUpdater() {
                        return snapshotEntityGroup.groupStatusUpdater();
                    }

                    @Override
                    public void scaleToTps(double tps, double expectedTps) {
                        entityGroup1.scaleToTps(tps, expectedTps);
                        entityGroup2.scaleToTps(tps, expectedTps);
                    }
                };
            }
        };
    }
}
