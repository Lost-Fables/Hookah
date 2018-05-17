package net.lordofthecraft.hookah.scenarios;

import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;

import net.lordofthecraft.hookah.HookahPlugin;
import net.lordofthecraft.hookah.PacketHandler;
import net.md_5.bungee.api.ChatColor;
import net.minecraft.server.v1_12_R1.EntityArmorStand;
import net.minecraft.server.v1_12_R1.EntityCow;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class ScenarioParadise extends Scenario{
	
	public ScenarioParadise(Player player) {
		super(player);
	}

	final private String[] headURLs = new String[]{
			"http://textures.minecraft.net/texture/6ea177f1b74ca57a1cce938e8d994bc1f637e5f69c82eff29612a13ba8b2dd7", //orange
			"http://textures.minecraft.net/texture/126e346287a21dbfca5b58c142d8d5712bdc84f5b75d4314ed2a83b222effa", //cyan
			"http://textures.minecraft.net/texture/b24070c9b6659ed25b2ca126915f4d8820fafce4324ed9a8f4b8a506345307f", //white
			"http://textures.minecraft.net/texture/88662ba0708e8d60d56365ec2bc00ff1792f16634fc845a843a84de081ea4f", //purple
			"http://textures.minecraft.net/texture/211ab3a1132c9d1ef835ea81d972ed9b5cd8ddff0a07c55a749bcfcf8df5", //gold
			"http://textures.minecraft.net/texture/5e48615df6b7ddf3ad495041876d9169bdc983a3fa69a2aca107e8f251f7687", //green
			"http://textures.minecraft.net/texture/884e92487c6749995b79737b8a9eb4c43954797a6dd6cd9b4efce17cf475846", //red
	};
	
	private List<FloatingHead> floatingHeads = new ArrayList<>();
	private List<FloatingCow> floatingCows = new ArrayList<>();
	private BukkitTask floatTask;
	
	public boolean play() {
		PacketHandler.toggleRedTint(player, true);
		player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 800, 1), true);
		player.playSound(player.getLocation(), Sound.RECORD_CHIRP, 1f, 1f);
		
		//Spawn the 3 cows that circle the player's head
		for (int i = 0; i < 3; i++) {
			tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahPlugin.plugin, new Runnable() {
				public void run() {
					floatingCows.add(new FloatingCow());
				}
			}, 16 * i));
		}
		
		//Repeating task that summons a floating head every tick
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahPlugin.plugin, new Runnable() {
			public void run() {
				//generates a random position in a 32x12x32 cube around the player
				Location startPosition = new Location(player.getWorld(), 
						player.getLocation().getX() - 16 + random.nextInt(32), 
						player.getLocation().getY() - 4 + random.nextInt(12), 
						player.getLocation().getZ() - 16 + random.nextInt(32));
				floatingHeads.add(new FloatingHead(startPosition));
			}
		}, 0, 2));
		
		startFloating();
		
		//Task that cleans everything once the scenario is over.
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskLater(HookahPlugin.plugin, new Runnable() {
			public void run() {
				remove();
			}
		}, 800));
		
		return true;
	}
	
	//Used to force stop this scenario
	public void remove() {
		stopFloating();	
		PacketHandler.toggleRedTint(player, false);
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 1));
		player.stopSound(Sound.RECORD_CHIRP);
		cleanTasks();
		if (activeScenarios.containsKey(player.getUniqueId()))
			activeScenarios.remove(player.getUniqueId());
	}
	
	private void startFloating() {
		floatTask = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahPlugin.plugin, new Runnable() {
			public void run() {
				FloatingHead[] headsArray = floatingHeads.toArray(new FloatingHead[floatingHeads.size()]);
				for (int i = 0; i < headsArray.length; i++) {
					headsArray[i].move();
				}
				
				FloatingCow[] cowsArray = floatingCows.toArray(new FloatingCow[floatingCows.size()]);
				for (int i = 0; i < cowsArray.length; i++) {
					cowsArray[i].move();
				}
			}
		}, 0, 1);
	}
	
	private void stopFloating() {
		if (floatTask != null) {
			floatTask.cancel();
		}
		FloatingHead[] headsArray = floatingHeads.toArray(new FloatingHead[floatingHeads.size()]);
		for (int i = 0; i < headsArray.length; i++) {
			headsArray[i].remove();
		}	
		FloatingCow[] cowsArray = floatingCows.toArray(new FloatingCow[floatingCows.size()]);
		for (int i = 0; i < cowsArray.length; i++) {
			cowsArray[i].remove();
		}
	}
	
	private ItemStack getRandomDecoration() {
		return getSkull(headURLs[random.nextInt(headURLs.length)]);
	}
	
	private class FloatingHead {
		
		private EntityArmorStand head;
		private double centerY; //the center point the head gravitates to
		private double x; //used in bobbing equation
		private double intensity; //how high/low the head bobs
		
		public FloatingHead(Location loc) {
			head = new EntityArmorStand(((CraftWorld) loc.getWorld()).getHandle());
			head.setLocation(loc.getX(), loc.getY(), loc.getZ(), 0, 0);
			head.setInvisible(true);
			PacketHandler.spawnNMSLivingEntity(player, head);
			
			centerY = head.locY;
			intensity = (Math.random() * 4) + 1;
			
			//Attach the head to the armorstand 1 tick later to make sure it's had time to spawn
			Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahPlugin.plugin, new Runnable() {
				public void run() {
					PacketHandler.sendEquipment(player, head.getId(), ItemSlot.HEAD, getRandomDecoration());
				}
			}, 1);
		}
		
		//Moves the head to the next position to simulate bouncing
		private void move() {
			x += Math.PI/12;
			head.locY = intensity * Math.cos(x) + centerY;
			head.yaw = spinYaw(head.yaw, 3f);
					
			PacketHandler.teleportFakeEntity(player, head.getId(), 
				new Location(player.getWorld(), head.locX, head.locY, head.locZ, head.yaw, 0));
		}
		
		public void remove() {
			PacketHandler.removeFakeMobs(player, new int[]{head.getId()});
		}
	}
	
	private class FloatingCow {
		
		private EntityCow cow;
		private double angle; //Rotation angle around the player
		
		public FloatingCow () {
			cow = new EntityCow(((CraftWorld) player.getWorld()).getHandle());
			cow.setLocation(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ(), 0, 0);
			cow.setCustomName(rainbowColoredName("Disco Cow"));
			cow.setCustomNameVisible(true);
			PacketHandler.spawnNMSLivingEntity(player, cow);
		}
		
		//rotates the cow to the next position
		private void move() {
			angle += Math.PI/24;
	
			cow.locX = Math.sin(angle) * 2.5 + player.getLocation().getX();
			cow.locZ = Math.cos(angle) * 2.5 + player.getLocation().getZ();
			
			Location loc = new Location(player.getWorld(), cow.locX, player.getLocation().getY() + 1.5, cow.locZ);
			loc.setDirection(player.getLocation().subtract(loc).toVector()); //Look at the player
			
			PacketHandler.teleportFakeEntity(player, cow.getId(), loc);
		}
		
		public void remove() {
			PacketHandler.removeFakeMobs(player, new int[]{cow.getId()});
		}
	}
	
	//Returns the parametered string in rainbow colors
	private String rainbowColoredName(String name) {
		String rainbowName = "";
		for (char c: name.toCharArray()) {
			rainbowName += randomRainbowColor();
			rainbowName += c;
		}
		return rainbowName;
	}
	
	//Generates a random pretty ChatColor
	private ChatColor randomRainbowColor() {
		switch(random.nextInt(6)) {
			case 0:
				return ChatColor.AQUA;
			case 1:
				return ChatColor.GREEN;
			case 2:
				return ChatColor.LIGHT_PURPLE;
			case 3:
				return ChatColor.YELLOW;
			case 4:
				return ChatColor.RED;
			case 5:
				return ChatColor.GOLD;
			default:
				return ChatColor.WHITE;
		}
	}
}
