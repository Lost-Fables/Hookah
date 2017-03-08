package net.lordofthecraft.Scenarios;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;

import net.lordofthecraft.HookahMain;
import net.lordofthecraft.PacketHandler;
import net.minecraft.server.v1_11_R1.EntityArmorStand;

public class ScenarioDimension extends Scenario{
	
	private Chunk chunk; //The chunk affected by the scenario
	
	public ScenarioDimension(Player player) {
		super(player);
	}
	
	public boolean play() {
		chunk = player.getLocation().getChunk();
		
		List<MultiBlockChangeInfo> chunkData = spawnDimension();;
		EntityArmorStand camera = initCamera();
		
		//Repeating task that plays portal sounds around the player
		BukkitTask ambientTask = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahMain.plugin, new Runnable () {
			public void run() {
				if (random.nextInt(2) == 0)
						player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_AMBIENT, SoundCategory.VOICE, 1f, 1f);
			}
		}, 0, 20);
		
		//Task used to time the effects sent to the player to create the scenario
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahMain.plugin, new Runnable() {
			public void run() {
				ambientTask.cancel();
				player.stopSound(Sound.BLOCK_PORTAL_AMBIENT);
				player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
				
				Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahMain.plugin, new Runnable() {
					public void run () {
						player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, SoundCategory.VOICE, 1f, 1f);
						PacketHandler.moveCamera(player, camera.getId());
					}
				}, 60);
			}
		}, 200);
		//Task that ends the scenario
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahMain.plugin, new Runnable() {
			public void run () {
				activeScenarios.remove(player.getUniqueId());
				PacketHandler.moveCamera(player, player.getEntityId());
				PacketHandler.removeFakeMobs(player, new int[]{camera.getId()});
				removeDimension(chunkData);
				player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 1));
			}
		}, 400);
		
		return true;
	}
	
	private List<MultiBlockChangeInfo> spawnDimension() {
		List<MultiBlockChangeInfo> chunkData = new ArrayList<>(); //record of all changes in the chunk
		
		//3x3 surface area at y=255
		for (int y = 255; y >= 253; y--) {
			for (int x = 6; x <= 8; x++) {
				chunkData.add(new MultiBlockChangeInfo(
						new Location(player.getWorld(), x, y, 15), 
						WrappedBlockData.createData(Material.END_GATEWAY)));
			}
		}
		
		PacketHandler.sendChunkData(player, chunk, chunkData);
		return chunkData;
	}
	
	private void removeDimension(List<MultiBlockChangeInfo> chunkData) {
		for (MultiBlockChangeInfo data: chunkData) {
			data.setData(WrappedBlockData.createData(Material.AIR));
		}
		PacketHandler.sendChunkData(player, chunk, chunkData);
	}
	
	private EntityArmorStand initCamera() {
		Location loc = player.getLocation().getChunk().getBlock(7, 253, 14).getLocation();
		
		EntityArmorStand camera = new EntityArmorStand(((CraftWorld) player.getWorld()).getHandle());
		camera.setInvisible(true);
		camera.setLocation(loc.getX() + .5, loc.getY(), loc.getZ() + .5, 0, 0);	
		PacketHandler.spawnFakeLivingEntity(player, camera);
		
		return camera;
	}
}
