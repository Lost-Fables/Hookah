package net.lordofthecraft.Scenarios;

import net.lordofthecraft.HookahMain;
import net.lordofthecraft.PacketHandler;
import net.minecraft.server.v1_12_R1.EntityArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class ScenarioSpin extends Scenario {
	
	public ScenarioSpin(Player player) {
		super(player);
	}

	private float speed = 0;
	private EntityArmorStand camera;
	
	public boolean play() {
		PacketHandler.toggleRedTint(player, true);
		player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 860, 2));
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 2));
		player.playSound(player.getLocation(), Sound.RECORD_STAL, 1f, 1f);
		
		camera = initCamera();
		
		//Delay moving the camera for a few seconds
		Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahMain.plugin, new Runnable() {
			public void run() {
				PacketHandler.moveCamera(player, camera.getId());
			}
		}, 60);
		
		//Rotates the camera linearly faster every tick
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahMain.plugin, new Runnable() {
			public void run() {
				speed += 0.03f;
				camera.yaw = spinYaw(camera.yaw, speed);
				PacketHandler.sendEntityLookPacket(player, camera.getId(), camera.yaw, camera.pitch);
			}
		}, 0, 1));
		
		//Plays the elyrta flight sound throughout the scenario
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahMain.plugin, new Runnable() {
			public void run() {
				player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 0.5f, 1f);
			}
		}, 220, 180));
		
		//Ends the scenario after a certain amount of time
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskLater(HookahMain.plugin, new Runnable() {
			public void run () {
				remove();
			}
		}, 760));
		
		return true;
	}
	
	public void remove() {
		PacketHandler.toggleRedTint(player, false);
		PacketHandler.moveCamera(player, player.getEntityId());
		PacketHandler.removeFakeMobs(player, new int[]{camera.getId()});
		player.stopSound(Sound.RECORD_STAL);
		player.stopSound(Sound.ITEM_ELYTRA_FLYING);
		cleanTasks();
		if (activeScenarios.containsKey(player.getUniqueId()))
			activeScenarios.remove(player.getUniqueId());
	}
	
	private EntityArmorStand initCamera() {
		Location loc = player.getLocation();
		loc.setY(loc.getY() + 0.5);
		
		EntityArmorStand camera = new EntityArmorStand(((CraftWorld) player.getWorld()).getHandle());
		camera.setInvisible(true);
		camera.setLocation(loc.getX(), loc.getY(), loc.getZ(), player.getLocation().getYaw(), 0);
		camera.setNoGravity(true);
		PacketHandler.spawnNMSLivingEntity(player, camera);
		
		return camera;
	}
}
