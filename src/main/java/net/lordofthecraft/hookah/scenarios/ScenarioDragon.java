package net.lordofthecraft.hookah.scenarios;

import net.lordofthecraft.hookah.HookahPlugin;
import net.lordofthecraft.hookah.PacketHandler;
import net.minecraft.server.v1_13_R2.EntityEnderDragon;
import net.minecraft.server.v1_13_R2.EntityZombie;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ScenarioDragon extends Scenario{

	public ScenarioDragon(Player player) {
		super(player);
	}
	
	private EntityZombie camera;
	private EntityEnderDragon dragon;
	private double yPos = 0; //used to calculate the y of the dragon
	private double angle = 0; //angle of rotation
	private double radius = 100; //radius from the player's position that the flight path will take
	private Location centerLoc; //player's initial position
	
	public boolean play() {
		PacketHandler.toggleRedTint(player, true);
		centerLoc = player.getLocation();
		centerLoc.setY(calculateHighestY() + 5);
		
		//Zombie used as camera
		camera = new EntityZombie(((CraftWorld) player.getWorld()).getHandle());
		camera.setInvisible(true);
		camera.setLocation(centerLoc.getX(), centerLoc.getY(), centerLoc.getZ(), 0, 0);
		PacketHandler.spawnNMSLivingEntity(player, camera);
		
		//dragon
		dragon = new EntityEnderDragon(((CraftWorld) player.getWorld()).getHandle());
		dragon.setSilent(true);
		dragon.setLocation(centerLoc.getX(), centerLoc.getY(), centerLoc.getZ(), 0, 0);
		PacketHandler.spawnNMSLivingEntity(player, dragon);
		player.playSound(player.getLocation().subtract(0, 5, 0), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
		
		//Delay moving the camera for artistic effect
		Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahPlugin.plugin, () -> PacketHandler.moveCamera(player, camera.getId()), 60);
		
		//Plays dragon sounds
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahPlugin.plugin, () -> playRandomAmbientSound(), 60, 60));
		
		//Plays the elytra flight sound during the scenario
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahPlugin.plugin, () -> player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 0.5f, 1f), 60, 180));
		
		//Task that makes the dragon moves around
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahPlugin.plugin, () -> moveCameraAndDragon(), 0, 1));
		
		//Task that ends the scenario
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskLater(HookahPlugin.plugin, () -> remove(), 600));
		
		return true;
	}
	
	//Used to force stop the scenario
	public void remove() {
		PacketHandler.removeFakeMobs(player, new int[]{camera.getId(), dragon.getId()});
		PacketHandler.moveCamera(player, player.getEntityId());
		PacketHandler.toggleRedTint(player, false);
		player.stopSound(Sound.ITEM_ELYTRA_FLYING);
		player.stopSound(Sound.ENTITY_ENDER_DRAGON_GROWL);
		player.stopSound(Sound.ENTITY_ENDER_DRAGON_FLAP);
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 1));
		cleanTasks();
		activeScenarios.remove(player.getUniqueId());
	}
	
	private double calculateHighestY() {
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
		return highestY;
	}
	
	private void moveCameraAndDragon() {
		Location previousLoc = new Location(player.getWorld(), camera.locX, camera.locY, camera.locZ);
		yPos += Math.PI/36;			
		angle += Math.PI/288;
		
		camera.locX = Math.sin(angle) * radius + centerLoc.getX();
		camera.locZ = Math.cos(angle) * radius + centerLoc.getZ();
		
		camera.locY = 4 * Math.cos(yPos) + centerLoc.getY();
		
		Location loc = new Location(player.getWorld(), camera.locX, camera.locY, camera.locZ);
		loc.setDirection(previousLoc.subtract(loc).toVector()); //Look in front on it
		
		Location copyloc = loc.clone();
		//keeps the dragon 1 tick behind the camera to simulate dragon riding
		Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahPlugin.plugin, () -> PacketHandler.teleportFakeEntity(player, dragon.getId(), copyloc), 1);
		
		loc.setY(loc.getY() + 2.5);
		loc.setYaw(loc.getYaw() + 180);
		loc.setPitch(-loc.getPitch() + 25);
		PacketHandler.teleportFakeEntity(player, camera.getId(), loc);
	}
	
	private void playRandomAmbientSound() {
		if (random.nextInt(6) == 0)
			player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
		else if (random.nextInt() > 1)
			playWingsFlappingSound();			
	}
	
	private void playWingsFlappingSound() {
		for (int i = 0; i < 3; i++) {
			Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahPlugin.plugin, () -> player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f), i * 10);
		}
	}
}
