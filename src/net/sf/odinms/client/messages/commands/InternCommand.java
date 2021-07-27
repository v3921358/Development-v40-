package net.sf.odinms.client.messages.commands;

import java.awt.Point;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.constants.ServerConstants;
import net.sf.odinms.constants.ServerConstants.PlayerGMRank;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.world.remote.WorldLocation;
import net.sf.odinms.provider.MapleData;
import net.sf.odinms.provider.MapleDataProvider;
import net.sf.odinms.provider.MapleDataProviderFactory;
import net.sf.odinms.provider.MapleDataTool;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MaplePortal;
import net.sf.odinms.server.MapleTrade;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MapleMonsterInformationProvider;
import net.sf.odinms.server.life.MapleNPC;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.server.maps.MapleReactor;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.Pair;
import net.sf.odinms.tools.StringUtil;

/**
 *
 * @author Emilyx3
 */
public class InternCommand {
    
    static boolean usedCommandIntern = false;

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.INTERN;
    }

    public static boolean executeInternCommand (MapleClient c, String[] splitted) {
        if (c.getPlayer().getGMLevel() >= 3) {
            MapleCharacter player = c.getPlayer();
            MapleCharacter victim;
            // MapleCharacter target;
            MapleMap targetmap;
            MapleMap map = c.getPlayer().getMap();
            StringBuilder builder = new StringBuilder();
            if (player.getGMLevel() < 6 && usedCommandIntern == false) {
//                    FileoutputUtil.log("GMLog.txt", "\r\nIGN: " + player.getName() + " || Command: " + InternCommand.joinStringFrom(splitted, 0) + " \r\n");
                    usedCommandIntern = true;
                    TimerManager.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                           usedCommandIntern = false;  
                        }
                    }, 10);
                }
            switch (splitted[0].substring(1).toLowerCase()) {
                // Start of Eric's Commands
                case "hide": 
                    player.toggleHide(false, player.isHidden() ? false : true);
                    return true;
                case "map":
                    if (Integer.parseInt(splitted[1]) == 682000700) {
                        return true; // no no
                    }
                    c.getPlayer().changeMap(Integer.parseInt(splitted[1]), 0);
                    return true;
                case "gmmap":
                    c.getPlayer().changeMap(180000000, 0);
                    return true;
                case "whereami":
                    c.getPlayer().dropMessage(5, "You are on map " + c.getPlayer().getMapId());
                    return true;
                case "say":
                    for (ChannelServer ch : ChannelServer.getAllInstances())
                        for (MapleCharacter chrs : ch.getPlayerStorage().getAllCharacters())
                            if (splitted.length > 1) {
                                chrs.dropMessage(6, player.getName() + " : " + StringUtil.joinStringFrom(splitted, 1));
                            } else {
                                c.getPlayer().dropMessage(6, "Syntax: say <message>");
                            }
                    return true;
                case "online":
                    for (ChannelServer ch : ChannelServer.getAllInstances()) {
                        c.getPlayer().dropMessage(6, "Characters connected to channel " + ch.getChannel() + ":");
                        c.getPlayer().dropMessage(6, ChannelServer.getInstance(ch.getChannel()).getPlayerStorage().getOnlinePlayers(true));
                    }
                    return true;
                case "onlinechannel":
                    c.getPlayer().dropMessage(6, "Characters connected to channel " + Integer.parseInt(splitted[1]) + ":");
                    c.getPlayer().dropMessage(6, ChannelServer.getInstance(c.getChannel()).getPlayerStorage().getOnlinePlayers(true));
                    return true;
                case "itemcheck":
                    if (splitted.length < 3 || splitted[1] == null || splitted[1].equals("") || splitted[2] == null || splitted[2].equals("")) {
                        c.getPlayer().dropMessage(6, "!itemcheck <playername> <itemid>");
                        return true;
                    } else {
                        int item = Integer.parseInt(splitted[2]);
                        MapleCharacter chr = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                        int itemamount = chr.getItemQuantity(item, true);
                        if (itemamount > 0) {
                            c.getPlayer().dropMessage(6, chr.getName() + " has " + itemamount + " (" + item + ").");
                        } else {
                            c.getPlayer().dropMessage(6, chr.getName() + " doesn't have (" + item + ")");
                        }
                    }
                    return true;
                case "song":
                    c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.musicChange(splitted[1]));
                    return true;
                case "charinfo":
                    builder = new StringBuilder();
                    MapleCharacter other = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (other == null) {
                        builder.append("...does not exist");
                        c.getPlayer().dropMessage(6, builder.toString());
                        return true;
                    }
                    builder.append(MapleClient.getLogMessage(other, ""));
                    builder.append(" at ").append(other.getPosition().x);
                    builder.append(" /").append(other.getPosition().y);

                    builder.append(" || HP : ");
                    builder.append(other.getHp());
                    builder.append(" /");
                    builder.append(other.getCurrentMaxHp());

                    builder.append(" || MP : ");
                    builder.append(other.getMp());
                    builder.append(" /");
                    builder.append(other.getCurrentMaxMp());

                    builder.append(" || WATK : ");
                    builder.append(other.getTotalWatk());
                    builder.append(" || MATK : ");
                    builder.append(other.getTotalMagic());
                    builder.append(" || MAXDAMAGE : ");
                    builder.append(other.getCurrentMaxBaseDamage());

                    builder.append(" || STR : ");
                    builder.append(other.getStr());
                    builder.append(" || DEX : ");
                    builder.append(other.getDex());
                    builder.append(" || INT : ");
                    builder.append(other.getInt());
                    builder.append(" || LUK : ");
                    builder.append(other.getLuk());

                    builder.append(" || Total STR : ");
                    builder.append(other.getTotalStr());
                    builder.append(" || Total DEX : ");
                    builder.append(other.getTotalDex());
                    builder.append(" || Total INT : ");
                    builder.append(other.getTotalInt());
                    builder.append(" || Total LUK : ");
                    builder.append(other.getTotalLuk());

                    builder.append(" || EXP : ");
                    builder.append(other.getExp());
                    builder.append(" || MESO : ");
                    builder.append(other.getMeso());

                    builder.append(" || party : ");
                    builder.append(other.getParty() == null ? -1 : other.getParty().getId());

                    c.getPlayer().dropMessage(builder.toString());
                    return true;
                case "nearestportal":
                    MaplePortal portal = c.getPlayer().getMap().findClosestPortal(c.getPlayer().getPosition());
                    c.getPlayer().dropMessage(6, portal.getName() + " id: " + portal.getId() + " script: " + portal.getScriptName());
                    return true;
                case "spawndebug":
                    c.getPlayer().dropMessage(6, c.getPlayer().getMap().spawnDebug());
                    return true;
                case "fakerelog":
                    c.getSession().write(MaplePacketCreator.getCharInfo(player));
                    final MapleMap mapp = player.getMap();
                    mapp.removePlayer(player);
                    mapp.addPlayer(player);
                    return true;
                case "cleardrops":
                    map = player.getMap(); 
                    double range = Double.POSITIVE_INFINITY; 
                    java.util.List<MapleMapObject> items = map.getMapObjectsInRange(player.getPosition(), range, Arrays.asList(MapleMapObjectType.ITEM)); 
                    for (MapleMapObject itemmo : items) { 
                        map.removeMapObject(itemmo); 
                        map.broadcastMessage(MaplePacketCreator.removeItemFromMap(itemmo.getObjectId(), 0, player.getId())); 
                    } 
                    player.dropMessage("You have destroyed " + items.size() + " items on the ground.");
                    return true;
                case "goto":
                    HashMap<String, Integer> gotomaps = new HashMap<String, Integer>();
                    gotomaps.put("gmmap", 180000000);
                    gotomaps.put("southperry", 2000000);
                    gotomaps.put("amherst", 1010000);
                    gotomaps.put("henesys", 100000000);
                    gotomaps.put("ellinia", 101000000);
                    gotomaps.put("perion", 102000000);
                    gotomaps.put("kerning", 103000000);
                    gotomaps.put("harbor", 104000000);
                    gotomaps.put("sleepywood", 105000000);
                    gotomaps.put("florina", 110000000);
                    gotomaps.put("orbis", 200000000);
                    gotomaps.put("happyville", 209000000);
                    gotomaps.put("elnath", 211000000);
                    gotomaps.put("ludibrium", 220000000);
                    gotomaps.put("aquaroad", 230000000);
                    gotomaps.put("leafre", 240000000);
                    gotomaps.put("mulung", 250000000);
                    gotomaps.put("herbtown", 251000000);
                    gotomaps.put("omegasector", 221000000);
                    gotomaps.put("koreanfolktown", 222000000);
                    gotomaps.put("newleafcity", 600000000);
                    gotomaps.put("sharenian", 990000000);
                    gotomaps.put("pianus", 230040420);
                    gotomaps.put("horntail", 240060200);
                    gotomaps.put("chorntail", 240060201);
                    gotomaps.put("griffey", 240020101);
                    gotomaps.put("manon", 240020401);
                    gotomaps.put("zakum", 280030000);
                    gotomaps.put("czakum", 280030001);
                    gotomaps.put("papulatus", 220080001);
                    gotomaps.put("showatown", 801000000);
                    gotomaps.put("zipangu", 800000000);
                    gotomaps.put("ariant", 260000100);
                    gotomaps.put("nautilus", 120000000);
                    gotomaps.put("boatquay", 541000000);
                    gotomaps.put("malaysia", 550000000);
                    gotomaps.put("erev", 130000000);
                    gotomaps.put("ellin", 300000000);
                    gotomaps.put("kampung", 551000000);
                    gotomaps.put("singapore", 540000000);
                    gotomaps.put("amoria", 680000000);
                    gotomaps.put("timetemple", 270000000);
                    gotomaps.put("pinkbean", 270050100);
                    gotomaps.put("fm", 910000000);
                    gotomaps.put("freemarket", 910000000);
                    gotomaps.put("oxquiz", 109020001);
                    gotomaps.put("ola", 109030101);
                    gotomaps.put("fitness", 109040000);
                    gotomaps.put("snowball", 109060000);
                    gotomaps.put("golden", 950100000);
                    gotomaps.put("phantom", 610010000);
                    gotomaps.put("cwk", 610030000);
                    gotomaps.put("rien", 140000000);
                    gotomaps.put("edel", 310000000);
                    gotomaps.put("ardent", 910001000);
                    gotomaps.put("craft", 910001000);
                    gotomaps.put("pvp", 960000000);
                    gotomaps.put("future", 271000000);
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(6, "Syntax: !goto <mapname>");
                    } else {
                        if (gotomaps.containsKey(splitted[1])) {
                            targetmap = c.getChannelServer().getMapFactory().getMap(gotomaps.get(splitted[1]));
                            if (targetmap == null) {
                                c.getPlayer().dropMessage(6, "Map does not exist");
                                return true;
                            }
                            MaplePortal targetPortal = targetmap.getPortal(0);
                            c.getPlayer().changeMap(targetmap, targetPortal);
                        } else {
                            if (splitted[1].equals("locations")) {
                                c.getPlayer().dropMessage(6, "Use !goto <location>. Locations are as follows:");
                                for (String s : gotomaps.keySet()) {
                                    builder.append(s).append(", ");
                                }
                                c.getPlayer().dropMessage(6, builder.substring(0, builder.length() - 2));
                            } else {
                                c.getPlayer().dropMessage(6, "Invalid command syntax - Use !goto <location>. For a list of locations, use !goto locations.");
                            }
                        }
                    }
                    return true;
                case "monsterdebug":
                    range = Double.POSITIVE_INFINITY;
                    if (splitted.length > 1) {
                        //&& !splitted[0].equals("!killmonster") && !splitted[0].equals("!hitmonster") && !splitted[0].equals("!hitmonsterbyoid") && !splitted[0].equals("!killmonsterbyoid")) {
                        int irange = Integer.parseInt(splitted[1]);
                        if (splitted.length <= 2) {
                            range = irange * irange;
                        } else {
                            map = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(splitted[2]));
                        }
                    }
                    if (map == null) {
                        c.getPlayer().dropMessage(6, "Map does not exist");
                        return true;
                    }
                    MapleMonster mob;
                    for (MapleMapObject monstermo : map.getMapObjectsInRange(c.getPlayer().getPosition(), range, Arrays.asList(MapleMapObjectType.MONSTER))) {
                        mob = (MapleMonster) monstermo;
                        c.getPlayer().dropMessage(6, "Monster " + mob.toString());
                    }
                    return true;
                case "lookportals":
                    for (MaplePortal portal1l : c.getPlayer().getMap().getPortals()) {
                        c.getPlayer().dropMessage(5, "Portal: ID: " + portal1l.getId() + " script: " + portal1l.getScriptName() + " name: " + portal1l.getName() + " pos: " + portal1l.getPosition().x + "," + portal1l.getPosition().y + " target: " + portal1l.getTargetMapId() + " / " + portal1l.getTarget());
                    }
                    return true;
                case "mynpcpos":
                    Point pos = c.getPlayer().getPosition();
                    c.getPlayer().dropMessage(6, "X: " + pos.x + " | Y: " + pos.y + " | RX0: " + (pos.x + 50) + " | RX1: " + (pos.x - 50));
                    return true;
                case "warp":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    if (victim != null) {
                        if (splitted.length == 2) {
                            MapleMap target = victim.getMap();
                            c.getPlayer().changeMap(target, target.findClosestSpawnpoint(victim.getPosition()));
                        } else {
                            int mapid = Integer.parseInt(splitted[2]);
                            MapleMap target = ChannelServer.getInstance(c.getChannel()).getMapFactory().getMap(mapid);
                            victim.changeMap(target, target.getPortal(0));
                        }
                    } else {
                        try {
                            victim = c.getPlayer();
                            WorldLocation loc = c.getChannelServer().getWorldInterface().getLocation(splitted[1]);
                            if (loc != null) {
                                player.dropMessage("You will be cross-channel warped. This may take a few seconds.");
                                // WorldLocation loc = new WorldLocation(40000, 2);
                                MapleMap target = c.getChannelServer().getMapFactory().getMap(loc.map);
                                c.getPlayer().cancelAllBuffs();
                                String ip = c.getChannelServer().getIP(loc.channel);
                                c.getPlayer().getMap().removePlayer(c.getPlayer());
                                victim.setMap(target);
                                String[] socket = ip.split(":");
                                if (c.getPlayer().getTrade() != null) {
                                    MapleTrade.cancelTrade(c.getPlayer());
                                }
                                c.getPlayer().saveToDB(true);
                                if (c.getPlayer().getCheatTracker() != null)
                                    c.getPlayer().getCheatTracker().dispose();
                                ChannelServer.getInstance(c.getChannel()).removePlayer(c.getPlayer());
                                c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION);
                                try {
                                    MaplePacket packet = MaplePacketCreator.getChannelChange(ServerConstants.getIP(), Integer.parseInt(socket[1]));
                                    c.getSession().write(packet);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                int mapid = Integer.parseInt(splitted[1]);
                                MapleMap target = c.getChannelServer().getMapFactory().getMap(mapid);
                                c.getPlayer().changeMap(target, target.getPortal(0));
                            }
                        } catch (Exception e) {
                            player.dropMessage("Something went wrong " + e.getMessage());
                        }
                    }
                    return true;
                case "warphere":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestSpawnpoint(c.getPlayer().getPosition()));
                    return true;
                case "warpmap":
                    for (MapleCharacter chr : player.getMap().getCharacters()) {
                        chr.changeMap(Integer.parseInt(splitted[1]), 0);
                    }
                    return true;
                case "lookup": 
                case "search":
                case "find":
                    if (splitted.length > 2) { 
                        String type = splitted[1]; 
                        String search = StringUtil.joinStringFrom(splitted, 2); 
                        MapleData data = null; 
                        MapleDataProvider dataProvider = MapleDataProviderFactory.getDataProvider(new File(System.getProperty("net.sf.odinms.wzpath") + "/" + "String.wz")); 
                        player.dropMessage("<<Type: " + type + " | Search: " + search + ">>"); 
                        if (type.equalsIgnoreCase("NPC") || type.equalsIgnoreCase("NPCS")) { 
                            List<String> retNpcs = new ArrayList<String>(); 
                            data = dataProvider.getData("Npc.img"); 
                            List<Pair<Integer, String>> npcPairList = new LinkedList<Pair<Integer, String>>(); 
                            for (MapleData npcIdData : data.getChildren()) { 
                                int npcIdFromData = Integer.parseInt(npcIdData.getName()); 
                                String npcNameFromData = MapleDataTool.getString(npcIdData.getChildByPath("name"), "NO-NAME"); 
                                npcPairList.add(new Pair<Integer, String>(npcIdFromData, npcNameFromData)); 
                            } 
                            for (Pair<Integer, String> npcPair : npcPairList) { 
                                if (npcPair.getRight().toLowerCase().contains(search.toLowerCase())) { 
                                    retNpcs.add(npcPair.getLeft() + " - " + npcPair.getRight()); 
                                } 
                            } 
                            if (retNpcs != null && retNpcs.size() > 0) { 
                                for (String singleRetNpc : retNpcs) { 
                                    player.dropMessage(singleRetNpc); 
                                } 
                            } else { 
                                player.dropMessage("No NPC's Found"); 
                            } 
                        } else if (type.equalsIgnoreCase("MAP") || type.equalsIgnoreCase("MAPS")) { 
                            List<String> retMaps = new ArrayList<String>(); 
                            data = dataProvider.getData("Map.img"); 
                            List<Pair<Integer, String>> mapPairList = new LinkedList<Pair<Integer, String>>(); 
                            for (MapleData mapAreaData : data.getChildren()) { 
                                for (MapleData mapIdData : mapAreaData.getChildren()) { 
                                    int mapIdFromData = Integer.parseInt(mapIdData.getName()); 
                                    String mapNameFromData = MapleDataTool.getString(mapIdData.getChildByPath("streetName"), "NO-NAME") + " - " + MapleDataTool.getString(mapIdData.getChildByPath("mapName"), "NO-NAME"); 
                                    mapPairList.add(new Pair<Integer, String>(mapIdFromData, mapNameFromData)); 
                                } 
                            } 
                            for (Pair<Integer, String> mapPair : mapPairList) { 
                                if (mapPair.getRight().toLowerCase().contains(search.toLowerCase())) { 
                                    retMaps.add(mapPair.getLeft() + " - " + mapPair.getRight()); 
                                } 
                            } 
                            if (retMaps != null && retMaps.size() > 0) { 
                                for (String singleRetMap : retMaps) { 
                                    player.dropMessage(singleRetMap); 
                                } 
                            } else { 
                                player.dropMessage("No Maps Found"); 
                            } 
                        } else if (type.equalsIgnoreCase("MOB") || type.equalsIgnoreCase("MOBS") || type.equalsIgnoreCase("MONSTER") || type.equalsIgnoreCase("MONSTERS")) { 
                            List<String> retMobs = new ArrayList<String>(); 
                            data = dataProvider.getData("Mob.img"); 
                            List<Pair<Integer, String>> mobPairList = new LinkedList<Pair<Integer, String>>(); 
                            for (MapleData mobIdData : data.getChildren()) { 
                                int mobIdFromData = Integer.parseInt(mobIdData.getName()); 
                                String mobNameFromData = MapleDataTool.getString(mobIdData.getChildByPath("name"), "NO-NAME"); 
                                mobPairList.add(new Pair<Integer, String>(mobIdFromData, mobNameFromData)); 
                            } 
                            for (Pair<Integer, String> mobPair : mobPairList) { 
                                if (mobPair.getRight().toLowerCase().contains(search.toLowerCase())) { 
                                    retMobs.add(mobPair.getLeft() + " - " + mobPair.getRight()); 
                                } 
                            } 
                            if (retMobs != null && retMobs.size() > 0) { 
                                for (String singleRetMob : retMobs) { 
                                    player.dropMessage(singleRetMob); 
                                } 
                            } else { 
                                player.dropMessage("No Mob's Found"); 
                            } 
                        } else if (type.equalsIgnoreCase("REACTOR") || type.equalsIgnoreCase("REACTORS")) { 
                            player.dropMessage("NOT ADDED YET"); 
                        } else if (type.equalsIgnoreCase("ITEM") || type.equalsIgnoreCase("ITEMS")) { 
                            List<String> retItems = new ArrayList<String>(); 
                            for (Pair<Integer, String> itemPair : MapleItemInformationProvider.getInstance().getAllItems()) { 
                                if (itemPair.getRight().toLowerCase().contains(search.toLowerCase())) { 
                                    retItems.add(itemPair.getLeft() + " - " + itemPair.getRight()); 
                                } 
                            } 
                            if (retItems != null && retItems.size() > 0) { 
                                for (String singleRetItem : retItems) { 
                                    player.dropMessage(singleRetItem); 
                                } 
                            } else { 
                                player.dropMessage("No Item's Found"); 
                            } 
                        } else if (type.equalsIgnoreCase("SKILL") || type.equalsIgnoreCase("SKILLS")) { 
                            List<String> retSkills = new ArrayList<String>(); 
                            data = dataProvider.getData("Skill.img"); 
                            List<Pair<Integer, String>> skillPairList = new LinkedList<Pair<Integer, String>>(); 
                            for (MapleData skillIdData : data.getChildren()) { 
                                int skillIdFromData = Integer.parseInt(skillIdData.getName()); 
                                String skillNameFromData = MapleDataTool.getString(skillIdData.getChildByPath("name"), "NO-NAME"); 
                                skillPairList.add(new Pair<Integer, String>(skillIdFromData, skillNameFromData)); 
                            } 
                            for (Pair<Integer, String> skillPair : skillPairList) { 
                                if (skillPair.getRight().toLowerCase().contains(search.toLowerCase())) { 
                                    retSkills.add(skillPair.getLeft() + " - " + skillPair.getRight()); 
                                } 
                            } 
                            if (retSkills != null && retSkills.size() > 0) { 
                                for (String singleRetSkill : retSkills) { 
                                    player.dropMessage(singleRetSkill); 
                                } 
                            } else { 
                                player.dropMessage("No Skills Found"); 
                            } 
                        } else { 
                            player.dropMessage("Sorry, that search call is unavailable"); 
                        } 
                    } else { 
                        player.dropMessage("Invalid search.  Proper usage: '!search <type> <search for>', where <type> is MAP, USE, ETC, CASH, EQUIP, MOB (or MONSTER), or SKILL."); 
                    } 
                    return true;
                case "killall":
                case "killalldrops":
			map = c.getPlayer().getMap();
			double rangez = Double.POSITIVE_INFINITY;
			if (splitted.length > 1) {
                            int irange = Integer.parseInt(splitted[1]);
                            rangez = irange * irange;
			}
			List<MapleMapObject> monsters = map.getMapObjectsInRange(c.getPlayer().getPosition(), rangez, Arrays.asList(MapleMapObjectType.MONSTER));
			for (MapleMapObject monstermo : monsters) {
				MapleMonster monster = (MapleMonster) monstermo;
				map.killMonster(monster, c.getPlayer(), splitted[0].equalsIgnoreCase("killalldrops"));
			}
                        player.dropMessage("Killed " + monsters.size() + " monsters <3");
                    return true;
                default:
                    if (c.getPlayer().getGMLevel() >= 4) {
                        return GMCommand.executeGMCommand(c, splitted);
                    } else {
                        c.getPlayer().dropMessage(5, splitted[0].substring(1) + " does not exist.");
                        return false;
                    }
            }
        } else {
            c.getPlayer().dropMessage(5, "You are not a GM Level 3 (Intern), how the fuck did you get this far?!");
            return true;
        }
    }
    
    static String joinStringFrom(String arr[], int start) {
        StringBuilder builder = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            builder.append(arr[i]);
            if (i != arr.length - 1) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }
    
    public static class WhoComparator implements Comparator<Pair<String, Long>>, Serializable {
        @Override
        public int compare(Pair<String, Long> o1, Pair<String, Long> o2) {
            if (o1.getRight() > o2.getRight()) {
                return 1;
            } else if (o1.getRight() == o2.getRight()) {
                return 0;
            } else {
                return -1;
            }
        }
    }
}
