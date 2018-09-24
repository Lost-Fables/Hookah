package net.lordofthecraft.hookah;

import io.github.archemedes.customitem.CustomTag;
import net.lordofthecraft.hookah.scenarios.Scenario.Scenarios;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class Recipe {
	
	private static List<Recipe> recipes = new ArrayList<>();
	private static int highestId = 0;
	
	private List<ItemStack> ingredients;
	private ItemStack drugItem;
	private int id;
	private int chargesPerDrug;
	private DefaultHigh.HighType defaultHigh; //The default effects given by this drug
	private double scenarioOdds;
	private List<Scenarios> scenarios = new ArrayList<>();
	
	public Recipe(List<ItemStack> ingredients, ItemStack drugItem, int chargesPerDrug, DefaultHigh.HighType defaultHigh,
			String name, List<String> lore, double scenarioOdds, List<Scenarios> scenarios) {
		this.ingredients = ingredients;
		setDrugItem(drugItem);
		this.chargesPerDrug = chargesPerDrug;
		this.defaultHigh = defaultHigh;
		this.id = highestId++;
		setName(name);
		setLore(lore);
		this.scenarioOdds = scenarioOdds;
		this.scenarios = scenarios;
	}
	
	public List<ItemStack> getIngredients() {
		return ingredients;
	}
	
	//Returns the ingredients as a List of strings
	public List<String> getIngredientsToString() {
		List<String> ingredients = new ArrayList<>();
		for (ItemStack ingredient: getIngredients()) {
			ingredients.add(ingredient.getType().toString());
		}
		return ingredients;
	}
	
	//Adds a NBT to the drug item for future identification purposes
	private void setDrugItem(ItemStack drugItem) {
        CustomTag tag = new CustomTag();
        tag.put("isDrug", "true");
        this.drugItem = tag.apply(drugItem);
	}
	
	public ItemStack getDrugItem() {
		return drugItem;
	}
	
	public int getId() {
		return id;
	}
	
	public void setName(String name) {
		ItemMeta meta;
		if (drugItem.hasItemMeta())
			meta = drugItem.getItemMeta();
		else
			meta = Bukkit.getItemFactory().getItemMeta(drugItem.getType());

		meta.setDisplayName(name);
		drugItem.setItemMeta(meta);
	}
	
	public String getName() {
		if (drugItem.getItemMeta().hasDisplayName())
			return drugItem.getItemMeta().getDisplayName();
		else
			return drugItem.getType().toString();
	}
	
	public void setLore(List<String> lore) {
		ItemMeta meta;
		if (drugItem.hasItemMeta())
			meta = drugItem.getItemMeta();
		else
			meta = Bukkit.getItemFactory().getItemMeta(drugItem.getType());
		
		meta.setLore(lore);
		drugItem.setItemMeta(meta);
	}
	
	public List<String> getLore() {
		return drugItem.getItemMeta().getLore();
	}
	
	public int getChargesPerDrug() {
		return chargesPerDrug;
	}
	
	public DefaultHigh.HighType getDefaultHigh() {
		return defaultHigh;
	}
	
	public void setScenarioOdds(double scenarioOdds) {
		this.scenarioOdds = scenarioOdds;
	}
	
	public double getScenarioOdds(){
		return scenarioOdds;
	}
	
	public void addScenario(Scenarios scenario) {
		scenarios.add(scenario);
	}
	
	public List<Scenarios> getScenarios() {
		return scenarios;
	}
	
	//returns the Scenarios as a List of strings
	public List<String> getScenariosToString() {
		List<String> scenarios = new ArrayList<>();
		if (getScenarios().size() == Scenarios.length)
			scenarios.add("*");
		else {
			for (Scenarios scenario: getScenarios()) {
				scenarios.add(scenario.toString());
			}
		}
		return scenarios;
	}
	
	public static List<Recipe> getRecipes() {
		return recipes;
	}
	
	public static void clearRecipes() {
		recipes.clear();
	}
}
