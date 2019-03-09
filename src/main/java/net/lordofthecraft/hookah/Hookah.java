package net.lordofthecraft.hookah;

import io.github.archemedes.customitem.CustomTag;
import net.lordofthecraft.hookah.scenarios.Scenario;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class Hookah {
	
	private static Random random = new Random();
	private static HashMap<WeakLocation, Hookah> hookahs = new HashMap<>(); //Global collection of all Hoo-Kahs
	private static List<ItemStack> interfaceItems = generateInterfaceItems();
	
	private WeakLocation location;
	private Inventory inventory = generateDefaultInventory();
	private Recipe currentDrug = null;
	private int charges = 0;
	
	public void setLocation(WeakLocation location) {
		this.location = location;
	}
	
	public WeakLocation getLocation() {
		return location;
	}
	
	public void setInventory(Inventory inventory) {
		this.inventory = inventory;
	}
	
	public Inventory getInventory() {
		return inventory;
	}

	public static boolean isHookah(Location location) {
	  return hookahs.keySet().contains(new WeakLocation(location));
  }

	/**
	 * Only returns ingredient slots that contain an ItemStack
	 * @return List of slot id values that contain ingredients, null if empty
	 */ //TODO rewrite this to return List<ItemStack>
	public List<Integer> getIngredientSlots() {
		List<Integer> slots = new ArrayList<>();
		for (int slot = 10; slot <= 13; slot++) {
			if (inventory.getItem(slot) == null || inventory.getItem(slot).getItemMeta() == null) continue;
			slots.add(slot);
		}
		return slots;
	}
	
	public void setCurrentDrug(Recipe currentDrug) {
		this.currentDrug = currentDrug;
		updateInfoPaper();
	}
	
	public Recipe getCurrentDrug() {
		return currentDrug;
	}
	
	public void setCharges(int charges) {
		this.charges = charges;
		updateInfoPaper();
	}
	
	public int getCharges() {
		return charges;
	}
	
	public void addCharges(int amount) {
		charges += amount;
		updateInfoPaper();
	}
	
	public boolean removeCharges(int amount) {
		if (charges < amount) return false;
		charges -= amount;
		if (charges == 0) currentDrug = null;
		updateInfoPaper();
		return true;
	}
	
	public void useCharge(Player player) {
		Location loc = location.convertToLocation();
		if (loc == null) return;
		loc.add(0.5, 1.5, 0.5);
			
		loc.getWorld().spawnParticle(Particle.CLOUD, loc, 100, 1, 1, 1, 0);
		loc.getWorld().spawnParticle(Particle.SMOKE_NORMAL, loc, 50, 0.5, 1, 0.5, 0);
		loc.getWorld().spawnParticle(Particle.SMOKE_LARGE, loc, 25, 2, 1, 2, 0);
		
		//If the player is not in a scenario, it applies the default high of this drug
		if (!Scenario.getActiveScenarios().containsKey(player.getUniqueId()))
			new DefaultHigh(currentDrug.getDefaultHigh()).send(player);
		
		//Determines if a scenario should be played according to the odds specified in the drug's recipe.
		//and then plays that scenario to the player. Also removes a default high a player may have in case
		//it could interfere with a scenario
		if (random.nextDouble() <= currentDrug.getScenarioOdds() && currentDrug.getScenarios().size() > 0) {
			if (DefaultHigh.getActiveHighs().containsKey(player.getUniqueId())) DefaultHigh.remove(player);
			new Scenario(player, currentDrug.getScenarios().get(random.nextInt(currentDrug.getScenarios().size())))
				.play();
		}
		
		removeCharges(1);
	}
	
	private void updateInfoPaper() {
		ItemStack infoPaper = inventory.getItem(31);
		ItemMeta meta = infoPaper.getItemMeta();
		if (currentDrug == null)
			meta.setDisplayName(ChatColor.DARK_AQUA + "Current Drug: " + ChatColor.WHITE + "None");
		else
			meta.setDisplayName(ChatColor.DARK_AQUA + "Current Drug: " + ChatColor.WHITE + currentDrug.getName());
		meta.setLore(Collections.singletonList(ChatColor.DARK_AQUA + "Charges Left: " + ChatColor.WHITE + String.valueOf(charges)));
		infoPaper.setItemMeta(meta);
		
		inventory.setItem(31, infoPaper);
	}
	
	public boolean combineIngredients(Player player, boolean maxAmount) {
		//TODO I hate myself for this part
		for (Recipe recipe: Recipe.getRecipes()) {
			Map<ItemStack, Boolean> recipeChecklist = new HashMap<>();
			Map<ItemStack, Integer> ingredientAmounts = new HashMap<>();
			
			//init the checklist
			for (ItemStack ingredient: recipe.getIngredients()) {
				recipeChecklist.put(ingredient, false);
			}
			
			boolean recipeFail = true;
			//10, 11, 12, 13
			for (int slot = 10; slot <= 13; slot++) {
				if (inventory.getItem(slot) == null || inventory.getItem(slot).getItemMeta() == null) continue;
				
				for (ItemStack ingredient: recipeChecklist.keySet()) {
					if (inventory.getItem(slot).getType() == ingredient.getType()) {
						recipeChecklist.put(ingredient, true);
						ingredientAmounts.put(ingredient, inventory.getItem(slot).getAmount());
						recipeFail = false;
						break;
					}
				}
				if (recipeFail) break;
				
			}
			if (recipeFail) continue;
			
			for (boolean component: recipeChecklist.values()) {
				if (!component) recipeFail = true;
			}
			
			if (recipeFail) continue;
			
			//Recipe complete
			
			//Determines how many resulting drugs to give
			ItemStack resultDrug = recipe.getDrugItem().clone();
			int lowestValue = 64;
			for (Integer amount: ingredientAmounts.values()) {
				if (lowestValue > amount) lowestValue = amount;
			}
			resultDrug.setAmount(lowestValue);
			if (!maxAmount) lowestValue = 1;
			
			//Create/Add recipe result
			ItemStack resultSlotItem = inventory.getItem(16);
			if (resultSlotItem == null) //Nothing in the result slot
				inventory.setItem(16, (maxAmount ? resultDrug : recipe.getDrugItem())); //Give only 1 if if not shift clicking
			else if (resultSlotItem.getItemMeta().equals(recipe.getDrugItem().getItemMeta())) {
				if (resultSlotItem.getAmount() == 64) return false;
				if ((resultSlotItem.getAmount() + lowestValue) > 64) //If our lowest value goes past a stack, we lower it
					lowestValue = 64 - resultSlotItem.getAmount();
				inventory.getItem(16).setAmount(inventory.getItem(16).getAmount() + (maxAmount ? lowestValue : 1)); //deduct 1 if not shift-clicking
			}
			else
				return false; //Another item already in the result slot
			
			//Remove ingredients
			for (Integer slot: getIngredientSlots()) {
				for (ItemStack ingredient: recipe.getIngredients()) {
					ItemStack ingredientInInv = inventory.getItem(slot); //variable for readability

					if (isSameIngredient(ingredientInInv, ingredient)) {
						if (ingredientInInv.getAmount() > lowestValue)
							ingredientInInv.setAmount(ingredientInInv.getAmount() - (maxAmount ? lowestValue : 1)); //deduct 1 if not shift-clicking
						else
							inventory.clear(slot);	
						break;
					}
				}
			}
			
			player.playSound(player.getLocation(), Sound.BLOCK_CHORUS_FLOWER_DEATH, SoundCategory.VOICE, 1f, 1f);
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Compares the Material Id and the data of the ItemStack to determine
	 * if they are the same ingredient. Mainly used to improve readability
	 * @return
	 */
	private boolean isSameIngredient(ItemStack i1, ItemStack i2) {
		return i1.getType() == i2.getType();
	}
	
	
	public boolean lightDrug(Recipe recipe) {
		
		if (inventory.getItem(16).getAmount() > 1)
			inventory.getItem(16).setAmount(inventory.getItem(16).getAmount() - 1);
		else
			inventory.clear(16);
		
		//Add onto charges if its the same drug
		if (recipe.equals(currentDrug)) {
			addCharges(recipe.getChargesPerDrug());
		}
		else {
			setCurrentDrug(recipe);
			setCharges(recipe.getChargesPerDrug());
		}
		
		Location loc = location.convertToLocation();
		if (loc != null)
			loc.getWorld().playSound(loc, Sound.BLOCK_BREWING_STAND_BREW, SoundCategory.VOICE, 1f, 1f);
		
		return true;
	}	
	
	public void playWrongRecipe() {
		inventory.setItem(7, interfaceItems.get(5));
		inventory.setItem(15, interfaceItems.get(5));
		inventory.setItem(17, interfaceItems.get(5));
		inventory.setItem(25, interfaceItems.get(5));
		
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahPlugin.plugin, () -> {
			inventory.setItem(7, interfaceItems.get(4));
			inventory.setItem(15, interfaceItems.get(4));
			inventory.setItem(17, interfaceItems.get(4));
			inventory.setItem(25, interfaceItems.get(4));
		}, 20);
	}	
	
	public static void addHookah(WeakLocation loc, Hookah hookah) {
		//Save the location as an NBT inside the infoPaper item as "world;x;y;z"
        CustomTag tag = new CustomTag();
        tag.put("hookahloc", loc.toString());
        hookah.getInventory().setItem(31, tag.apply(hookah.getInventory().getItem(31)));
		
		hookah.setLocation(loc);
		hookahs.put(loc, hookah);
	}
	
	public static void removeHookah(WeakLocation loc) {
		hookahs.remove(loc);
	}
	
	public static Hookah getHookah(WeakLocation loc) {
		return hookahs.get(loc);
	}
	
	public static Set<WeakLocation> getLocations() {
		return hookahs.keySet();
	}
	
	public static List<ItemStack> getInterfaceItems() {
		return interfaceItems;
	}
	
	public static ItemStack generateHookah() {
		ItemStack hookah = new ItemStack(Material.BREWING_STAND, 1);
		ItemMeta meta = hookah.getItemMeta();
		meta.setDisplayName("Hoo-Kah");
		meta.setLore(Arrays.asList(ChatColor.GRAY + "A simple device used to produce and consume", 
				ChatColor.GRAY + "a various amount of high quality produce."));
		hookah.setItemMeta(meta);

        CustomTag ct = new CustomTag();
        ct.put("hookah", "hookah");
        return ct.apply(hookah);
	}
	
	private static Inventory generateDefaultInventory() {
		Inventory hookahMenu = Bukkit.getServer().createInventory(null, 36,
				ChatColor.DARK_AQUA + "Hoo-Kah");
		
		for(int i = 0; i < 36; i++) {
			switch (i) {
			case 10:
			case 11:
			case 12:
			case 13:
			case 16:
				break; //Empty slot
			case 7:
			case 15:
			case 17:
			case 25:
				hookahMenu.setItem(i, interfaceItems.get(4)); //Green Glass
				break;
			case 30:
				hookahMenu.setItem(i, interfaceItems.get(0)); //combine button
				break;
			case 31:
				hookahMenu.setItem(i, interfaceItems.get(1)); //Info paper
				break;
			case 32:
				hookahMenu.setItem(i, interfaceItems.get(2)); //Light button
				break;
			default:
				hookahMenu.setItem(i, interfaceItems.get(3)); //black glass
				break;
			}
		}
		
		return hookahMenu;
	}
	
	private static List<ItemStack> generateInterfaceItems() {
		List<ItemStack> interfaceItems = new ArrayList<>();
		
		ItemStack combineButton = new ItemStack(Material.ENDER_PEARL, 1);
		ItemMeta meta = combineButton.getItemMeta();
		meta.setDisplayName(ChatColor.DARK_GREEN + "Combine Ingredients");
		combineButton.setItemMeta(meta);
		
		ItemStack infoPaper = new ItemStack(Material.PAPER, 1);
		meta = infoPaper.getItemMeta();
		meta.setDisplayName((ChatColor.DARK_AQUA + "Current Drug: " + ChatColor.WHITE + "None"));
		meta.setLore(Collections.singletonList(ChatColor.DARK_AQUA + "Charges Left: " + ChatColor.WHITE + "0"));
		infoPaper.setItemMeta(meta);
		
		ItemStack light = new ItemStack(Material.BLAZE_POWDER, 1);
		meta = light.getItemMeta();
		meta.setDisplayName(ChatColor.RED + "Light Drug");
		light.setItemMeta(meta);

		ItemStack blackGlass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
		meta = blackGlass.getItemMeta();
		meta.setDisplayName(" ");
		blackGlass.setItemMeta(meta);
		
		ItemStack greenGlass = blackGlass.clone();
		greenGlass.setType(Material.GREEN_STAINED_GLASS_PANE);
		
		ItemStack redGlass = blackGlass.clone();
		redGlass.setType(Material.RED_STAINED_GLASS_PANE);
		
		interfaceItems.add(combineButton);
		interfaceItems.add(infoPaper);
		interfaceItems.add(light);
		interfaceItems.add(blackGlass);
		interfaceItems.add(greenGlass);
		interfaceItems.add(redGlass);
		
		return interfaceItems;
	}
}


