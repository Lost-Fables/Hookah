package net.lordofthecraft.hookah.scenarios;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.scheduler.BukkitTask;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

public class Scenario {
	
	protected static Map<UUID, Scenario> activeScenarios = new HashMap<>();
	protected static Random random = new Random();
	protected List<BukkitTask> tasksToCleanup = new ArrayList<>(); //Holds all tasks used in a scenario
	protected Player player; //Keeps track of the player who's experiencing this scenario
	private Scenarios type;
	
	
	public enum Scenarios {
		BROTHER,
		DRAGON,
		DIMENSION,
		SPIN,
		HEADS,
		PARADISE,
		BABY,
		SNOWMAN;
		
		public static int length = 9;
	}
	
	public Scenario(Player player) {
		this.player = player;
	}
	
	public Scenario(Player player, Scenarios type) {
		this.player = player;
		this.type = type;
	}
	
	public boolean play() {
		if (activeScenarios.containsKey(player.getUniqueId())) return false;
		
		Scenario scenario = null;
		switch (type) {
		case BROTHER:
			scenario = new ScenarioBrother(player);
			break;
		case DRAGON:
			scenario = new ScenarioDragon(player);
			break;
		case DIMENSION:
			scenario = new ScenarioDimension(player);
			break;
		case SPIN:
			scenario = new ScenarioSpin(player);
			break;
		case HEADS:
			scenario = new ScenarioHeads(player);
			break;
		case PARADISE:
			scenario = new ScenarioParadise(player);
			break;
		case BABY:
			scenario = new ScenarioBabyBrain(player);
			break;
		case SNOWMAN:
			scenario = new ScenarioSnowman(player);
			break;
		}
		activeScenarios.put(player.getUniqueId(), scenario);
		scenario.play();
		
		return true;
	}
	
	public void remove() {
		//Remove the current Scenario
	}
	
	public void cleanTasks() {
		for (BukkitTask task: tasksToCleanup) {
			task.cancel();
		}
	}
	
	public static Map<UUID, Scenario> getActiveScenarios() {
		return activeScenarios;
	}
	
	protected float spinYaw(float yaw, float speed) {
		yaw = yaw - speed;
		if (yaw <= -180)
			yaw = 180;	
		return yaw;
	}
	
	//Retrieves a player head according to a texture url
	public ItemStack getSkull(String url) {
	    ItemStack head = new ItemStack(Material.PLAYER_HEAD,1);
	    if(url.isEmpty())return head;
	   
	    SkullMeta headMeta = (SkullMeta) head.getItemMeta();
	    GameProfile profile = new GameProfile(UUID.randomUUID(), null);
	    byte[] encodedData = Base64.encodeBase64(String.format("{textures:{SKIN:{url:\"%s\"}}}", url).getBytes());
	    profile.getProperties().put("textures", new Property("textures", new String(encodedData)));
	    Field profileField = null;
	    try {
	        profileField = headMeta.getClass().getDeclaredField("profile");
	        profileField.setAccessible(true);
	        profileField.set(headMeta, profile);
	    } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
	        e.printStackTrace();
	    }
	    head.setItemMeta(headMeta);
	    return head;
	}
}
