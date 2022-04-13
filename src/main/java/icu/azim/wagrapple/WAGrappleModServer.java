package icu.azim.wagrapple;

import icu.azim.wagrapple.entity.GrappleLineEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;

public class WAGrappleModServer implements net.fabricmc.api.DedicatedServerModInitializer{

    @Override
    public void onInitializeServer() {
        ServerPlayNetworking.registerGlobalReceiver(WAGrappleMod.DETACH_LINE_PACKET_ID, (server, player, handler, attachedData, responseSender) -> {
            boolean detach = attachedData.readBoolean();
            server.execute(() -> {
                if (WAGrappleMod.GRAPPLE_COMPONENT.get(player).isGrappled() && detach) {
                    int id = WAGrappleMod.GRAPPLE_COMPONENT.get(player).getLineId();
                    if (id > 0) {
                        Entity e = player.world.getEntityById(id);
                        if (e != null) {
                            e.remove(Entity.RemovalReason.UNLOADED_WITH_PLAYER);
                        }
                    }
                    WAGrappleMod.GRAPPLE_COMPONENT.get(player).setLineId(-1);
                    WAGrappleMod.GRAPPLE_COMPONENT.get(player).setGrappled(!detach);
                    WAGrappleMod.GRAPPLE_COMPONENT.get(player).applySyncPacket(attachedData);
                }
 
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(WAGrappleMod.UPDATE_LINE_PACKET_ID, (server, player, handler, data, responseSender) -> GrappleLineEntity.handleSyncPacket(server, player, data));
        System.out.println("server init done");
    }
}
