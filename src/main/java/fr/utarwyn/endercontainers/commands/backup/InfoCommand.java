package fr.utarwyn.endercontainers.commands.backup;

import fr.utarwyn.endercontainers.Config;
import fr.utarwyn.endercontainers.backup.Backup;
import fr.utarwyn.endercontainers.backup.BackupManager;
import fr.utarwyn.endercontainers.commands.parameter.Parameter;
import fr.utarwyn.endercontainers.util.Locale;
import fr.utarwyn.endercontainers.util.PluginMsg;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InfoCommand extends AbstractBackupCommand {

	public InfoCommand(BackupManager manager) {
		super("info", manager);

		this.setPermission(Config.PERM_PREFIX + "backup.info");
		this.addParameter(Parameter.STRING);
	}

	@Override
	public void perform(CommandSender sender) {
		String name = this.readArg();
		Backup backup = this.manager.getBackupByName(name);

		if (backup == null) {
			this.sendTo(sender, ChatColor.RED + Locale.backupUnknown.replace("%backup%", name));
			return;
		}

		PluginMsg.pluginBar(sender);
		sender.sendMessage(" ");
		sender.sendMessage(" §7  " + Locale.backupLabelName + ": §r" + backup.getName() + " §7(" + backup.getType() + ")");
		sender.sendMessage(" §7  " + Locale.backupLabelDate + ": §r" + backup.getDate());
		sender.sendMessage(" §7  " + Locale.backupLabelBy + ": §e" + backup.getCreatedBy());
		sender.sendMessage(" ");
		sender.sendMessage(" §8  " + Locale.backupLabelLoadCmd + ": §d/ecp backup load " + name);
		sender.sendMessage(" §8  " + Locale.backupLabelRmCmd + ": §c/ecp backup remove " + name);
		sender.sendMessage(" ");
		PluginMsg.endBar(sender);
	}

	@Override
	public void performPlayer(Player player) {

	}

	@Override
	public void performConsole(CommandSender sender) {

	}

}