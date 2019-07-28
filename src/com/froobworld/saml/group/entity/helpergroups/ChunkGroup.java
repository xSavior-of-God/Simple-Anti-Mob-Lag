package com.froobworld.saml.group.entity.helpergroups;

import com.froobworld.saml.group.GroupStatusUpdater;
import com.froobworld.saml.group.ProtoGroup;
import com.froobworld.saml.group.entity.EntityGroup;
import com.froobworld.saml.group.entity.EntityGroupParser;
import com.froobworld.saml.group.entity.SnapshotEntity;
import com.google.gson.JsonObject;
import org.bukkit.entity.LivingEntity;

import java.util.Map;

public class ChunkGroup implements EntityGroup {

    @Override
    public String getName() {
        return "default_chunk";
    }

    @Override
    public ProtoMemberStatus inProtoGroup(SnapshotEntity entity, ProtoGroup<? extends SnapshotEntity> protoGroup) {
        return (entity.getLocation().getBlockX() >> 4 == protoGroup.getCentre().getLocation().getBlockX() >> 4 && entity.getLocation().getBlockZ() >> 4 == protoGroup.getCentre().getLocation().getBlockZ() >> 4) ? ProtoMemberStatus.MEMBER : ProtoMemberStatus.NON_MEMBER;
    }

    @Override
    public MembershipEligibility getMembershipEligibility(SnapshotEntity candidate) {
        return MembershipEligibility.CENTRE_OR_MEMBER;
    }

    @Override
    public GroupStatusUpdater<SnapshotEntity> groupStatusUpdater() {
        return new GroupStatusUpdater<SnapshotEntity>() {
            @Override
            public void updateStatus(SnapshotEntity member) {}

            @Override
            public boolean isGroup() {
                return true;
            }
        };
    }

    @Override
    public Map<String, Object> getSnapshotProperties(LivingEntity entity) {
        return null;
    }

    public static EntityGroupParser<ChunkGroup> parser() {
        return new EntityGroupParser<ChunkGroup>() {
            @Override
            public ChunkGroup fromJson(JsonObject jsonObject) {
                return new ChunkGroup();
            }
        };
    }

}
