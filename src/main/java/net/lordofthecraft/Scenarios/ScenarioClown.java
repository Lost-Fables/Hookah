package net.lordofthecraft.Scenarios;

import com.comphenix.packetwrapper.WrapperPlayServerMultiBlockChange;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.google.common.primitives.Ints;
import net.lordofthecraft.HookahMain;
import net.lordofthecraft.PacketHandler;
import net.minecraft.server.v1_12_R1.EntityCow;
import net.minecraft.server.v1_12_R1.PacketPlayOutSpawnEntityLiving;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_12_R1.CraftChunk;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * ALERT ALERT ALERT
 * This scenario is not part of the plugin. This is the original concept for
 * disco but needs a massive rework because its too intense for slower
 * clients and some of the calculations need to be simplified.
 *
 */

public class ScenarioClown extends Scenario{
	ScenarioClown(Player player) {
		super(player);
	}

	private List<Integer> cows = new ArrayList<Integer>();
	private List<Integer> cowTasks = new ArrayList<Integer>();
	private List<Chunk> affectedChunks = new ArrayList<Chunk>();
	private List<Chunk> currentChunks = new ArrayList<Chunk>();
	private Location centerLoc;
	private int minY;
	private int maxY;
	boolean formWalls;
	
	public void play(int duration) {
		formWalls = false;
		centerLoc = player.getLocation();
		
		minY = (int) centerLoc.getY() - 3;
		maxY = (int) centerLoc.getY() + 10;
		
		player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration + 60, 2));
		player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, duration + 60 , 2));
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2));
		
		PacketHandler.toggleRedTint(player, true);
		
		//start disco spam;
		int taskID = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(HookahMain.plugin, new Runnable() {
			public void run () {
				spawnDisco(formWalls);
				if (formWalls) formWalls = false;
				spawnDiscoCow((int) (Math.random() * 8 + 6));
			}
		}, 0, 20);
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahMain.plugin, new Runnable() {
			public void run () {
				Bukkit.getServer().getScheduler().cancelTask(taskID);
				PacketHandler.toggleRedTint(player, false);
				
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahMain.plugin, new Runnable() {
					public void run () {
						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2));
						for (int task: cowTasks) {
							Bukkit.getServer().getScheduler().cancelTask(task);
						}
						PacketHandler.removeFakeMobs(player, Ints.toArray(cows));
						removeDisco(player);	
					}
				}, 10);
			}
		}, duration);
	}
	private void spawnDiscoCow(int amount) {
		for(int i = 0; i < amount; i++) {
			EntityCow cow = new EntityCow(((CraftWorld) centerLoc.getWorld()).getHandle());
			cow.setCustomNameVisible(true);
			
			Location customloc = currentChunks.get(6).getBlock(0, (int) (Math.random() * (maxY - minY) + (minY + 5)), 0).getLocation();
			
			customloc.setX(customloc.getX() + (int) (Math.random() * 45 + 1));
			customloc.setZ(customloc.getZ() + (int) (Math.random() * 45 + 1));
			
			cow.setLocation(customloc.getX(), customloc.getY(), customloc.getZ(), player.getLocation().getYaw(), player.getLocation().getPitch());
			
			cows.add(cow.getId());
			
			((CraftPlayer) player).getHandle().playerConnection.sendPacket(new PacketPlayOutSpawnEntityLiving(cow));
			
			cowTasks.add(Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahMain.plugin, new Runnable() {
				public void run () {
					cows.remove(new Integer(cow.getId()));
					PacketHandler.removeFakeMobs(player, new int[]{cow.getId()});
					PacketHandler.sendFakeParticle(player, new Location(player.getWorld(), cow.locX, cow.locY, cow.locZ), 
							EnumWrappers.Particle.SPELL_MOB_AMBIENT, 200);
					PacketHandler.sendFakeParticle(player, new Location(player.getWorld(), cow.locX, cow.locY, cow.locZ), 
							EnumWrappers.Particle.EXPLOSION_LARGE, 50);
					PacketHandler.sendFakeParticle(player, new Location(player.getWorld(), cow.locX, cow.locY, cow.locZ), 
							EnumWrappers.Particle.EXPLOSION_NORMAL, 100);
					PacketHandler.sendFakeParticle(player, new Location(player.getWorld(), cow.locX, cow.locY, cow.locZ), 
							EnumWrappers.Particle.HEART, 100);
				}
			}, (int) (Math.random() * 60 + 10)));
		}
		
		player.playSound(player.getLocation(), Sound.ENTITY_COW_AMBIENT, SoundCategory.AMBIENT, 1f, 1f);
	}
	
	private void spawnDisco(boolean formWalls) {
		Chunk centerChunk = player.getLocation().getChunk(); //Get the chunk the player is in
		currentChunks = getChunks(centerChunk); //Get a list of all the chunks in the radius (3x3 grid)
		
		for (Chunk chunk: currentChunks) {
			if (!affectedChunks.contains(chunk))
				affectedChunks.add(chunk);
		}
		
		//List of block change records for each chunk
		List<List<MultiBlockChangeInfo>> records = new ArrayList<List<MultiBlockChangeInfo>>();
		
		WrappedBlockData lighting = randomLighting();
		
		//for each chunk
		for (int i = 0; i < 9; i++) {
			Chunk currentChunk = currentChunks.get(i);
			World world = currentChunk.getWorld();
			
			//record of all changes in the chunk
			List<MultiBlockChangeInfo> chunkData = new ArrayList<MultiBlockChangeInfo>();
			
			//Iterates on the Y axis (Vertical)
			for (int y = minY; y < maxY; y++) {
				//Iterates on the X axis (Cardinal)
				for (int x = 0; x < 16; x++) {
					//Iterates on the Z axis (Cardinal)
					for (int z = 0; z < 16; z++) {
						//1/32 chance of turning into a light source, otherwise turns into wool
						WrappedBlockData changedBlock = WrappedBlockData.createData(Material.WOOL, randomWoolColor());
						if ((int) (Math.random() * 32) == 20)
							changedBlock = lighting;
						
						//fill ceiling
						if (formWalls && y == maxY - 1)
							chunkData.add(new MultiBlockChangeInfo(
									new Location(world, x, y, z), changedBlock));
						//If we are at the border
						else if (formWalls && checkIfBorder(i, x, z))
							chunkData.add(new MultiBlockChangeInfo(
									new Location(world, x, y, z), changedBlock));
						//Any other block turned into wool if not air
						else if (currentChunk.getBlock(x, y, z).getType() != Material.AIR) {
							for (BlockFace face: BlockFace.values()) {
								if (currentChunk.getBlock(x, y, z).getRelative(face).getType() == Material.AIR) {
									chunkData.add(new MultiBlockChangeInfo(
											new Location(world, x, y, z), changedBlock));
									break;
								}
							}
						}
					}
				}
			}
			//Add the data for this chunk into the records at the index of the current chunk
			records.add(i, chunkData); 
		}
		
		//List of Block change arrays to be sent in the packets
		for (int i = 0; i < 9; i++) {
			WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange();
			packet.setChunk(new ChunkCoordIntPair(currentChunks.get(i).getX(), currentChunks.get(i).getZ()));
			packet.setRecords(records.get(i).toArray(new MultiBlockChangeInfo[records.get(i).size()]));
			
			//Delay packets to reduce lag
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahMain.plugin, new Runnable() {
                public void run() {
                    packet.sendPacket(player);
				}
			}, i * 2);
		}
	}
	
	private boolean checkIfBorder(int index, int x, int z) {
		switch (index) {
		case 0:
			if (z == 0 || x == 15) return true;
			break;
		case 1:
			if (x == 15) return true;
			break;
		case 2:
			if (z == 15 || x == 15) return true;
			break;
		case 3:
			if (z == 0) return true;
			break;
		case 4:
			return false;
		case 5:
			if (z == 15) return true;
			break;
		case 6:
			if (z == 0 || x == 0) return true;
			break;
		case 7:
			if (x == 0) return true;
			break;
		case 8:
			if (z == 15 || x == 0) return true;
			break;
		}
		
		return false;
	}
	
	private WrappedBlockData randomLighting() {
		WrappedBlockData data = null;
		
		switch((int) (Math.random() * 4)) {
			case 0:
				data = WrappedBlockData.createData(Material.JACK_O_LANTERN);
				break;
			case 1:
				data = WrappedBlockData.createData(Material.REDSTONE_LAMP_ON);
				break;
			case 2:
				data = WrappedBlockData.createData(Material.SEA_LANTERN);
				break;
			case 3:
				data = WrappedBlockData.createData(Material.MAGMA);
				break;
		}
		
		return data;
	}
	
	//Get all the chunks that will be affected by the scenario
	private List<Chunk> getChunks(Chunk centerChunk) {
		List<Chunk> chunks = new ArrayList<Chunk>();
		for (int x = 1; x >= -1; x--) {
			for (int z = -1; z <= 1; z++) {
				chunks.add(centerChunk.getWorld().getChunkAt(centerChunk.getX() + x, centerChunk.getZ() + z));
			}
		}
		return chunks;
	}
	
	private int randomWoolColor() {
		return (int) ((Math.random() * 6) + 1);
	}
	
	private void removeDisco(Player player) {
		for (Chunk chunk: affectedChunks) {
			if (chunk.isLoaded())
				PacketHandler.sendNMSChunkPacket(player, ((CraftChunk) chunk).getHandle());
		}
	}
}
