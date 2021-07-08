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
package net.skinsrestorer.api.bukkit;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.skinsrestorer.shared.exception.FieldNotFoundException;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Objects;
import java.util.UUID;

public class BukkitHeadAPI {
    private BukkitHeadAPI() {
    }

    public static void setSkull(ItemStack head, String b64stringTexture) {
        GameProfile profile = new GameProfile(UUID.randomUUID(), null);
        PropertyMap propertyMap = profile.getProperties();

        if (propertyMap == null) {
            throw new IllegalStateException("Profile doesn't contain a property map");
        }

        //noinspection unchecked
        propertyMap.put("textures", new Property("textures", b64stringTexture));

        ItemMeta headMeta = head.getItemMeta();
        Class<?> headMetaClass = Objects.requireNonNull(headMeta).getClass();

        try {
            ReflectionUtil.getField(headMetaClass, "profile", GameProfile.class, 0).set(headMeta, profile);
        } catch (FieldNotFoundException | IllegalAccessException e) {
            e.printStackTrace();
        }

        head.setItemMeta(headMeta);
    }
}
