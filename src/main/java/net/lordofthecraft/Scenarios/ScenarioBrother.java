package net.lordofthecraft.Scenarios;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import com.comphenix.protocol.wrappers.EnumWrappers.Particle;

import net.lordofthecraft.HookahMain;
import net.lordofthecraft.PacketHandler;
import net.md_5.bungee.api.ChatColor;

public class ScenarioBrother extends Scenario {
	
	public ScenarioBrother(Player player) {
		super(player);
	}
	
	private Spirit brother;
	
	public boolean play() {
		PacketHandler.toggleRedTint(player, true);
		
		brother = new Spirit();
		brother.spawn(player, Particle.CLOUD, Particle.SLIME, 1, ChatColor.AQUA + "Kindred Spirit");
		player.playSound(player.getLocation(), Sound.ENTITY_CAT_PURR, SoundCategory.VOICE, 1f, 1f);
		
		//Sends a random emote to the player every so often
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskTimerAsynchronously(HookahMain.plugin, new Runnable(){
			public void run() {
				randomPurrSound();
				if (random.nextInt(5) == 0) 
					player.sendMessage(Emotes.values()[random.nextInt(Emotes.values().length)].emote);
			}
		}, 200, 200));
		
		//Task that ends the scenario
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskLaterAsynchronously(HookahMain.plugin, new Runnable(){
			public void run() {
				PacketHandler.toggleRedTint(player, false);
				brother.remove(player);
				cleanTasks();
				activeScenarios.remove(player.getUniqueId());
			}
		}, 3600));
		
		return true;
	}
	
	//Used to force stop the scenario
	public void remove() {
		cleanTasks();
		brother.remove(player);
	}
	
	private void randomPurrSound() {
		Sound sound = random.nextInt(2) == 0 ? Sound.ENTITY_CAT_PURREOW : Sound.ENTITY_CAT_PURR;
		player.playSound(new Location(player.getWorld(), player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ()),
				sound, SoundCategory.VOICE, 1f, 1f);
	}
	
	//TODO might want to move these into a .txt or .yml file
	//All the different emotes the spirit can speak to the player
	enum Emotes {
		ADMIRE_HAIR(ChatColor.AQUA + "The kindred spirit lets out a light chuckle before cheerfully exclaiming "
				+ ChatColor.WHITE + "\"I never noticed you had such beautiful hair!\"" + ChatColor.AQUA + "."),
		ADMIRE_FIGURE(ChatColor.AQUA + "The kindred spirit stops for a moment to admire your charming figure."),
		AMBIENT_FLY(ChatColor.AQUA + "The spirit flies about the open air around you, radiating pure joy and happiness "
				+ "from its smokey being. " + ChatColor.WHITE + "\"Weeeeeeee!\"" + ChatColor.AQUA + "."),
		ADMIRE_VOICE(ChatColor.AQUA + "The spirit audibly shows signs of satisfaction and joy, " + ChatColor.WHITE
				+ "\"Your voice is music to my ears. You are truly a majestic creature.\"" + ChatColor.AQUA + ".");
		
		private final String emote;
		Emotes(String emote) {
			this.emote = emote;
		}
	}
}
