package net.lordofthecraft.Scenarios;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;

import net.lordofthecraft.HookahMain;
import net.lordofthecraft.PacketHandler;
import net.minecraft.server.v1_11_R1.EntityArmorStand;

public class ScenarioHeads extends Scenario {
	
	public ScenarioHeads(Player player) {
		super(player);
	}
	
	final private String popUpSkinURL = "http://textures.minecraft.net/texture/f4edb1f1ef2ba92ccceb3ddd927c1d4d3ff4635f61cca697efa914ca2e688";
	private int interval = 40; //Time in ticks until the next time blindness is toggled
	private int rapidFlashingTask;
	
	public boolean play() {
		
		List<Spirit> heads = new ArrayList<Spirit>();
		
		PacketHandler.toggleRedTint(player, true);
		
		//Spawns 20 floating heads over the course of the scenario
		for (int i = 0; i < 20; i++) {
			Spirit head = new Spirit();
			head.setStartT((Math.random() * 25) + 10);
			head.spawn(player, null, null, 1, null);
			heads.add(head);
			
			//Delay the spawn so they appear exactly 30 ticks apart from eachother
			Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahMain.plugin, new Runnable() {
				public void run() {
					PacketHandler.sendEquipment(player, head.getStand().getId(), ItemSlot.HEAD, 
							new ItemStack(Material.SKULL_ITEM, 1, (short) (new Random().nextInt(3))));
				}
			}, i * 30);
		}
		
		flashBlindness(); //Starts the blindness flashing
		
		//Mega task that manages all the events of this scenario
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahMain.plugin, new Runnable() {
			public void run () {
				for (Spirit head: heads) {
					head.remove(player);
				}
				
				Bukkit.getServer().getScheduler().cancelTask(rapidFlashingTask);
				player.removePotionEffect(PotionEffectType.BLINDNESS);
				
				//Place the popUp 1 block away from the player's face and have it facing the player.
				Location popUpLocation = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(1));
				popUpLocation.setDirection(player.getLocation().subtract(popUpLocation).toVector());
				
				EntityArmorStand popUp = new EntityArmorStand(((CraftWorld) player.getWorld()).getHandle());
				popUp.setLocation(popUpLocation.getX(), popUpLocation.getY(), popUpLocation.getZ(), popUpLocation.getYaw(), popUpLocation.getPitch());
				popUp.setInvisible(true);
				PacketHandler.spawnFakeLivingEntity(player, popUp);
				
				//Prevent movement
				player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 254));
				player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 60, -5));
				
				//delay the attaching the head to make sure the armorstand has had time to spawn
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahMain.plugin, new Runnable() {
					public void run() {
						player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, SoundCategory.VOICE, 1f, 1f);
						PacketHandler.sendEquipment(player, popUp.getId(), ItemSlot.HEAD, getSkull(popUpSkinURL));
					}
				}, 2);
				
				//Cleans and clears everything. Ends the scenario.
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahMain.plugin, new Runnable() {
					public void run () {
						player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 1));
						PacketHandler.removeFakeMobs(player, new int[]{popUp.getId()});
						PacketHandler.toggleRedTint(player, false);
						activeScenarios.remove(player.getUniqueId());
					}
				}, 30);
			}
		}, 800);
		
		return true;
	}
	
	//TODO I really hate the way I made this
	private void flashBlindness() {
		if (--interval > 3)
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahMain.plugin, new Runnable() {
				public void run () {
					if (new Random().nextInt(2) == 0)
						player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.VOICE, 1f, 1f);
					toggleBlindness();	
					flashBlindness();
				}
			}, interval);
		else
			rapidFlashingTask = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(HookahMain.plugin, new Runnable() {
				public void run () {
					player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, SoundCategory.VOICE, 1f, 1f);
					toggleBlindness();
				}
			}, 3, 3);	
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

