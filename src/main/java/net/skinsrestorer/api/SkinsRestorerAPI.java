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
package net.skinsrestorer.api;

import lombok.Getter;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.storage.SkinStorage;
import net.skinsrestorer.shared.utils.connections.MojangAPI;

/**
 * API Example: <a href="https://github.com/SkinsRestorer/SkinsRestorerAPIExample">https://github.com/SkinsRestorer/SkinsRestorerAPIExample</a>
 * For more info please refer first to <a href="https://github.com/SkinsRestorer/SkinsRestorerX/wiki/SkinsRestorerAPI">https://github.com/SkinsRestorer/SkinsRestorerX/wiki/SkinsRestorerAPI</a>
 * Advanced help or getting problems? join our discord before submitting issues!
 */
@SuppressWarnings({"unused"})
public abstract class SkinsRestorerAPI {
    @Getter
    private static SkinsRestorerAPI api;
    private final MojangAPI mojangAPI;
    private final SkinStorage skinStorage;

    protected SkinsRestorerAPI(MojangAPI mojangAPI, SkinStorage skinStorage) {
        if (SkinsRestorerAPI.api == null)
            setInstance(this);

        this.mojangAPI = mojangAPI;
        this.skinStorage = skinStorage;
    }

    private static void setInstance(SkinsRestorerAPI api) {
        if (SkinsRestorerAPI.api == null)
            SkinsRestorerAPI.api = api;
    }

    /**
     * Returned object needs to be casted to either BungeeCord's property or
     * Mojang's property (old or new)
     *
     * @param uuid The players uuid
     * @return Property object (New Mojang, Old Mojang or Bungee)
     **/
    public IProperty getProfile(String uuid) {
        return mojangAPI.getProfile(uuid);
    }

    /**
     * Get a players custom skin.
     *
     * @param name The players name
     * @return the players custom skin name if set or null if not set
     */
    public String getSkinName(String name) {
        return skinStorage.getSkinName(name);
    }

    public IProperty getSkinData(String skin) {
        return skinStorage.getSkinData(skin);
    }

    /**
     * Check if a player got a custom skin.
     *
     * @param name The players name
     * @return true if a player has a custom skin set
     */
    public boolean hasSkin(String name) {
        return skinStorage.getSkinName(name) != null;
    }

    /**
     * Saves custom player's skin name to database
     *
     * @param name Players name
     * @param skin Skin name
     **/
    public void setSkinName(String name, String skin) {
        skinStorage.setSkinName(name, skin);
    }

    public void setSkin(String playerName, String skinName) throws SkinRequestException {
        skinStorage.setSkinName(playerName, skinName);
        skinStorage.getSkinForPlayer(skinName, false);
    }

    public void removeSkin(String playerName) {
        skinStorage.removeSkin(playerName);
    }

    public abstract void applySkin(PlayerWrapper playerWrapper) throws SkinRequestException;

    public abstract void applySkin(PlayerWrapper playerWrapper, String name) throws SkinRequestException;

    public abstract void applySkin(PlayerWrapper playerWrapper, IProperty props);
}
