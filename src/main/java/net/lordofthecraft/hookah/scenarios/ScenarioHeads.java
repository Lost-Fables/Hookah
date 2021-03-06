package net.lordofthecraft.hookah.scenarios;

import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;

import net.lordofthecraft.hookah.HookahPlugin;
import net.lordofthecraft.hookah.PacketHandler;
import net.minecraft.server.v1_13_R2.EntityArmorStand;
import org.bukkit.*;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ScenarioHeads extends Scenario {
	
	public ScenarioHeads(Player player) {
		super(player);
	}
	
	final private String popUpSkinURL = "http://textures.minecraft.net/texture/f4edb1f1ef2ba92ccceb3ddd927c1d4d3ff4635f61cca697efa914ca2e688";
	private int interval = 40; //Time in ticks until the next time blindness is toggled
	private List<Spirit> heads = new ArrayList<>();
	private BukkitTask flashTask, rapidFlashingTask;
	
	public boolean play() {
		PacketHandler.toggleRedTint(player, true);
		
		//Spawns 20 floating heads over the course of the scenario
		for (int i = 0; i < 20; i++) {
			Spirit head = new Spirit();
			head.setStartT((Math.random() * 25) + 10);
			head.spawn(player, null, null, 1, null);
			heads.add(head);
			
			//Delay the spawn so they appear exactly 30 ticks apart from each other
			Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahPlugin.plugin, () -> PacketHandler.sendEquipment(player, head.getStand().getId(), ItemSlot.HEAD,
																														new ItemStack(Material.WITHER_SKELETON_SKULL)), i * 30);
		}
		
		flashBlindness(); //Starts the blindness flashing
		
		//Mega task that manages all the events of this scenario
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskLater(HookahPlugin.plugin, () -> {
			for (Spirit head: heads) {
				head.remove(player);
			}

			rapidFlashingTask.cancel();
			player.removePotionEffect(PotionEffectType.BLINDNESS);

			//Place the popUp 1 block away from the player's face and have it facing the player.
			Location popUpLocation = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(1));
			popUpLocation.setDirection(player.getLocation().subtract(popUpLocation).toVector());
			EntityArmorStand popUp = new EntityArmorStand(((CraftWorld) player.getWorld()).getHandle());
			popUp.setLocation(popUpLocation.getX(), popUpLocation.getY(), popUpLocation.getZ(), popUpLocation.getYaw(), popUpLocation.getPitch());
			popUp.setInvisible(true);
			PacketHandler.spawnNMSLivingEntity(player, popUp);

			//Prevent movement
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 254));
			player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 60, -5));

			//delay the attaching the head to make sure the armorstand has had time to spawn
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahPlugin.plugin, () -> {
				player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, SoundCategory.VOICE, 1f, 1f);
				PacketHandler.sendEquipment(player, popUp.getId(), ItemSlot.HEAD, getSkull(popUpSkinURL));
			}, 2);

			//Cleans and clears everything. Ends the scenario.
			Bukkit.getServer().getScheduler().runTaskLater(HookahPlugin.plugin, () -> {
				PacketHandler.removeFakeMobs(player, new int[]{popUp.getId()});
				remove();
			}, 30);
		}, 800));
		
		return true;
	}
	
	//Used to force stop the scenario
	public void remove() {
		heads.forEach((head) -> head.remove(player));
		if (flashTask != null) 
			flashTask.cancel();
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
		PacketHandler.toggleRedTint(player, false);
		cleanTasks();
		activeScenarios.remove(player.getUniqueId());
	}
	
	//TODO I really hate the way I made this
	private void flashBlindness() {
		if (--interval > 3)
			flashTask = Bukkit.getServer().getScheduler().runTaskLater(HookahPlugin.plugin, () -> {
				if (new Random().nextInt(2) == 0)
					player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.VOICE, 1f, 1f);
				toggleBlindness();
				flashBlindness();
			}, interval);
		else {
			rapidFlashingTask = Bukkit.getServer().getScheduler().runTaskTimer(HookahPlugin.plugin, () -> {
				player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.VOICE, 1f, 1f);
				toggleBlindness();
			}, 3, 3);
			tasksToCleanup.add(rapidFlashingTask);
		}
	}
	
	private void toggleBlindness() {
		if (player.hasPotionEffect(PotionEffectType.BLINDNESS)) {
			player.removePotionEffect(PotionEffectType.BLINDNESS);
			player.removePotionEffect(PotionEffectType.SLOW);
		}
		else {
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
			player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 1));
			player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, SoundCategory.VOICE, 1f, 1f);
		}
	}
}

