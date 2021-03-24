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
package net.skinsrestorer.bungee.utils;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.connection.LoginResult.Property;
import net.skinsrestorer.api.PlayerWrapper;
import net.skinsrestorer.api.bungeecord.events.SkinApplyBungeeEvent;
import net.skinsrestorer.bungee.SkinsRestorer;
import net.skinsrestorer.shared.interfaces.ISRApplier;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import net.skinsrestorer.shared.utils.log.SRLogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class SkinApplierBungee implements ISRApplier {
    private final SkinsRestorer plugin;
    private final SRLogger log;

    public SkinApplierBungee(SkinsRestorer plugin) {
        this.plugin = plugin;
        log = plugin.getSrLogger();
    }

    public void applySkin(final ProxiedPlayer p, String nick, InitialHandler handler) throws Exception {
        if (p == null)
            return;

        if (handler == null) {
            handler = (InitialHandler) p.getPendingConnection();
        }

        nick = plugin.getProxy().getPluginManager().callEvent(new SkinApplyBungeeEvent(p, nick)).getNick();

        Property textures = (Property) plugin.getSkinStorage().getOrCreateSkinForPlayer(nick, false);

        if (handler.isOnlineMode()) {
            sendUpdateRequest(p, textures);
            return;
        }

        LoginResult profile = handler.getLoginProfile();

        if (profile == null) {
            try {
                // NEW BUNGEECORD (id, name, property)
                profile = new LoginResult(null, null, new Property[]{textures});
            } catch (Exception error) {
                // FALL BACK TO OLD (id, property)
                profile = (LoginResult) ReflectionUtil.invokeConstructor(LoginResult.class,
                        new Class<?>[]{String.class, Property[].class},
                        null, new Property[]{textures});
            }
        }

        Property[] newProps = new Property[]{textures};

        profile.setProperties(newProps);
        ReflectionUtil.setObject(InitialHandler.class, handler, "loginProfile", profile);

        if (plugin.isMultiBungee()) {
            sendUpdateRequest(p, textures);
        } else {
            sendUpdateRequest(p, null);
        }
    }

    public void applySkin(final PlayerWrapper p) throws Exception {
        applySkin(p.get(ProxiedPlayer.class), p.get(ProxiedPlayer.class).getName(), null);
    }

    private void sendUpdateRequest(ProxiedPlayer p, Property textures) {
        if (p == null)
            return;

        if (p.getServer() == null)
            return;

        log.debug("Sending skin update request for " + p.getName());

        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(b);
        try {
            out.writeUTF("SkinUpdate");

            if (textures != null) {
                out.writeUTF(textures.getName());
                out.writeUTF(textures.getValue());
                out.writeUTF(textures.getSignature());
            }

            p.getServer().sendData("sr:skinchange", b.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
