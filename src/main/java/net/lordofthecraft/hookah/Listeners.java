package net.lordofthecraft.hookah;


import io.github.archemedes.customitem.CustomTag;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.lordofthecraft.hookah.scenarios.Scenario;
import net.lordofthecraft.omniscience.api.data.LocationTransaction;
import net.lordofthecraft.omniscience.api.entry.OEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class Listeners implements Listener{
	
	private List<UUID> cooldowns = new ArrayList<>();
	private boolean loggingEnabled;
	
	public Listeners (HookahPlugin plugin) {
		loggingEnabled = (plugin.getServer().getPluginManager().getPlugin("Omniscience") != null);
	}

	@EventHandler(ignoreCancelled = false, priority = EventPriority.LOW)
	public void onOpenInventory(InventoryOpenEvent e) {
		InventoryHolder ih = e.getInventory().getHolder();
		if (ih instanceof BrewingStand) {
			BrewingStand st = (BrewingStand) ih;
			WeakLocation loc = new WeakLocation(st.getBlock().getLocation());
			if (Hookah.getLocations().contains(loc)) {
				e.setCancelled(true);
				if (e.getPlayer().getGameMode() == GameMode.CREATIVE && !e.getPlayer().hasPermission("lc.admin") && !e.getPlayer().hasPermission("lc.tech")) {
					return;
				}
				Hookah currentHookah = Hookah.getHookah(loc);
				Player p = (Player) e.getPlayer();
				if (!p.isSneaking()) //Open hookah inventory
					p.openInventory(currentHookah.getInventory());
				else { //Take a hit
					if (currentHookah.getCharges() != 0 && !cooldowns.contains(e.getPlayer().getUniqueId())) {
						currentHookah.useCharge(p);
						startCooldown(p);
					}
				}
			}
		}
	}

	@EventHandler //Keeps track of hookah locations
	public void onBlockPlace(BlockPlaceEvent e) {
        if (!isHookah(e.getItemInHand())) return;
		
		Hookah.addHookah(new WeakLocation(e.getBlock().getLocation()), new Hookah());
	}

    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST) //Keeps track of hookah locations
    public void onBlockBreak(BlockBreakEvent e) {
        if (!Hookah.getLocations().contains(new WeakLocation(e.getBlock().getLocation()))) return;
        e.setCancelled(true);
        Block block = e.getBlock();

		if (loggingEnabled) {
			BlockState air = e.getBlock().getState();
			air.setType(Material.AIR);
			OEntry.create().source(e.getPlayer()).brokeBlock(new LocationTransaction<>(e.getBlock().getLocation(), e.getBlock().getState(), air)).save();
		}

        block.setType(Material.AIR);
        
        if (e.getPlayer().getGameMode() != GameMode.CREATIVE)
            block.getWorld().dropItem(block.getLocation(), Hookah.generateHookah());
        
        Inventory inventory = Hookah.getHookah(new WeakLocation(block.getLocation())).getInventory();
        
        //Close all open inventories
        for (HumanEntity viewer: inventory.getViewers().toArray(new HumanEntity[0])) {
            viewer.closeInventory();
        }
        
        //10, 11, 12, 13, 16 (Drop any items inside the hookah)
        for (int slot = 10; slot <= 16; slot++) {
            if (slot == 14 || slot == 15) continue;
            if (inventory.getItem(slot) == null) continue;
            block.getWorld().dropItemNaturally(block.getLocation(), inventory.getItem(slot));
        }
        
        Hookah.removeHookah(new WeakLocation(block.getLocation()));
        
    }
	
	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		//GREAT WALL OF GYNA
		if (!(e.getInventory().getSize() > 31)) return;
		if (e.getInventory().getItem(31) == null) return;
        if (!CustomTag.hasCustomTag(e.getInventory().getItem(31), "hookahloc")) return;
		if (e.getCurrentItem() == null) return;
		if (e.getCurrentItem().getItemMeta() == null) return;
		if (e.getRawSlot() >= e.getInventory().getSize()) return;

		//prevent info paper from being picked up
        if (CustomTag.hasCustomTag(e.getCurrentItem(), "hookahloc")) {
            e.setCancelled(true);
			return;
		}
        CustomTag tag = CustomTag.getFrom(e.getInventory().getItem(31));

		//prevent any other interface items from being picked up
		for (ItemStack item: Hookah.getInterfaceItems()) {
            if (item.getItemMeta().equals(e.getCurrentItem().getItemMeta())) {
                e.setCancelled(true);
            }

        }

		//Get the current Hookah from the NBT inside the info paper
		String stockedLoc[] = // "world;x;y;z"
                tag.get("hookahloc").split(";");
		Hookah currentHookah = Hookah.getHookah(new WeakLocation(stockedLoc[0],
				Integer.parseInt(stockedLoc[1]),
				Integer.parseInt(stockedLoc[2]),
				Integer.parseInt(stockedLoc[3])));

        //combine ingredients
		if (e.getCurrentItem().getItemMeta().equals(Hookah.getInterfaceItems().get(0).getItemMeta())) {
            if (currentHookah.combineIngredients((Player) e.getWhoClicked(), (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY))) {
                ((Player) e.getWhoClicked()).updateInventory();
            } else {
				currentHookah.playWrongRecipe();
                ((Player) e.getWhoClicked()).playSound(e.getWhoClicked().getLocation(),
						Sound.BLOCK_REDSTONE_TORCH_BURNOUT, SoundCategory.VOICE, 1f, 1f);
			}
		}

        //light drug
		if (e.getCurrentItem().getItemMeta().equals(Hookah.getInterfaceItems().get(2).getItemMeta())) {
			ItemStack item = e.getInventory().getItem(16);
			if (item == null) return;
            for (Recipe recipe : Recipe.getRecipes()) {
				if (item.isSimilar(recipe.getDrugItem())) {
					currentHookah.lightDrug(recipe);
					return;
				}
			}
		}
	}
	
	// 1 second cooldown between hits from the Hoo-Kah
	private void startCooldown(final Player player) {
		cooldowns.add(player.getUniqueId());
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(HookahPlugin.plugin, () -> cooldowns.remove(player.getUniqueId()), 20);
	}
	
	@EventHandler //Remove high when player drinks milk
	public void onMilkConsume(PlayerItemConsumeEvent e) {
		if (e.getItem().getType() != Material.MILK_BUCKET) return;
		if (DefaultHigh.getActiveHighs().containsKey(e.getPlayer().getUniqueId())) {
			DefaultHigh.remove(e.getPlayer());
			e.getPlayer().sendMessage(ChatColor.AQUA + "Your vision clears and you suddenly feel a lot better.");
		}
	}
	
	@EventHandler //Cancel scenario and high tasks when player logs out
	public void onPlayerQuit(PlayerQuitEvent e) {
		//scenario
		if (Scenario.getActiveScenarios().containsKey(e.getPlayer().getUniqueId())) {
			Scenario.getActiveScenarios().get(e.getPlayer().getUniqueId()).remove();
			Scenario.getActiveScenarios().remove(e.getPlayer().getUniqueId());
		}
		
		//high
		if (DefaultHigh.getActiveHighs().containsKey(e.getPlayer().getUniqueId()))
			DefaultHigh.remove(e.getPlayer());
	}

    static boolean isHookah(ItemStack is) {
        return is.getType() == Material.BREWING_STAND && CustomTag.hasCustomTag(is, "hookah");
    }
}
