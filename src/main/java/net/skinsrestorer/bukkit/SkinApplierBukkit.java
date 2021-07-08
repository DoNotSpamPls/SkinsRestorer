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
package net.skinsrestorer.bukkit;

import io.papermc.lib.PaperLib;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.bukkit.events.SkinApplyBukkitEvent;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.bukkit.skinapplier.PaperSkinRefresher;
import net.skinsrestorer.bukkit.skinapplier.SpigotSkinRefresher;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class SkinApplierBukkit {
    private final SkinsRestorer plugin;
    private final SRLogger log;
    private final Consumer<Player> refresh;
    private boolean checkOptFileChecked = false;
    private boolean disableDismountPlayer;
    private boolean enableDismountEntities;
    private boolean enableRemountPlayer;

    public SkinApplierBukkit(SkinsRestorer plugin, SRLogger log) {
        this.plugin = plugin;
        this.log = log;
        refresh = detectRefresh();
    }

    private Consumer<Player> detectRefresh() {
        if (PaperLib.isPaper()) {
            // force SpigotSkinRefresher for unsupported plugins (ViaVersion & other ProtocolHack).
            // Ran with #getPlugin() != null instead of #isPluginEnabled() as older Spigot builds return false during the login process even if enabled
            boolean viaVersion = plugin.getServer().getPluginManager().getPlugin("ViaVersion") != null;
            boolean protocolSupportExists = plugin.getServer().getPluginManager().getPlugin("ProtocolSupport") != null;
            if (viaVersion || protocolSupportExists) {
                log.info("Unsupported plugin (ViaVersion or ProtocolSupport) detected, forcing SpigotSkinRefresher");
                return new SpigotSkinRefresher(plugin, log);
            }

            // use PaperSkinRefresher if no VersionHack plugin found
            try {
                return new PaperSkinRefresher(log);
            } catch (ExceptionInInitializerError e) {
                e.printStackTrace();
            }
        }

        return new SpigotSkinRefresher(plugin, log);
    }

    /**
     * Applies the skin In other words, sets the skin data, but no changes will
     * be visible until you reconnect or force update with
     *
     * @param player   Player
     * @param property Property Object
     */
    protected void applySkin(Player player, IProperty property) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            SkinApplyBukkitEvent applyEvent = new SkinApplyBukkitEvent(player, property);

            Bukkit.getPluginManager().callEvent(applyEvent);

            if (applyEvent.isCancelled())
                return;

            // delay 1 server tick so we override online-mode
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                try {
                    if (property == null)
                        return;

                    Object ep = ReflectionUtil.invokeMethod(player.getClass(), player, "getHandle");
                    Object profile = ReflectionUtil.invokeMethod(ep.getClass(), ep, "getProfile");
                    Object propMap = ReflectionUtil.invokeMethod(profile.getClass(), profile, "getProperties");
                    ReflectionUtil.invokeMethod(propMap, "clear");
                    ReflectionUtil.invokeMethod(propMap.getClass(), propMap, "put", new Class[]{Object.class, Object.class}, "textures", property);

                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> updateSkin(player));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    /**
     * Instantly updates player's skin
     *
     * @param player - Player
     */
    public void updateSkin(Player player) {
        if (!player.isOnline())
            return;

        if (!checkOptFileChecked)
            checkOptFile();

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            Entity vehicle = player.getVehicle();

            // Dismounts a player on refreshing, which prevents desync caused by riding a horse, or plugins that allow sitting
            if ((Config.DISMOUNT_PLAYER_ON_UPDATE || !disableDismountPlayer) && vehicle != null) {
                vehicle.removePassenger(player);

                if (Config.REMOUNT_PLAYER_ON_UPDATE || enableRemountPlayer) {
                    // This is delayed to next tick to allow the accepter to propagate if necessary (IE: Paper's health update)
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        // This is not really necessary, as addPassenger on vanilla despawned vehicles won't do anything, but better to be safe in case the server has plugins that do strange things
                        if (vehicle.isValid()) {
                            vehicle.addPassenger(player);
                        }
                    }, 1);
                }
            }

            // Dismounts all entities riding the player, preventing desync from plugins that allow players to mount each other
            if ((Config.DISMOUNT_PASSENGERS_ON_UPDATE || enableDismountEntities) && !player.isEmpty()) {
                for (Entity passenger : player.getPassengers()) {
                    player.removePassenger(passenger);
                }
            }

            for (Player ps : Bukkit.getOnlinePlayers()) {
                // Some older spigot versions only support hidePlayer(player)
                try {
                    ps.hidePlayer(plugin, player);
                } catch (NoSuchMethodError ignored) {
                    //noinspection deprecation
                    ps.hidePlayer(player);
                }

                try {
                    ps.showPlayer(plugin, player);
                } catch (NoSuchMethodError ignored) {
                    //noinspection deprecation
                    ps.showPlayer(player);
                }
            }

            refresh.accept(player);
        });
    }

    private void checkOptFile() {
        File fileDisableDismountPlayer = new File(plugin.getDataFolder(), "disablesdismountplayer");
        File fileEnableDismountEntities = new File(plugin.getDataFolder(), "enablesdismountentities");
        File fileDisableRemountPlayer = new File(plugin.getDataFolder(), "disablesremountplayer");

        if (fileDisableDismountPlayer.exists())
            disableDismountPlayer = true;

        if (fileEnableDismountEntities.exists())
            enableDismountEntities = true;

        if (fileDisableRemountPlayer.exists())
            enableRemountPlayer = false;

        checkOptFileChecked = true;
    }
}
