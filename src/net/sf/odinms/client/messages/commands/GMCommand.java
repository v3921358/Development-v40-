package net.sf.odinms.client.messages.commands;

import java.rmi.RemoteException;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.Item;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MapleJob;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.client.Skill;
import net.sf.odinms.client.SkillFactory;
import net.sf.odinms.client.messages.CommandProcessorUtil;
import net.sf.odinms.constants.ServerConstants.PlayerGMRank;
import net.sf.odinms.net.MaplePacket;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.net.handler.InsultBot;
import net.sf.odinms.net.handler.MLIABot;
import net.sf.odinms.net.handler.MapleFML;
import net.sf.odinms.scripting.event.EventManager;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.MapleShop;
import net.sf.odinms.server.MapleShopFactory;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MapleMonsterStats;
import net.sf.odinms.server.maps.MapleMap;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.StringUtil;

/**
 *
 * @author Eric
 * @rev: 3.4 - Commands from SuperGMCommand transferred. 
 * @rev: 3.4 - Fixed setOwner function disabled for GMLevel >= 5
 */
public class GMCommand {

    static boolean usedCommandGM;
    static int marriage_prompter = 0;
    static int marriage_prompter_vic = 0;
    static int promptMarriage;
    
    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.GM;
    }

    public static boolean executeGMCommand(MapleClient c, String[] splitted) {
        if (c.getPlayer().getGMLevel() >= PlayerGMRank.GM.getLevel()) {
            MapleCharacter player = c.getPlayer();
            final MapleCharacter playerf = player;
            MapleCharacter victim;// = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            //MapleCharacter chr;// = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
            EventManager em;// = c.getChannelServer().getEventSM().getEventManager(splitted[1]);
            MapleMap map = c.getPlayer().getMap();
            MapleCharacter target;
            MapleMonster mob;
            StringBuilder sb = new StringBuilder();
            Skill skill;
            if (player.getGMLevel() < 6 && usedCommandGM == false) {
//                    FileoutputUtil.log("GMLog.txt", "\r\nIGN: " + player.getName() + " || Command: " + InternCommand.joinStringFrom(splitted, 0) + " \r\n");
                    usedCommandGM = true;
                    TimerManager.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                           usedCommandGM = false;  
                        }
                    }, 10);
                }
            switch (splitted[0].substring(1).toLowerCase()) {
                case "getskill":
                    int skillId = Integer.parseInt(splitted[1]);
                    int level = SuperGMCommand.getOptionalIntArg(splitted, 2, 1);
                    int masterlevel = SuperGMCommand.getOptionalIntArg(splitted, 3, 1);
                    c.getPlayer().changeSkillLevel(SkillFactory.getSkill(skillId), level, masterlevel);
                    return true;
                case "gmshop":
                    MapleShopFactory.getInstance().getShop(1337).sendShop(c);
                    return true;
                case "fml":
                    for (ChannelServer ch : ChannelServer.getAllInstances())
                        for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters())
                            chr.dropMessage(6, MapleFML.getFML());
                    return true;
                case "mlia":
                    for (ChannelServer ch : ChannelServer.getAllInstances())
                        for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters())
                            chr.dropMessage(6, MLIABot.findMLIA());
                    return true;
                case "insult":
                    for (MapleCharacter chr : c.getChannelServer().getPlayerStorage().getAllCharacters())
                        chr.dropMessage(6, player.getName() + " : " + InsultBot.getInsult());
                    return true;
                case "spawn":
                    int mid = Integer.parseInt(splitted[1]);
                    int num = Math.min(SuperGMCommand.getOptionalIntArg(splitted, 2, 1), 500);
                    Integer hp = getNamedIntArg(splitted, 1, "hp");
                    Integer exp = getNamedIntArg(splitted, 1, "exp");
                    Double php = getNamedDoubleArg(splitted, 1, "php");
                    Double pexp = getNamedDoubleArg(splitted, 1, "pexp");

                    MapleMonster onemob = MapleLifeFactory.getMonster(mid);

                    int newhp = 0;
                    int newexp = 0;

                    double oldExpRatio = ((double) onemob.getHp() / onemob.getExp());

                    if (hp != null) {
                            newhp = hp.intValue();
                    } else if (php != null) {
                            newhp = (int) (onemob.getMaxHp() * (php.doubleValue() / 100));
                    } else {
                            newhp = onemob.getMaxHp();
                    }
                    if (exp != null) {
                            newexp = exp.intValue();
                    } else if (pexp != null) {
                            newexp = (int) (onemob.getExp() * (pexp.doubleValue() / 100));
                    } else {
                            newexp = onemob.getExp();
                    }

                    if (newhp < 1) {
                            newhp = 1;
                    }
                    double newExpRatio = ((double) newhp / newexp);
                    if (player.getGMLevel() < 6 && (newExpRatio < oldExpRatio && newexp > 0)) {
                        player.dropMessage("The new hp/exp ratio is better than the old one. (" + newExpRatio + " < " + oldExpRatio + ") Please don't do this");
                        return true;
                    }

                    MapleMonsterStats overrideStats = new MapleMonsterStats();
                    overrideStats.setHp(newhp);
                    overrideStats.setExp(newexp);
                    overrideStats.setMp(onemob.getMaxMp());

                    for (int i = 0; i < num; i++) {
                        MapleMonster spawn = MapleLifeFactory.getMonster(mid);
                        spawn.setHp(newhp);
                        spawn.setOverrideStats(overrideStats);
                        c.getPlayer().getMap().spawnMonsterOnGroudBelow(spawn, c.getPlayer().getPosition());
                    }
                    return true;
                case "clock":
                    player.getMap().broadcastMessage(MaplePacketCreator.getClock(SuperGMCommand.getOptionalIntArg(splitted, 1, 60)));
                    return true;
                case "ox":
                    if (splitted.length == 1)
                        player.dropMessage(5, "Invalid command, try !ox help");
                    switch(splitted[1].toLowerCase()) {
                        case "instructions":
                        case "inst":
                            //c.getSession().write(MaplePacketCreator.getEventInstructions());
                            c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.getEventInstructions());
                            break;
                        case "ask":
                        case "quiz":
                            if (splitted.length < 5) {
                                System.out.println("Invalid Syntax. Syntax: !oxquiz <question set (0/1/2/etc)> <question (0/1/2/etc)> <ask (true/false)>");
                                return true;
                            }
                            int questionSet = Integer.parseInt(splitted[2]);
                            int question = Integer.parseInt(splitted[3]);
                            boolean ask = Boolean.parseBoolean(splitted[4]);
                            c.getSession().write(MaplePacketCreator.showOXQuiz(questionSet, question, ask));
                            break;
                        case "open":
                        case "close":
                            if (c.getChannelServer().getEventMap() > 0 && splitted[1].equalsIgnoreCase("open")) {
                                c.getChannelServer().setEventMap(-1);
                                c.getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(6, "The entrance to the OX Quiz has closed."));
                                return true; // wait.. this shouldn't happen! :(
                            }
                            c.getChannelServer().setEventMap(splitted[1].equalsIgnoreCase("open") ? 109020001 : -1);
                            c.getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(6, splitted[1].equalsIgnoreCase("open") ? "Hello Tespia! Let's play an OX Quiz! Type @joinevent to enter!" : "The entrance to the OX Quiz has closed."));
                            break;
                        case "warp":
                        case "me":
                            c.getPlayer().changeMap(109020001);
                            break;
                        case "help":
                        default:
                            player.dropMessage(5, "!ox <instructions/ask/help/open/close/warp> - Each command will give details if it requires an input otherwise executes.");
                            break;
                    }
                    return true;
                case "coconut":
                    if (splitted.length == 1)
                        c.getPlayer().dropMessage(6, "Invalid syntax. Syntax: !coconut <open/close/start/warp>");
                    switch(splitted[1].toLowerCase()) {
                        case "open":
                        case "close":
                            if (c.getChannelServer().getEventMap() > 0 && splitted[1].equalsIgnoreCase("open")) {
                                c.getChannelServer().setEventMap(-1);
                                c.getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(6, "The entrance to the Coconut Season has closed."));
                                return true; // wait.. this shouldn't happen! :(
                            }
                            c.getChannelServer().setEventMap(splitted[1].equalsIgnoreCase("open") ? 109020001 : -1);
                            c.getChannelServer().broadcastPacket(MaplePacketCreator.serverNotice(6, splitted[1].equalsIgnoreCase("open") ? "Hello Tespia! Let's play the Coconut Season! Type @joinevent to enter!" : "The entrance to the Coconut Season has closed."));
                            break;
                        case "start":
                            for (MapleCharacter chr : c.getPlayer().getMap().getCharacters()) {
                                c.getPlayer().getMap().startEvent(chr);
                            }
                            break;
                        case "warp":
                            c.getPlayer().changeMap(109080000);
                            break;
                        default:
                            c.getPlayer().dropMessage(6, "Invalid syntax. Syntax: !coconut <open/close/start/warp>");
                            break;
                    }
                    return true;
                case "gainmeso":
                case "meso":
                    c.getPlayer().gainMeso(Integer.parseInt(splitted[1]), true);
                    return true;
                case "heal":
                    if (splitted.length != 2) {
                        player.setHp(player.getMaxHp());
                        player.updateSingleStat(MapleStat.HP, player.getHp());
                        player.setMp(player.getMaxMp());
                        player.updateSingleStat(MapleStat.MP, player.getMp());
                        return true;
                    }
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.setHp(victim.getMaxHp());
                    victim.updateSingleStat(MapleStat.HP, victim.getHp());
                    victim.setMp(victim.getMaxMp());
                    victim.updateSingleStat(MapleStat.MP, victim.getMp());
                    return true;
                case "zakum":
                    player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(8800000), player.getPosition());
                    for (int x = 8800003; x < 8800011; x++) {
                        player.getMap().spawnMonsterOnGroudBelow(MapleLifeFactory.getMonster(x), player.getPosition());
                    }
                    return true;
                case "reloadmap":
                    c.getChannelServer().getMapFactory().disposeMap(player.getMapId());
                    player.dropMessage("The map has been reloaded and disposed.");
                    return true;
                case "buffme":
                    final int[] array = {5001000, 5001002, 5001003, 9101008, 2001002, 1101007, 1005, 2301003, 5121009, 1111002, 4111001, 4111002, 4211003, 4211005, 1321000, 2321004, 3121002};
                    for (int iahhgs : array) {
                        SkillFactory.getSkill(iahhgs).getEffect(SkillFactory.getSkill(iahhgs).getMaxLevel()).applyTo(player);
                    }
                    return true;
                case "healmap":
                    for (MapleCharacter mch : player.getMap().getCharacters()) {
                        if (mch != null) {
                           mch.setHp(mch.getMaxHp());
                           mch.updateSingleStat(MapleStat.HP, mch.getHp());
                           mch.setMp(mch.getMaxMp());
                           mch.updateSingleStat(MapleStat.MP, mch.getMp());
                        }
                    }
                    return true;
                case "dc":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[splitted.length - 1]);
                    if (victim != null && c.getPlayer().getGMLevel() > victim.getGMLevel()) {
                        victim.getClient().getSession().close();
                        victim.getClient().disconnect();
                    } else {
                        c.getPlayer().dropMessage(6, "The victim does not exist.");
                    }
                    return true;
                case "kill":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(6, "Syntax: !kill <player>");
                        return true;
                    }
                    victim = null;
                    for (int i = 1; i < splitted.length; i++) {
                        try {
                            victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[i]);
                        } catch (Exception e) {
                            c.getPlayer().dropMessage(6, "Player " + splitted[i] + " not found.");
                        }
                        if (player.getGMLevel() > victim.getGMLevel()) {
                            victim.setHp(0);
                            victim.setMp(0);
                            victim.updateSingleStat(MapleStat.HP, 0);
                            victim.updateSingleStat(MapleStat.MP, 0);
                        }
                    }
                    return true;
                case "resetstats":
                    c.getPlayer().setStr(4);
                    c.getPlayer().updateSingleStat(MapleStat.STR, c.getPlayer().getStr());
                    c.getPlayer().setDex(4);
                    c.getPlayer().updateSingleStat(MapleStat.DEX, c.getPlayer().getDex());
                    c.getPlayer().setInt(4);
                    c.getPlayer().updateSingleStat(MapleStat.INT, c.getPlayer().getInt());
                    c.getPlayer().setLuk(4);
                    c.getPlayer().updateSingleStat(MapleStat.LUK, c.getPlayer().getLuk());
                    return true;
                case "maxstats":
                case "maxall":
                    c.getPlayer().setStr(32767);
                    c.getPlayer().updateSingleStat(MapleStat.STR, c.getPlayer().getStr());
                    c.getPlayer().setDex(32767);
                    c.getPlayer().updateSingleStat(MapleStat.DEX, c.getPlayer().getDex());
                    c.getPlayer().setInt(32767);
                    c.getPlayer().updateSingleStat(MapleStat.INT, c.getPlayer().getInt());
                    c.getPlayer().setLuk(32767);
                    c.getPlayer().updateSingleStat(MapleStat.LUK, c.getPlayer().getLuk());
                    c.getPlayer().setHp(30000);
                    c.getPlayer().updateSingleStat(MapleStat.HP, c.getPlayer().getHp());
                    c.getPlayer().setMaxHp(30000);
                    c.getPlayer().updateSingleStat(MapleStat.MAXHP, c.getPlayer().getMaxHp());
                    c.getPlayer().setMp(30000);
                    c.getPlayer().updateSingleStat(MapleStat.MP, c.getPlayer().getMp());
                    c.getPlayer().setMaxMp(30000);
                    c.getPlayer().updateSingleStat(MapleStat.MAXMP, c.getPlayer().getMaxMp());
                    return true;
                case "fame":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(6, "Syntax: !fame <player> <amount>");
                        return true;
                    }
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    int fame;
                    try {
                        fame = Integer.parseInt(splitted[2]);
                    } catch (NumberFormatException nfe) {
                        c.getPlayer().dropMessage(6, "Invalid Number...");
                        return true;
                    }
                    if (victim != null && player.getGMLevel() >= victim.getGMLevel()) {
                        victim.setFame(fame); // what idiot adds? it's set :(
                        victim.updateSingleStat(MapleStat.FAME, victim.getFame());
                    }
                    return true;
                case "ap":
                    player.setRemainingAp(Integer.parseInt(splitted[1]));
                    player.updateSingleStat(MapleStat.AVAILABLEAP, player.getRemainingAp());
                    return true;
                case "sp":
                    c.getPlayer().setRemainingSp(CommandProcessorUtil.getOptionalIntArg(splitted, 1, 1));
                    c.getPlayer().updateSingleStat(MapleStat.AVAILABLESP, 0); // we don't care the value here
                    return true;
                case "warpportal":
                    player.changeMap(Integer.parseInt(splitted[1]), Integer.parseInt(splitted[2]));
                    return true;
                case "job":
                    c.getPlayer().changeJob(MapleJob.getById(Integer.parseInt(splitted[1])));
                    return true;
                case "level":
                    c.getPlayer().setLevel(Integer.parseInt(splitted[1]) <= 1 ? 0 : (Integer.parseInt(splitted[1]) - 2));
                    if (Integer.parseInt(splitted[1]) > 1)
                        c.getPlayer().levelUp();
                    c.getPlayer().levelUp();
                    if (c.getPlayer().getExp() != 0) {
                        c.getPlayer().setExp(0);
                        c.getPlayer().updateSingleStat(MapleStat.EXP, 0);
                    }
                    return true;
                case "item":
                    short quantity = (short) SuperGMCommand.getOptionalIntArg(splitted, 2, 1);
                    MapleInventoryManipulator.addById(c, Integer.parseInt(splitted[1]), quantity, c.getPlayer().getName() + "used !item with quantity " + quantity);
                    return true;
                case "drop":
                    MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                    int itemId = Integer.parseInt(splitted[1]);
                    quantity = (short) SuperGMCommand.getOptionalIntArg(splitted, 2, 1);
                    IItem toDrop;
                    if (ii.getInventoryType(itemId) == MapleInventoryType.EQUIP)
                        toDrop = ii.getEquipById(itemId);
                    else
                        toDrop = new Item(itemId, (byte) 0, (short) quantity);
                    StringBuilder logMsg = new StringBuilder("Created by ");
                    logMsg.append(c.getPlayer().getName());
                    logMsg.append(" using !drop. Quantity: ");
                    logMsg.append(quantity);
                    toDrop.log(logMsg.toString(), false);
                    toDrop.setOwner(player.getName());
                    c.getPlayer().getMap().spawnItemDrop(c.getPlayer(), c.getPlayer(), toDrop, c.getPlayer().getPosition(), true, true);
                    return true;
                case "shop":
                    MapleShopFactory sfact = MapleShopFactory.getInstance();
                    MapleShop shop = sfact.getShop(SuperGMCommand.getOptionalIntArg(splitted, 1, 1));
                    shop.sendShop(c);
                    return true;
                case "killmap":
                    for (MapleCharacter map2 : c.getPlayer().getMap().getCharacters()) {
                        if (map2 != null && map2 != player) {
                            if (player.getGMLevel() < 6 && map2.isGM()) {
                                player.dropMessage(map2.getName() + " is a GM.");
                            } else {
                                map2.setHp(0);
                                map2.setMp(0);
                                map2.updateSingleStat(MapleStat.HP, 0);
                                map2.updateSingleStat(MapleStat.MP, 0);
                            }
                        }
                    }
                    return true;
                case "notice":
                    int joinmod = 1;
                    int range = -1;
                    if (splitted[1].equals("m")) {
                        range = 0;
                    } else if (splitted[1].equals("c")) {
                        range = 1;
                    } else if (splitted[1].equals("w")) {
                        range = 2;
                    }
                    int tfrom = 2;
                    if (range == -1) {
                        range = 2;
                        tfrom = 1;
                    }
                    int type = getNoticeType(splitted[tfrom]);
                    if (type == -1) {
                        type = 0;
                        joinmod = 0;
                    }
                    String prefix = "";
                    if (splitted[tfrom].equals("nv")) {
                        prefix = "[Notice] ";
                    }
                    joinmod += tfrom;
                    MaplePacket packet = MaplePacketCreator.serverNotice(type, prefix + StringUtil.joinStringFrom(splitted, joinmod));
                    if (range == 0) {
                        c.getPlayer().getMap().broadcastMessage(packet);
                    } else if (range == 1) {
                        ChannelServer.getInstance(c.getChannel()).broadcastPacket(packet);
                    } else if (range == 2) {
                        try {
                            ChannelServer.getInstance(c.getChannel()).getWorldInterface().broadcastMessage(c.getPlayer().getName(), packet.getBytes());
                        } catch (RemoteException e) {
                            c.getChannelServer().reconnectWorld();
                        }
                    }
                    return true;
                case "whatsmyip":
                    c.getPlayer().dropMessage(5, "IP: " + c.getSession().getRemoteAddress().toString().split(":")[0]);
                    return true;
                case "tdrops":
                    c.getPlayer().getMap().toggleDrops();
                    return true;
                default:
                    if (c.getPlayer().getGMLevel() >= 5) {
                        return SuperGMCommand.executeSuperGMCommand(c, splitted);
                    } else {
                        c.getPlayer().dropMessage(5, splitted[0].substring(1) + " does not exist.");
                        return false;
                    }
            }
        } else {
            c.getPlayer().dropMessage(5, "You are not a GM Level 4 (Game Master), how the fuck did you get this far?!");
            return true;
        }
    }
    
    static int getNoticeType(String typestring) {
        switch (typestring) {
            case "n":
                return 0;
            case "p":
                return 1;
            case "l":
                return 2;
            case "nv":
                return 5;
            case "v":
                return 5;
            case "b":
                return 6;
            default:
                return -1;
        }
    }
    
    static String getNamedArg(String splitted[], int startpos, String name) {
        for (int i = startpos; i < splitted.length; i++) {
                if (splitted[i].equalsIgnoreCase(name) && i + 1 < splitted.length) {
                    return splitted[i + 1];
                }
        }
        return null;
    }
    
    static Integer getNamedIntArg(String splitted[], int startpos, String name) {
        String arg = getNamedArg(splitted, startpos, name);
        if (arg != null) {
                try {
                    return Integer.parseInt(arg);
                } catch (NumberFormatException nfe) {
                    // swallow - we don't really care
                }
        }
        return null;
    }
    
    static Double getNamedDoubleArg(String splitted[], int startpos, String name) {
        String arg = getNamedArg(splitted, startpos, name);
        if (arg != null) {
                try {
                        return Double.parseDouble(arg);
                } catch (NumberFormatException nfe) {
                        // swallow - we don't really care
                }
        }
        return null;
    }
}
