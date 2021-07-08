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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.RequiredArgsConstructor;
import net.skinsrestorer.api.exception.SkinRequestException;
import net.skinsrestorer.api.property.IProperty;
import net.skinsrestorer.shared.storage.Config;
import net.skinsrestorer.shared.storage.Locale;
import net.skinsrestorer.shared.utils.MetricsCounter;
import net.skinsrestorer.shared.utils.log.SRLogLevel;
import net.skinsrestorer.shared.utils.log.SRLogger;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
public class MineSkinAPI {
    private final SRLogger logger;
    private final MojangAPI mojangAPI;
    private final Queue<UUID> queue = new LinkedList<>();
    private final Map<UUID, AtomicInteger> fails = new HashMap<>();

    public IProperty genSkin(String url, @Nullable String skinType, @Nullable UUID methodUUID) throws SkinRequestException {
        if (methodUUID == null) {
            methodUUID = UUID.randomUUID();
            queue.add(methodUUID);
        }

        fails.putIfAbsent(methodUUID, new AtomicInteger());

        if (fails.get(methodUUID).get() >= 5) {
            queue.remove(methodUUID);
            throw new SkinRequestException(Locale.MS_API_FAILED);
        }

        try {
            if (queue.element().equals(methodUUID)) {
                String skinVariant = skinType != null && (skinType.equalsIgnoreCase("steve") || skinType.equalsIgnoreCase("slim")) ? "&variant=" + skinType : "";

                try {
                    final String output = queryURL("url=" + URLEncoder.encode(url, "UTF-8") + skinVariant);
                    if (output.isEmpty()) //api time out
                        throw new SkinRequestException(Locale.ERROR_UPDATING_SKIN);

                    final JsonElement elm = new JsonParser().parse(output);
                    final JsonObject obj = elm.getAsJsonObject();

                    if (obj.has("data")) {
                        final JsonObject dta = obj.get("data").getAsJsonObject();

                        if (dta.has("texture")) {
                            final JsonObject tex = dta.get("texture").getAsJsonObject();

                            return mojangAPI.createProperty("textures", tex.get("value").getAsString(), tex.get("signature").getAsString());
                        }
                    } else if (obj.has("error")) {
                        final String errResp = obj.get("error").getAsString();

                        // If we send to many request, go sleep and try again.
                        if (errResp.equals("Too many requests")) {
                            fails.get(methodUUID).incrementAndGet();

                            // If "Too many requests"
                            if (obj.has("delay")) {
                                TimeUnit.SECONDS.sleep(obj.get("delay").getAsInt());

                                return genSkin(url, skinType, methodUUID); // try again after nextRequest
                            } else if (obj.has("nextRequest")) {
                                final long nextRequestMilS = (long) ((obj.get("nextRequest").getAsDouble() * 1000) - System.currentTimeMillis());

                                if (nextRequestMilS > 0)
                                    TimeUnit.MILLISECONDS.sleep(nextRequestMilS);

                                return genSkin(url, skinType, methodUUID); // try again after nextRequest
                            } else {
                                TimeUnit.SECONDS.sleep(2);

                                return genSkin(url, skinType, methodUUID); // try again after nextRequest
                            }
                        }

                        if (errResp.equals("Failed to generate skin data") || errResp.equals("Failed to change skin")) {
                            logger.debug("[ERROR] MS " + errResp + ", trying again... ");
                            TimeUnit.SECONDS.sleep(5);

                            return genSkin(url, skinType, methodUUID); // try again
                        } else if (errResp.equals("No accounts available")) {
                            logger.debug("[ERROR] " + errResp + " for: " + url);

                            throw new SkinRequestException(Locale.ERROR_MS_FULL);
                        }

                        logger.debug("[ERROR] MS:reason: " + errResp);
                        throw new SkinRequestException(Locale.ERROR_INVALID_URLSKIN);
                    }
                } catch (SkinRequestException e) {
                    //return SkinRequestException if caught.
                    throw new SkinRequestException(e.getMessage());
                } catch (IOException e) {
                    logger.debug(SRLogLevel.WARNING, "[ERROR] MS API Failure IOException (connection/disk): (" + url + ") " + e.getLocalizedMessage());
                } catch (JsonSyntaxException e) {
                    logger.debug(SRLogLevel.WARNING, "[ERROR] MS API Failure JsonSyntaxException (encoding): (" + url + ") " + e.getLocalizedMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                }

                // throw exception after all tries have failed
                logger.debug("[ERROR] MS:could not generate skin url: " + url);
                throw new SkinRequestException(Locale.MS_API_FAILED);
            } else {
                try {
                    TimeUnit.MILLISECONDS.sleep(100);
                } catch (InterruptedException e) {
                    throw new SkinRequestException(Locale.MS_API_FAILED);
                }

                return genSkin(url, skinType, methodUUID);
            }
        } finally {
            queue.remove(methodUUID);
        }
    }

    private String queryURL(String query) throws IOException {
        for (int i = 0; i < 3; i++) { // try 3 times, if server not responding
            try {
                MetricsCounter.incrAPI("https://api.mineskin.org/generate/url/");
                HttpsURLConnection con = (HttpsURLConnection) new URL("https://api.mineskin.org/generate/url/").openConnection();

                con.setRequestMethod("POST");
                con.setRequestProperty("Content-length", String.valueOf(query.length()));
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                con.setRequestProperty("User-Agent", "SkinsRestorer");
                con.setConnectTimeout(90000);
                con.setReadTimeout(90000);
                con.setDoOutput(true);
                con.setDoInput(true);

                if (!Config.MINESKIN_API_KEY.isEmpty())
                    con.setRequestProperty("Authorization", Config.MINESKIN_API_KEY);

                DataOutputStream output = new DataOutputStream(con.getOutputStream());
                output.writeBytes(query);
                output.close();
                StringBuilder outStr = new StringBuilder();
                InputStream is;

                try {
                    is = con.getInputStream();
                } catch (Exception e) {
                    is = con.getErrorStream();
                }

                DataInputStream input = new DataInputStream(is);
                for (int c = input.read(); c != -1; c = input.read())
                    outStr.append((char) c);

                input.close();
                return outStr.toString();
            } catch (Exception ignored) {
            }
        }

        return "";
    }
}
