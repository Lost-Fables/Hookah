package net.lordofthecraft.Scenarios;

import net.lordofthecraft.HookahMain;
import net.lordofthecraft.PacketHandler;
import net.minecraft.server.v1_12_R1.EntitySnowman;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

public class ScenarioSnowman extends Scenario{

	public ScenarioSnowman(Player player) {
		super(player);
	}
	
	private final String[] names = new String[] {
			"Jerry", "Jeffrey", "Robert", "Carter", "Jamal",
			"Tai", "Bob", "John", "Phil", "Frosty The Snowman",
			"Alfred", "Iblees", "Ariel", "Frederick"
	};
	
	private Map<Spirit, EntitySnowman> snowmen = new HashMap<>();

	public boolean play() {
		
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 30, 1));
		player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 800, 1));
		PacketHandler.toggleRedTint(player, true);
		
		//5 snowmen
		for (int i = 0; i < 5; i++) {
			tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskLater(HookahMain.plugin, new Runnable() {
				public void run() {
					Spirit path = new Spirit();
					path.spawn(player, null, null, 1, null);
					path.setStartT(20);
					EntitySnowman snowman = new EntitySnowman(((CraftWorld) player.getWorld()).getHandle());
					snowman.setLocation(path.getStand().locX, path.getStand().locY, path.getStand().locZ, 0, 0);
					snowman.setCustomName(ChatColor.AQUA + names[random.nextInt(names.length)]);
					snowman.setCustomNameVisible(true);
					snowman.setHasPumpkin(false);
					PacketHandler.spawnNMSLivingEntity(player, snowman);
					snowmen.put(path, snowman);
				}
			}, i * 40));	
		}
		
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskTimer(HookahMain.plugin, new Runnable() {
			public void run() {
				for (Spirit path: snowmen.keySet()) {
					if (random.nextInt(100) == 0) player.playSound(player.getLocation(), Sound.ENTITY_SNOWMAN_AMBIENT, 1f, 1f);
					Location loc = new Location(player.getWorld(), path.getStand().locX, path.getStand().locY, path.getStand().locZ, 0, 0);
					loc.setDirection(player.getLocation().subtract(loc).toVector()); //look at player
					PacketHandler.teleportFakeEntity(player, snowmen.get(path).getId(), loc);
				}
			}
		}, 0, 1));
		
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskLater(HookahMain.plugin, new Runnable() {
			public void run() {
				remove();
			}
		}, 800));
		
		return true;
	}
	
	public void remove() {
		for (Spirit path: snowmen.keySet()) {
			PacketHandler.removeFakeMobs(player, new int[]{snowmen.get(path).getId()});
			path.remove(player);
		}
		PacketHandler.toggleRedTint(player, false);
		cleanTasks();
		if (activeScenarios.containsKey(player.getUniqueId()))
			activeScenarios.remove(player.getUniqueId());
	}
}
