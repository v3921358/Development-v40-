/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.odinms.constants;

import net.sf.odinms.client.MapleInventoryType;

/**
 *
 * @author Eric
 */
public class ServerConstants {
    // Our Rate Multipliers
    public static int EXP_RATE = 100;
    public static int MESO_RATE = 100;
    public static int PET_EXP_RATE = 2;
    // The scrolling header
    public static String SERVER_MESSAGE = "Welcome to Development v40 Beta Edition! | Find a bug? Report it to Eric! | Working Features: Fame, Parties, Buddylists, Trading, Seats, Messenger, Storages, Ring Effects, Mini Games, Reporting, Notes, Pets, and more!";
    // Enable players to return to the town scroll even if they're not in that town. (Return to Henesys using Henesys return scroll via Florina Beach)
    public static boolean ReturnScrollAnywhere = true; 
    // Enable/Disable the Cash Shop
    public static boolean ENABLE_CS = true;
    // Maps classified as Jail; blocked command usage
    public static final int[] JAIL_MAPS = {280090000};
    // Block Teleport Rock maps
    public static int[] BLOCKED_VIP_MAPS = {0, 180000000};
    // How many channels, TODO: remove keys and allow multiple channel ports to 19
    public static int Channels = 5;
    // The name via /find, World Select, etc.
    public static String WORLD_NAME = "Tespia", SERVER_NAME = "Development";
    // Time in milliseconds between loginqueue runs
    public static int LoginInterval = 500;
    // Time in milliseconds between ranking updates (default: 30 minutes)
    public static long RankingInterval = 1800000;
    // Total user limit of the server
    public static int UserLimit = 500;
    // Database Information
    public static String DB_SCHEMA = "odinms", DB_USER = "root", DB_PASS = "";
    // Defaults for channelservers
    public static String ip = "25.83.173.150";
    public static int PORT = 7575;
    // Active event scripts
    public static String Events = "lolcastle,3rdjob,ZakumPQ,KerningPQ";
    
    // For CC/Server use, not CSERV registering
    public static byte[] getIP() {
        String[] ipString = ip.split("\\.");
        byte[] ipByte = new byte[] { (byte)(Integer.parseInt(ipString[0])), (byte)(Integer.parseInt(ipString[1])), (byte)(Integer.parseInt(ipString[2])), (byte)(Integer.parseInt(ipString[3])) };
        return ipByte;
    }
    
    public static boolean isJail(int mapid) {
        boolean jailed = false;
        for (int i = 0; i < JAIL_MAPS.length; i++) {
            if (mapid == JAIL_MAPS[i]) {
                jailed = true;
            }
        }
        return jailed;
    }
    
    public static MapleInventoryType getInventoryType(int itemId) {
        byte type = (byte) (itemId / 1000000);
        if (type < 1 || type > 5) {
            return MapleInventoryType.UNDEFINED;
        }
        return MapleInventoryType.getByType(type);
    }
    
    public static boolean isPet(int itemId) {
        return itemId / 10000 == 500;
    }
    
    public static enum PlayerGMRank {
        NORMAL('@', 0),
        DONATOR('!', 1),
        SUPERDONATOR('!', 2),
        INTERN('!', 3),
        GM('!', 4),
        SUPERGM('!', 5),
        ADMIN('!', 6), 
        GOD('!', 100);
        private char commandPrefix;
        private int level;

        PlayerGMRank(char ch, int level) {
            commandPrefix = ch;
            this.level = level;
        }

        public char getCommandPrefix() {
            return commandPrefix;
        }

        public int getLevel() {
            return level;
        }
    }

    public static enum CommandType {
        NORMAL(0);
        private int level;

        CommandType(int level) {
            this.level = level;
        }

        public int getType() {
            return level;
        }
    }
}
