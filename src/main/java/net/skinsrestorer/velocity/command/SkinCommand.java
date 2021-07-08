/*
 * #%L
 * SkinsRestorer
 * %%
 * Copyright (C) 2021 SkinsRestorer
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package net.skinsrestorer.velocity.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.InvalidCommandArgument;
import co.aikar.commands.annotation.*;
import co.aikar.commands.velocity.contexts.OnlinePlayer;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import lombok.RequiredArgsConstructor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.CooldownStorage;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.log.SRLogger;
import net.skinsrestorer.velocity.SkinsRestorer;

import java.util.concurrent.TimeUnit;

// TODO: update deprecated CommandSource#sendMessage() for velocity
@RequiredArgsConstructor
@SuppressWarnings("deprecation")
@CommandAlias("skin")
@CommandPermission("%skin")
public class SkinCommand extends BaseCommand {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    @Default
    @SuppressWarnings({"deprecation"})
    public void onDefault(CommandSource source) {
        onHelp(source, getCurrentCommandManager().generateCommandHelp());
    }

    @Default
    @CommandPermission("%skinSet")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxDefaultCommand")
    @SuppressWarnings({"unused"})
    public void onSkinSetShort(Player player, @Single String skin) {
        onSkinSetOther(player, new OnlinePlayer(player), skin, null);
    }

    @HelpCommand
    @Syntax(" [help]")
    public void onHelp(CommandSource commandSource, CommandHelp help) {
        if (Config.ENABLE_CUSTOM_HELP)
            sendHelp(commandSource);
        else
            help.showHelp();
    }


    @Subcommand("clear")
    @CommandPermission("%skinClear")
    @Description("%helpSkinClear")
    @SuppressWarnings({"unused"})
    public void onSkinClear(Player player) {
        onSkinClearOther(player, new OnlinePlayer(player));
    }

    @Subcommand("clear")
    @CommandPermission("%skinClearOther")
    @CommandCompletion("@players")
    @Syntax("%SyntaxSkinClearOther")
    @Description("%helpSkinClearOther")
    public void onSkinClearOther(CommandSource source, @Single OnlinePlayer target) {
        plugin.getService().execute(() -> {
            if (!source.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(getSenderName(source))) {
                source.sendMessage(plugin.deserialize(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(getSenderName(source)))));
                return;
            }

            final Player player = target.getPlayer();
            final String pName = player.getUsername();
            final String skin = plugin.getSkinStorage().getDefaultSkinName(pName, true);

            // remove users defined skin from database
            plugin.getSkinStorage().removeSkin(pName);

            if (setSkin(source, player, skin, false, true, null)) {
                if (source == player)
                    source.sendMessage(plugin.deserialize(Locale.SKIN_CLEAR_SUCCESS));
                else
                    source.sendMessage(plugin.deserialize(Locale.SKIN_CLEAR_ISSUER.replace("%player", pName)));
            }
        });
    }


    @Subcommand("update")
    @CommandPermission("%skinUpdate")
    @Description("%helpSkinUpdate")
    @SuppressWarnings({"unused"})
    public void onSkinUpdate(Player player) {
        onSkinUpdateOther(player, new OnlinePlayer(player));
    }

    @Subcommand("update")
    @CommandPermission("%skinUpdateOther")
    @CommandCompletion("@players")
    @Description("%helpSkinUpdateOther")
    @Syntax("%SyntaxSkinUpdateOther")
    public void onSkinUpdateOther(CommandSource source, @Single OnlinePlayer target) {
        plugin.getService().execute(() -> {
            if (!source.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(getSenderName(source))) {
                source.sendMessage(plugin.deserialize(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(getSenderName(source)))));
                return;
            }

            final Player player = target.getPlayer();
            String skin = plugin.getSkinStorage().getSkinName(player.getUsername());

            try {
                if (skin != null) {
                    //filter skinUrl
                    if (skin.startsWith(" ")) {
                        source.sendMessage(plugin.deserialize(Locale.ERROR_UPDATING_URL));
                        return;
                    }

                    if (!plugin.getSkinStorage().updateSkinData(skin)) {
                        source.sendMessage(plugin.deserialize(Locale.ERROR_UPDATING_SKIN));
                        return;
                    }

                } else {
                    // get DefaultSkin
                    skin = plugin.getSkinStorage().getDefaultSkinName(player.getUsername(), true);
                }
            } catch (SkinRequestException e) {
                source.sendMessage(plugin.deserialize(e.getMessage()));
                return;
            }

            if (setSkin(source, player, skin, false, false, null)) {
                if (source == player)
                    source.sendMessage(plugin.deserialize(Locale.SUCCESS_UPDATING_SKIN));
                else
                    source.sendMessage(plugin.deserialize(Locale.SUCCESS_UPDATING_SKIN_OTHER.replace("%player", player.getUsername())));
            }
        });
    }

    @Subcommand("set")
    @CommandPermission("%skinSet")
    @CommandCompletion("@skin")
    @Description("%helpSkinSet")
    @Syntax("%SyntaxSkinSet")
    public void onSkinSet(Player player, String[] skin) {
        if (skin.length == 0)
            throw new InvalidCommandArgument(true);

        onSkinSetOther(player, new OnlinePlayer(player), skin[0], null);
    }

    @Subcommand("set")
    @CommandPermission("%skinSetOther")
    @CommandCompletion("@players @skin")
    @Description("%helpSkinSetOther")
    @Syntax("%SyntaxSkinSetOther")
    public void onSkinSetOther(CommandSource source, OnlinePlayer target, String skin, @Optional SkinType skinType) {
        plugin.getService().execute(() -> {
            final Player player = target.getPlayer();
            if (Config.PER_SKIN_PERMISSIONS && !source.hasPermission("skinsrestorer.skin." + skin)) {
                if (!source.hasPermission("skinsrestorer.ownskin") && !getSenderName(source).equalsIgnoreCase(player.getUsername()) || !skin.equalsIgnoreCase(getSenderName(source))) {
                    source.sendMessage(LegacyComponentSerializer.legacySection().deserialize(Locale.PLAYER_HAS_NO_PERMISSION_SKIN));
                    return;
                }
            }

            if (setSkin(source, player, skin, true, false, skinType) && (source != player)) {
                source.sendMessage(LegacyComponentSerializer.legacySection().deserialize(Locale.ADMIN_SET_SKIN.replace("%player", player.getUsername())));
            }
        });
    }

    @Subcommand("url")
    @CommandPermission("%skinSetUrl")
    @CommandCompletion("@skinUrl")
    @Description("%helpSkinSetUrl")
    @Syntax("%SyntaxSkinUrl")
    @SuppressWarnings({"unused"})
    public void onSkinSetUrl(Player player, String url, @Optional SkinType skinType) {
        if (!C.validUrl(url)) {
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(Locale.ERROR_INVALID_URLSKIN));
            return;
        }

        onSkinSetOther(player, new OnlinePlayer(player), url, skinType);
    }

    private boolean setSkin(CommandSource source, Player player, String skin) {
        return setSkin(source, player, skin, true, false, null);
    }

    // if save is false, we won't save the skin skin name
    // because default skin names shouldn't be saved as the users custom skin
    private boolean setSkin(CommandSource source, Player player, String skin, boolean save, boolean clear, SkinType skinType) {
        if (skin.equalsIgnoreCase("null")) {
            source.sendMessage(plugin.deserialize(Locale.INVALID_PLAYER.replace("%player", skin)));
            return false;
        }

        if (Config.DISABLED_SKINS_ENABLED && !clear && !source.hasPermission("skinsrestorer.bypassdisabled")) {
            for (String dskin : Config.DISABLED_SKINS)
                if (skin.equalsIgnoreCase(dskin)) {
                    source.sendMessage(plugin.deserialize(Locale.SKIN_DISABLED));
                    return false;
                }
        }

        final String senderName = getSenderName(source);
        if (!source.hasPermission("skinsrestorer.bypasscooldown") && CooldownStorage.hasCooldown(senderName)) {
            source.sendMessage(plugin.deserialize(Locale.SKIN_COOLDOWN.replace("%s", "" + CooldownStorage.getCooldown(senderName))));
            return false;
        }

        CooldownStorage.setCooldown(senderName, Config.SKIN_CHANGE_COOLDOWN, TimeUnit.SECONDS);

        final String pName = player.getUsername();
        final String oldSkinName = plugin.getSkinStorage().getSkinName(pName);
        if (C.validUrl(skin)) {
            if (!source.hasPermission("skinsrestorer.command.set.url")
                    && !Config.SKINWITHOUTPERM
                    && !clear) {//ignore /skin clear when defaultSkin = url
                source.sendMessage(plugin.deserialize(Locale.PLAYER_HAS_NO_PERMISSION_URL));
                CooldownStorage.resetCooldown(senderName);
                return false;
            }

            if (!C.allowedSkinUrl(skin)) {
                source.sendMessage(plugin.deserialize(Locale.SKINURL_DISALLOWED));
                CooldownStorage.resetCooldown(senderName);
                return false;
            }

            try {
                source.sendMessage(plugin.deserialize(Locale.MS_UPDATING_SKIN));
                String skinentry = " " + pName; // so won't overwrite premium playernames
                if (skinentry.length() > 16) // max len of 16 char
                    skinentry = skinentry.substring(0, 16);
                plugin.getSkinStorage().setSkinData(skinentry, plugin.getMineSkinAPI().genSkin(skin, String.valueOf(skinType), null),
                        Long.toString(System.currentTimeMillis() + (100L * 365 * 24 * 60 * 60 * 1000))); // "generate" and save skin for 100 years
                plugin.getSkinStorage().setSkinName(pName, skinentry); // set player to "whitespaced" name then reload skin
                plugin.getSkinsRestorerAPI().applySkin(new PlayerWrapper(player));
                if (!Locale.SKIN_CHANGE_SUCCESS.isEmpty() && !Locale.SKIN_CHANGE_SUCCESS.equals(Locale.PREFIX))
                    player.sendMessage(plugin.deserialize(Locale.SKIN_CHANGE_SUCCESS.replace("%skin", "skinUrl")));
                return true;
            } catch (SkinRequestException e) {
                source.sendMessage(plugin.deserialize(e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                source.sendMessage(plugin.deserialize(Locale.INVALID_PLAYER.replace("%player", skin)));
            }
            // set CoolDown to ERROR_COOLDOWN and rollback to old skin on exception
        } else {
            try {
                plugin.getSkinStorage().getSkinForPlayer(skin, false);
                if (save)
                    plugin.getSkinStorage().setSkinName(pName, skin);
                plugin.getSkinsRestorerAPI().applySkin(new PlayerWrapper(player));
                if (!Locale.SKIN_CHANGE_SUCCESS.isEmpty() && !Locale.SKIN_CHANGE_SUCCESS.equals(Locale.PREFIX))
                    player.sendMessage(plugin.deserialize(Locale.SKIN_CHANGE_SUCCESS.replace("%skin", skin)));
                return true;
            } catch (SkinRequestException e) {
                source.sendMessage(plugin.deserialize(e.getMessage()));
                // set custom skin name back to old one if there is an exception
            } catch (Exception e) {
                e.printStackTrace();
                source.sendMessage(plugin.deserialize(Locale.INVALID_PLAYER.replace("%player", skin)));
            }
        }
        // set CoolDown to ERROR_COOLDOWN and rollback to old skin on exception
        CooldownStorage.setCooldown(senderName, Config.SKIN_ERROR_COOLDOWN, TimeUnit.SECONDS);
        rollback(player, oldSkinName, save);
        return false;
    }

    private void rollback(Player player, String oldSkinName, boolean save) {
        if (save)
            plugin.getSkinStorage().setSkinName(player.getUsername(), oldSkinName != null ? oldSkinName : player.getUsername());
    }

    private void sendHelp(CommandSource commandSource) {
        if (!Locale.SR_LINE.isEmpty())
            commandSource.sendMessage(plugin.deserialize(Locale.SR_LINE));
        commandSource.sendMessage(plugin.deserialize(Locale.CUSTOM_HELP_IF_ENABLED.replace("%ver%", plugin.getVersion())));
        if (!Locale.SR_LINE.isEmpty())
            commandSource.sendMessage(plugin.deserialize(Locale.SR_LINE));
    }

    private String getSenderName(CommandSource source) {
        return source instanceof Player ? ((Player) source).getUsername() : "CONSOLE";
    }

    @SuppressWarnings("unused")
    public enum SkinType {
        STEVE,
        SLIM,
    }
}
