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

import com.cryptomorin.xseries.XMaterial;
import com.mojang.authlib.properties.Property;
import lombok.Getter;
import net.skinsrestorer.api.bukkit.BukkitHeadAPI;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.C;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class SkinsGUI extends ItemStack implements Listener {
    @Getter
    private static final Map<String, Integer> menus = new ConcurrentHashMap<>();
    private final SkinsRestorer plugin;
    @Getter
    private final SRLogger log;

    public SkinsGUI(SkinsRestorer plugin, SRLogger log) {
        this.plugin = plugin;
        this.log = log;
    }

    public Inventory getGUI(Player player, int page, Map<String, Object> skinsList) {
        Inventory inventory = Bukkit.createInventory(player, 54, C.c(Locale.SKINSMENU_TITLE_NEW).replace("%page", String.valueOf(page)));

        ItemStack none = new GuiGlass(GlassType.NONE).getItemStack();
        ItemStack delete = new GuiGlass(GlassType.DELETE).getItemStack();
        ItemStack prev = new GuiGlass(GlassType.PREV).getItemStack();
        ItemStack next = new GuiGlass(GlassType.NEXT).getItemStack();

        // White Glass line
        inventory.setItem(36, none);
        inventory.setItem(37, none);
        inventory.setItem(38, none);
        inventory.setItem(39, none);
        inventory.setItem(40, none);
        inventory.setItem(41, none);
        inventory.setItem(42, none);
        inventory.setItem(43, none);
        inventory.setItem(44, none);

        // Empty place previous
        inventory.setItem(45, none);
        inventory.setItem(46, none);
        inventory.setItem(47, none);

        // Middle button //remove skin
        inventory.setItem(48, delete);
        inventory.setItem(49, delete);
        inventory.setItem(50, delete);


        // Empty place next
        inventory.setItem(53, none);
        inventory.setItem(52, none);
        inventory.setItem(51, none);

        // If page is above 1, adding Previous Page button.
        if (page > 1) {
            inventory.setItem(45, prev);
            inventory.setItem(46, prev);
            inventory.setItem(47, prev);
        }

        skinsList.forEach((name, property) -> {
            if (name.chars().anyMatch(i -> Character.isLetter(i) && Character.isUpperCase(i))) {
                log.info("ERROR: skin " + name + ".skin contains a Upper case!");
                log.info("Please rename the file name to a lower case!.");
                return;
            }

            inventory.addItem(createSkull(name, property));
        });

        // If the page is not empty, adding Next Page button.
        if (inventory.firstEmpty() == -1 || inventory.getItem(26) != null && page < 999) {
            inventory.setItem(53, next);
            inventory.setItem(52, next);
            inventory.setItem(51, next);
        }

        return inventory;
    }

    public Inventory getGUI(Player player, int page) {
        if (page > 999)
            page = 999;
        int skinNumber = 36 * page;

        Map<String, Object> skinsList = plugin.getSkinStorage().getSkins(skinNumber);
        ++page; // start counting from 1
        return getGUI(player, page, skinsList);
    }

    private ItemStack createSkull(String name, Object property) {
        ItemStack is = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta sm = (SkullMeta) Objects.requireNonNull(is).getItemMeta();

        List<String> lore = new ArrayList<>();
        lore.add(C.c(Locale.SKINSMENU_SELECT_SKIN));
        Objects.requireNonNull(sm).setDisplayName(name);
        sm.setLore(lore);
        is.setItemMeta(sm);

        try {
            BukkitHeadAPI.setSkull(is, ((Property) property).getValue());
        } catch (Exception e) {
            log.info("ERROR: could not add '" + name + "' to SkinsGUI, skin might be corrupted or invalid!");
            e.printStackTrace();
        }

        return is;
    }

    // TODO: increase performance by excluding non item clicks from this event before event#getView() (use if performance will be increased.)
    @EventHandler(ignoreCancelled = true)
    public void onCLick(InventoryClickEvent event) {
        // Cancel if clicked outside inventory
        if (event.getClickedInventory() == null)
            return;

        if (event.getView().getTopInventory().getType() != InventoryType.CHEST)
            return;

        try {
            if (!event.getView().getTitle().contains("Skins Menu") && !event.getView().getTitle().contains(Locale.SKINSMENU_TITLE_NEW.replace("%page", "")))
                return;
        } catch (IllegalStateException ex) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player))
            return;

        Player player = (Player) event.getWhoClicked();

        // Cancel picking up items
        if (event.getCurrentItem() == null) {
            event.setCancelled(true);
            return;
        }

        ItemStack currentItem = event.getCurrentItem();

        // Cancel white panels.
        if (!currentItem.hasItemMeta()) {
            event.setCancelled(true);
            return;
        }

        if (plugin.isBungeeEnabled()) {
            switch (Objects.requireNonNull(XMaterial.matchXMaterial(currentItem))) {
                case PLAYER_HEAD:
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        String skin = Objects.requireNonNull(currentItem.getItemMeta()).getDisplayName();
                        plugin.requestSkinSetFromBungeeCord(player, skin);
                    });
                    player.closeInventory();
                    break;
                case RED_STAINED_GLASS_PANE:
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () ->
                            plugin.requestSkinClearFromBungeeCord(player));
                    player.closeInventory();
                    break;
                case GREEN_STAINED_GLASS_PANE:
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        int currentPageG = getMenus().get(player.getName());
                        getMenus().put(player.getName(), currentPageG + 1);
                        plugin.requestSkinsFromBungeeCord(player, currentPageG + 1);
                    });
                    break;
                case YELLOW_STAINED_GLASS_PANE:
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        int currentPageY = getMenus().get(player.getName());
                        getMenus().put(player.getName(), currentPageY - 1);
                        plugin.requestSkinsFromBungeeCord(player, currentPageY - 1);
                    });
                    break;
                default:
                    break;
            }
            event.setCancelled(true);
            return;
        }

        // TODO: use #setSkin() function from SkinCommand.class
        switch (Objects.requireNonNull(XMaterial.matchXMaterial(currentItem))) {
            case PLAYER_HEAD:
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    final String skinName = Objects.requireNonNull(currentItem.getItemMeta()).getDisplayName();
                    plugin.getSkinCommand().onSkinSetShort(player, skinName);
                });
                player.closeInventory();
                break;
            case RED_STAINED_GLASS_PANE:
                plugin.getSkinCommand().onSkinClear(player);
                player.closeInventory();
                break;
            case GREEN_STAINED_GLASS_PANE:
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    final int currentPageA = getMenus().get(player.getName());
                    getMenus().put(player.getName(), currentPageA + 1);
                    Inventory newInventory = getGUI((player).getPlayer(), currentPageA + 1);

                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
                            player.openInventory(newInventory));
                });
                break;
            case YELLOW_STAINED_GLASS_PANE:
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    final int currentPageB = getMenus().get(player.getName());
                    getMenus().put(player.getName(), currentPageB - 1);
                    Inventory newInventory = getGUI((player).getPlayer(), currentPageB - 1);

                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () ->
                            player.openInventory(newInventory));
                });
                break;
            default:
                break;
        }
        event.setCancelled(true);
    }

    private enum GlassType {
        NONE, PREV, NEXT, DELETE
    }

    public static class GuiGlass {
        @Getter
        private ItemStack itemStack;
        @Getter
        private String text;

        public GuiGlass(GlassType glassType) {
            switch (glassType) {
                case NONE:
                    itemStack = XMaterial.WHITE_STAINED_GLASS_PANE.parseItem();
                    text = " ";
                    break;
                case PREV:
                    itemStack = XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem();
                    text = C.c(Locale.SKINSMENU_PREVIOUS_PAGE);
                    break;
                case NEXT:
                    itemStack = XMaterial.GREEN_STAINED_GLASS_PANE.parseItem();
                    text = C.c(Locale.SKINSMENU_NEXT_PAGE);
                    break;
                case DELETE:
                    itemStack = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
                    text = C.c(Locale.SKINSMENU_REMOVE_SKIN);
                    break;
            }

            ItemMeta itemMeta = Objects.requireNonNull(itemStack).getItemMeta();
            Objects.requireNonNull(itemMeta).setDisplayName(text);
            itemStack.setItemMeta(itemMeta);
        }
    }
}