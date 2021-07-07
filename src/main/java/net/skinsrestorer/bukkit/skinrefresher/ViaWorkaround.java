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
package net.skinsrestorer.bukkit.skinrefresher;

import com.viaversion.viabackwards.protocol.protocol1_15_2to1_16.Protocol1_15_2To1_16;
import com.viaversion.viaversion.api.Via;
import com.viaversion.viaversion.api.connection.UserConnection;
import com.viaversion.viaversion.api.protocol.packet.PacketWrapper;
import com.viaversion.viaversion.api.protocol.version.ProtocolVersion;
import com.viaversion.viaversion.api.type.Type;
import com.viaversion.viaversion.protocols.protocol1_15to1_14_4.ClientboundPackets1_15;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import org.bukkit.entity.Player;
import us.myles.ViaVersion.api.protocol.ProtocolRegistry;

public final class ViaWorkaround {
    private ViaWorkaround() {
    }

    public static boolean isProtocolNewer() {
        return ProtocolRegistry.SERVER_PROTOCOL >= ProtocolVersion.v1_16.getVersion();
    }

    public static boolean sendCustomPacketVia(Player player, Object craftHandle, Integer dimension, Object world, Object gamemodeId) throws Exception {
        UserConnection connection = Via.getManager().getConnectionManager().getConnectedClient(player.getUniqueId());
        if (connection != null
                && connection.getProtocolInfo() != null
                && connection.getProtocolInfo().getProtocolVersion() < ProtocolVersion.v1_16.getVersion()) {
            // ViaBackwards double-sends isProtocolNewer respawn packet when its dimension ID matches the current world's.
            // In order to get around this, we send isProtocolNewer packet directly into Via's connection, bypassing the 1.16 conversion step
            // and therefore bypassing their workaround.
            // TODO: This assumes 1.16 methods; probably stop hardcoding this when 1.17 comes around
            Object worldServer = ReflectionUtil.invokeMethod(craftHandle, "getWorldServer");

            PacketWrapper packet = PacketWrapper.create(ClientboundPackets1_15.RESPAWN.ordinal(), null, connection);

            packet.write(Type.INT, dimension);
            packet.write(Type.LONG, (long) ReflectionUtil.invokeMethod(world, "getSeed"));
            packet.write(Type.UNSIGNED_BYTE, ((Integer) gamemodeId).shortValue());
            packet.write(Type.STRING, (boolean) ReflectionUtil.invokeMethod(worldServer, "isFlatWorld") ? "flat" : "default");
            packet.send(Protocol1_15_2To1_16.class);
            return false;
        }

        return true;
    }
}
