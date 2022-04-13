package icu.azim.wagrapple.components;

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import icu.azim.wagrapple.WAGrappleMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

public class GrappledPlayerComponent implements AutoSyncedComponent, EntityComponentInitializer {
    private boolean grappled = false;
    private int lineId = -1;
    @SuppressWarnings("unused")
    private PlayerEntity owner;

    public GrappledPlayerComponent() {
    }

    public GrappledPlayerComponent(PlayerEntity owner) {
        this.owner = owner;
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        grappled = tag.getBoolean("grappled");
        lineId = tag.getInt("lineid");
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        tag.putBoolean("grappled", grappled);
        tag.putInt("lineid", lineId);
    }

    public boolean isGrappled() {
        return grappled;
    }


    public void setGrappled(boolean b) {
        grappled = b;
    }


    public int getLineId() {
        return lineId;
    }

    public void setLineId(int id) {
        lineId = id;
    }

    @Override
    public String toString() {
        return "grappled: " + grappled + " lineId" + lineId;
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(WAGrappleMod.GRAPPLE_COMPONENT, GrappledPlayerComponent::new, RespawnCopyStrategy.NEVER_COPY);
    }
}
