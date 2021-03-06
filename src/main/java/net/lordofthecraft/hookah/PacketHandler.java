package net.lordofthecraft.hookah;

import com.comphenix.packetwrapper.WrapperPlayServerCamera;
import com.comphenix.packetwrapper.WrapperPlayServerEntityDestroy;
import com.comphenix.packetwrapper.WrapperPlayServerEntityEquipment;
import com.comphenix.packetwrapper.WrapperPlayServerEntityLook;
import com.comphenix.packetwrapper.WrapperPlayServerEntityTeleport;
import com.comphenix.packetwrapper.WrapperPlayServerMultiBlockChange;
import com.comphenix.packetwrapper.WrapperPlayServerSpawnEntityLiving;
import com.comphenix.packetwrapper.WrapperPlayServerWorldBorder;
import com.comphenix.packetwrapper.WrapperPlayServerWorldParticles;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;
import com.comphenix.protocol.wrappers.EnumWrappers.WorldBorderAction;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedParticle;
import java.util.List;
import net.minecraft.server.v1_13_R2.EntityLiving;
import net.minecraft.server.v1_13_R2.PacketPlayOutMapChunk;
import net.minecraft.server.v1_13_R2.PacketPlayOutSpawnEntityLiving;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/*
 * Class containing all the different methods we use to
 * send various types of packets to a player.
 */
public abstract class PacketHandler {
	//Sadly uses NMS
	public static void spawnNMSLivingEntity (Player player, EntityLiving entity) {
		((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(entity));
	}
	
	public static void spawnLivingEntity (Player player, Entity entity) {
		WrapperPlayServerSpawnEntityLiving packet = new WrapperPlayServerSpawnEntityLiving();
		packet.setEntityID(entity.getEntityId());
		packet.setType(entity.getType());
		packet.setUniqueId(entity.getUniqueId());
		packet.setX(entity.getLocation().getX());
		packet.setY(entity.getLocation().getY());
		packet.setZ(entity.getLocation().getZ());
		packet.setYaw(entity.getLocation().getYaw());
		packet.setPitch(entity.getLocation().getPitch());
		packet.sendPacket(player);
	}
	
	public static void removeFakeMobs (Player player, int[] mobs) {
		WrapperPlayServerEntityDestroy packet = new WrapperPlayServerEntityDestroy();
		packet.setEntityIds(mobs);
		packet.sendPacket(player);
	}
	
	public static void teleportFakeEntity(Player player, int entityId, Location loc) {
		WrapperPlayServerEntityTeleport packet = new WrapperPlayServerEntityTeleport();
		packet.setEntityID(entityId);
		packet.setOnGround(false);
		packet.setX(loc.getX());
		packet.setY(loc.getY());
		packet.setZ(loc.getZ());
		packet.setYaw(loc.getYaw());
		packet.setPitch(loc.getPitch());
		packet.sendPacket(player);
	}
	
	public static void sendEntityLookPacket (Player player, int entityId, float yaw, float pitch) {
		WrapperPlayServerEntityLook look = new WrapperPlayServerEntityLook();
		look.setYaw(yaw);
		look.setPitch(pitch);
		look.setEntityID(entityId);
		look.setOnGround(false);
		look.sendPacket(player);
	}
	
	public static void sendEquipment(Player player, int entityId, ItemSlot slot, ItemStack item) {
		WrapperPlayServerEntityEquipment packet = new WrapperPlayServerEntityEquipment();
		packet.setEntityID(entityId);
		packet.setItem(item);
		packet.setSlot(slot);
		packet.sendPacket(player);
	}
	
	public static void sendFakeParticle(Player player, Location loc, WrappedParticle particleType, int amount) {
		WrapperPlayServerWorldParticles packet = new WrapperPlayServerWorldParticles();
		packet.setLongDistance(false);
		packet.setNumberOfParticles(amount);
		packet.setOffsetX(0);
		packet.setOffsetY(0);
		packet.setOffsetZ(0);
		packet.setParticleData(0);
		packet.setParticleType(particleType);
		packet.setX((float) loc.getX());
		packet.setY((float) loc.getY());
		packet.setZ((float) loc.getZ());
		packet.sendPacket(player);
	}
	
	public static void moveCamera(Player player, int entityId) {
		WrapperPlayServerCamera packet = new WrapperPlayServerCamera();
		packet.setCameraId(entityId);
		packet.sendPacket(player);
	}
	
	//NMS :(
    public static void sendNMSChunkPacket(Player player, net.minecraft.server.v1_13_R2.Chunk chunk) {
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(new PacketPlayOutMapChunk(chunk, 0xffff));
	}
	
	public static void sendChunkData(Player player, Chunk chunk, List<MultiBlockChangeInfo> chunkData) {
		WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange();
		packet.setChunk(new ChunkCoordIntPair(chunk.getX(), chunk.getZ()));
		packet.setRecords(chunkData.toArray(new MultiBlockChangeInfo[0]));
		packet.sendPacket(player);
	}
	
	public static void toggleRedTint(Player player, boolean giveTint) {
		WrapperPlayServerWorldBorder packet = new WrapperPlayServerWorldBorder();
		packet.setAction(WorldBorderAction.SET_WARNING_BLOCKS);
		
		if (giveTint)
			packet.setWarningDistance(Integer.MAX_VALUE);	
		else 
			packet.setWarningDistance(0);
		
		packet.sendPacket(player);
	}
}
