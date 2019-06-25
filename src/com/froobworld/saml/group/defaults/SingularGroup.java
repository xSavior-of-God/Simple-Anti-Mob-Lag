package com.froobworld.saml.group.defaults;

import com.froobworld.saml.group.Group;
import com.froobworld.saml.group.GroupStatusUpdater;
import com.froobworld.saml.group.ProtoGroup;
import com.froobworld.saml.group.TypedEntity;
import org.bukkit.entity.LivingEntity;

public class SingularGroup implements Group {

    @Override
    public String getName() {
        return "default_singular";
    }

    @Override
    public boolean inProtoGroup(LivingEntity entity, ProtoGroup protoGroup) {
        return false;
    }

    @Override
    public boolean canBeCentre(LivingEntity entity) {
        return true;
    }

    @Override
    public int assignTypeId(LivingEntity entity) {
        return 0;
    }

    @Override
    public GroupStatusUpdater groupStatusUpdater() {
        return new GroupStatusUpdater() {
            private boolean group;

            @Override
            public void updateStatus(TypedEntity typedEntity) {
                group = true;
            }

            @Override
            public boolean isGroup() {
                return group;
            }
        };
    }
}
