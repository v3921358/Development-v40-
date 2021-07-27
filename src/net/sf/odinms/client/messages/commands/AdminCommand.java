package net.sf.odinms.client.messages.commands;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import net.sf.odinms.client.Equip;
import net.sf.odinms.client.IItem;
import net.sf.odinms.client.Item;
import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleCharacterUtil;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.client.MapleInventory;
import net.sf.odinms.client.MapleInventoryType;
import net.sf.odinms.client.MapleJob;
import net.sf.odinms.client.MaplePet;
import net.sf.odinms.client.MapleStat;
import net.sf.odinms.constants.ServerConstants.PlayerGMRank;
import net.sf.odinms.database.DatabaseConnection;
import net.sf.odinms.net.channel.ChannelServer;
import net.sf.odinms.server.MapleInventoryManipulator;
import net.sf.odinms.server.MapleItemInformationProvider;
import net.sf.odinms.server.ShutdownServer;
import net.sf.odinms.server.life.MapleLifeFactory;
import net.sf.odinms.server.life.MapleMonster;
import net.sf.odinms.server.life.MapleNPC;
import net.sf.odinms.server.maps.MapleMapItem;
import net.sf.odinms.server.maps.MapleMapObject;
import net.sf.odinms.server.maps.MapleMapObjectType;
import net.sf.odinms.tools.MaplePacketCreator;
import net.sf.odinms.tools.performance.CPUSampler;

/**
 *
 * @author Eric
 */
public class AdminCommand {
    static boolean superBaal = false;

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.ADMIN;
    }
    
    public static boolean executeAdminCommand(MapleClient c, String[] splitted) {
        MapleCharacter player = c.getPlayer();
        final MapleCharacter playerf = player;
        MapleCharacter victim; 
        ChannelServer cserv = c.getChannelServer();
        if (c.getPlayer().getGMLevel() >= PlayerGMRank.ADMIN.getLevel()) {
            switch (splitted[0].substring(1).toLowerCase()) {
                // Start of Eric's Commands
                case "gc":
                    System.gc();
                    player.dropMessage("Free Memory = " + Runtime.getRuntime().freeMemory() + ".");
                    return true;
                case "itemvac": 
                    List<MapleMapObject> items = player.getMap().getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.ITEM));
                    for (MapleMapObject item : items) {
                        MapleMapItem mapItem = (MapleMapItem) item;
                        if (mapItem.getMeso() > 0) {
                            player.gainMeso(mapItem.getMeso(), true);
                        } else {
                            MapleInventoryManipulator.addFromDrop(c, mapItem.getItem(), "");
                        }
                        mapItem.setPickedUp(true);
                        player.getMap().removeMapObject(item); 
                        player.getMap().broadcastMessage(MaplePacketCreator.removeItemFromMap(mapItem.getObjectId(), 2, player.getId()), mapItem.getPosition());
                    }
                    return true;
                case "makepet":
                    int petid = Integer.parseInt(splitted[1]);
                    if (petid >= 5000000 && petid <= 5000005) {
                        int petId = MaplePet.createPet(petid); 
                        MapleInventoryManipulator.addById(c, petid, (short)1, player.getName(), petId); 
                    } else {
                        player.dropMessage("Item is not a pet..");
                    }
                    return true;
                case "warpallhere":
                    for (MapleCharacter chrs : ChannelServer.getInstance(c.getChannel()).getPlayerStorage().getAllCharacters()) {
                        if (!chrs.isGM() && chrs != player && chrs.getMapId() != player.getMapId()) {
                            chrs.changeMap(player.getMapId());
                        }
                    }
                    return true;
                case "iplist":
                    for (MapleCharacter chr : ChannelServer.getInstance(c.getChannel()).getPlayerStorage().getAllCharacters()){
                        if (chr == player || chr.getGMLevel() > 5) {
                            player.dropMessage(5, chr.getName() + " is an Admin, will not show IP.");
                        } else {
                            player.dropMessage(chr.getClient().getSession().getRemoteAddress() + "     - " + chr.getName() + "");
                        }
                    }
                    return true;
                case "rickroll": // big array is big (worry)
                    String[] lyrics = {"Never gonna tell a lie and hurt you", "Never gonna say goodbye", "Never gonna make you cry", "Never gonna run around and desert you", "Never gonna let you down", "Never gonna give you up", "Never gonna tell a lie and hurt you",
                    "Never gonna say goodbye", "Never gonna make you cry", "Never gonna run around and desert you", "Never gonna let you down", "Never gonna give you up", "Never gonna tell a lie and hurt you", "Never gonna say goodbye", "Never gonna make you cry", 
                    "Never gonna run around and desert you", "Never gonna let you down", "Never gonna give you up", "Gotta make you understand", "I just wanna tell you how I'm feeling", "We know the game and we're gonna play it", "Inside, we both know what's been going on", "You're too shy to say it", "Your heart's been aching, but",
                    "We've known each other for so long", "(Give you up)", "Never gonna give, never gonna give", "(Give you up)", "Never gonna give, never gonna give", "(Ooh, give you up)", "(Ooh, give you up)", "Never gonna tell a lie and hurt you", "Never gonna say goodbye", "Never gonna make you cry", "Never gonna run around and desert you", 
                    "Never gonna let you down", "Never gonna give you up", "Never gonna tell a lie and hurt you", "Never gonna say goodbye", "Never gonna make you cry", "Never gonna run around and desert you", "Never gonna let you down", "Never gonna give you up", "Don't tell me you're too blind to see", "And if you ask me how I'm feeling", 
                    "We know the game and we're gonna play it", "Inside, we both know what's been going on", "You're too shy to say it", "Your heart's been aching, but", "We've known each other for so long", "Never gonna tell a lie and hurt you", "Never gonna say goodbye", "Never gonna make you cry", "Never gonna run around and desert you", 
                    "Never gonna let you down", "Never gonna give you up", "Gotta make you understand", "I just wanna tell you how I'm feeling", "You wouldn't get this from any other guy", "A full commitment's what I'm thinking of", "You know the rules and so do I", "We're no strangers to love"}; 
                    for (String i : lyrics)
                        for (ChannelServer ch : ChannelServer.getAllInstances())
                            for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters())
                                chr.dropMessage(1, i);
                    return true;
                case "rapgod":
                    String[] rapgod = {"Look, I was gonna go easy on you not to hurt your feelings", "But I'm only going to get this one chance", "(Six minutes, six minutes)", "Something's wrong, I can feel it", "(Six minutes, six minutes, Slim Shady, you're on)", "Just a feeling I've got", "Like something's about to happen", "But I don't know what", "If that means, what I think it means, we're in trouble", "Big trouble. And if he is as bananas as you say", "I'm not taking any chances", "You were just what the doctor ordered", "...", "I'm beginning to feel like a Rap God, Rap God", "All my people from the front to the back nod, back nod", "Now who thinks their arms are long enough to slap box, slap box?", "They said I rap like a robot, so call me rap-bot", "...", "But for me to rap like a computer must be in my genes", "I got a laptop in my back pocket", 
                    "My pen'll go off when I half-cock it", "Got a fat knot from that rap profit", "Made a living and a killing off it", "Ever since Bill Clinton was still in office", "With Monica Lewinski feeling on his nutsack", "I'm an MC still as honest", "But as rude and as indecent as all hell", "Syllables, skill-a-holic (Kill 'em all with)", "This flippity, dippity-hippity hip-hop", "You don't really wanna get into a pissing match", "With this rappity-rap", "Packing a mack in the back of the Ac", "backpack rap, crap, yap-yap, yackety-yack", "and at the exact same time", "I attempt these lyrical acrobat stunts while I'm practicing that", "I'll still be able to break a motherfuckin' table", "Over the back of a couple of faggots and crack it in half", "Only realized it was ironic", "I was signed to Aftermath after the fact", 
                    "How could I not blow? All I do is drop 'F' bombs", "Feel my wrath of attack", "Rappers are having a rough time period", "Here's a Maxi-Pad", "It's actually disastrously bad", "For the wack while I'm masterfully constructing this masterpiece yeah", "...", "'Cause I'm beginning to feel like a Rap God, Rap God", "All my people from the front to the back nod, back nod", "Now who thinks their arms are long enough to slap box, slap box?", "Let me show you maintaining this shit ain't that hard, that hard", "...", "Everybody want the key and the secret to rap", "Immortality like I have got", "Well, to be truthful the blueprint's", "Simply rage and youthful exuberance", "Everybody loves to root for a nuisance", "Hit the earth like an asteroid", "and did nothing but shoot for the moon since (PPEEYOOM)", 
                    "MC's get taken to school with this music", "'Cause I use it as a vehicle to 'bus the rhyme'", "Now I lead a New School full of students", "Me? Me, I'm a product of Rakim", "Lakim Shabazz, 2Pac, N-W-A., Cube, hey, Doc, Ren", "Yella, Eazy, thank you, they got Slim", "Inspired enough to one day grow up", "Blow up and being in a position", "To meet Run-D.M.C. and induct them", "Into the motherfuckin' Rock n'", "Roll Hall of Fame even though I walk in the church", "And burst in a ball of flames", "Only Hall of Fame I'll be inducted in is the alcohol of fame", "On the wall of shame", "You fags think it's all a game", "'Til I walk a flock of flames", "Off a plank and", "Tell me what in the fuck are you thinking?", "Little gay looking boy", "So gay I can barely say it with a 'straight' face looking boy", 
                    "You're witnessing a mass-occur like you're watching a church gathering", "And take place looking boy", "Oy vey, that boy's gay", "That's all they say looking boy", "You get a thumbs up, pat on the back", "And a 'way to go' from your label every day looking boy", "Hey, looking boy, what d'you say looking boy?", "I get a 'hell yeah' from Dre looking boy", "I'mma work for everything I have", "Never asked nobody for shit", "Git out my face looking boy", "Basically boy you're never gonna be capable", "of keeping up with the same pace looking boy, 'cause", "...", "I'm beginning to feel like a Rap God, Rap God", "All my people from the front to the back nod, back nod", "The way I'm racing around the track, call me Nascar, Nascar", "Dale Earnhardt of the trailer park, the White Trash God", 
                    "Kneel before General Zod this planet's Krypton, no Asgard, Asgard", "...", "So you'll be Thor and I'll be Odin", "You rodent, I'm omnipotent", "Let off then I'm reloading", "Immediately with these bombs I'm totin'", "And I should not be woken", "I'm the walking dead", "But I'm just a talking head, a zombie floating", "But I got your mom deep throating", "I'm out my Ramen Noodle", "We have nothing in common, poodle", "I'm a Doberman, pinch yourself", "In the arm and pay homage, pupil", "It's me", "My honesty's brutal", "But it's honestly futile if I don't utilize", "What I do though for good", "At least once in a while so I wanna make sure", "Somewhere in this chicken scratch I scribble and doodle", "Enough rhymes to", "Maybe try to help get some people through tough times", "But I gotta keep a few punchlines", 
                    "Just in case 'cause even you unsigned", "Rappers are hungry looking at me like it's lunchtime", "I know there was a time where once I", "Was king of the underground", "But I still rap like I'm on my Pharoahe Monch grind", "So I crunch rhymes", "But sometimes when you combine", "Appeal with the skin color of mine", "You get too big and here they come trying to", "Censor you like that one line I said", "On 'I'm Back' from the Mathers LP", "One when I tried to say I'll take seven kids from Columbine", "Put 'em all in a line", "Add an AK-47, a revolver and a nine", "See if I get away with it now", "That I ain't as big as I was, but I'm", "Morphin' into an immortal coming through the portal", "You're stuck in a time warp from two thousand four though", "And I don't know what the fuck that you rhyme for", "You're pointless as Rapunzel", 
                    "With fucking cornrows", "You write normal, fuck being normal", "And I just bought a new ray gun from the future", "Just to come and shoot ya", "Like when Fabulous made Ray J mad", "'Cause Fab said he looked like a fag", "At Mayweather's pad singin' to a man", "While he play piano", "Man, oh man, that was the 24/7 special", "On the cable channel", "So Ray J went straight to radio station the very next day", "Hey, Fab, I'mma kill you", "Lyrics coming at you at supersonic speed, (JJ Fad)", "Uh, summa lumma dooma lumma you assuming I'm a human", "What I gotta do to get it through to you I'm superhuman", "Innovative and I'm made of rubber, so that anything you say is", "Ricochet in off a me and it'll glue to you", "And I'm devastating more than ever demonstrating", "How to give a motherfuckin' audience a feeling like it's levitating", 
                    "Never fading, and I know that haters are forever waiting", "For the day that they can say I fell off, they'll be celebrating", "'Cause I know the way to get 'em motivated", "I make elevating music", "You make elevator music", "Oh, he's too mainstream.", "Well, that's what they do", "When they get jealous, they confuse it", "It's not hip hop, it's pop.", "'Cause I found a hella way to fuse it", "With rock, shock rap with Doc", "Throw on 'Lose Yourself' and make 'em lose it", "I don't know how to make songs like that", "I don't know what words to use", "Let me know when it occurs to you", "While I'm ripping any one of these verses that versus you", "It's curtains, I'm inadvertently hurtin' you", "How many verses I gotta murder to", "Prove that if you were half as nice,", "your songs you could sacrifice virgins to", "Unghh, school flunky, pill junky", 
                    "But look at the accolades these skills brung me", "Full of myself, but still hungry", "I bully myself 'cause I make me do what I put my mind to", "When I'm a million leagues above you", "Ill when I speak in tongues", "But it's still tongue-and-cheek, fuck you", "I'm drunk so Satan take the fucking wheel", "I'm asleep in the front seat", "Bumping Heavy D and the Boys", "Still chunky, but funky", "But in my head there's something", "I can feel tugging and struggling", "Angels fight with devils and", "Here's what they want from me", "They're asking me to eliminate some of the women hate", "But if you take into consideration the bitter hatred I had", "Then you may be a little patient and more sympathetic to the situation", "And understand the discrimination", "But fuck it", "Life's handing you lemons", "Make lemonade then", 
                    "But if I can't batter the women", "How the fuck am I supposed to bake them a cake then?", "Don't mistake him for Satan", "It's a fatal mistake if you think I need to be overseas", "And take a vacation to trip a broad", "And make her fall on her face and", "Don't be a retard, be a king?", "Think not", "Why be a king when you can be a God?"};
                    for (String i : rapgod)
                        for (ChannelServer ch : ChannelServer.getAllInstances())
                            for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters())
                                chr.dropMessage(1, i);
                    return true;
                case "killeveryone":
                    for (MapleCharacter mch : cserv.getPlayerStorage().getAllCharacters())
                        if (mch.getGMLevel() > 6) {
                            player.dropMessage(6, "[Error]: " + mch.getName() + " is an Owner, he will not be killed!");
                        } else {
                            if (mch != null) {
                                mch.setHp(0);
                                mch.setMp(0);
                                mch.updateSingleStat(MapleStat.HP, 0);
                                mch.updateSingleStat(MapleStat.MP, 0);
                                mch.dropMessage(6, "BOOM HEADSHOT!");
                            }
                        }
                    return true;
                case "saveall":
                    for (ChannelServer ch : ChannelServer.getAllInstances())
                        for (MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                            chr.dropMessage(6, "Development is saving all users, please wait..");
                            chr.saveToDB(true);
                            chr.dropMessage("Save Completed!");
                        }
                    return true;
                case "jobperson":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    //if (MapleCarnivalChallenge.getJobNameById(Integer.parseInt(splitted[2])).length() == 0) { //wut?
                    //    player.dropMessage(5, "Invalid Job!");
                    //    return true;
                    //}
                    victim.changeJob(MapleJob.getById(Integer.parseInt(splitted[2])));
                    return true;
                case "levelperson":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    victim.setLevel((short)(Short.parseShort(splitted[2])-1));
                    victim.levelUp();
                    victim.levelUp();
                    if (victim.getExp() < 0) {
                        victim.gainExp(-victim.getExp(), false, false, true);
                    }
                    return true;
                case "startprofiling":
                    CPUSampler sampler = CPUSampler.getInstance();
                    sampler.addIncluded("client");
                    sampler.addIncluded("constants"); //or should we do Packages.constants etc.?
                    sampler.addIncluded("database");
                    sampler.addIncluded("handling");
                    sampler.addIncluded("provider");
                    sampler.addIncluded("scripting");
                    sampler.addIncluded("server");
                    sampler.addIncluded("tools");
                    sampler.start();
                    return true;
                case "setgmlevel":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    int gmlevel = (byte)Integer.parseInt(splitted[2]);
                    victim.setGMLevel(gmlevel);
                    player.dropMessage(5, "Done.");
                    return true;
                case "getmap":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    player.dropMessage(5, victim.getName() + " is at " + victim.getMap().getMapName() + " (Map " + victim.getMapId() + ")");
                    return true;
                case "checkplayers":
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    int playerzzz = victim.getMap().getCharacters().size();
                    StringBuilder builder = new StringBuilder("Names of players on " + victim.getName() + "'s map: ").append(victim.getMap().getCharacters().size()).append(", ");
                    for (MapleCharacter chr2 : victim.getMap().getCharacters()) {
                        if (builder.length() > 150) { // wild guess :o
                            builder.setLength(builder.length() - 2);
                            c.getPlayer().dropMessage(6, builder.toString());
                            builder = new StringBuilder();
                        }
                        builder.append(MapleCharacterUtil.makeMapleReadable(chr2.getName()));
                        builder.append(", ");
                    }
                    builder.setLength(builder.length() - 2);
                    c.getPlayer().dropMessage(5, "There are " + playerzzz + " players on " + victim.getName() + "'s map.");
                    c.getPlayer().dropMessage(5, builder.toString());
                    return true;
                case "setname":
                    if (splitted.length != 3) {
                    }
                    victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                    String newname = splitted[2];
                    if (splitted.length == 3) {
                        if (MapleCharacter.getIdByName(newname, 0) == -1) {
                            if (victim != null) {
                                victim.setName(newname);
                                victim.saveToDB(true);
                                victim.getClient().disconnect(); 
                                player.dropMessage(splitted[1] + " is now named " + newname + "");
                            } else {
                                player.dropMessage("The player " + splitted[1] + " is either offline or not in this channel");
                                return true;
                            }
                        } else {
                            player.dropMessage("Character name in use.");
                            return true;
                        }
                    } else {
                        player.dropMessage("Incorrect syntax !");
                        return true;
                    }
                    return true;
                case "stopprofiling":
                    CPUSampler sampler2 = CPUSampler.getInstance();
                    try {
                        String filename = "odinprofile.txt";
                        if (splitted.length > 1) {
                            filename = splitted[1];
                        }
                        File file = new File(filename);
                        if (file.exists()) {
                            c.getPlayer().dropMessage(6, "The entered filename already exists, choose a different one");
                            return true;
                        }
                        sampler2.stop();
                        FileWriter fw = new FileWriter(file);
                        sampler2.save(fw, 1, 10);
                        fw.close();
                    } catch (IOException e) {
                        System.err.println("Error saving profile" + e);
                    }
                    sampler2.reset();
                    return true;
                case "makemsi":
                    try {
                        int itemid = Integer.parseInt(splitted[1]);
                        Equip equip = (Equip) MapleItemInformationProvider.getInstance().getEquipById(itemid);
                        if (equip == null) {
                            c.getPlayer().dropMessage("Item does not exist.");
                        } else {
                            equip.makeMSI(c.getPlayer().getName());
                            MapleInventoryManipulator.addbyItem(c, (Item) equip);
                            c.getPlayer().dropMessage("You just got a " + MapleItemInformationProvider.getInstance().getName(itemid) + "!");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    return true;
                case "stripeveryone":
                    ChannelServer cs = c.getChannelServer();
                    for (MapleCharacter mchr : cs.getPlayerStorage().getAllCharacters()) {
                        if (mchr.isGM()) {
                            continue;
                        }
                        MapleInventory equipped = mchr.getInventory(MapleInventoryType.EQUIPPED);
                        MapleInventory equip = mchr.getInventory(MapleInventoryType.EQUIP);
                        List<Byte> ids = new ArrayList<Byte>();
                        for (IItem item : equipped.list()) {
                            ids.add((byte)item.getPosition());
                        }
                        for (byte id : ids) {
                            MapleInventoryManipulator.unequip(mchr.getClient(), id, equip.getNextFreeSlot());
                        }
                    }
                    return true;
                case "stripmap":
                    for (MapleCharacter victims : player.getMap().getCharacters()) {
                        if (!victims.isGM()) {
                            victims.unequipEverything();
                        } else {
                            player.dropMessage("[Error]: " + victims.getName() + " is a GM and won't be stripped.");
                        }
                    }
                    return true;
                case "pnpc":
                    if (splitted.length < 1) {
                        c.getPlayer().dropMessage(6, "!pnpc <npcid>");
                        return true;
                    }
                    int npcId = Integer.parseInt(splitted[1]);
                    MapleNPC npc = MapleLifeFactory.getNPC(npcId);
                    if (npc != null && !npc.getName().equals("MISSINGNO")) {
                        final int xpos = c.getPlayer().getPosition().x;
                        final int ypos = c.getPlayer().getPosition().y;
                        final int fh = c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId();
                        npc.setPosition(c.getPlayer().getPosition());
                        npc.setCy(ypos);
                        npc.setRx0(xpos);
                        npc.setRx1(xpos);
                        npc.setFh(fh);
                        npc.setCustom(true);
                        try {
                            Connection con = DatabaseConnection.getConnection();
                            try(PreparedStatement ps = con.prepareStatement("INSERT INTO wz_customlife ( idd, f, fh, type, cy, rx0, rx1, x, y, mid ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )")) {
                                ps.setInt(1, npcId);
                                ps.setInt(2, 0); // 1 = hide, 0 = show
                                ps.setInt(3, fh);
                                ps.setString(4, "n");
                                ps.setInt(5, ypos);
                                ps.setInt(6, xpos);
                                ps.setInt(7, xpos);
                                ps.setInt(8, xpos);
                                ps.setInt(9, ypos);
                                ps.setInt(10, c.getPlayer().getMapId());
                                ps.executeUpdate();
                            }
                        } catch (SQLException e) {
                            c.getPlayer().dropMessage(6, "Failed to save NPC to the database");
                            return true;
                        }
                        c.getPlayer().getMap().addMapObject(npc);
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.spawnNPC(npc));
                        c.getPlayer().getMap().broadcastMessage(MaplePacketCreator.spawnNPCRequest(npc));
                        c.getPlayer().dropMessage(6, "Please do not reload this map or else the NPC will disappear till the next restart.");
                    } else {
                        c.getPlayer().dropMessage(6, "You have entered an invalid Npc-Id");
                        return true;
                    }
                    return true;
                case "pmob1":
                    if (splitted.length < 2) {
                        c.getPlayer().dropMessage(6, "!pmob1 <mobid> <mobTime>");
                        return true;
                    }
                    int mobid = Integer.parseInt(splitted[1]);
                    int mobTime = Integer.parseInt(splitted[2]);
                    MapleMonster pmob;
                    try {
                        pmob = MapleLifeFactory.getMonster(mobid);
                    } catch (RuntimeException e) {
                        c.getPlayer().dropMessage(5, "Error: " + e.getMessage());
                        return true;
                    }
                    if (pmob != null) {
                        final int xpos = c.getPlayer().getPosition().x;
                        final int ypos = c.getPlayer().getPosition().y;
                        final int fh = c.getPlayer().getMap().getFootholds().findBelow(c.getPlayer().getPosition()).getId();
                        pmob.setPosition(c.getPlayer().getPosition());
                        pmob.setCy(ypos);
                        pmob.setRx0(xpos);
                        pmob.setRx1(xpos);
                        pmob.setFh(fh);
                        try {
                            Connection con = DatabaseConnection.getConnection();
                            try (PreparedStatement ps = con.prepareStatement("INSERT INTO wz_customlife (idd, f, fh, cy, type, rx0, rx1, x, y, mid, mobtime) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                                ps.setInt(1, mobid);
                                ps.setInt(2, 0); // 1 = right , 0 = left
                                ps.setInt(3, fh);
                                ps.setInt(4, ypos);
                                ps.setInt(5, xpos);
                                ps.setInt(6, xpos);
                                ps.setString(7, "m");
                                ps.setInt(8, xpos);
                                ps.setInt(9, ypos);
                                ps.setInt(10, c.getPlayer().getMapId());
                                ps.setInt(11, mobTime);
                                ps.executeUpdate();
                            }
                        } catch (SQLException e) {
                            c.getPlayer().dropMessage(6, "Failed to save NPC to the database");
                        }
                        c.getPlayer().getMap().addMonsterSpawn(pmob, mobTime);
                        c.getPlayer().dropMessage(6, "Please do not reload this map or else the MOB will disappear till the next restart.");
                    } else {
                        c.getPlayer().dropMessage(6, "You have entered an invalid Mob-Id");
                        return true;
                    }
                return true;
                case "pmob":
                   npcId = Integer.parseInt(splitted[1]);
                   int monsterId;
                   mobTime = Integer.parseInt(splitted[2]);
                   int xpos = player.getPosition().x;
                   int ypos = player.getPosition().y;
                   int fh = player.getMap().getFootholds().findBelow(player.getPosition()).getId();
                   if (splitted[2] == null) {
                       mobTime = 0;
                   }
                   MapleMonster mob = MapleLifeFactory.getMonster(npcId);
                   if (mob != null && !mob.getName().equalsIgnoreCase("MISSINGNO")) {
                       mob.setPosition(player.getPosition());
                       mob.setCy(ypos);
                       mob.setRx0(xpos + 50);
                       mob.setRx1(xpos - 50);
                       mob.setFh(fh);
                       try {
                           Connection con = DatabaseConnection.getConnection();
                           PreparedStatement ps = con.prepareStatement("INSERT INTO wz_customlife ( idd, f, fh, type, cy, rx0, rx1, x, y, mobtime, mid ) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                           ps.setInt(1, npcId);
                           ps.setInt(2, 0);
                           ps.setInt(3, fh);
                           ps.setString(4, "m");
                           ps.setInt(5, ypos);
                           ps.setInt(6, xpos + 50);
                           ps.setInt(7, xpos - 50);
                           ps.setInt(8, xpos);
                           ps.setInt(9, ypos);
                           ps.setInt(10, mobTime);
                           ps.setInt(11, player.getMapId());
                           ps.executeUpdate();
                       } catch (SQLException e) {
                           player.dropMessage("Failed to save MOB to the database");
                       }
                       player.getMap().addMonsterSpawn(mob, mobTime);
                   } else {
                       player.dropMessage("You have entered an invalid Npc-Id");
                   }
                    return true;
                case "shutdown":
                case "shutdownworld":
                    int time = 60000;
			if (splitted.length > 1) {
                            time = Integer.parseInt(splitted[1]) * 60000;
			}
			if (splitted[0].equalsIgnoreCase("shutdown")) 
                            c.getChannelServer().shutdown(time); 
                        else 
                            c.getChannelServer().shutdownWorld(time);
                    return true;
                case "shutdownnow":
                    new ShutdownServer(c.getChannel()).run();
                    return true;
                default:
                    if (c.getPlayer().getGMLevel() >= 100) {
                        return GodCommand.executeGodCommand(c, splitted);
                    } else {
                        c.getPlayer().dropMessage(5, splitted[0].substring(1) + " does not exist.");
                        return false;
                    }
            }
        } else {
            c.getPlayer().dropMessage(5, "LOL Did you really just type an Owner command? I CALL H@CK$.!");
            return true;
        }
    }
    
    public static String now(String dateFormat) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        return sdf.format(cal.getTime());
    }
}
