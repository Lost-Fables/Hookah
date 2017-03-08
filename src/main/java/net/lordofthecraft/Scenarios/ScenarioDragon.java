package net.lordofthecraft.Scenarios;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import net.lordofthecraft.HookahMain;
import net.lordofthecraft.PacketHandler;
import net.minecraft.server.v1_11_R1.EntityEnderDragon;
import net.minecraft.server.v1_11_R1.EntityZombie;

public class ScenarioDragon extends Scenario{

	private double yPos = 0; //used to calculate the y of the dragon
	private double angle = 0; //angle of rotation
	private double radius = 100; //radius from the player's position that the flight path will take
	private Location centerLoc; //player's initial position
	
	public ScenarioDragon(Player player) {
		super(player);
	}
	
	public boolean play() {
		PacketHandler.toggleRedTint(player, true);
		centerLoc = player.getLocation();
		
		//TODO look to make this a lot more efficient
		double highestY = 0;
		for (int x = (int) (centerLoc.getX() - radius); x <= centerLoc.getX() + radius; x++) {
			for (int z = (int) (centerLoc.getZ() - radius); z <= centerLoc.getZ() + radius; z++) {
				double highestY_atLoc = player.getWorld().getHighestBlockYAt(x, z); 
				if (highestY_atLoc > highestY)
					highestY = highestY_atLoc;
			}
		}
		if (highestY >= 245) highestY = 245; //Prevents the dragon from going above build height
		centerLoc.setY(highestY + 5);
		
		//Spawns a fake zombie that will be used as the player's camera
		EntityZombie camera = new EntityZombie(((CraftWorld) player.getWorld()).getHandle());
		camera.setInvisible(true);
		camera.setLocation(centerLoc.getX(), centerLoc.getY(), centerLoc.getZ(), 0, 0);
		PacketHandler.spawnFakeLivingEntity(player, camera);
		
		//Spawns the dragon
		EntityEnderDragon dragon = new EntityEnderDragon(((CraftWorld) player.getWorld()).getHandle());
		dragon.setSilent(true);
		dragon.setLocation(centerLoc.getX(), centerLoc.getY(), centerLoc.getZ(), 0, 0);
		PacketHandler.spawnFakeLivingEntity(player, dragon);
		
		player.playSound(player.getLocation().subtract(0, 5, 0), Sound.ENTITY_ENDERDRAGON_GROWL, 1f, 1f);
		//Delay moving the player's camera to give the dragon time to spawn
		Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahMain.plugin, new Runnable() {
			public void run() {
				PacketHandler.moveCamera(player, camera.getId());
			}
		}, 60);
		
		//Task that plays random sounds during the flight
		BukkitTask soundTask = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahMain.plugin, new Runnable() {
			public void run() {
				playRandomAmbientSound();
			}
		}, 60, 60);
		
		//Plays the elytra flight sound during the scenario
		BukkitTask elytraSoundTask = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahMain.plugin, new Runnable() {
			public void run() {
				player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 0.5f, 1f);
			}
		}, 60, 180);
		
		//Task that makes the dragon moves around
		BukkitTask flightTask = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahMain.plugin, new Runnable() {
			public void run() {
				Location previousLoc = new Location(player.getWorld(), camera.locX, camera.locY, camera.locZ);
				yPos += Math.PI/36;			
				angle += Math.PI/288;
				
				camera.locX = Math.sin(angle) * radius + centerLoc.getX();
				camera.locZ = Math.cos(angle) * radius + centerLoc.getZ();
				
				camera.locY = 4 * Math.cos(yPos) + centerLoc.getY();
				
				Location loc = new Location(player.getWorld(), camera.locX, camera.locY, camera.locZ);
				loc.setDirection(previousLoc.subtract(loc).toVector()); //Look infront on it
				
				Location copyloc = loc.clone();
				//keeps the dragon 1 tick behind the camera to simulate dragon riding
				Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahMain.plugin, new Runnable() {
					public void run() {
						PacketHandler.teleportFakeEntity(player, dragon.getId(), copyloc);
					}
				}, 1);
				
				loc.setY(loc.getY() + 2.5);
				loc.setYaw(loc.getYaw() + 180);
				loc.setPitch(-loc.getPitch() + 25);
				PacketHandler.teleportFakeEntity(player, camera.getId(), loc);
			}
		}, 0, 1);
		
		//Task that ends the scenario
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahMain.plugin, new Runnable() {
			public void run() {
				PacketHandler.removeFakeMobs(player, new int[]{camera.getId(), dragon.getId()});
				PacketHandler.moveCamera(player, player.getEntityId());
				PacketHandler.toggleRedTint(player, false);
				player.stopSound(Sound.ITEM_ELYTRA_FLYING);
				player.stopSound(Sound.ENTITY_ENDERDRAGON_GROWL);
				player.stopSound(Sound.ENTITY_ENDERDRAGON_FLAP);
				soundTask.cancel();
				elytraSoundTask.cancel();
				flightTask.cancel();
				player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 1));
				activeScenarios.remove(player.getUniqueId());
			}
		}, 600);
		
		return true;
	}
	
	private void playRandomAmbientSound() {
		if (random.nextInt(6) == 0)
			player.playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 1f, 1f);
		else if (random.nextInt() > 2)
			playWingsFlappingSound();			
	}
	
	private void playWingsFlappingSound() {
		for (int i = 0; i < 3; i++) {
			Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahMain.plugin, new Runnable() {
				public void run() {
					player.playSound(player.getLocation(), Sound.ENTITY_ENDERDRAGON_FLAP, 1f, 1f);
				}
			}, i * 10);
		}
	}
}
