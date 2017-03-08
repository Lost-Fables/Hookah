package net.lordofthecraft;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import io.github.archemedes.customitem.Customizer;
import net.lordofthecraft.Scenarios.Scenario;

public class Listeners implements Listener{
	
	private List<UUID> cooldowns = new ArrayList<>();
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (!(e.getAction() == Action.RIGHT_CLICK_BLOCK)) return;
		if (!(e.getClickedBlock().getType() == Material.BREWING_STAND)) return;
		if (!(Hookah.getLocations().contains(e.getClickedBlock().getLocation()))) return;
		
		e.setCancelled(true);
		
		if (!e.getPlayer().isSneaking()) //Open hookah inventory
			e.getPlayer().openInventory(Hookah.getHookah(e.getClickedBlock().getLocation()).getInventory());
		else { //Take a hit
			Hookah currentHookah = Hookah.getHookah(e.getClickedBlock().getLocation());
			if (currentHookah.getCharges() != 0 && !cooldowns.contains(e.getPlayer().getUniqueId())) {
				currentHookah.useCharge(e.getPlayer());
				startCooldown(e.getPlayer());
			}
		}
	}
	
	@EventHandler //Keeps track of hookah locations
	public void onBlockPlace(BlockPlaceEvent e) {
		if (e.getItemInHand().getType() != Material.BREWING_STAND_ITEM) return;
		if (!(e.getItemInHand().getItemMeta().hasDisplayName())) return;
		if (!(e.getItemInHand().getItemMeta().getDisplayName().equals("Hoo-Kah"))) return;
		if (!(Customizer.hasCompound((e.getItemInHand())))) return;
		if (!(Customizer.getCompound(e.getItemInHand()).hasKey("isHookah"))); //NBT to make sure its a hookah
		
		Hookah.addHookah(e.getBlock().getLocation(), new Hookah());
	}
	
	@EventHandler //Keeps track of hookah locations
	public void onBlockBreak(BlockBreakEvent e) {
		if (!Hookah.getLocations().contains(e.getBlock().getLocation())) return;
		e.setCancelled(true);
		Block block = e.getBlock();
		block.setType(Material.AIR);
		
		if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
			block.getWorld().dropItem(block.getLocation(), Hookah.generateHookah());
		
		Inventory inventory = Hookah.getHookah(block.getLocation()).getInventory();
		//10, 11, 12, 13, 16 (Drop any items inside the hookah)
		for (int slot = 10; slot <= 16; slot++) {
			if (slot == 14 || slot == 15) continue;
			if (inventory.getItem(slot) == null) continue;
			block.getWorld().dropItemNaturally(block.getLocation(), inventory.getItem(slot));
		}
		
		Hookah.removeHookah(block.getLocation());
	}
	
	@EventHandler 
	public void onInventoryClick(InventoryClickEvent e) {
		//GREAT WALL OF GYNA
		if (!(e.getInventory().getSize() > 31)) return;
		if (e.getInventory().getItem(31) == null) return;
		if (Customizer.hasCompound(e.getInventory().getItem(31)))
			if (!Customizer.getCompound(e.getInventory().getItem(31)).hasKey("location")) return;
		if (e.getCurrentItem() == null) return;
		if (e.getCurrentItem().getItemMeta() == null) return;
		if (e.getRawSlot() >= e.getInventory().getSize()) return;
		
		//prevent info paper from being picked up
		if (Customizer.hasCompound(e.getCurrentItem())) {
			if (Customizer.getCompound(e.getCurrentItem()).hasKey("location")) e.setCancelled(true);
			return;
		}
		//prevent any other interface items from being picked up
		for (ItemStack item: Hookah.getInterfaceItems()) {
			if (item.getItemMeta().equals(e.getCurrentItem().getItemMeta()))
				e.setCancelled(true);
		}
		
		//Get the current Hookah from the NBT inside the info paper
		String stockedLoc[] = // "world;x;y;z"
				Customizer.getCompound(e.getInventory().getItem(31)).getValue("location").getValue().split(";");
		Hookah currentHookah = Hookah.getHookah(new Location(Bukkit.getWorld(stockedLoc[0]), 
				Double.parseDouble(stockedLoc[1]), 
				Double.parseDouble(stockedLoc[2]), 
				Double.parseDouble(stockedLoc[3])));
		
		//combine ingredients
		if (e.getCurrentItem().getItemMeta().equals(Hookah.getInterfaceItems().get(0).getItemMeta())) {
			if (currentHookah.combineIngredients((Player) e.getWhoClicked(), (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY)))
				((Player) e.getWhoClicked()).updateInventory();
			else {
				currentHookah.playWrongRecipe();
				((Player) e.getWhoClicked()).playSound(((Player) e.getWhoClicked()).getLocation(), 
						Sound.BLOCK_REDSTONE_TORCH_BURNOUT, SoundCategory.VOICE, 1f, 1f);
			}
		}
		
		//light drug
		if (e.getCurrentItem().getItemMeta().equals(Hookah.getInterfaceItems().get(2).getItemMeta())) {
			if (e.getInventory().getItem(16) == null) return;
			for (Recipe recipe: Recipe.getRecipes()) {
				if (e.getInventory().getItem(16).getItemMeta().equals(recipe.getDrugItem().getItemMeta()) &&
						Customizer.getCompound(e.getInventory().getItem(16)).hasKey("isDrug")) {
					currentHookah.lightDrug(recipe);
					return;
				}
			}
		}
	}
	
	// 1 second cooldown between hits from the Hoo-Kah
	private void startCooldown(final Player player) {
		cooldowns.add(player.getUniqueId());
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahMain.plugin, new Runnable() {
			public void run() {
				cooldowns.remove(player.getUniqueId());
			}
		}, 20);
	}
	
	@EventHandler //Remove high when player drinks milk
	public void onMilkConsume(PlayerItemConsumeEvent e) {
		if (e.getItem().getType() != Material.MILK_BUCKET) return;
		if (DefaultHigh.getActiveHighs().containsKey(e.getPlayer().getUniqueId())) {
			Bukkit.getServer().getScheduler().cancelTask(DefaultHigh.getActiveHighs().get(
					e.getPlayer().getUniqueId()).durationTask);
			Bukkit.getServer().getScheduler().cancelTask(DefaultHigh.getActiveHighs().get(
					e.getPlayer().getUniqueId()).nauseaTask);
			DefaultHigh.getActiveHighs().remove(e.getPlayer().getUniqueId());
			PacketHandler.toggleRedTint(e.getPlayer(), false);
			e.getPlayer().sendMessage(ChatColor.AQUA + "Your vision clears and you suddenly feel a lot better.");
		}
	}
	
	@EventHandler //Cancel scenario when player logs out
	public void onPlayerQuit(PlayerQuitEvent e) {
		if (Scenario.getActiveScenarios().contains(e.getPlayer().getUniqueId()))
			Scenario.getActiveScenarios().remove(e.getPlayer().getUniqueId());
	}
}
