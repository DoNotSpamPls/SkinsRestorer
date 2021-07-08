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
package net.skinsrestorer.shared.storage;

import net.skinsrestorer.shared.utils.log.SRLogger;

import java.io.File;
import java.io.InputStream;
import java.util.List;

public class Config {
    public static boolean SKINWITHOUTPERM = false;
    public static int SKIN_CHANGE_COOLDOWN = 30;
    public static int SKIN_ERROR_COOLDOWN = 5;
    public static boolean ENABLE_CUSTOM_HELP = false;
    public static boolean DISABLE_PREFIX = false;
    public static boolean DEFAULT_SKINS_ENABLED = false;
    public static boolean DEFAULT_SKINS_PREMIUM = false;
    public static List<String> DEFAULT_SKINS = null;
    public static boolean DISABLED_SKINS_ENABLED = false;
    public static List<String> DISABLED_SKINS = null;
    public static boolean CUSTOM_GUI_ENABLED = false;
    public static boolean CUSTOM_GUI_ONLY = false;
    public static List<String> CUSTOM_GUI_SKINS = null;
    public static boolean PER_SKIN_PERMISSIONS = false;
    public static int SKIN_EXPIRES_AFTER = 20;
    public static boolean MULTIBUNGEE_ENABLED = false;
    public static boolean MYSQL_ENABLED = false;
    public static String MYSQL_HOST = "localhost";
    public static String MYSQL_PORT = "3306";
    public static String MYSQL_DATABASE = "db";
    public static String MYSQL_USERNAME = "root";
    public static String MYSQL_PASSWORD = "pass";
    public static String MYSQL_SKINTABLE = "Skins";
    public static String MYSQL_PLAYERTABLE = "Players";
    public static String MYSQL_CONNECTIONOPTIONS = "verifyServerCertificate=false&useSSL=false&serverTimezone=UTC";
    public static boolean NO_SKIN_IF_LOGIN_CANCELED = true;
    public static boolean RESTRICT_SKIN_URLS_ENABLED = false;
    public static List<String> RESTRICT_SKIN_URLS_LIST = null;
    public static String MINESKIN_API_KEY = "";
    public static boolean DISMOUNT_PLAYER_ON_UPDATE = true;
    public static boolean REMOUNT_PLAYER_ON_UPDATE = true;
    public static boolean DISMOUNT_PASSENGERS_ON_UPDATE = false;
    public static boolean DISABLE_ONJOIN_SKINS = false;
    public static boolean DISABLE_AUTO_UPDATE_SKIN = false;
    public static boolean DEBUG = false;


    // UPCOMING MULTIPLE LANGUAGE SUPPORT
    public static String LOCALE_FILE = "english.yml";

    // private static YamlConfig config = new YamlConfig("plugins" + File.separator + "SkinsRestorer" + File.separator + "", "config", false);
    private static YamlConfig config;

    public static void load(File path, InputStream is, SRLogger logger) {
        config = new YamlConfig(path, "config.yml", false, logger);
        config.saveDefaultConfig(is);
        config.reload();
        SKINWITHOUTPERM = config.getBoolean("SkinWithoutPerm", SKINWITHOUTPERM);
        SKIN_CHANGE_COOLDOWN = config.getInt("SkinChangeCooldown", SKIN_CHANGE_COOLDOWN);
        SKIN_ERROR_COOLDOWN = config.getInt("SkinErrorCooldown", SKIN_ERROR_COOLDOWN);
        ENABLE_CUSTOM_HELP = config.getBoolean("EnableCustomHelp", ENABLE_CUSTOM_HELP);
        DISABLE_PREFIX = config.getBoolean("DisablePrefix", DISABLE_PREFIX);
        DEFAULT_SKINS_ENABLED = config.getBoolean("DefaultSkins.Enabled", DEFAULT_SKINS_ENABLED);
        DEFAULT_SKINS_PREMIUM = config.getBoolean("DefaultSkins.ApplyForPremium", DEFAULT_SKINS_PREMIUM);
        DEFAULT_SKINS = config.getStringList("DefaultSkins.Names", ".skin");
        DISABLED_SKINS_ENABLED = config.getBoolean("DisabledSkins.Enabled", DISABLED_SKINS_ENABLED);
        DISABLED_SKINS = config.getStringList("DisabledSkins.Names");
        CUSTOM_GUI_ENABLED = config.getBoolean("CustomGUI.Enabled", CUSTOM_GUI_ENABLED);
        CUSTOM_GUI_ONLY = config.getBoolean("CustomGUI.ShowOnlyCustomGUI", CUSTOM_GUI_ONLY);
        CUSTOM_GUI_SKINS = config.getStringList("CustomGUI.Names");
        PER_SKIN_PERMISSIONS = config.getBoolean("PerSkinPermissions", PER_SKIN_PERMISSIONS);
        SKIN_EXPIRES_AFTER = config.getInt("SkinExpiresAfter", SKIN_EXPIRES_AFTER);
        MULTIBUNGEE_ENABLED = config.getBoolean("MultiBungee.Enabled", MULTIBUNGEE_ENABLED);
        MYSQL_ENABLED = config.getBoolean("MySQL.Enabled", MYSQL_ENABLED);
        MYSQL_HOST = config.getString("MySQL.Host", MYSQL_HOST);
        MYSQL_PORT = config.getString("MySQL.Port", MYSQL_PORT);
        MYSQL_DATABASE = config.getString("MySQL.Database", MYSQL_DATABASE);
        MYSQL_USERNAME = config.getString("MySQL.Username", MYSQL_USERNAME);
        MYSQL_PASSWORD = config.getString("MySQL.Password", MYSQL_PASSWORD);
        MYSQL_SKINTABLE = config.getString("MySQL.SkinTable", MYSQL_SKINTABLE);
        MYSQL_PLAYERTABLE = config.getString("MySQL.PlayerTable", MYSQL_PLAYERTABLE);
        MYSQL_CONNECTIONOPTIONS = config.getString("MySQL.ConnectionOptions", MYSQL_CONNECTIONOPTIONS);
        DISABLE_ONJOIN_SKINS = config.getBoolean("DisableOnJoinSkins", DISABLE_ONJOIN_SKINS);
        DISABLE_AUTO_UPDATE_SKIN = config.getBoolean("DisableAutoUpdateSkin", DISABLE_AUTO_UPDATE_SKIN);
        NO_SKIN_IF_LOGIN_CANCELED = config.getBoolean("NoSkinIfLoginCanceled", NO_SKIN_IF_LOGIN_CANCELED);
        RESTRICT_SKIN_URLS_ENABLED = config.getBoolean("RestrictSkinUrls.Enabled", RESTRICT_SKIN_URLS_ENABLED);
        RESTRICT_SKIN_URLS_LIST = config.getStringList("RestrictSkinUrls.List");
        MINESKIN_API_KEY = config.getString("MineskinAPIKey", MINESKIN_API_KEY);
        DISMOUNT_PLAYER_ON_UPDATE = config.getBoolean("DismountPlayerOnSkinUpdate", DISMOUNT_PLAYER_ON_UPDATE);
        REMOUNT_PLAYER_ON_UPDATE = config.getBoolean("RemountPlayerOnSkinUpdate", REMOUNT_PLAYER_ON_UPDATE);
        DISMOUNT_PASSENGERS_ON_UPDATE = config.getBoolean("DismountPassengersOnSkinUpdate", DISMOUNT_PASSENGERS_ON_UPDATE);
        DEBUG = config.getBoolean("Debug", DEBUG);

        if (DEFAULT_SKINS_ENABLED && DEFAULT_SKINS.isEmpty()) {
            logger.warning("[Config] no DefaultSkins found! Disabling DefaultSkins.");
            DEFAULT_SKINS_ENABLED = false;
        }

        if (RESTRICT_SKIN_URLS_ENABLED && RESTRICT_SKIN_URLS_LIST.isEmpty()) {
            logger.warning("[Config] no RestrictSkinUrls found! Disabling RestrictSkinUrls.");
            RESTRICT_SKIN_URLS_ENABLED = false;
        }

        if (!CUSTOM_GUI_ENABLED)
            CUSTOM_GUI_ONLY = false;

        if (!DISMOUNT_PLAYER_ON_UPDATE)
            REMOUNT_PLAYER_ON_UPDATE = false;

        try {
            if (config.getBoolean("UseOldSkinHelp")) {
                logger.warning("[Config] UseOldSkinHelp has been renamed! use \"EnableCustomHelp\"");
                ENABLE_CUSTOM_HELP = true;
            }
        } catch (Exception ignored) {
        }

        if (MINESKIN_API_KEY.equals("key"))
            MINESKIN_API_KEY = "";
    }

    public static void set(String path, Object value) {
        config.set(path, value);
    }
}
