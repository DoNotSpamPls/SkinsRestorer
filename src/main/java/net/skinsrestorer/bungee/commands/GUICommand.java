package net.skinsrestorer.bungee.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.HelpCommand;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.skinsrestorer.bungee.SkinsRestorer;
import net.skinsrestorer.shared.storage.Locale;

/**
 * Created by McLive on 21.12.2019.
 */
@CommandAlias("skins")
@CommandPermission("%skins")
public class GUICommand extends BaseCommand {
    private final SkinsRestorer plugin;

    public GUICommand(SkinsRestorer plugin) {
        this.plugin = plugin;
    }

    @HelpCommand
    public static void onHelp(CommandSender sender, CommandHelp help) {
        sender.sendMessage(TextComponent.fromLegacyText("SkinsRestorer Help"));
        help.showHelp();
    }

    @Default
    @CommandPermission("%skins")
    public void onDefault(ProxiedPlayer p) {
        p.sendMessage(TextComponent.fromLegacyText(Locale.SKINSMENU_OPEN));

        plugin.getPluginMessageListener().sendGuiOpenRequest(p);
    }
}