package net.sf.odinms.client.messages.commands;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map.Entry;
import net.sf.odinms.client.Equip;
import net.sf.odinms.client.ISkill;
import net.sf.odinms.client.Item;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleRing;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.messages.CommandProcessorUtil;
import net.sf.odinms.constants.ServerConstants.PlayerGMRank;
import net.sf.odinms.net.RecvPacketOpcode;
import net.sf.odinms.net.SendPacketOpcode;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.scripting.portal.PortalScriptManager;
import net.sf.odinms.scripting.reactor.ReactorScriptManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleShopFactory;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MapleMonsterInformationProvider;
import net.sf.odinms.server.life.MapleNPC;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.StringUtil;

/**
 * @author: Eric
 * @rev: 3.9 - Moved several commands to GMCommand
 * 
 */
public class SuperGMCommand {
    
    static boolean usedCommandSuperGM = false;

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.SUPERGM;
    }
    
    public static boolean executeSuperGMCommand(MapleClient c, String[] splitted) {
        if (c.getPlayer().getGMLevel() >= PlayerGMRank.SUPERGM.getLevel()) {
            StringBuilder builder = new StringBuilder();
            MapleCharacter player = c.getPlayer();
            MapleCharacter chrs;
            MapleCharacter victim;
            MapleCharacter target;
            MapleMonster mob;
            double range = Double.POSITIVE_INFINITY;
            int damage;
            MapleMap map = c.getPlayer().getMap();
            Thread[] threads = new Thread[Thread.activeCount()];
            //byte ret;
            if (player.getGMLevel() < 6 && usedCommandSuperGM == false) {
//                    FileoutputUtil.log("GMLog.txt", "\r\nIGN: " + player.getName() + " || Command: " + InternCommand.joinStringFrom(splitted, 0) + " \r\n");
                    usedCommandSuperGM = true;
                    TimerManager.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                           usedCommandSuperGM = false;  
                        }
                    }, 10);
                }
            switch (splitted[0].substring(1).toLowerCase()) {
                // Start of Eric's Commands
                case "proitem":
                    try {
                        int itemid = Integer.parseInt(splitted[1]);
                        int stats = Integer.parseInt(splitted[2]);
                        Equip equip = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemid);
                        if (equip == null) {
                            c.getPlayer().dropMessage("Item does not exist.");
                        } else {
                            equip.makeProItem(c.getPlayer().getName(), (short) stats);
                            MapleInventoryManipulator.addbyItem(c, (Item) equip);
                            c.getPlayer().dropMessage("You just got a " + MapleItemInformationProvider.getInstance().getName(itemid) + "!");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return true;
                case "maxmeso":
                case "maxmesos":
                    player.gainMeso(Integer.MAX_VALUE - player.getMeso(), true);
                    return true;
                case "str":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    short amount = Short.parseShort(splitted[2]);
                    if (victim != null) {
                        if (victim.getStr() + amount < 32767) {
                            victim.setStr(amount);
                            player.dropMessage(5, "Added " + amount + " AP to " + victim.getName() + "'s " + splitted[0]);
                        } else
                            player.dropMessage(5, victim.getName() + "'s " + splitted[0] + " can't exceed 32767.");
                    } else
                        player.dropMessage(5, "Unable to find the player.");
                    return true;
                case "dex":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    amount = Short.parseShort(splitted[2]);
                    if (victim != null) {
                        if (victim.getDex() + amount < 32767) {
                            victim.setDex(amount);
                            player.dropMessage(5, "Added " + amount + " AP to " + victim.getName() + "'s " + splitted[0]);
                        } else
                            player.dropMessage(5, victim.getName() + "'s " + splitted[0] + " can't exceed 32767.");
                    } else
                        player.dropMessage(5, "Unable to find the player.");
                    return true;
                case "int":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    amount = Short.parseShort(splitted[2]);
                    if (victim != null) {
                        if (victim.getInt() + amount < 32767) {
                            victim.setInt(amount);
                            player.dropMessage(5, "Added " + amount + " AP to " + victim.getName() + "'s " + splitted[0]);
                        } else
                            player.dropMessage(5, victim.getName() + "'s " + splitted[0] + " can't exceed 32767.");
                    } else
                        player.dropMessage(5, "Unable to find the player.");
                    return true;
                case "luk":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    amount = Short.parseShort(splitted[2]);
                    if (victim != null) {
                        if (victim.getLuk() + amount < 32767) {
                            victim.setLuk(amount);
                            player.dropMessage(5, "Added " + amount + " AP to " + victim.getName() + "'s " + splitted[0]);
                        } else
                            player.dropMessage(5, victim.getName() + "'s " + splitted[0] + " can't exceed 32767.");
                    } else
                        player.dropMessage(5, "Unable to find the player.");
                    return true;
                case "whatsmyid":
                    player.dropMessage(6, "Your Current Player ID In The Database Is : " + player.getId());
                    return true;
                case "mypos":
                    player.dropMessage(6, "X = " + player.getPosition().x + ", Y = " + player.getPosition().y);
                    return true;
                case "giftnx":
                    c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]).modifyCSPoints(1, Integer.parseInt(splitted[2]) * 2);
                    player.dropMessage("Gifted " + Integer.parseInt(splitted[2]) + " NX to " + c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]).getName());
                    c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]).dropMessage(5, player.getName() + " has gifted you " + Integer.parseInt(splitted[1]) + " NX.");
                    return true;
                case "npc":
                    int npcId = Integer.parseInt(splitted[1]);
                    MapleNPC npc = MapleLifeFactory.getNPC(npcId);
                    if (npc != null && !npc.getName().equals("MISSINGNO")) {
                        npc.setPosition(c.getPlayer().getPosition());
                        npc.setCy(c.getPlayer().getPosition().y);
                        npc.setRx0(c.getPlayer().getPosition().x + 50);
                        npc.setRx1(c.getPlayer().getPosition().x - 50);
                        npc.setFh(c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId());
                        npc.setCustom(true);
                        c.getPlayer().getMap().addMapObject(npc);
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.spawnNPCRequest(npc));
                    } else {
                        player.dropMessage("You have entered an invalid Npc-Id");
                    }
                    return true;
                case "giveskill":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    ISkill skill = SkillFactory.getSkill(Integer.parseInt(splitted[2]));
                    byte level = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 3, 1);
                    byte masterlevel = (byte) CommandProcessorUtil.getOptionalIntArg(splitted, 4, 1);

                    if (level > skill.getMaxLevel()) {
                        level = (byte) skill.getMaxLevel();
                    }
                    if (masterlevel > skill.getMaxLevel()) {
                        masterlevel = (byte) skill.getMaxLevel();
                    }
                    victim.changeSkillLevel(skill, level, masterlevel);
                    return true;
                case "supermax":
                    c.getPlayer().maxAllSkills();
                    return true;
                case "ring":
                    if (splitted[1].equalsIgnoreCase("info")) {
                        player.dropMessage("Syntax: !ring <itemid> <partner ign>");
                        return true;
                    }
                    int itemId = Integer.parseInt(splitted[1]);
                    String partnerName = splitted[2];
                    int partnerId = MapleCharacter.getIdByName(partnerName, 0);
                    int ret = MapleRing.createRing(itemId, player, c.getChannelServer().getPlayerStorage().getCharacterById(partnerId));
                    if (ret <= 0) {
                        player.dropMessage("There was an unknown error.");
                        player.dropMessage("Make sure the person you are attempting to create a ring with is online.");
                    }
                    return true;
                case "gaincash":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(5, "Need amount.");
                        return true;
                    }
                    c.getPlayer().modifyCSPoints(1, Integer.parseInt(splitted[1]), true);
                    return true;
                case "gainmp":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(5, "Need amount.");
                        return true;
                    }
                    c.getPlayer().modifyCSPoints(2, Integer.parseInt(splitted[1]), true);
                    return true;
                case "reloadops":
                    SendPacketOpcode.reloadValues();
                    RecvPacketOpcode.reloadValues();
                    return true;
                case "reloaddrops":
                    MapleMonsterInformationProvider.getInstance().clearDrops();
                    ReactorScriptManager.getInstance().clearDrops();
                    return true;
                case "reloadportals":
                    PortalScriptManager.getInstance().clearScripts();
                    return true;
                case "reloadshops":
                    MapleShopFactory.getInstance().clear();
                    return true;
                case "reloadevents":
                    for (ChannelServer ch : ChannelServer.getAllInstances()) {
                        ch.reloadEvents();
                    }
                    return true;
                case "resetmap": // meh..todo?
                case "resetreactor":
                    c.getPlayer().getMap().resetReactors();
                    return true;
                case "sendallnote":
                    if (splitted.length >= 1) {
                        String text = StringUtil.joinStringFrom(splitted, 1);
                        for (MapleCharacter mch : c.getChannelServer().getPlayerStorage().getAllCharacters()) {
                            c.getPlayer().sendNote(mch.getName(), text);
                        }
                    } else {
                        c.getPlayer().dropMessage(6, "Use it like this, !sendallnote <text>");
                        return true;
                    }
                    return true;
                case "sendnote":
                    if (splitted.length >= 2) { // raaaight? :(
                        String text = StringUtil.joinStringFrom(splitted, 2);
                        victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                        c.getPlayer().sendNote(victim.getName(), text);
                    } else
                        c.getPlayer().dropMessage(6, "Use it like this, !sendnote <ign> <text>");
                    return true;
                default:
                    if (c.getPlayer().getGMLevel() >= 6) {
                        return AdminCommand.executeAdminCommand(c, splitted);
                    } else {
                        c.getPlayer().dropMessage(5, splitted[0].substring(1) + " does not exist.");
                        return false;
                    }
            }
        } else {
            c.getPlayer().dropMessage(5, "You are not a GM Level 5 (Super Game Master), how the fuck did you get this far?!");
            return true;
        }
    }
        
    public static class BookComparator implements Comparator<Entry<Integer, Integer>>, Serializable {

            @Override
            public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
                if (o1.getValue() > o2.getValue()) {
                    return 1;
                } else if (o1.getValue() == o2.getValue()) {
                    return 0;
                } else {
                    return -1;
                }
            }
    }
    
    public static int getOptionalIntArg(String splitted[], int position, int def) {
        if (splitted.length > position) {
            try {
                return Integer.parseInt(splitted[position]);
            } catch (NumberFormatException nfe) {
                return def;
            }
        }
        return def;
    }
}
