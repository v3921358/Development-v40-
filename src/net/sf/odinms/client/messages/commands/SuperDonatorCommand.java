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
 */
public class SuperDonatorCommand {
    static boolean usedCommandSDonator;

    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.SUPERDONATOR;
    }

    public static boolean executeSuperDonatorCommand (MapleClient c, String[] splitted) {
        MapleCharacter player = c.getPlayer();
        if (!player.isGM() && ServerConstants.isJail(c.getPlayer().getMapId())) {
            c.getPlayer().dropMessage(1, "You may not use commands in this map.");
            return true;
        }
        if (c.getPlayer().getGMLevel() >= 2) {
             if (player.getGMLevel() < 3 && usedCommandSDonator == false) {
//                    FileoutputUtil.log("DonorLog.txt", "\r\nIGN: " + player.getName() + " || Command: " + InternCommand.joinStringFrom(splitted, 0) + " \r\n");
                    usedCommandSDonator = true;
                    TimerManager.getInstance().schedule(new Runnable() {
                        @Override
                        public void run() {
                           usedCommandSDonator = false;  
                        }
                    }, 10);
                }
            switch (splitted[0].substring(1).toLowerCase()) {
                case "null":
                case "fixme":
                    c.getSession().write(MaplePacketCreator.enableActions());
                    return true;
                default:
                    if (c.getPlayer().getGMLevel() >= 3) {
                        return InternCommand.executeInternCommand(c, splitted);
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