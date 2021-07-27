/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.odinms.client.messages;

import java.util.ArrayList;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.messages.commands.*;
import net.sf.odinms.client.messages.commands.GodCommand;
import net.sf.odinms.client.messages.commands.AdminCommand;
import net.sf.odinms.client.messages.commands.GMCommand;
import net.sf.odinms.client.messages.commands.InternCommand;
import net.sf.odinms.client.messages.commands.PlayerCommand;
import net.sf.odinms.constants.ServerConstants.CommandType;
import net.sf.odinms.constants.ServerConstants.PlayerGMRank;
import net.sf.odinms.database.DatabaseConnection;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.tools.Pair;


public class CommandProcessor {

    private final static HashMap<String, CommandObject> commands = new HashMap<>();
    private final static HashMap<Integer, ArrayList<String>> commandList = new HashMap<>();

    static {

        Class<?>[] CommandFiles = {
            PlayerCommand.class, InternCommand.class, GMCommand.class, AdminCommand.class, DonatorCommand.class, SuperDonatorCommand.class, SuperGMCommand.class,
            GodCommand.class
        };

        for (Class<?> clasz : CommandFiles) {
            try {
                PlayerGMRank rankNeeded = (PlayerGMRank) clasz.getMethod("getPlayerLevelRequired", new Class<?>[]{}).invoke(null, (Object[]) null);
                Class<?>[] a = clasz.getDeclaredClasses();
                ArrayList<String> cL = new ArrayList<>();
                for (Class<?> c : a) {
                    try {
                        if (!Modifier.isAbstract(c.getModifiers()) && !c.isSynthetic()) {
                            Object o = c.newInstance();
                            boolean enabled;
                            try {
                                enabled = c.getDeclaredField("enabled").getBoolean(c.getDeclaredField("enabled"));
                            } catch (NoSuchFieldException ex) {
                                enabled = true; //Enable all coded commands by default.
                            }
                            if (o instanceof CommandExecute && enabled) {
                                cL.add(rankNeeded.getCommandPrefix() + c.getSimpleName().toLowerCase());
                                commands.put(rankNeeded.getCommandPrefix() + c.getSimpleName().toLowerCase(), new CommandObject((CommandExecute) o, rankNeeded.getLevel()));
				if (rankNeeded.getCommandPrefix() != PlayerGMRank.GM.getCommandPrefix() && rankNeeded.getCommandPrefix() != PlayerGMRank.NORMAL.getCommandPrefix()) { //add it again for GM
                                    commands.put("!" + c.getSimpleName().toLowerCase(), new CommandObject((CommandExecute) o, PlayerGMRank.GM.getLevel()));
				}
                            }
                        }
                    } catch (Exception ex) {
                        System.out.println("Command error: " + ex.getMessage());
                    }
                }
                Collections.sort(cL);
                commandList.put(rankNeeded.getLevel(), cL);
            } catch (Exception ex) {
                System.out.println("Command error: " + ex.getMessage());
            }
        }
    }

    public static void dropHelp(MapleClient c) {
	final StringBuilder sb = new StringBuilder("Command list: ");
	for (int i = 0; i <= c.getPlayer().getGMLevel(); i++) {
	    if (commandList.containsKey(i)) {
	        for (String s : commandList.get(i)) {
		    sb.append(s);
		    sb.append(" ");
	        }
	    }
	}
	c.getPlayer().dropMessage(6, sb.toString());
    }

    public static boolean processCommand(MapleClient c, String line, CommandType type) {
        if (c.getPlayer().getMapId() == 180000001 && !c.getPlayer().isGM()) {
            c.getPlayer().dropMessage(6, "You may not use commands in this map.");
            return true;
        }
        if (line.charAt(0) == PlayerGMRank.NORMAL.getCommandPrefix() || (c.getPlayer().getGMLevel() > PlayerGMRank.NORMAL.getLevel() && line.charAt(0) == PlayerGMRank.DONATOR.getCommandPrefix()) || line.charAt(0) == PlayerGMRank.SUPERDONATOR.getCommandPrefix()) {
            String[] splitted = line.split(" ");
            splitted[0] = splitted[0].toLowerCase();

            CommandObject co = commands.get(splitted[0]);
            if (co == null || co.getType() != type) {
                c.getPlayer().dropMessage(6, "That player command does not exist.");
                return true;
            }
            try {
                int ret = co.execute(c, splitted); //Don't really care about the return value. ;D
            } catch (Exception e) {
                c.getPlayer().dropMessage(6, "There was an error.");
                e.printStackTrace();
                if (c.getPlayer().isGM()) {
                    c.getPlayer().dropMessage(6, "Error: " + e);
                }
            }
            return true;
        }

        if (c.getPlayer().getGMLevel() > PlayerGMRank.NORMAL.getLevel()) {
            if (line.charAt(0) == PlayerGMRank.SUPERGM.getCommandPrefix() || line.charAt(0) == PlayerGMRank.INTERN.getCommandPrefix() || line.charAt(0) == PlayerGMRank.GM.getCommandPrefix() || line.charAt(0) == PlayerGMRank.ADMIN.getCommandPrefix() || line.charAt(0) == PlayerGMRank.GOD.getCommandPrefix()) { //Redundant for now, but in case we change symbols later. This will become extensible.
                String[] splitted = line.split(" ");
                splitted[0] = splitted[0].toLowerCase();

                CommandObject co = commands.get(splitted[0]);
                if (co == null) {
                    if (splitted[0].equals(line.charAt(0) + "help")) {
                        dropHelp(c);
		        return true;
		    }
                    c.getPlayer().dropMessage(6, "That command does not exist.");
                    return true;
                }
                if (c.getPlayer().getGMLevel() >= co.getReqGMLevel()) {
                    int ret = 0;
		    try {
			ret = co.execute(c, splitted);
		    } catch (ArrayIndexOutOfBoundsException x) {
			c.getPlayer().dropMessage(6, "The command was not used properly: " + x);
		    } catch (Exception e) {
			System.out.println("Command execution error: " + e.getMessage());
		    }
                    if (ret > 0 && c.getPlayer() != null) { //incase d/c after command or something
                        if (c.getPlayer().isGM()) {
                            logCommandToDB(c.getPlayer(), line, "gmlog");
			} else {
                            logCommandToDB(c.getPlayer(), line, "internlog");
			}
                    }
                } else {
                    c.getPlayer().dropMessage(6, "You do not have the privileges to use that command.");
                }
                return true;
            }
        }
        return false;
    }

    private static void logCommandToDB(MapleCharacter player, String command, String table) {
        PreparedStatement ps = null;
        try {
            ps = DatabaseConnection.getConnection().prepareStatement("INSERT INTO " + table + " (cid, command, mapid) VALUES (?, ?, ?)");
            ps.setInt(1, player.getId());
            ps.setString(2, command);
            ps.setInt(3, player.getMap().getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            System.out.println("Unable to log command.. Error: " + ex.getMessage());
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {//Err.. Fuck?

            }
        }
    }
    
    public static ArrayList<Pair<Integer, String>> getMobsIDsFromName(String search) {
        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File("wz/String.wz"));
        ArrayList<Pair<Integer, String>> retMobs = new ArrayList<Pair<Integer, String>>();
        MapleData data = dataProvider.getData("Mob.img");
        List<Pair<Integer, String>> mobPairList = new LinkedList<Pair<Integer, String>>();
        for (MapleData mobIdData : data.getChildren()) {
            int mobIdFromData = Integer.parseInt(mobIdData.getName());
            String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME");
            mobPairList.add(new Pair<Integer, String>(mobIdFromData, mobNameFromData));
        }
        for (Pair<Integer, String> mobPair : mobPairList) {
            if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) {
                retMobs.add(mobPair);
            }
        }
        return retMobs;
    }

    public static String getMobNameFromID(int id) {
        try {
            return "not coded yet";//MapleLifeFactory.getMonster(id).getName();
        } catch (Exception e) {
            return null; 
        }
    }
}
