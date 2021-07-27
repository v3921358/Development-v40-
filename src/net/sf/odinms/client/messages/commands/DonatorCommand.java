package net.sf.odinms.client.messages.commands;

import net.sf.odinms.client.MapleCharacter;
import net.sf.odinms.client.MapleClient;
import net.sf.odinms.constants.ServerConstants;
import net.sf.odinms.constants.ServerConstants.PlayerGMRank;
import net.sf.odinms.server.TimerManager;
import net.sf.odinms.tools.MaplePacketCreator;

/**
 *
 * @author Eric
 * @rev: 2.7 - Added a new !tickle power command.
 * 
 */
public class DonatorCommand {
    static boolean usedCommandDonator;

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.DONATOR;
    }
    
    public static boolean executeDonatorCommand(MapleClient c, String[] splitted) {
        MapleCharacter player = c.getPlayer();
        if (!player.isGM() && ServerConstants.isJail(c.getPlayer().getMapId())) {
            c.getPlayer().dropMessage(1, "You may not use commands in this map.");
            return true;
        }
        if (!player.isGM() && c.getPlayer().getMapId() == 0) { // && c.getPlayer().inJQ()) {
            switch (splitted[0].substring(1).toLowerCase()) {
                case "exit":
                    if (ServerConstants.isJail(c.getPlayer().getMapId())) {
                        player.dropMessage(6, "Nice try. :)");
                        return true;
                    }
                    player.changeMap(100000000); // should i give a choice fm/henesys?
                    return true;
                default: 
                    player.dropMessage(-1, "You can't use @commands during a Jump Quest. To exit the Jump Quest, type @exit.");
                    return false;
            }
        }
        if (player.getGMLevel() >= 1) {
             if (player.getGMLevel() < 3 && usedCommandDonator == false) {
//                    FileoutputUtil.log("DonorLog.txt", "\r\nIGN: " + player.getName() + " || Command: " + InternCommand.joinStringFrom(splitted, 0) + " \r\n");
                    usedCommandDonator = true;
                    TimerManager.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                           usedCommandDonator = false;  
                        }
                    }, 10);
                }
            switch (splitted[0].substring(1).toLowerCase()) {
                case "donor":
                    player.changeMap(100000000);
                    player.dropMessage(5, "Welcome to Donor Island!");
                    return true;
                default:
                    if (c.getPlayer().getGMLevel() >= 2) {
                        return SuperDonatorCommand.executeSuperDonatorCommand(c, splitted);
                    } else {
                        c.getPlayer().dropMessage(5, splitted[0].substring(1) + " does not exist.");
                        return false;
                    }
            }
        } else {
            return true;
        }
    }
}
