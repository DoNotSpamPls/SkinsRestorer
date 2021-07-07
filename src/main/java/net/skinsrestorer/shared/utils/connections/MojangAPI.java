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
package net.skinsrestorer.shared.utils.connections;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.*;
import net.skinsrestorer.shared.exception.ReflectionException;
import net.skinsrestorer.shared.serverinfo.Platform;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.ReflectionUtil;
import net.skinsrestorer.shared.utils.log.SRLogLevel;
import net.skinsrestorer.shared.utils.log.SRLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MojangAPI {
    private static final String UUID_URL = "https://api.minetools.eu/uuid/%name%";
    private static final String UUID_URL_MOJANG = "https://api.mojang.com/users/profiles/minecraft/%name%";
    private static final String UUID_URL_BACKUP = "https://api.ashcon.app/mojang/v2/user/%name%";

    private static final String SKIN_URL = "https://api.minetools.eu/profile/%uuid%";
    private static final String SKIN_URL_MOJANG = "https://sessionserver.mojang.com/session/minecraft/profile/%uuid%?unsigned=false";
    private static final String SKIN_URL_BACKUP = "https://api.ashcon.app/mojang/v2/user/%uuid%";
    private final SRLogger logger;
    private final Platform platform;
    private Class<? extends IProperty> propertyClass;

    public MojangAPI(SRLogger logger, Platform platform) {
        this.logger = logger;
        this.platform = platform;

        if (platform == Platform.BUKKIT) {
            propertyClass = BukkitProperty.class;
        } else if (platform == Platform.BUNGEECORD) {
            propertyClass = BungeeProperty.class;
        } else if (platform == Platform.VELOCITY) {
            propertyClass = VelocityProperty.class;
        }
    }

    public IProperty createProperty(String name, String value, String signature) {
        // use our own property class if we are on sponge
        if (platform == Platform.SPONGE)
            return new GenericProperty(name, value, signature);

        try {
            return (IProperty) ReflectionUtil.invokeConstructor(propertyClass, name, value, signature);
        } catch (ReflectionException e) {
            e.printStackTrace();
        }

        return null;
    }

    // TODO: Deal with duplicated code

    public IProperty getProfile(String uuid) {
        return getProfile(uuid, true);
    }

    public IProperty getProfile(String uuid, boolean tryNext) {
        try {
            String output = readURL(SKIN_URL.replace("%uuid%", uuid));
            JsonObject obj = new Gson().fromJson(output, JsonObject.class);

            if (obj.has("raw")) {
                JsonObject raw = obj.getAsJsonObject("raw");

                if (raw.has("status") && raw.get("status").getAsString().equalsIgnoreCase("ERR")) {
                    return getProfileMojang(uuid, true);
                }

                GenericProperty property = new GenericProperty();
                if (property.valuesFromJson(raw)) {
                    return createProperty("textures", property.getValue(), property.getSignature());
                }
            }
        } catch (Exception e) {
            if (tryNext)
                return getProfileMojang(uuid, true);
        }

        return null;
    }

    public IProperty getProfileMojang(String uuid, boolean tryNext) {
        if (tryNext)
            logger.debug("Trying Mojang API to get skin property for " + uuid + ".");

        try {
            String output = readURL(SKIN_URL_MOJANG.replace("%uuid%", uuid));
            JsonObject obj = new Gson().fromJson(output, JsonObject.class);

            GenericProperty property = new GenericProperty();
            if (obj.has("properties") && property.valuesFromJson(obj)) {
                return createProperty("textures", property.getValue(), property.getSignature());
            }
        } catch (Exception e) {
            if (tryNext)
                return getProfileBackup(uuid, true);
        }

        return null;
    }

    protected IProperty getProfileBackup(String uuid, boolean tryNext) {
        if (tryNext)
            logger.debug("Trying backup API to get skin property for " + uuid + ".");

        try {
            String output = readURL(SKIN_URL_BACKUP.replace("%uuid%", uuid), 10000);
            JsonObject obj = new Gson().fromJson(output, JsonObject.class);
            JsonObject textures = obj.get("textures").getAsJsonObject();
            JsonObject rawTextures = textures.get("raw").getAsJsonObject();

            return createProperty("textures", rawTextures.get("value").getAsString(), rawTextures.get("signature").getAsString());
        } catch (Exception e) {
            logger.debug(SRLogLevel.WARNING, "Failed to get skin property from backup API. (" + uuid + ")");
        }

        return null;
    }

    public String getUUID(String name) throws SkinRequestException {
        return getUUID(name, true);
    }

    /**
     * @param name Name of the player
     * @return Dash-less UUID (String)
     * @throws SkinRequestException If player is NOT_PREMIUM or server is RATE_LIMITED
     */
    protected String getUUID(String name, boolean tryNext) throws SkinRequestException {
        try {
            String output = readURL(UUID_URL.replace("%name%", name));

            JsonObject obj = new Gson().fromJson(output, JsonObject.class);
            if (obj.has("status") && obj.get("status").getAsString().equalsIgnoreCase("ERR")) {
                return getUUIDMojang(name, true);
            }

            if (obj.get("id") == null)
                throw new SkinRequestException(Locale.NOT_PREMIUM);

            return obj.get("id").getAsString();
        } catch (IOException e) {
            if (tryNext)
                return getUUIDMojang(name, true);
        }

        return null;
    }

    public String getUUIDMojang(String name, boolean tryNext) throws SkinRequestException {
        if (tryNext)
            logger.debug("Trying Mojang API to get UUID for player " + name + ".");

        try {
            String output = readURL(UUID_URL_MOJANG.replace("%name%", name));

            if (output.isEmpty())
                throw new SkinRequestException(Locale.NOT_PREMIUM);

            JsonObject obj = new Gson().fromJson(output, JsonObject.class);

            if (obj.has("error")) {
                if (tryNext)
                    return getUUIDBackup(name, true);
                return null;
            }

            return obj.get("id").getAsString();
        } catch (IOException e) {
            if (tryNext)
                return getUUIDBackup(name, true);
        }

        return null;
    }

    protected String getUUIDBackup(String name, boolean tryNext) throws SkinRequestException {
        if (tryNext)
            logger.debug("Trying backup API to get UUID for player " + name + ".");

        try {
            String output = readURL(UUID_URL_BACKUP.replace("%name%", name), 10000);

            JsonObject obj = new Gson().fromJson(output, JsonObject.class);

            if (obj.has("code")) {
                if (obj.get("error").getAsString().equalsIgnoreCase("Not Found")) {
                    throw new SkinRequestException(Locale.NOT_PREMIUM);
                }

                throw new SkinRequestException(Locale.ALT_API_FAILED);
            }

            return obj.get("uuid").getAsString().replace("-", "");
        } catch (IOException e) {
            throw new SkinRequestException(Locale.NOT_PREMIUM); // TODO: check flow of code
        }
    }

    private String readURL(String url) throws IOException {
        return readURL(url, 5000);
    }

    private String readURL(String url, int timeout) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        MetricsCounter.incrAPI(url);

        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "SkinsRestorer");
        con.setConnectTimeout(timeout);
        con.setReadTimeout(timeout);
        con.setDoOutput(true);

        String line;
        StringBuilder output = new StringBuilder();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

        while ((line = in.readLine()) != null)
            output.append(line);

        in.close();
        return output.toString();
    }
}
