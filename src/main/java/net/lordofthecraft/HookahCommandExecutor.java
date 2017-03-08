package net.lordofthecraft;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.lordofthecraft.Scenarios.Scenario;
import net.lordofthecraft.Scenarios.Scenario.Scenarios;
import net.md_5.bungee.api.ChatColor;

public class HookahCommandExecutor implements CommandExecutor {

	private final HookahMain plugin;
	
	public HookahCommandExecutor(HookahMain plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("hookah") && checkPermission(sender, "hookah.listcommands")) {
			if (args.length > 0) {
				// /hookah scenarios
				if (args[0].equalsIgnoreCase("scenarios"))
					if (checkPermission(sender, "hookah.listscenarios")) listScenarios(sender);
				
				// /hookah givescenario <scenarioName> <playerName>
				if (args[0].equalsIgnoreCase("givescenario")) 
					if (checkPermission(sender, "hookah.givescenario")) giveScenario(sender, args);
				
				// /hookah recipes
				if (args[0].equalsIgnoreCase("recipes")) 
					if (checkPermission(sender, "hookah.listrecipes")) listRecipes(sender);
				
				// /hookah getDrug <drugName>
				if (args[0].equalsIgnoreCase("getDrug")) 
					if (checkPermission(sender, "hookah.getdrug")) getDrug(sender, args);
				
				// /hookah getHookah
				if (args[0].equalsIgnoreCase("getHookah")) 
					if (checkPermission(sender, "hookah.gethookah")) getHookah(sender);
			}
			else listHookahCommands(sender); // /hookah
		}
		return true;
	}
	
	private boolean checkPermission(CommandSender sender, String permission) {
		if (sender.hasPermission(permission))
			return true;
		else
			sendMessageToSender(sender, true, "You do not have the required permissions for this");
		return false;
	}
	
	private void listScenarios(CommandSender sender) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			player.sendMessage(ChatColor.GOLD + "[HooKah] " + ChatColor.WHITE + "Scenarios: ");
			for (Scenarios scenario: Scenarios.values()) {
				player.sendMessage("- " + scenario.toString());
			}
		} else {
			plugin.getLogger().info("Scenarios: ");
			for (Scenarios scenario: Scenarios.values()) {
				plugin.getLogger().info("- " + scenario.toString());
			}
		}
	}
	
	private void giveScenario(CommandSender sender, String args[]) {
		if (args.length >= 3) {
			Player receiver = Bukkit.getPlayer(args[2]);
			if (receiver == null)
				sendMessageToSender(sender, true, "Player not found");
			else {
				try {
					if (new Scenario(receiver, Scenarios.valueOf(args[1].toUpperCase())).play())
						sendMessageToSender(sender, false, "Scenario successfully played to " + receiver.getName());
					else
						sendMessageToSender(sender, true, receiver.getName() + " is already experiencing a scenario at the moment.");
				} catch (IllegalArgumentException e) {
					if (sender instanceof Player)
						sender.sendMessage(ChatColor.GOLD + "[HooKah] " + ChatColor.RED + "\"" + ChatColor.DARK_RED + args[1] 
								+ ChatColor.RED + "\"" + " is not a valid scenario name.");
					else
						plugin.getLogger().info("\"" + args[1] + "\" is not a valid scenario name");
				}
			}
		} else
			sendMessageToSender(sender, true, "Invalid amount of arguments! /hookah giveScenario <scenarioName> <player>");
	}
	
	private void listRecipes(CommandSender sender) {
		for (Recipe recipe: Recipe.getRecipes()) {
			String ingredients = "";
			for (ItemStack ingredient: recipe.getIngredients()) {
				ingredients += ingredient.getType().toString();
				if (ingredient.getDurability() != 0)
					ingredients += ":" + ingredient.getDurability();
				ingredients += " ";
			}
			
			if (sender instanceof Player) {
				sender.sendMessage(ChatColor.GOLD + "Drug: " + ChatColor.WHITE + recipe.getName());
				sender.sendMessage(ChatColor.GOLD + "     Ingredients: " + ChatColor.WHITE + ingredients);
				
			} else {
				plugin.getLogger().info("Drug: " + recipe.getName());
				plugin.getLogger().info("     Ingredients: " + ingredients);
			}	
		}
	}
	
	private void getDrug(CommandSender sender, String[] args) {
		if (sender instanceof Player) {
			if (args.length >= 2) {
				String drugName = "";
				if (args.length > 2) {
					for (int i = 1; i < args.length; i++) {
						if (i == args.length - 1)
							drugName += args[i];
						else
							drugName += args[i] + " ";
					}
				} else
					drugName = args[1];
				
				boolean found = false;
				for (Recipe recipe: Recipe.getRecipes()) {
					if (drugName.equalsIgnoreCase(recipe.getName())) {
						found = true;
						((Player) sender).getInventory().addItem(recipe.getDrugItem());
						break;
					}
				}
				if (found)
					sendMessageToSender(sender, false, "The drug has been added to your inventory!");
				else
					sendMessageToSender(sender, true, "Specified drug could not be found!");
			} else
				sendMessageToSender(sender, true, "Invalid amount of arguments! /hookah giveDrug <drugName>");
		} else
			plugin.getLogger().info("You must be in-game to run this command!");
	}
	
	private void getHookah (CommandSender sender) {
		if (sender instanceof Player) {
			((Player) sender).getInventory().addItem(Hookah.generateHookah());
			sendMessageToSender(sender, false, "A Hoo-Kah has successfully been added to your inventory!");
		} else
			plugin.getLogger().info("You must be in-game to run this command!");
	}
	
	private void listHookahCommands(CommandSender sender) {
		if (sender instanceof Player) {
			sender.sendMessage(ChatColor.GOLD + "[HooKah] " + ChatColor.WHITE + "Commands: ");
			sender.sendMessage(ChatColor.GOLD + "- scenarios: " + ChatColor.WHITE + "lists all possible scenarios");
			sender.sendMessage(ChatColor.GOLD + "- giveScenario <name> <player>: " + ChatColor.WHITE + "applies a scenario to a player");
			sender.sendMessage(ChatColor.GOLD + "- recipes: " + ChatColor.WHITE + "lists all drug recipes");
			sender.sendMessage(ChatColor.GOLD + "- getDrug <name>: " + ChatColor.WHITE + "gives you 1 drug of this name");
			sender.sendMessage(ChatColor.GOLD + "- getHookah: " + ChatColor.WHITE + "gives you a Hoo-Kah");
		} else {
			plugin.getLogger().info("Commands: ");
			plugin.getLogger().info("- scenarios: lists all possible scenarios");
			plugin.getLogger().info("- giveScenario <name> <player>: applies a scenario to a player");
			plugin.getLogger().info("- recipes: lists all drug recipes");
			plugin.getLogger().info("- getDrug <name>: gives you 1 drug of this name");
			plugin.getLogger().info("- getHookah: gives you a Hoo-Kah");
		}
	}
	
	private void sendMessageToSender(CommandSender sender, boolean negative, String message) {
		ChatColor color = negative ? ChatColor.RED : ChatColor.GREEN;
		
		if (sender instanceof Player)
			sender.sendMessage(ChatColor.GOLD + "[HooKah] " + color + message);
		else
			plugin.getLogger().info(message);
	}

}
