package icu.azim.wagrapple.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.tag.Tag;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class Util {

    public static void writeVec3d(PacketByteBuf buffer, Vec3d vec) {
        buffer.writeDouble(vec.x);
        buffer.writeDouble(vec.y);
        buffer.writeDouble(vec.z);
    }
    
    public static Vec3d readVec3d(PacketByteBuf buffer) {
        double x = buffer.readDouble();
        double y = buffer.readDouble();
        double z = buffer.readDouble();
        return new Vec3d(x,y,z);
    }

    public static Vec3d readVec3d(NbtCompound tag, String name) {
        NbtCompound ctag = tag.getCompound(name);
        return new Vec3d(ctag.getDouble("x"), ctag.getDouble("y"), ctag.getDouble("z"));
    }
    
    public static void writeVec3d(NbtCompound tag, String name, Vec3d vec) {
        NbtCompound ctag = new NbtCompound();
        ctag.putDouble("x", vec.x);
        ctag.putDouble("y", vec.y);
        ctag.putDouble("z", vec.z);
        tag.put(name, ctag);
    }
    
    public static BlockPos readBlockPos(NbtCompound tag, String name) {
        NbtCompound ctag = tag.getCompound(name);
        return new BlockPos(ctag.getInt("x"), ctag.getInt("y"), ctag.getInt("z"));
    }
    
    public static void writeBlockPos(NbtCompound tag, String name, BlockPos vec) {
        NbtCompound ctag = new NbtCompound();
        ctag.putInt("x", vec.getX());
        ctag.putInt("y", vec.getY());
        ctag.putInt("z", vec.getZ());
        tag.put(name, ctag);
    }
    
    public static Collection<Identifier> getTagsFor(Block block, Map<Identifier, Tag<Block>> entries) {
        List<Identifier> list = Lists.newArrayList();
        for (Map.Entry<Identifier, Tag<Block>> entry : entries.entrySet()) {
            if (entry.getValue().values().contains(block)) {
                list.add(entry.getKey());
            }
        }
        return list;
    }
    
    public static int getClosest(double a, double b, double x) {
        return (Math.abs(a-x)<Math.abs(b-x)?1:2);
    }
    
    public static Vec3d getPlayerShoulder(PlayerEntity player, int hand, float tickDelta) {
        double x = 0;
        double y = 1.3;
        double z = 0;
        double yaw = (player.prevBodyYaw == 0 ? player.bodyYaw : MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw)) % 360;
        yaw = yaw * Math.PI / 180;
        yaw += hand == -1 ? -Math.PI * 3 / 4 : +Math.PI / 4;
        double ycos = Math.cos(yaw);
        double ysin = Math.sin(yaw);
        x = x + (player.prevX == 0 ? player.getX() : MathHelper.lerp(tickDelta, player.prevX, player.getX())) - (ycos + ysin) * 0.2; // * 0.8D;
        y = y + (player.prevY == 0 ? player.getY() : MathHelper.lerp(tickDelta, player.prevY, player.getY())) - ((player.isSneaking() && !player.getAbilities().flying) ? 0.3 : 0);
        z = z + (player.prevZ == 0 ? player.getZ() : MathHelper.lerp(tickDelta, player.prevZ, player.getZ())) - (ysin - ycos) * 0.2; // * 0.8D;
        return new Vec3d(x,y,z);
    }
}
