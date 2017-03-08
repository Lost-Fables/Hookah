package net.lordofthecraft;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

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
		saveLocations();
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
		loadLocations();
		
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
	}
	
	//save hoo-kah locations into a locations.yml file
	private void saveLocations() {
		validateLocations();
		FileConfiguration locationsFile = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "locations.yml"));
		List<String> locations = new ArrayList<>();
		for (Location loc: Hookah.getLocations()) {
			locations.add( //"world;x;y;z"
					loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ());
		}
		locationsFile.set("locations", locations);
		try {
			locationsFile.save(new File(getDataFolder(), "locations.yml"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//load hoo-kah locations from a locations.yml file
	private void loadLocations() {
		for (String loc: YamlConfiguration.loadConfiguration(
				new File(getDataFolder(), "locations.yml")).getStringList("locations")) {
			String[] data = loc.split(";");
			Hookah.addHookah(new Location(Bukkit.getWorld(data[0]),
					Double.parseDouble(data[1]),
					Double.parseDouble(data[2]),
					Double.parseDouble(data[3])), new Hookah());
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
