package net.sf.odinms.client.messages.commands;

import java.awt.Point;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.messages.MessageCallback;
import net.sf.odinms.client.messages.ServernoticeMapleClientMessageCallback;
import net.sf.odinms.constants.ServerConstants;
import net.sf.odinms.constants.ServerConstants.PlayerGMRank;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.scripting.npc.NPCScriptManager;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.server.maps.SavedLocationType;
import net.sf.odinms.tools.MaplePacketCreator;

public class PlayerCommand {
    
    /**
     * 
     * @author: Eric
     * @param: <All commands updated and/or coded by Eric.
     * @return: Use only for Development v40 Beta
     */ 
   
    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.NORMAL;
    }
    
    private static ResultSet ranking(boolean gm) {
        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps;
            if (!gm)
                ps = con.prepareStatement("SELECT level, name, job FROM characters WHERE gm < 3 ORDER BY level DES LIMIT 10");
            else
                ps = con.prepareStatement("SELECT name, gm FROM characters WHERE gm >= 3");
            return ps.executeQuery(); 
        } catch (SQLException ex) {}
        return null;
    }

    
    public static boolean executePlayerCommands(MapleClient c, String[] splitted) {
        final MapleCharacter player = c.getPlayer();
        MapleCharacter victim;
        MessageCallback mc = new ServernoticeMapleClientMessageCallback(player.getClient()); // #v62dayz
        if (!player.isGM() && ServerConstants.isJail(c.getPlayer().getMapId())) {
            mc.dropMessage(1, "You may not use commands in this map.");
            return true;
        }
        if (!player.isGM() && c.getPlayer().getMapId() == 0) {// && c.getPlayer().inJQ()) {
            switch (splitted[0].substring(1).toLowerCase()) {
                case "exit":
                    if (ServerConstants.isJail(c.getPlayer().getMapId())) {
                        player.dropMessage(6, "Nice try. :)");
                        return true;
                    }
                    player.changeMap(100000000); // should i give a choice fm/henesys?
                    return true;
                default: 
                    player.dropMessage(1, "You can't use @commands during a Jump Quest. To exit the Jump Quest, type @exit.");
                    return false;
            }
        }
        switch (splitted[0].substring(1).toLowerCase()) {
            case "commands":
            case "help":
            case "helpmeplz":
            case "listcommands":
                player.dropMessage(6, "Development Commands Coming Soon! Find the hidden ones (:");
                return true;
            case "storage":
                player.getStorage().sendStorage(c, 22000);
                return true;
            case "ranking":
                ResultSet rs;
                rs = ranking(false);
                player.dropMessage("Top 10 Players of " + ServerConstants.SERVER_NAME + ":");
                int zzz = 1;
                try {
                    while (rs.next()) {
                        String job = getJobyNameById(rs.getInt("job"));
                        player.dropMessage(zzz + ". " + rs.getString("name") + " : Job: " + job + " |  Level: " + rs.getInt("level"));
                        zzz++;
                    }
                } catch (SQLException e) {}
                return true;
            case "relog":
                c.getSession().write(MaplePacketCreator.getCharInfo(player));
                player.getMap().removePlayer(player);
                player.getMap().addPlayer(player);
                return true;
            case "home":
                player.changeMap(100000000, 0);
                player.dropMessage(5, "Welcome home, " + player.getName() + "!");
                return true;
            case "partyfix":
                player.setParty(null); 
                player.dropMessage("Please Relog or CC to finish changes."); 
                return true;
            case "bosshp":
                List<MapleMapObject> mobs = c.getPlayer().getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
                for (MapleMapObject mob : mobs) {
                    MapleMonster m = (MapleMonster) mob;
                    if (m.isBoss()) {
                        player.dropMessage("Boss: " + m.getName() + " | HP: " + m.getHp() + "/" + m.getMaxHp() + "");
                    }
                }
                return true;
            case "mobhp":
                MapleMap map = player.getMap();
                List<MapleMapObject> monsters = map.getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
                for (MapleMapObject monstermo : monsters) {
                    MapleMonster monster = (MapleMonster) monstermo;
                    player.dropMessage(6, "Monster: " + monster.getName() + " | HP: " + monster.getHp() + "/" + monster.getMaxHp());
                }
                return true;
            case "emo":
                if (player.isAlive()) {
                    player.setHp(0);
                    player.updateSingleStat(MapleStat.HP, 0);
                    c.getPlayer().dropMessage(0, "Congratulations, You have just gone Emo.");
                }
                return true;
            case "rates":
                int exp = ServerConstants.EXP_RATE;
                int meso = ServerConstants.MESO_RATE;
                player.dropMessage(ServerConstants.SERVER_NAME + "'s Rates: EXP: " + exp + "x | MESO: " + meso + "x");
                return true;
            case "expfix":
                player.setExp(0);
                player.updateSingleStat(MapleStat.EXP, player.getExp());
                player.dropMessage(6, "Your EXP has been reset to 0% - you're now fixed!");
                return true;
            case "clear":
                //if (!player.Spam(60000, 55) || player.isGM()) { // I like @clear better so let's skip check for GMs
                if (player.isGM()) { // for now
                    map = player.getMap(); 
                    double range = Double.POSITIVE_INFINITY; 
                    java.util.List<MapleMapObject> items = map.getMapObjectsInRange(player.getPosition(), range, Arrays.asList(MapleMapObjectType.ITEM)); 
                    for (MapleMapObject itemmo : items) { 
                        map.removeMapObject(itemmo); 
                        map.broadcastMessage(MaplePacketCreator.removeItemFromMap(itemmo.getObjectId(), 0, player.getId())); 
                    }
                    player.dropMessage("Drops Cleared.");
                } else
                    player.dropMessage(5, "Only GMs may @clear for now, sorry!");
                    //player.dropMessage(5, "You may only use this command every minute.");
                return true;
            case "kin":
            case "nimakin":
            case "styler":
                NPCScriptManager.getInstance().start(c, 9900000);
                return true;
                case "gmplz":
            case "makemeagm":
                c.getPlayer().setHp(0);
                c.getPlayer().updateSingleStat(MapleStat.HP, 0);
                c.getPlayer().dropMessage(1, "-TrollFace-");
                c.getPlayer().dropMessage(1, "CAUSE I KEEL JOO!");
                c.getPlayer().dropMessage(1, "That's too bad,");
                c.getPlayer().dropMessage(1, "Not even a little bit?");
                c.getPlayer().dropMessage(1, "You sure?");
                c.getPlayer().dropMessage(1, "Still not mad?!");
                c.getPlayer().dropMessage(1, "UMadYet?");
                c.getPlayer().dropMessage(1, "UMad?");
                c.getPlayer().dropMessage(1, "NOOB!!!");
                c.getPlayer().dropMessage(1, "NOOB!!!");
                c.getPlayer().dropMessage(1, "Wait.. you like JB and are my bitch? AW HEEEEEELLL NO!");
                c.getPlayer().dropMessage(1, player.getName() + " is Eric's bitch."); // yep :)
                c.getPlayer().dropMessage(1, "Lol, you like JB.. nub. ");
                c.getPlayer().dropMessage(1, player.getName() + " loves Justin Bieber!");
                c.getPlayer().dropMessage(1, "You have to press 'OK' to my jokes, hahaha!");
                c.getPlayer().dropMessage(1, "Eric was a Troll Master, is he still?");
                c.getPlayer().dropMessage(1, "Do you like being called a noob?");
                c.getPlayer().dropMessage(1, "NOOB!!!");
                c.getPlayer().dropMessage(1, "NOOB!!!");
                c.getPlayer().dropMessage(1, "You think you had a chance? hahaha ouch!");
                c.getPlayer().dropMessage(1, "Like, really.. You typed this<3");
                c.getPlayer().dropMessage(1, "so iherduliekmudkipz");
                c.getPlayer().dropMessage(1, "YUISWANTGM?");
                c.getPlayer().dropMessage(1, "NOOB!!!");
                c.getPlayer().dropMessage(1, "#YoloSwag420"); //LOL.
                return true;
            case "save":
                //if (player.Spam(60000, 1)) { // save every minute sounds nice
                //    player.dropMessage("Please don't flood our database.");
                //} else {
                    player.saveToDB(true);
                    player.dropMessage("Save Complete.");
                //}
                return true;
            case "fm": // TODO: Multi-FM
                c.getPlayer().saveLocation(SavedLocationType.FREE_MARKET);
                map = c.getChannelServer().getMapFactory().getMap(100000110);
                c.getPlayer().changeMap(map, map.getPortal(0));
                return true;
            case "dispose":
                NPCScriptManager.getInstance().dispose(c);
                c.getSession().write(MaplePacketCreator.enableActions());
                player.dropMessage("Done.");
                return true;
            case "go":
                HashMap<String, Integer> maps = new HashMap<String, Integer>();
                        maps.put("henesys", 100000000);
                        maps.put("ellinia", 101000000);
                        maps.put("perion", 102000000);
                        maps.put("kerning", 103000000);
                        maps.put("lith", 104000000);
                        maps.put("amoria", 680000000);
                        maps.put("sleepywood", 105040300); 
                        maps.put("florina", 110000000);
                        maps.put("orbis", 200000000);
                        maps.put("happy", 209000000);
                        maps.put("elnath", 211000000);
                        maps.put("ereve", 130000000);
                        maps.put("ludi", 220000000);
                        maps.put("omega", 221000000);
                        maps.put("korean", 222000000);
                        maps.put("aqua", 230000000);
                        maps.put("maya", 100000001);
                        maps.put("leafre", 240000000);
                        maps.put("mulung", 250000000);
                        maps.put("herb", 251000000);
                        maps.put("nlc", 600000000);
                        maps.put("shrine", 800000000);
                        maps.put("showa", 801000000);
                        maps.put("fm", 100000110);
                    if (splitted.length != 2) {
                        StringBuilder builder = new StringBuilder("Syntax: @go <mapname>");
                        for (String mapss : maps.keySet()) {
                            if (1 % 10 == 0) {// 10 maps per line
                                player.dropMessage(builder.toString());
                            } else {
                                builder.append(mapss).append(", ");
                            }
                        }
                        player.dropMessage(builder.toString());
                    } else if (maps.containsKey(splitted[1])) {
                        int map_ = maps.get(splitted[1]);
                        if (map_ == 910000000) {
                            player.saveLocation(SavedLocationType.FREE_MARKET);
                        }
                        player.changeMap(map_);
                    } else {
                        StringBuilder builder = new StringBuilder("Syntax: @go <mapname>");
                        for (String mapss : maps.keySet()) {
                            if (1 % 10 == 0) {// 10 maps per line 
                                player.dropMessage(builder.toString());
                            } else {
                                builder.append(mapss).append(", ");
                            }
                        }
                        player.dropMessage(builder.toString());
                    }
                    maps.clear();
                return true;
            case "online":
            case "connected":
                for (ChannelServer cs : ChannelServer.getAllInstances()) {
                    c.getPlayer().dropMessage(6, "Total Players Online: " + cs.getPlayerStorage().getOnlinePlayers(false));
                }
                c.getPlayer().dropMessage(6, "Characters on Channel " + c.getChannel() + ":");
                c.getPlayer().dropMessage(6, ChannelServer.getInstance(c.getChannel()).getPlayerStorage().getOnlinePlayers(false));
                return true;
            default:
                c.getPlayer().dropMessage(5, splitted[0].substring(1) + " does not exist.");
                return false;
        }
    }
    
    /**
     * @author: Eric
     * @param id - Gets the JOBID
     * @return - Returns the String of the Job rather than an enum.
     */

    public static String getJobyNameById(int id) {
        switch(id) {
            case 100:
                return "Warrior"; // Warrior
            case 110:
                return "Fighter";
            case 111:
                return "Crusader";
            case 112:
                return "Hero";
            case 120:
                return "Page";
            case 121:
                return "White Knight";
            case 122:
                return "Paladin";
            case 130:
                return "Spearman";
            case 131:
                return "Dragon Knight";
            case 132:
                return "Dark Knight";
            case 200:
                return "Magician"; // Magician
            case 210:
                return "Wizard (Fire, Poison)";
            case 211:
                return "Mage (Fire, Poison)"; 
            case 212:
                return "Arch Mage (Fire, Poison)";
            case 220:
                return "Wizard (Ice, Lightninig)";
            case 221:
                return "Mage (Ice, Lightning)";
            case 222:
                return "Arch Mage (Ice, Lightning)";
            case 230:
                return "Cleric";
            case 231:
                return "Priest";
            case 232:
                return "Bishop";
            case 300:
                return "Archer"; // Bowman
            case 310:
                return "Hunter";
            case 311:
                return "Ranger";
            case 312:
                return "Bowmaster";
            case 320:
                return "Crossbowman";
            case 321:
                return "Sniper";
            case 322:
                return "Marksman";
            case 400: 
                return "Rogue"; // Thief
            case 410:
                return "Assassin";
            case 411:
                return "Hermit";
            case 412:
                return "Night Lord";
            case 420:
                return "Bandit";
            case 421:
                return "Chief Bandit";
            case 422:
                return "Shadower";
            case 500:
                return "GM";
            default:
                return "Beginner";
        }
    }
}
