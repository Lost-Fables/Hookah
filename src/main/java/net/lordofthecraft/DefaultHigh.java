package net.lordofthecraft;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class DefaultHigh implements Listener{
	
	protected enum HighType {
		HUNGER,
		SPEED,
		SLOW
	}
	
	class ActiveHigh {
		int durationTask;
		int nauseaTask;
		double amplifier;
		
		private ActiveHigh(int durationTask, int nauseaTask, double amplifier) {
			this.durationTask = durationTask;
			this.nauseaTask = nauseaTask;
			this.amplifier = amplifier;
		}
	}
	
	private static final int ceiling = 4200; //High duration cannot go past this value of time (ticks)
	
	private static Map<UUID, ActiveHigh> activeHighs = new HashMap<>();
	private HighType type;
	
	public DefaultHigh(HighType type) {
		this.type = type;
	}
	
	public static Map<UUID, ActiveHigh> getActiveHighs() {
		return activeHighs;
	}
	
	public void send(Player player) {
		//Any consecutive hits exponentially gets longer
		if (activeHighs.containsKey(player.getUniqueId())) {
			double amplifier = activeHighs.get(player.getUniqueId()).amplifier;
			activeHighs.get(player.getUniqueId()).amplifier = amplifier * 1.4;
			
			int time = (int) (amplifier * 200);
			if (time > ceiling) time = ceiling;
			
			Bukkit.getServer().getScheduler().cancelTask(activeHighs.get(player.getUniqueId()).durationTask);
			activeHighs.get(player.getUniqueId()).durationTask = startDurationTask(player, time);
		} else //First hit
			activeHighs.put(player.getUniqueId(),
					new ActiveHigh(startDurationTask(player, 200), startNauseaTask(player), 1.5));
	}
	
	private int startDurationTask(Player player, int time) {
		switch (type) {
		case HUNGER:
			sendHungerEffect(player, time);
			break;
		case SPEED:
			sendSpeedEffect(player, time);
			break;
		case SLOW:
			sendSlowEffect(player, time);
			break;
		}
		
		return Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahMain.plugin, new Runnable() {
			public void run() {
				Bukkit.getServer().getScheduler().cancelTask(activeHighs.get(player.getUniqueId()).nauseaTask);
				activeHighs.remove(player.getUniqueId());
				PacketHandler.toggleRedTint(player, false);
				player.removePotionEffect(PotionEffectType.CONFUSION);
			}
		}, time);
	}
	
	private int startNauseaTask(Player player) {
		return Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(HookahMain.plugin, new Runnable() {
			public void run() {
					player.addPotionEffect(new PotionEffect(PotionEffectType.CONFUSION, 300, 4), true);
			}
		}, 0, 290);	
	}
	
	private void sendHungerEffect(Player player, int time) {
		player.addPotionEffect(new PotionEffect(PotionEffectType.HUNGER, time, 0), true);
		PacketHandler.toggleRedTint(player, true);
	}
	
	private void sendSpeedEffect(Player player, int time) {
		player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, time, 0), true);
		PacketHandler.toggleRedTint(player, true);
	}
	
	private void sendSlowEffect(Player player, int time) {
		player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, time, 1), true);
		PacketHandler.toggleRedTint(player, true);
	}
}
