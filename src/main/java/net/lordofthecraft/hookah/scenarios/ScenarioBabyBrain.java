package net.lordofthecraft.hookah.scenarios;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import net.lordofthecraft.arche.ArcheCore;
import net.lordofthecraft.arche.interfaces.PersonaHandler;
import net.lordofthecraft.arche.persona.ArchePersonaHandler;
import net.lordofthecraft.hookah.HookahPlugin;
import net.lordofthecraft.hookah.PacketHandler;
import net.md_5.bungee.api.ChatColor;

public class ScenarioBabyBrain extends Scenario{

	public ScenarioBabyBrain(Player player) {
		super(player);
	}
	
	private final String[] emotes = new String[]{
			ChatColor.AQUA + "suddenly starts thrashing their arms around, spouting random gibberish, " + ChatColor.WHITE 
				+ "\"Gehgah goo hehe flebagohh\"",
			ChatColor.AQUA + "stares blankly at the ground, drool seeping from their mouth.",
			ChatColor.AQUA + "stays motionless for a moment. They begin to smell like feces.",
			ChatColor.AQUA + "rocks back and forth before clapping their hands like a maniac, giggling as they do so.",
			ChatColor.AQUA + "drops to the ground begins to crawl around.",
	};
	
	public boolean play() {
		
		player.sendMessage(ChatColor.AQUA + "You suddenly feel overwhelmed by wonder and curiousity. It is as if " +
				"you have returned to a younger, much more primitive and innocent version of yourself.");
		
		player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 1800, 2));
		PacketHandler.toggleRedTint(player, true);
		
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskTimer(HookahPlugin.plugin, new Runnable() {
			public void run() {
				randomEvent();
			}
		}, 300, 300));
		
		tasksToCleanup.add(Bukkit.getServer().getScheduler().runTaskLater(HookahPlugin.plugin, new Runnable() {
			public void run() {
				remove();
			}
		}, 1800));
		
		return true;
	}
	
	public void remove() {
		PacketHandler.toggleRedTint(player, false);
		cleanTasks();
		if (activeScenarios.containsKey(player.getUniqueId()))
			activeScenarios.remove(player.getUniqueId());
	}
	
	private void randomEvent() {
		switch(random.nextInt(7)) {
		case 0:
			vomit();
			break;
		case 1:
			fart();
			break;
		case 2:
			trip();
			break;
		default:
			randomEmote();
			break;
		}
	}
	
	private void randomEmote() {
		//TODO Get archecore persona name
		//TODO maybe find chatcolor too
		String personaName = ChatColor.AQUA + getPersonaName() + " ";
		String emote = personaName + emotes[random.nextInt(emotes.length)];
		player.sendMessage(emote);
		sendMessageToNearbyPlayers(emote, 20);
	}
	
	private void fart() {
		player.sendMessage(ChatColor.DARK_GREEN + "You suddenly feel your bowels tighten as you let out a loud and disturbing fart");
		sendMessageToNearbyPlayers(ChatColor.DARK_GREEN + getPersonaName() + " lets out a very loud and disturbing fart, it's stench "
				+ "sickening you at the first whiff of its scent.", 20);
		player.getWorld().spawnParticle(Particle.SMOKE_LARGE, player.getLocation(), 75, 1, 1, 1, 0);
	}
	
	private void vomit() {
		player.sendMessage(ChatColor.DARK_GREEN + "You suddenly feel sickness rushing up your body, the contents of your previous meal "
				+ "spilling all over the ground. A childish smile fills your face, " + ChatColor.WHITE + "\"lorafum toletb licoc tspi pu ga ga... he he he\"");
		sendMessageToNearbyPlayers(ChatColor.DARK_GREEN + getPersonaName() + "'s widen for a moment before they start vomitting all over " +
				" the floor, a playful smile rested on their face, " + ChatColor.WHITE + "\"lorafum toletb licoc tspi pu ga ga... he he he\"", 20);
		
		player.getWorld().playSound(player.getLocation(), Sound.ENTITY_EVOCATION_ILLAGER_DEATH, 1f, 1f);
		
		Location vomitLoc = player.getLocation().add(player.getLocation().getDirection().setY(0).normalize().multiply(1)).add(0, 1, 0);
		player.getWorld().spawnParticle(Particle.SLIME, vomitLoc, 75, 0.3, 0.3, 0.1);
	}
	
	private void trip() {
		player.sendMessage(ChatColor.AQUA + "You suddenly lose your balance, trip and fall face first into the ground " +
				ChatColor.WHITE + "\"Gegu hedat geeh... ehhh\"");
		sendMessageToNearbyPlayers(getPersonaName() + " suddenly loses their balance, trip and fall face first onto the ground, muttering" +
				" random gibberish " + ChatColor.WHITE + "\"Gegu hedat geeh... ehhh\"" , 20);
		player.damage(1);
		player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 1));
	}
	
	private String getPersonaName() {
		PersonaHandler personaHandler = ArcheCore.getControls().getPersonaHandler();
		if (personaHandler.hasPersona(player))
			return personaHandler.getPersona(player).getName();
		else
			return "Unknown";
	}
	
	private void sendMessageToNearbyPlayers(String message, int radius) {
		for (Player nearbyPlayer: player.getWorld().getPlayers()) {
			if (nearbyPlayer.getLocation().distance(player.getLocation()) <= radius) {
				if (nearbyPlayer.equals(player)) continue;
				nearbyPlayer.sendMessage(message);
			}
		}
	}
}
