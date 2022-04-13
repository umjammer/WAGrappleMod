package icu.azim.wagrapple.entity;

import java.util.stream.Stream;

import dev.onyxstudios.cca.api.v3.component.ComponentContainer;
import icu.azim.wagrapple.WAGrappleMod;
import icu.azim.wagrapple.WAGrappleModClient;
import icu.azim.wagrapple.render.GrappleLineRenderer;
import icu.azim.wagrapple.util.Util;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Arm;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import net.minecraft.world.World;

public class GrappleLineEntity extends Entity {

    private PlayerEntity player;
    private Vec3d motion;

    private GrappleLineHandler lineHandler;
    private BlockHitResult initialResult;
    private Vec3d direction;
    private float lpitch;
    private float lyaw;

    private double boostSpeed;
    private int boostCooldown;
    private int debugc;

    @SuppressWarnings("unused")
    private boolean checked = false;
    private boolean ticked = false;

    public GrappleLineEntity(EntityType<? extends Entity> type, World world) {
        super(type, world);
        lineHandler = new GrappleLineHandler(this, 16);
        motion = new Vec3d(0, 0, 0);
        boostSpeed = 1;
        direction = new Vec3d(0, 0, 0);
        boostCooldown = 15;
        debugc = -1;
        boostSpeed = 1;
        this.ignoreCameraFrustum = true;
    }

    public GrappleLineEntity(World world, PlayerEntity player, double length, double boostSpeed, BlockHitResult res) {
        this(WAGrappleMod.GRAPPLE_LINE, world);
        initialResult = res;
        this.boostSpeed = boostSpeed;
        Vec3d pos = res.getPos();
        this.setPosition(pos.x, pos.y, pos.z);
        this.updateTrackedPosition(pos.x, pos.y, pos.z);
        this.player = player;
        lineHandler= new GrappleLineHandler(this, length);
        lineHandler.add(res);
        if (world.isClient) {
            world.playSound(player, pos.x, pos.y, pos.z, SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.3F, 1.0F);
        }
    }

    @Override
    protected void initDataTracker() {
        // FishingBobberEntityRenderer
    }

    public static void handleSyncPacket(MinecraftClient client, PacketByteBuf data) {
        NbtCompound tag = data.readNbt();
        int id = tag.getInt("eid");
        client.execute(() -> {
            PlayerEntity player = client.player;
            Entity e = player.world.getEntityById(id);
            if (!(e instanceof GrappleLineEntity)) {
                return;
            }
            GrappleLineEntity line = (GrappleLineEntity)e;
            line.readCustomDataFromNbt(tag);
        });
    }

    public static void handleSyncPacket(MinecraftServer server, PlayerEntity player, PacketByteBuf data) {
        NbtCompound tag = data.readNbt();
        int id = tag.getInt("eid");
        server.execute(() -> {
            Entity e = player.world.getEntityById(id);
            if (!(e instanceof GrappleLineEntity)) {
                return;
            }
            GrappleLineEntity line = (GrappleLineEntity)e;
            line.echoEntityDataToClients(tag);
        });
    }

    public void sendEntityDataToServer() {
        if (!world.isClient) {
            System.out.println("attempted to sync from wrong side (expected client, got server)");
            return;
        }
        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
        NbtCompound data = new NbtCompound();
        this.writeCustomDataToNbt(data);
        passedData.writeNbt(data);
        ClientPlayNetworking.send(WAGrappleMod.UPDATE_LINE_PACKET_ID, passedData);
    }

    private void echoEntityDataToClients(NbtCompound tag) {
        if (world.isClient) {
            System.out.println("attempted to sync from wrong side (expected server, got client)");
            return;
        }
        Stream<ServerPlayerEntity> watchingPlayers = Stream.concat(PlayerLookup.tracking(this).stream(), PlayerLookup.tracking(this.getPlayer()).stream()).distinct().filter(player -> player != this.getPlayer());

        PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
        passedData.writeNbt(tag);
        watchingPlayers.forEach(player -> ServerPlayNetworking.send(player, WAGrappleMod.UPDATE_LINE_PACKET_ID, passedData));
    }

    @Override
    public Packet<?> createSpawnPacket() {
        PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
        data.writeInt(this.getId());
        data.writeInt(this.player.getId());
        data.writeDouble(lineHandler.getMaxLen());
        data.writeDouble(boostSpeed);
        data.writeUuid(this.getUuid());
        data.writeBlockHitResult(initialResult);
        return new CustomPayloadS2CPacket(WAGrappleMod.CREATE_LINE_PACKET_ID, data);
    }

    public GrappleLineHandler getHandler(){
        return lineHandler;
    }

    @Override
    public void tick() {
        if (player == null || !player.isAlive()) {
            this.remove(RemovalReason.UNLOADED_WITH_PLAYER);
            return;
        }
        if (!ticked) {
            ticked = true;
        }
        if (world.isClient) {
            if (boostCooldown > 0) boostCooldown--;
            if (debugc > 0) debugc--;

            lineHandler.tick();
            if (this.isRemoved()) {
                return;
            }

            int hand = player.getMainArm() == Arm.RIGHT ? 1 : -1;
            ItemStack main = player.getMainHandStack();
            if (main.getItem() != WAGrappleMod.GRAPPLE_ITEM) {
                hand = -hand;
                main = player.getOffHandStack();
                if (main.getItem() != WAGrappleMod.GRAPPLE_ITEM) { // neither of hands have the hook item, removing
                    destroyLine();
                    return;
                }
            }
            handlePlayerInput(hand);
            if (this.isRemoved()) {
                return;
            }
            grapplePhysicsTick(hand);
            if (this.isRemoved()) {
                return;
            }
            movementPhysicsTick(hand);
            if (this.isRemoved()) {
                return;
            }

        } else {
            if (!WAGrappleMod.GRAPPLE_COMPONENT.get(player).isGrappled()) {
                this.remove(RemovalReason.CHANGED_DIMENSION);
            }
        }

        super.tick();
    }

    public void handlePlayerInput(int hand) {

//        if (player.teleporting) { // TODO couldn't find an alternative
//            destroyLine();
//            return;
//        }


        if (WAGrappleModClient.getAscend().isPressed() && WAGrappleModClient.getDescend().isPressed()) {
            return; //not moving anywhere
        }
        if (player.getAbilities().flying || player.isOnGround()) {
            boostCooldown = 5;
        }

        if (WAGrappleModClient.getBoost().isPressed() && !player.getAbilities().flying && (boostCooldown == 0)) {
            Vec3d origin = lineHandler.getLastPiecePos();
            Vec3d direction = player.getCameraPosVec(0).subtract(origin).normalize().multiply(-boostSpeed);
            player.addVelocity(direction.x, direction.y, direction.z);

            detachLine();
        }

        if (WAGrappleModClient.getAscend().isPressed()) {
            if(lineHandler.getMaxLen() - lineHandler.getPiecesLen() > 1) {
                lineHandler.setMaxLen(lineHandler.getMaxLen() - 0.1);
            }
        }

        if (WAGrappleModClient.getDescend().isPressed()) {
            lineHandler.setMaxLen(lineHandler.getMaxLen() + 0.1);
        }

        if (WAGrappleModClient.getDebug().isPressed() && debugc == 0) {
            System.out.println("debug pressed");
            GrappleLineRenderer.debug = !GrappleLineRenderer.debug;
            debugc = 60;
        }
    }

    public void grapplePhysicsTick(int hand) {

        BlockHitResult res = this.world.raycast(new RaycastContext(Util.getPlayerShoulder(player, hand, 1), lineHandler.getPiecePos(lineHandler.size() - 1), ShapeType.COLLIDER, FluidHandling.NONE, player));

        if (res.getType() == Type.BLOCK) {
            lineHandler.add(res);
        } else {

        }
    }

    public void movementPhysicsTick(int hand) {
        Vec3d origin = lineHandler.getLastPiecePos();
        double distanceToOrigin = player.getPos().distanceTo(origin);

        this.direction = Util.getPlayerShoulder(player, hand, 1).subtract(origin).normalize();
        calcAxis();
        double totalLen = distanceToOrigin + lineHandler.getPiecesLen();
        if (distanceToOrigin > lineHandler.getMaxLen() * 2) {
            destroyLine();
        }

        if (totalLen > lineHandler.getMaxLen()) {

            Vec3d originToPlayer = origin.subtract(player.getPos());
            Vec3d direction = originToPlayer.normalize().multiply(totalLen - lineHandler.getMaxLen());
            Vec3d projection = project(player.getVelocity(),originToPlayer);

            Vec3d newSpeed = player.getVelocity().subtract(projection);

            // double angle = getAngle(new Vec3d(0, 1, 0), direction.normalize()) * 180 / Math.PI;
            newSpeed = newSpeed.multiply((player.getVelocity().length() - 0.001) / newSpeed.length());



            if (newSpeed.lengthSquared() < direction.lengthSquared()) { // outside of the radius, but not swinging
                newSpeed = newSpeed.add(direction);
            }
            motion = newSpeed;//.add(direction);

            if (MinecraftClient.getInstance().options.forwardKey.isPressed() && player.getPos().y < origin.y) {
                motion = motion.add(player.getRotationVector().normalize().multiply(0.05));
            }
            if (motion.lengthSquared() > 6.25) {
                motion = motion.normalize().multiply(2.5);
            }
            player.setVelocity(motion.x, motion.y, motion.z);
        } else {
            if (player.isOnGround() && totalLen < lineHandler.getMaxLen()) { // player moves towards the pivot point on land
                //do nothing actually, it's not this way in WA
            }
        }
    }

    public void destroyLine() {
        if (world.isClient) {
            player.playSound(SoundEvents.ENTITY_ITEM_BREAK, 1, 1);

            PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
            passedData.writeBoolean(true);
            ClientPlayNetworking.send(WAGrappleMod.DETACH_LINE_PACKET_ID, passedData);
        }
        this.remove(RemovalReason.CHANGED_DIMENSION);
    }

    public void detachLine() {
        if (world.isClient) {
            player.playSound(SoundEvents.ENTITY_ENDER_DRAGON_SHOOT, 0.5f, 1.5f);
            player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, 1.0F, 0.6F);

            PacketByteBuf passedData = new PacketByteBuf(Unpooled.buffer());
            passedData.writeBoolean(true);
            ClientPlayNetworking.send(WAGrappleMod.DETACH_LINE_PACKET_ID, passedData);
        }
        this.remove(RemovalReason.CHANGED_DIMENSION);
    }

    private Vec3d project(Vec3d a, Vec3d b) {
        return b.multiply(a.dotProduct(b) / b.dotProduct(b));
    }

    @SuppressWarnings("unused")
    private static double getAngle(Vec3d a, Vec3d b) {
        double part = (a.x * b.x + a.y * b.y + a.z * b.z) / (a.length() * b.length());
        return Math.acos(part);
    }

    public float getLinePitch() {
        return this.lpitch; 
    }

    public float getLineYaw() {
        return this.lyaw;
    }

    private void calcAxis() {
        this.lpitch = (float) Math.asin(-this.direction.y);
        this.lyaw = (float) Math.atan2(this.direction.x, this.direction.z);
    }

    public PlayerEntity getPlayer() {
        return player;
    }

    @Override
    public PistonBehavior getPistonBehavior() {
        return PistonBehavior.IGNORE;
    }

    @Override // draw the line no matter distance
    public boolean shouldRender(double distance) {
        return true;
    }

    @Override // draw the line no matter distance
    public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override // unused methods
    protected void readCustomDataFromNbt(NbtCompound tag) {
        if (ticked) {
            Vec3d pos = Util.readVec3d(tag, "pos");
            this.setPos(pos.x, pos.y, pos.z);
            lineHandler.updateFromCompound(tag);
        }
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound tag) {
        tag.putInt("eid", this.getId());
        tag.putDouble("maxLen", lineHandler.getMaxLen());
        tag.putInt("pieces", lineHandler.size());
        Util.writeVec3d(tag, "pos", this.getPos());

        for (int i = 0; i < lineHandler.size(); i++) {
            Util.writeBlockPos(tag, "bpos"+i, lineHandler.getPieceBlock(i));
            Util.writeVec3d(tag,"location"+i, lineHandler.getPiecePos(i));
            Util.writeVec3d(tag,"direction"+i, lineHandler.getDirection(i));
        }
    }

    public Vec3d getDirection() {
        return direction;
    }

    public double getMaxLength() {
        return lineHandler.getMaxLen();
    }

    @Override
    public ComponentContainer getComponentContainer() {
        return ComponentContainer.EMPTY;
    }
}
