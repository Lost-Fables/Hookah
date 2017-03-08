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
import net.minecraft.server.v1_11_R1.EntityArmorStand;

public class ScenarioSpin extends Scenario {
	
	public ScenarioSpin(Player player) {
		super(player);
	}

	private float speed = 0;
	
	public boolean play() {
		PacketHandler.toggleRedTint(player, true);
		player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 860, 2));
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 50, 2));
		player.playSound(player.getLocation(), Sound.RECORD_STAL, 1f, 1f);
		
		EntityArmorStand camera = initCamera();
		
		//Delay moving the camera for a few seconds
		Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahMain.plugin, new Runnable() {
			public void run() {
				PacketHandler.moveCamera(player, camera.getId());
			}
		}, 60);
		
		//Rotates the camera linearly faster every tick
		BukkitTask rotateTask = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahMain.plugin, new Runnable() {
			public void run() {
				speed += 0.03f;
				camera.yaw = spinYaw(camera.yaw, speed);
				PacketHandler.sendEntityLookPacket(player, camera.getId(), camera.yaw, camera.pitch);
			}
		}, 0, 1);
		
		//Plays the elyrta flight sound throughout the scenario
		BukkitTask soundTask = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahMain.plugin, new Runnable() {
			public void run() {
				player.playSound(player.getLocation(), Sound.ITEM_ELYTRA_FLYING, 0.5f, 1f);
			}
		}, 220, 180);
		
		//Ends the scenario after a certain amount of time
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahMain.plugin, new Runnable() {
			public void run () {
				rotateTask.cancel();
				soundTask.cancel();
				PacketHandler.toggleRedTint(player, false);
				PacketHandler.moveCamera(player, player.getEntityId());
				PacketHandler.removeFakeMobs(player, new int[]{camera.getId()});
				player.stopSound(Sound.RECORD_STAL);
				player.stopSound(Sound.ITEM_ELYTRA_FLYING);
				activeScenarios.remove(player.getUniqueId());
			}
		}, 760);
		
		return true;
	}
	
	private EntityArmorStand initCamera() {
		Location loc = player.getLocation();
		loc.setY(loc.getY() + 0.5);
		
		EntityArmorStand camera = new EntityArmorStand(((CraftWorld) player.getWorld()).getHandle());
		camera.setInvisible(true);
		camera.setLocation(loc.getX(), loc.getY(), loc.getZ(), player.getLocation().getYaw(), 0);
		camera.setNoGravity(true);
		PacketHandler.spawnFakeLivingEntity(player, camera);
		
		return camera;
	}
}
