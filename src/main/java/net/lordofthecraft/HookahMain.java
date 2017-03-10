package net.lordofthecraft;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
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
	
	//saves data to the config.yml and locations.yml files
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
	
	//loads data from the config.yml and locations.yml files
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
					getLogger().info("[DrugCore] " + "Recipe " + key + " failed to load.");
				}
			}
		}
		loadHookahs();
	}
	
	//save hoo-kah locations into a locations.yml file
	private void saveHookahs() {
		validateLocations();
		FileConfiguration locationsFile = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "hookahs.yml"));
		for (Location loc: Hookah.getLocations()) {
			Hookah hookah = Hookah.getHookah(loc);
			String drugName = "None";
			if (hookah.getCurrentDrug() != null)
				drugName = hookah.getCurrentDrug().getName();
			//"world;x;y;z;currentdrug;charges"/
			String location = loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ() + ";" + 
						drugName + ";" + hookah.getCharges();
			locationsFile.set("locations." + location, "");
			
			//Serialize items inside the hookah
			List<Map<String,Object>> data = new ArrayList<>();
			for (Integer itemPos: hookah.getIngredientSlots()) { //Ingredient Slots
				data.add(Customizer.serialize(hookah.getInventory().getItem(itemPos)));
			}
			if (hookah.getInventory().getItem(16) != null) //Drug Slot
				data.add(Customizer.serialize(hookah.getInventory().getItem(16)));
			
			locationsFile.set("locations." + location, data);
		}
		try {
			locationsFile.save(new File(getDataFolder(), "hookahs.yml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//load hoo-kah locations from a locations.yml file
	private void loadHookahs() {
		FileConfiguration hookahsFile = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "hookahs.yml"));
		ConfigurationSection sec = hookahsFile.getConfigurationSection("locations");
		
		if (sec.getKeys(false).size() != 0) {
			for (String key : sec.getKeys(false)) {
				String[] data = key.split(";");
				Hookah hookah = new Hookah();
				hookah.setCurrentDrug(null);
				hookah.setCharges(0);
				
				for (Recipe recipe: Recipe.getRecipes()) {
					if (recipe.getName().equalsIgnoreCase(data[4])) {
						hookah.setCurrentDrug(recipe);
						hookah.setCharges(Integer.parseInt(data[5]));
						break;
					}		
				}
				
				Hookah.addHookah(new Location(Bukkit.getWorld(data[0]),
						Double.parseDouble(data[1]),
						Double.parseDouble(data[2]),
						Double.parseDouble(data[3])), hookah);
				
				//Deserialize items that were inside the hookah
				int slot = 10;
				for (Map<?, ?> itemData: hookahsFile.getMapList("locations." + key)) {
					hookah.getInventory().setItem(slot, Customizer.deserialize((Map<String, Object>) itemData));
					slot++;
					if (slot == 14) slot = 16;
				}
			}
		}
	}
	
	//if there isnt a brewing stand block at the location, the location is erased
	private void validateLocations() {
		for (Location loc: Hookah.getLocations()) { 
			if (loc.getBlock().getType() != Material.BREWING_STAND) 
				Hookah.removeHookah(loc);
		}
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
