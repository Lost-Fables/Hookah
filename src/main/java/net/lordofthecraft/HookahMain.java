package net.lordofthecraft;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import io.github.archemedes.customitem.Customizer;
import net.lordofthecraft.Scenarios.Scenario.Scenarios;

public class HookahMain extends JavaPlugin{
	
	public static HookahMain plugin;
	
	public void onEnable() {
		
		this.getCommand("hookah").setExecutor(new HookahCommandExecutor(this));
		
		createConfig();
		loadDataFromFile();
		
		plugin = this;
		getServer().getPluginManager().registerEvents(new Listeners(), this);
	}
	
	public void onDisable() {
		saveDataToFile();
	}
	
	private void createConfig() {
		try {
			//create plugin folder if it doesn't exist
			if (!getDataFolder().exists())
				getDataFolder().mkdirs();
			
			//create config file if it doesn't exist
			if (!new File(getDataFolder(), "config.yml").exists())
				saveDefaultConfig();
		} catch (Exception e) {
			getLogger().info("[ALERT] An issue occured creating the config.yml. It might be missing be missing. Creating it...");
		}
	}
	
	//saves data to the config.yml and hookahs.yml files
	public void saveDataToFile() {
		saveHookahs();
		for (Recipe recipe: Recipe.getRecipes()) {
			String id = String.valueOf(recipe.getId());
			this.getConfig().set("recipes." + id + ".ingredients", recipe.getIngredientsToString());
			this.getConfig().set("recipes." + id + ".drug.itemtype", recipe.getDrugItem().getType().toString() + 
					(recipe.getDrugItem().getDurability() != 0 ? ":" + recipe.getDrugItem().getDurability(): ""));
			this.getConfig().set("recipes." + id + ".drug.name", recipe.getName());
			this.getConfig().set("recipes." + id + ".drug.lore", recipe.getLore());
			this.getConfig().set("recipes." + id + ".drug.chargesperdrug",
					recipe.getChargesPerDrug());
			this.getConfig().set("recipes." + id + ".drug.defaulthigh", recipe.getDefaultHigh().toString());
			this.getConfig().set("recipes." + id + ".drug.odds", recipe.getScenarioOdds());
			this.getConfig().set("recipes." + id + ".drug.scenarios", recipe.getScenariosToString());
		}
		this.saveConfig();
	}
	
	//loads data from the config.yml and hookahs.yml files
	public void loadDataFromFile() {
		ConfigurationSection section = this.getConfig().getConfigurationSection("recipes");
		if (section == null) return;
		
		if (section.getKeys(false).size() != 0) {
			for (String key : section.getKeys(false)) {
				try {
					List<ItemStack> ingredients = 
						parseIngredients(this.getConfig().getStringList("recipes." + key + ".ingredients"));
					ItemStack drugItem = parseItemType(this.getConfig().getString("recipes." + key + ".drug.itemtype"));
					String name = this.getConfig().getString("recipes." + key + ".drug.name");
					List<String> lore = this.getConfig().getStringList("recipes." + key + ".drug.lore");
					int chargesPerDrug = this.getConfig().getInt("recipes." + key + ".drug.chargesperdrug");
					DefaultHigh.HighType defaultHigh = 
							DefaultHigh.HighType.valueOf(this.getConfig().getString("recipes." + key + ".drug.defaulthigh"));
					double odds = this.getConfig().getDouble("recipes." + key + ".drug.odds");
					List<Scenarios> scenarios = 
							parseScenarios(this.getConfig().getStringList("recipes." + key + ".drug.scenarios"));
					
					Recipe.getRecipes().add(new Recipe(ingredients, drugItem, chargesPerDrug, defaultHigh, name, lore, odds, scenarios));
				} catch (Exception e) {
					getLogger().info("Recipe " + key + " failed to load.");
				}
			}
		}
		loadHookahs();
	}
	
	//save hookahs into a hookahs.yml file
	private void saveHookahs() {
		FileConfiguration hookahsFile = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "hookahs.yml"));
		validateLocations(hookahsFile);
		for (WeakLocation loc: Hookah.getLocations()) {
			Hookah hookah = Hookah.getHookah(loc);
			
			//location as "world;x;y;z"
			String locationKey = loc.toString();
			hookahsFile.set("hookahs." + locationKey, "");
			
			//Slots that contain items to be serialized
			List<Integer> affectedSlots = new ArrayList<>();
			
			//Serialize items inside the HooKah
			List<Map<String,Object>> items = new ArrayList<>();
			for (Integer itemPos: hookah.getIngredientSlots()) { //Ingredient Slots
				items.add(Customizer.serialize(hookah.getInventory().getItem(itemPos)));
				affectedSlots.add(itemPos);
			}
			if (hookah.getInventory().getItem(16) != null) { //Drug Slot
				items.add(Customizer.serialize(hookah.getInventory().getItem(16)));
				affectedSlots.add(16);
			}
			
			String drugName = "None";
			if (hookah.getCurrentDrug() != null)
				drugName = hookah.getCurrentDrug().getName();
				
			//data as "currentDrug;charges;affectedSlots" -> Example: "Cactus Green;16;11;12;16"
			String data = drugName + ";" + hookah.getCharges();
			for (Integer affectedSlot: affectedSlots) {
				data += ";" + affectedSlot;
			}
			hookahsFile.set("hookahs." + locationKey + ".data", data);
			hookahsFile.set("hookahs." + locationKey + ".items", items);
		}
		try {
			hookahsFile.save(new File(getDataFolder(), "hookahs.yml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//load hookahs from a hookahs.yml file
	private void loadHookahs() {
		FileConfiguration hookahsFile = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "hookahs.yml"));
		if (!hookahsFile.contains("hookahs")) return;
		ConfigurationSection sec = hookahsFile.getConfigurationSection("hookahs");
		
		if (sec.getKeys(false).size() != 0) {
			for (String key : sec.getKeys(false)) {
				String[] location = key.split(";");
				Hookah hookah = new Hookah();
				hookah.setCurrentDrug(null);
				hookah.setCharges(0);
				
				try {
					Hookah.addHookah(new WeakLocation(location[0],
							Integer.parseInt(location[1]),
							Integer.parseInt(location[2]),
							Integer.parseInt(location[3])), hookah);
				} catch (NullPointerException e) {
					this.getLogger().info("[ALERT] NPE trying to read " + key + ". Ignoring this location.");
				}
				
				String[] data = hookahsFile.getString("hookahs." + key + ".data").split(";");
				for (Recipe recipe: Recipe.getRecipes()) {
					if (recipe.getName().equalsIgnoreCase(data[0])) {
						hookah.setCurrentDrug(recipe);
						hookah.setCharges(Integer.parseInt(data[1]));
						break;
					}
				}
				List<Integer> slots = new ArrayList<>();
				if (data.length > 2) {
					for (int i = 2; i < data.length; i++) {
						slots.add(Integer.parseInt(data[i]));
					}
				}
					
				//Deserialize items that were inside the hookah
				Iterator<Integer> slotsIter = slots.iterator();
				for (Map<?, ?> itemData: hookahsFile.getMapList("hookahs." + key + ".items")) {
					hookah.getInventory().setItem(slotsIter.next(), Customizer.deserialize((Map<String, Object>) itemData));
				}
			}
		}
	}
	
	//if there isnt a brewing stand block at the location, the location is erased
	private void validateLocations(FileConfiguration hookahsFile) {
		List<WeakLocation> locationsToBeRemoved = new ArrayList<>();
		for (WeakLocation weakLoc: Hookah.getLocations()) {
			Location loc = weakLoc.convertToLocation();
			if (loc == null) {
				locationsToBeRemoved.add(weakLoc);
				hookahsFile.set(weakLoc.toString(), null); //Clear it from the hookahs.yml
			}
			else if (loc.getBlock().getType() != Material.BREWING_STAND) {
				locationsToBeRemoved.add(weakLoc);
				hookahsFile.set("hookahs." + weakLoc.toString(), null); //Clear it from the hookahs.yml
			}
		}
		Hookah.getLocations().removeAll(locationsToBeRemoved);
	}
	
	//Parses the String data retrieved from the config.yml
	private List<ItemStack> parseIngredients(List<String> data) {
		List<ItemStack> ingredients = new ArrayList<>();	
		for (String ingredient: data) {
			String[] components = ingredient.split(":");
			if (components.length > 1)
				ingredients.add(new ItemStack(Material.matchMaterial(components[0]), 1, Short.parseShort(components[1])));
			else
				ingredients.add(new ItemStack(Material.matchMaterial(components[0]), 1));
		}
		return ingredients;
	}
	
	//Parses the drug item from a string retrieved from the config.yml
	private ItemStack parseItemType(String data) {
		ItemStack drugItem;
		String[] components = data.split(":");
		if (components.length > 1)
			drugItem = new ItemStack (Material.matchMaterial(components[0]), 1, Short.parseShort(components[1]));
		else
			drugItem = new ItemStack (Material.matchMaterial(components[0]), 1);
		return drugItem;
	}
	
	//Parses the scenarios from a list of strings retrieved from the config.yml
	private List<Scenarios> parseScenarios(List<String> data) {
		List<Scenarios> scenarios = new ArrayList<>();
		
		if (data.contains("*")) scenarios = Arrays.asList(Scenarios.values());
		else if (data != null){
			for (String string: data) {
				scenarios.add(Scenarios.valueOf(string));
			}
		}
		return scenarios;
	}
}
