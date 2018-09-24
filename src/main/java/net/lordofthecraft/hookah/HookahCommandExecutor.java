package net.lordofthecraft.hookah;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import net.lordofthecraft.hookah.scenarios.Scenario;
import net.lordofthecraft.hookah.scenarios.Scenario.Scenarios;
import net.md_5.bungee.api.ChatColor;

public class HookahCommandExecutor implements CommandExecutor {

	private final HookahPlugin plugin;

	public HookahCommandExecutor(HookahPlugin plugin) {
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
				if (args[0].equalsIgnoreCase("drug")) 
					if (checkPermission(sender, "hookah.getdrug")) getDrug(sender, args);

				// /hookah getHookah
				if (args[0].equalsIgnoreCase("get")) 
					if (checkPermission(sender, "hookah.gethookah")) getHookah(sender);

				// /hookah getHookah
				if (args[0].equalsIgnoreCase("reload")) 
					if (checkPermission(sender, "hookah.reloadrecipes")) reloadRecipes(sender);
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
			player.sendMessage(ChatColor.GOLD + "[Hookah] " + ChatColor.WHITE + "Scenarios: ");
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
						sender.sendMessage(ChatColor.GOLD + "[Hookah] " + ChatColor.RED + "\"" + ChatColor.DARK_RED + args[1] 
								+ ChatColor.RED + "\"" + " is not a valid scenario name.");
					else
						plugin.getLogger().info("\"" + args[1] + "\" is not a valid scenario name");
				}
			}
		} else
			sendMessageToSender(sender, true, "Invalid amount of arguments! /hookah givescenario <scenario> <player>");
	}

	private void listRecipes(CommandSender sender) {
		for (Recipe recipe: Recipe.getRecipes()) {
			StringBuilder ingredients = new StringBuilder();
			for (ItemStack ingredient: recipe.getIngredients()) {
				ingredients.append(ingredient.getType().toString());
				ingredients.append(" ");
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
				StringBuilder drugName = new StringBuilder();
				if (args.length > 2) {
					for (int i = 1; i < args.length; i++) {
						if (i == args.length - 1)
							drugName.append(args[i]);
						else
							drugName.append(args[i]).append(" ");
					}
				} else
					drugName = new StringBuilder(args[1]);

				boolean found = false;
				for (Recipe recipe: Recipe.getRecipes()) {
					if (drugName.toString().equalsIgnoreCase(recipe.getName())) {
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
				sendMessageToSender(sender, true, "Invalid amount of arguments! /hookah drug <drugName>");
		} else
			plugin.getLogger().info("You must be in-game to run this command!");
	}

	private void getHookah (CommandSender sender) {
		if (sender instanceof Player) {
			((Player) sender).getInventory().addItem(Hookah.generateHookah());
			sendMessageToSender(sender, false, "A Hookah has successfully been added to your inventory!");
		} else
			plugin.getLogger().info("You must be in-game to run this command!");
	}

	private void reloadRecipes (CommandSender sender) {
		sendMessageToSender(sender, false, "Recipes reloaded successfully!");
		plugin.reloadRecipes();
	}

	private void listHookahCommands(CommandSender sender) {
		sender.sendMessage(ChatColor.GOLD + "[Hookah] " + ChatColor.WHITE + "Commands: ");
		sender.sendMessage(ChatColor.GOLD + "- scenarios: " + ChatColor.WHITE + "lists all possible scenarios");
		sender.sendMessage(ChatColor.GOLD + "- recipes: " + ChatColor.WHITE + "lists all drug recipes");
		sender.sendMessage(ChatColor.GOLD + "- drug <name>: " + ChatColor.WHITE + "gives you 1 drug of this name");
		sender.sendMessage(ChatColor.GOLD + "- get: " + ChatColor.WHITE + "gives you a Hoo-Kah");
		sender.sendMessage(ChatColor.GOLD + "- givescenario <scenario> <player>: " + ChatColor.WHITE + "plays a scenario for a player");
		sender.sendMessage(ChatColor.GOLD + "- reload: " + ChatColor.WHITE + "reloads the configuration file");
	}

	private void sendMessageToSender(CommandSender sender, boolean negative, String message) {
		ChatColor color = negative ? ChatColor.RED : ChatColor.GREEN;

		if (sender instanceof Player)
			sender.sendMessage(ChatColor.GOLD + "[Hookah] " + color + message);
		else
			plugin.getLogger().info(message);
	}

}
