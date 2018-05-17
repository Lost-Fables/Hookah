package net.lordofthecraft.hookah.scenarios;

import com.comphenix.packetwrapper.WrapperPlayServerMultiBlockChange;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.EnumWrappers.Particle;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;

import net.lordofthecraft.hookah.HookahPlugin;
import net.lordofthecraft.hookah.PacketHandler;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_12_R1.CraftChunk;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class ScenarioSpiritKo extends Scenario{

	public ScenarioSpiritKo(Player player) {
		super(player);
	}

	private List<Chunk> chunks = new ArrayList<Chunk>();
	private Location centerLoc;
	private int minY;
	private int maxY;
	
	public void play(int duration) {
		centerLoc = player.getLocation();
		
		minY = (int) centerLoc.getY() - 3;
		maxY = (int) centerLoc.getY() + 10;
		
		player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, duration + 40, 20));
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2));
		player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, duration + 40, 127));
		
		PacketHandler.toggleRedTint(player, true);
		
		spawnNether();
		Spirit spiritKO = new Spirit();
		spiritKO.spawn(player, Particle.SMOKE_LARGE, Particle.CLOUD, 2, ChatColor.GRAY + "Spirit KO");
		
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahPlugin.plugin, new Runnable() {
			public void run() {
				player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 40, 2));
				removeNether();
				spiritKO.remove(player);
				PacketHandler.toggleRedTint(player, false);
				activeScenarios.remove(player.getUniqueId());
			}
		}, duration);
	}
	
	public void spawnNether() {
		Chunk centerChunk = centerLoc.getChunk(); //Get the chunk the player is in
		chunks = getChunks(centerChunk); //Get a list of all the chunks in the radius (3x3 grid)
		
		//List of block change records for each chunk
		List<List<MultiBlockChangeInfo>> records = new ArrayList<List<MultiBlockChangeInfo>>();
		
		//for each chunk
		for (int i = 0; i < 9; i++) {
			Chunk currentChunk = chunks.get(i);
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
						WrappedBlockData changedBlock = WrappedBlockData.createData(Material.NETHERRACK);
						if ((int) (Math.random() * 10 + 1) == 5)
							changedBlock = WrappedBlockData.createData(Material.NETHER_BRICK);
						
						//Any other block turned into wool if not air
						if (currentChunk.getBlock(x, y, z).getType() != Material.AIR) {
							for (BlockFace face: BlockFace.values()) {
								if (currentChunk.getBlock(x, y, z).getRelative(face).getType() == Material.AIR) {
									chunkData.add(new MultiBlockChangeInfo(
											new Location(world, x, y, z), changedBlock));
									break;
								}
							}
						}
						else {
							if (currentChunk.getBlock(x, y, z).getRelative(BlockFace.DOWN).getType() != Material.AIR
									&& (int) (Math.random() * 20 + 1) == 1) {
								chunkData.add(new MultiBlockChangeInfo(new Location(world, x, y, z), 
										WrappedBlockData.createData(Material.FIRE)));
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
			packet.setChunk(new ChunkCoordIntPair(chunks.get(i).getX(), chunks.get(i).getZ()));
			packet.setRecords(records.get(i).toArray(new MultiBlockChangeInfo[records.get(i).size()]));
			
			//Delay packets to reduce lag
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahPlugin.plugin, new Runnable() {
                public void run() {
                    packet.sendPacket(player);
				}
			}, i * 2);
		}
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
	
	private void removeNether() {
		for (Chunk chunk: chunks) {
			if (chunk.isLoaded())
				PacketHandler.sendNMSChunkPacket(player, ((CraftChunk) chunk).getHandle());
		}
	}
}
