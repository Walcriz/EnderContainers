package fr.utarwyn.endercontainers.dependencies;

import fr.utarwyn.endercontainers.commands.AbstractCommand;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class EssentialsDependency extends Dependency {

	EssentialsDependency() {
		super("Essentials");
	}

	@Override
	public void onEnable() {
		// Remove the essentials /enderchest command from the server!
		Plugin essentialsPlugin = Bukkit.getPluginManager().getPlugin(this.getName());
		if (essentialsPlugin == null) return; // Not a good behavior :(

		List<String> overriddenCmds = essentialsPlugin.getConfig().getStringList("overridden-commands");
		PluginCommand pluginCommand = Bukkit.getPluginCommand("essentials:enderchest");

		// Server administrators can keep up the Essentials command by adding it to the list of overridden commands.
		if (pluginCommand != null && (overriddenCmds == null || !overriddenCmds.contains("enderchest"))) {
			AbstractCommand.unregister(pluginCommand);
		}
	}

	@Override
	public void onDisable() {

	}

	@Override
	public boolean onBlockChestOpened(Block block, Player player, boolean sendMessage) {
		return true;
	}

}