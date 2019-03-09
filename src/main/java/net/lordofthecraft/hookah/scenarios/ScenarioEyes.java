package net.lordofthecraft.hookah.scenarios;

import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot;

import net.lordofthecraft.hookah.HookahPlugin;
import net.lordofthecraft.hookah.PacketHandler;
import net.minecraft.server.v1_13_R2.EntityArmorStand;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public class ScenarioEyes extends Scenario{

	public ScenarioEyes(Player player) {
		super(player);
	}
	
	private final static String eyeBallSkinURL = "http://textures.minecraft.net/texture/99b81c4faa73eeb212f15462a9719af57dcc2c180a2437e325ef8b73487f8f";
	
	private final static String[] textureURLs = new String[]{
			"http://textures.minecraft.net/texture/cffae0868473ee5393b59010ffbdecba5682e85483b7029303e577fffbf133f", //green
			"http://textures.minecraft.net/texture/ea2ed8584b7cb3420daf9813eaaea7ced3bc240146f557153abb5eeddce96f", //purple
			"http://textures.minecraft.net/texture/f4edb1f1ef2ba92ccceb3ddd927c1d4d3ff4635f61cca697efa914ca2e688" //red
	};
	
	private List<EyeBall> eyeballs = new ArrayList<>();
	
	public boolean play() {
		
		//Spawns eyeballs every 5 seconds over 50 seconds
		for (int i = 0; i < 30; i++) {	
			Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahPlugin.plugin, () -> {
				EyeBall eyeball = new EyeBall();
				eyeballs.add(eyeball);
				eyeball.spawn();
				PacketHandler.sendEquipment(player, eyeball.getId(), ItemSlot.HEAD, getSkull(textureURLs[random.nextInt(textureURLs.length)]));
			}, i * 40);
		}
		
		BukkitTask soundTask = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahPlugin.plugin, () -> {
			if (random.nextInt(3) == 0)
				player.playSound(player.getLocation(), Sound.ENTITY_WITCH_AMBIENT, 1f, 1f);
		}, 0, 20);
		
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahPlugin.plugin, () -> {
			for (EyeBall eyeball: eyeballs) {
				eyeball.remove();
			}
			soundTask.cancel();
			player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 1));
			activeScenarios.remove(player.getUniqueId());
		}, 1200);
		
		return true;
	}
	
	public void remove() {
		//TODO make this
	}
	
	private class EyeBall {
		
		private EntityArmorStand stand;
		private BukkitTask lookTask;
		
		private EyeBall() {
			stand = new EntityArmorStand(((CraftWorld) player.getWorld()).getHandle());
			Location loc = randomLocationAroundPlayer();
			stand.setLocation(loc.getX(), loc.getY() + random.nextInt(1), loc.getZ(), 0, 0);
			stand.setInvisible(true);
			stand.setNoGravity(true);
			PacketHandler.spawnNMSLivingEntity(player, stand);
		}
		
		private void spawn() {
			//PacketHandler.teleportFakeEntity(player, stand.getId(), randomLocationAroundPlayer());
			//Stares at the player
			lookTask = Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahPlugin.plugin, () -> lookAtPlayer(), 0, 1);
		}
		
		private void lookAtPlayer() {
			Location eyeLoc = new Location(player.getWorld(), stand.locX, stand.locY, stand.locZ);
			eyeLoc.setDirection(player.getLocation().subtract(eyeLoc).toVector()); //Vector looking at player
			stand.yaw = eyeLoc.getYaw();
			stand.pitch = eyeLoc.getPitch();
			PacketHandler.sendEntityLookPacket(player, stand.getId(), stand.yaw, stand.pitch);
		}
		
		//8 block radius around player
		private Location randomLocationAroundPlayer() {
			Location loc = player.getLocation().clone();
			loc.subtract(8,0,8);
			loc.add(random.nextInt(16), 0, random.nextInt(16));
			return loc;
		}
		
		private int getId() {
			return stand.getId();
		}
		
		private void remove() {
			lookTask.cancel();
			PacketHandler.removeFakeMobs(player, new int[]{stand.getId()});
		}
	}
}
