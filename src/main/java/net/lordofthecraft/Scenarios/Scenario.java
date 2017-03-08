package net.lordofthecraft.Scenarios;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

public class Scenario {
	
	//TODO make this Map<UUID, Scenario>
	protected static List<UUID> activeScenarios = new ArrayList<>();
	protected static Random random = new Random();
	protected Player player; //Keeps track of the player who's experiencing this scenario
	private Scenarios type;
	
	
	public static enum Scenarios {
		SPIRIT_KO,
		BROTHER,
		DRAGON,
		DIMENSION,
		SPIN,
		HEAD,
		PARADISE;
		
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
		if (activeScenarios.contains(player.getUniqueId())) return false;
		activeScenarios.add(player.getUniqueId());
		
		switch (type) {
		case SPIRIT_KO:
			new ScenarioSpiritKo(player).play(800);
			break;
		case BROTHER:
			new ScenarioBrother(player).play();
			break;
		case DRAGON:
			new ScenarioDragon(player).play();
			break;
		case DIMENSION:
			new ScenarioDimension(player).play();
			break;
		case SPIN:
			new ScenarioSpin(player).play();
			break;
		case HEAD:
			new ScenarioHeads(player).play();
			break;
		case PARADISE:
			new ScenarioParadise(player).play();
			break;
		}
		
		return true;
	}
	
	public static List<UUID> getActiveScenarios() {
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
	    ItemStack head = new ItemStack(Material.SKULL_ITEM, 1, (short)3);
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
